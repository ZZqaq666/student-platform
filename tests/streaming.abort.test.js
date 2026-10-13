// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { streamingService } from '../src/api/streaming';

/**
 * 流式请求必须可中断：切换教材或离开页面时旧流不能继续回写。
 * 这里用可控的 AbortSignal 假 fetch 验证 signal 真的透传、
 * 且主动 abort 不会被当成故障上报给 onError。
 */

function makeChunkedResponse(sseFrames) {
  const encoder = new TextEncoder();
  let index = 0;
  return {
    ok: true,
    status: 200,
    body: {
      getReader: () => ({
        read: () => (index < sseFrames.length
          ? Promise.resolve({ done: false, value: encoder.encode(sseFrames[index++]) })
          : Promise.resolve({ done: true }))
      })
    }
  };
}

describe('streamingService 可中断流式请求', () => {
  beforeEach(() => {
    localStorage.setItem('token', 'test-token');
    vi.restoreAllMocks();
  });

  it('streamPost 把 signal 透传给 fetch，且未 abort 时正常收流', async () => {
    const controller = new AbortController();
    const frames = ['data: {"content":"片段1"}\n\n', 'data: {"content":"片段2","completed":true}\n\n'];
    const seen = [];
    let completed = false;

    const fetchMock = vi.fn(async (url, options) => {
      expect(options.signal).toBe(controller.signal);
      return makeChunkedResponse(frames);
    });
    global.fetch = fetchMock;

    await streamingService.streamPost(
      '/qa/ask/stream', { question: 'q' },
      (data) => seen.push(data.content),
      () => { completed = true; },
      () => {},
      { signal: controller.signal }
    );

    expect(seen).toEqual(['片段1', '片段2']);
    expect(completed).toBe(true);
  });

  it('abort 之后停止投递数据，并按 onAbort 而非 onError 收口', async () => {
    const controller = new AbortController();
    const onData = vi.fn();
    const onError = vi.fn();
    const onAbort = vi.fn();

    // 模拟浏览器行为：signal 触发时，fetch 与 in-flight 的 read() 都以 AbortError 拒绝
    global.fetch = (url, options) => {
      const signal = options.signal;
      const abortError = () => new DOMException('The user aborted a request.', 'AbortError');

      return new Promise((resolve, reject) => {
        signal.addEventListener('abort', () => reject(abortError()));
        resolve({
          ok: true,
          body: {
            getReader: () => ({
              read: () => new Promise((res, rej) => {
                if (signal.aborted) rej(abortError());
                else signal.addEventListener('abort', () => rej(abortError()));
              })
            })
          }
        });
      });
    };

    const pending = streamingService.streamPost(
      '/qa/ask/stream', { question: 'q' },
      onData, () => {}, onError,
      { signal: controller.signal, onAbort }
    );

    controller.abort();
    await pending;

    expect(onAbort).toHaveBeenCalledTimes(1);
    expect(onError).not.toHaveBeenCalled();
    expect(onData).not.toHaveBeenCalled();
  });

  it('真实故障仍然走 onError（不被 abort 分支吞掉）', async () => {
    const controller = new AbortController();
    const onError = vi.fn();
    const onAbort = vi.fn();

    global.fetch = async () => {
      throw new TypeError('network down');
    };

    await streamingService.streamPost(
      '/qa/ask/stream', { question: 'q' },
      () => {}, () => {}, onError,
      { signal: controller.signal, onAbort }
    );

    expect(onError).toHaveBeenCalledTimes(1);
    expect(onAbort).not.toHaveBeenCalled();
  });

  it('streamFormData 同样接受 signal', async () => {
    const controller = new AbortController();
    let capturedSignal = null;

    global.fetch = async (url, options) => {
      capturedSignal = options.signal;
      return makeChunkedResponse(['data: {"content":"ok","completed":true}\n\n']);
    };

    await streamingService.streamFormData(
      '/qa/ask/image/stream', new FormData(),
      () => {}, () => {}, () => {},
      { signal: controller.signal }
    );

    expect(capturedSignal).toBe(controller.signal);
  });
});
