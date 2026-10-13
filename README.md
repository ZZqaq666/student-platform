# ScholarPal 前端

Vue 3.5 + Vite 6 + Element Plus + Pinia，约 55 个源文件 / 1.8 万行。
纯 JavaScript（无 TypeScript），无 SSR。

## 本地运行

```bash
npm ci
npm run dev        # http://localhost:5173
npm run build      # 生产构建
npm test           # vitest 单元测试
```

`vite.config.js` 里 `/api` 代理到 `http://localhost:8080`，所以**开发时请求是同源的**，
不需要后端开 CORS 白名单；跨源部署才需要设置后端的 `APP_CORS_ALLOWED_ORIGINS`。

需要先起后端，且后端要有 MySQL 和 Redis（见后端 README）。

## 目录结构

按功能而非类型组织：`views/<模块>` 内部自带 `components/`、`composables/`、`utils/`。

| 目录 | 内容 |
|---|---|
| `src/api/` | `api.js` axios 实例 + 缓存 + 静默续期；`streaming.js` 流式请求（可中断） |
| `src/stores/auth.js` | 唯一的 Pinia store：token / refreshToken / userRole 落 localStorage |
| `src/views/Auth/` | 登录、注册 |
| `src/views/Qa/` | 问答中心（文本 + 图片，流式打字机） |
| `src/views/Bookshelf/` | 书架；已从单文件 8 千行拆成 10 组件 + 10 composables |
| `src/views/WrongBook/`、`Practice/` | 错题本、练习 |
| `src/views/KnowledgeGraph/` | ECharts 知识图谱 |
| `src/views/Senior/` | 学长答疑 |
| `src/components/`、`src/composables/` | **空占位目录**，项目内零引用；共享代码请放 `src/utils` |

## 认证与令牌

访问令牌只有 15 分钟，由 `api.js` 的响应拦截器静默续期：收到 401 → 用 refreshToken
调 `/auth/refresh` → 写回新令牌 → 重放原请求；并发请求共用一次刷新。续期失败才清凭证
跳登录页。因此不要在请求拦截器里做本地过期预判——那会抢在 401 之前把用户踢下线。

登出必须调 `/auth/logout`（`MainLayout` 已接），它会把访问令牌与刷新令牌一起加入服务端
吊销名单；只清 localStorage 是无效的登出。

## 两条硬约定

**1. 富文本出口必须先消毒再渲染。** `src/utils/markdown/` 下有多套渲染器，
`markdown-it` 实例配的是 `html: true` + `linkify: true`，所以载荷在入口还是纯
Markdown（例如 `[x](javascript:alert(1))`），入口消毒拦不住——**DOMPurify 必须作用在
`md.render()` 的输出上**。`v-html` 的每一处都要能追溯到一次出口消毒。

**2. mock 兜底只允许在开发环境。** 统一由 `src/utils/mockFallback.js` 的
`MOCK_FALLBACK_ENABLED`（即 `import.meta.env.DEV`）控制。历史上多处代码在接口失败、
甚至接口成功但返回空列表时塞假数据（假章节树、假笔记、假习题、假知识图谱），使后端故障
对用户完全不可见。生产环境一律走空状态 + 明确报错。

CI 里有一条断言会把这三类假数据"摇不进生产包"变成回归门禁，本地可这样自查：

```bash
npx vite build
grep -r "华东师范大学数学系\|第一章 高等数学基础\|不定积分的概念" dist/assets || echo "已摇出，OK"
```

## 测试

`tests/` 下是真实用例（不是 `--passWithNoTests` 那种空通过）。当前覆盖流式请求的可
中断性：`signal` 透传、主动 abort 不误报故障、真实故障仍上报。新增涉及异步/生命周期的
逻辑请配套补，`.github/workflows/ci.yml` 会跑 `vitest run` 与 `vite build`。

## 已知问题

- 首页的等级 / EXP / 连续天数 / 徽章是 `Home.vue` 里的初值占位（`ref(420)` 等），
  **没有任何接口写入**，不是真实学习统计。
- 生产包约 4.6 MB，其中 `vendor-markdown` 单块 1.36 MB：`utils/markdown/aiResponse.js`
  全量引入了 highlight.js 语言包，而 `views/Bookshelf/utils/markdown.js` 已正确使用
  `lib/core` + 按需语言。两处 Markdown 渲染实现重复，宜收敛。
- `package-lock.json` 与 `pnpm-lock.yaml` 同时存在且已漂移，CI 固定用 npm，
  pnpm 锁文件待清理。
- 无 ESLint / Prettier / TypeScript。
