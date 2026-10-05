/* 学生学科智能问答平台 - CDP 全页面联调验证 */
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const FRONT = 'http://127.0.0.1:5173';
const SHOT_DIR = path.join(__dirname, 'screenshots');
if (!fs.existsSync(SHOT_DIR)) fs.mkdirSync(SHOT_DIR, { recursive: true });

const CHROME_CANDIDATES = [
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
];
const PORT = 9777;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function findChrome() {
  for (const p of CHROME_CANDIDATES) if (fs.existsSync(p)) return p;
  throw new Error('no chrome/edge found');
}

async function main() {
  const exe = await findChrome();
  const profile = path.join(require('os').tmpdir(), 'cdp-sp-' + Date.now());
  const chrome = spawn(exe, [
    '--headless=new', '--no-sandbox', '--disable-gpu', '--hide-scrollbars',
    '--window-size=1440,1000', '--remote-debugging-port=' + PORT,
    '--user-data-dir=' + profile, 'about:blank',
  ], { stdio: 'ignore' });

  let ws = null;
  let msgId = 0;
  const pending = new Map();
  const net = [];
  const reqMethod = new Map();
  const results = [];
  const ok = (name, pass, extra = '') => {
    results.push((pass ? 'OK   ' : '!FAIL ') + name + (extra ? ' | ' + extra : ''));
    console.log((pass ? 'OK   ' : '!FAIL ') + name + (extra ? ' | ' + extra : ''));
  };

  try {
    // 轮询 CDP 就绪
    let target = null;
    for (let i = 0; i < 30; i++) {
      try {
        const list = await fetch('http://127.0.0.1:' + PORT + '/json/list').then((r) => r.json());
        target = list.find((t) => t.type === 'page');
        if (target) break;
      } catch (e) {}
      await sleep(400);
    }
    if (!target) throw new Error('CDP not ready');
    ws = new WebSocket(target.webSocketDebuggerUrl);
    await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
    ws.onmessage = (e) => {
      const m = JSON.parse(e.data);
      if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); }
      if (m.method === 'Network.requestWillBeSent' && m.params.request.url.includes('/api/'))
        reqMethod.set(m.params.requestId, m.params.request.method + ' ' + m.params.request.url.replace(FRONT, ''));
      if (m.method === 'Network.responseReceived' && m.params.response.url.includes('/api/')) {
        const key = reqMethod.get(m.params.requestId) || '? ' + m.params.response.url;
        net.push(m.params.response.status + ' ' + key);
      }
      if (m.method === 'Runtime.exceptionThrown')
        console.log('EXC ' + (m.params.exceptionDetails.exception?.description || '').slice(0, 150));
    };
    const send = (method, params = {}) => new Promise((res) => {
      const id = ++msgId;
      pending.set(id, res);
      ws.send(JSON.stringify({ id, method, params }));
    });
    const evaluate = async (expr) => {
      const r = await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true });
      if (r.result?.exceptionDetails) {
        console.log('EVAL-EXC ' + (r.result.exceptionDetails.exception?.description || '').slice(0, 200));
        return null;
      }
      return r.result?.result?.value;
    };

    await send('Runtime.enable');
    await send('Page.enable');
    await send('Network.enable');
    await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false });

    const shot = async (name) => {
      await sleep(350);
      const r = await send('Page.captureScreenshot', { format: 'png' });
      const buf = Buffer.from(r.result.data, 'base64');
      const file = path.join(SHOT_DIR, name + '.png');
      fs.writeFileSync(file, buf);
      console.log('SHOT ' + name + ' ' + buf.length + 'B md5=' + crypto.createHash('md5').update(buf).digest('hex').slice(0, 8));
      return file;
    };

    const waitClickable = async (sel, timeout = 25000) => {
      const t0 = Date.now();
      while (Date.now() - t0 < timeout) {
        const r = await evaluate(`(() => {
          const el = document.querySelector(${JSON.stringify(sel)});
          if (!el) return null;
          const b = el.getBoundingClientRect();
          if (!b.width || !b.height) return null;
          const h = document.elementFromPoint(Math.round(b.left + b.width / 2), Math.round(b.top + b.height / 2));
          if (!h || !(el === h || el.contains(h) || h.contains(el))) return null;
          return { x: Math.round(b.left + b.width / 2), y: Math.round(b.top + b.height / 2) };
        })()`);
        if (r) return r;
        await sleep(150);
      }
      return null;
    };

    // ===== 1. 登录页 =====
    await send('Page.navigate', { url: FRONT + '/login' });
    await sleep(2500);
    const loginText = await evaluate('document.body.innerText.slice(0, 300)');
    ok('登录页加载', !!loginText && loginText.length > 10, 'textLen=' + (loginText || '').length);
    await shot('01-login');

    // 填写登录表单
    const fill = await evaluate(`(() => {
      const set = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
      const inputs = [...document.querySelectorAll('input')];
      const u = inputs.find(i => /user|name|account|账号/i.test(i.placeholder || '') || i.type === 'text');
      const p = inputs.find(i => i.type === 'password');
      if (!u || !p) return { ok: false, inputs: inputs.map(i => i.type + '|' + i.placeholder) };
      set.call(u, 'testuser01'); u.dispatchEvent(new Event('input', { bubbles: true }));
      set.call(p, 'Test123456'); p.dispatchEvent(new Event('input', { bubbles: true }));
      return { ok: true };
    })()`);
    ok('登录表单填写', fill && fill.ok, JSON.stringify(fill).slice(0, 120));

    const submit = await waitClickable('.login-btn, .el-button--primary', 15000);
    ok('登录按钮可点', !!submit, JSON.stringify(submit));
    if (submit) {
      await send('Input.dispatchMouseEvent', { type: 'mousePressed', x: submit.x, y: submit.y, button: 'left', clickCount: 1 });
      await send('Input.dispatchMouseEvent', { type: 'mouseReleased', x: submit.x, y: submit.y, button: 'left', clickCount: 1 });
    }
    // 等待跳转
    let landed = '';
    for (let i = 0; i < 25; i++) {
      await sleep(400);
      landed = await evaluate('location.pathname') || '';
      if (landed === '/' || (landed !== '/login' && landed !== '')) break;
    }
    ok('登录跳转', landed === '/', 'landed=' + landed);

    // ===== 2. 首页 =====
    await sleep(1800);
    await shot('02-home');

    // ===== 3. 遍历主页面 =====
    const pages = [
      ['qa', '03-qa', '.qa-page, .chat-container, [class*=qa], main, #app'],
      ['bookshelf', '04-bookshelf', '[class*=shelf], [class*=book], main, #app'],
      ['practice', '05-practice', '[class*=practice], main, #app'],
      ['wrong-book', '06-wrong-book', '[class*=wrong], .el-table, main, #app'],
      ['senior', '07-senior', '[class*=senior], main, #app'],
      ['graph', '08-graph', 'canvas, [class*=graph], main, #app'],
    ];
    for (const [route, shotName, sel] of pages) {
      net.length = 0; reqMethod.clear();
      await send('Page.navigate', { url: FRONT + '/' + route });
      await sleep(3200);
      const info = await evaluate(`(() => {
        const el = document.querySelector(${JSON.stringify(sel)});
        return {
          path: location.pathname,
          textLen: (document.body.innerText || '').length,
          hasCanvas: !!document.querySelector('canvas'),
          tableRows: document.querySelectorAll('.el-table__row').length,
        };
      })()`);
      ok('页面 ' + route, info && info.path === '/' + route && info.textLen > 30,
        'path=' + (info ? info.path : '?') + ' textLen=' + (info ? info.textLen : 0) + ' rows=' + (info ? info.tableRows : 0));
      await shot(shotName);
      const errs = net.filter((n) => /^[45]/.test(n));
      if (errs.length) console.log('NET-ERR ' + route + ': ' + errs.slice(0, 5).join(' ; '));
    }

    // ===== 4. 登录态接口网络审计（wrong-book 页的请求） =====
    console.log('--- API 审计汇总（最后页面） ---');
    net.slice(0, 10).forEach((n) => console.log('  ' + n));

    console.log('===== SUMMARY =====');
    const fails = results.filter((r) => r.startsWith('!FAIL'));
    console.log('TOTAL=' + results.length + ' FAIL=' + fails.length);
  } finally {
    try { ws && ws.close(); } catch (e) {}
    try { chrome.kill(); } catch (e) {}
    try { fs.rmSync(profile, { recursive: true, force: true }); } catch (e) {}
  }
}

main().catch((e) => { console.error('FATAL ' + e.message); process.exit(1); });
