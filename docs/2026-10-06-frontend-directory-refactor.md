# 前端目录结构重构说明（2026-10-06）

## 一、重构后完整目录树

```
student-platform/
├── docs/                            # 项目文档（含 SQL 种子）
│   └── sql/
│       └── knowledge_graph.sql      # ← 原 src/resources/knowledge_graph.sql
├── public/
│   └── vite.svg
├── src/
│   ├── api/                         # 请求层：唯一出入口
│   │   ├── api.js                   # axios 实例 + apiService（唯一请求层）
│   │   └── streaming.js             # SSE 流式请求（问答场景专用）
│   ├── assets/                      # 静态资源
│   │   └── images/
│   │       └── auth-bg.png          # ← 原 src/assets/智能云.png
│   ├── components/                  # 【公共组件区】跨业务域复用组件（准入规则见下）
│   ├── composables/                 # 【公共组合式函数区】useXxx.js
│   ├── layouts/                     # 布局组件（复数约定）
│   │   └── MainLayout.vue           # ← 原 src/layout/
│   ├── router/
│   │   └── index.js                 # 路由表 + 登录守卫
│   ├── stores/                      # Pinia 状态管理（官方目录约定）
│   │   └── auth.js                  # ← 原 src/store/
│   ├── styles/
│   │   ├── tokens.css               # 设计令牌
│   │   └── global.css               # 全局样式 + Element Plus 变量对齐
│   ├── utils/                       # 工具函数（按域归组）
│   │   └── markdown/
│   │       ├── aiResponse.js        # ← 原 markdownUtils.js（AI 回复深度处理）
│   │       └── renderer.js          # ← 原 markdownRenderer.js（通用渲染）
│   ├── views/                       # 页面级视图（按业务域，PascalCase）
│   │   ├── Auth/                    #   Login.vue / Register.vue
│   │   ├── Bookshelf/               #   Bookshelf.vue
│   │   ├── Home/                    #   Home.vue
│   │   ├── KnowledgeGraph/          #   KnowledgeGraph.vue + utils/（feature-local）
│   │   ├── Practice/                #   PracticePage.vue
│   │   ├── Qa/                      #   QA.vue
│   │   ├── Senior/                  #   SeniorQA.vue + components/ + composables/
│   │   └── WrongBook/               #   WrongBook.vue + components/
│   ├── App.vue
│   └── main.js
├── e2e-verify.cjs                   # CDP 端到端验证脚本
└── vite.config.js                   # '@' → src 别名 + 代理 + 分包
```

## 二、关键文件迁移对照

| # | 原路径 | 新路径 | 需同步修改的引用 |
|---|--------|--------|------------------|
| 1 | `src/store/` | `src/stores/` | api/api.js、router/index.js、Auth/Login.vue、Qa/QA.vue、Senior/composables/useSeniorQA.js（共 5 处） |
| 2 | `src/layout/` | `src/layouts/` | router/index.js（MainLayout 懒加载） |
| 3 | `src/assets/智能云.png` | `src/assets/images/auth-bg.png` | Login.vue、Register.vue 的 CSS `url()` |
| 4 | `src/assets/学生学科问答平台图案美化.png`、`登录背景.png` | 无引用，移入 `_backup_src_20261006/` | 无 |
| 5 | `src/resources/knowledge_graph.sql` | `docs/sql/knowledge_graph.sql` | 无（src 不再混入非源码文件，`resources/` 目录撤销） |
| 6 | `src/utils/markdownUtils.js` | `src/utils/markdown/aiResponse.js` | Qa/QA.vue（3 个导出：processAiResponse / processLatexOnly / markdownToPlainText） |
| 7 | `src/utils/markdownRenderer.js` | `src/utils/markdown/renderer.js` | Senior/components/SeniorDetailDialog.vue（renderMarkdown） |
| 8 | 相对路径 `../../`、`../../../` 深层导入 | 统一 `@/` 别名 | Qa/QA.vue（3 条）、useSeniorQA.js（2 条）、SeniorDetailDialog.vue（1 条） |
| 9 | `from '@/router'` | `from '@/router/index.js'` | api/api.js（显式化） |
| 10 | `.js` 导入带扩展名 | 统一省略（`.vue` 保留，Vite 不补全 vue 扩展名） | 全部 14 个含 import 的文件 |

## 三、命名规范（本次固化的约定）

| 对象 | 规范 | 示例 |
|------|------|------|
| 技术性目录 | camelCase 单/复数按社区约定 | api、assets、components、composables、layouts、router、stores、styles、utils |
| 业务域视图目录 | PascalCase，与路由名对应 | Auth、Bookshelf、KnowledgeGraph、WrongBook |
| Vue 组件文件 | PascalCase，缩略词保持原样 | QA.vue、MainLayout.vue、SeniorAskDialog.vue |
| JS 模块文件 | camelCase | api.js、auth.js、dataProcessor.js |
| 组合式函数 | use 前缀 + PascalCase | useSeniorQA.js |
| 静态资源 | kebab-case ASCII 语义名 | auth-bg.png |
| 组件内私有结构 | 视图目录下 components/ composables/ utils/ | views/Senior/components/ |

**公共组件/组合式准入规则**：被 ≥2 个业务域引用才提升到 `src/components/`、`src/composables/`；单页面专用代码留在 `views/<域>/` 内（就近原则），防止公共区退化为垃圾场。当前两区为空（暂无跨域共享代码），已建 `.gitkeep` 占位。

## 四、改进点

### 可维护性
- **深层相对路径清零**：`../../../api/api.js` 这类"路径数数"导入全部改为 `@/` 别名，文件移动不再引发雪崩式路径修复
- **src 职责单一**：SQL 种子文件移出 src，静态资源只进 assets/，一眼可辨"源码 / 资源 / 文档"
- **工具函数语义化归组**：markdownRenderer / markdownUtils 两个名字相近、职责不同的文件，归入 `utils/markdown/` 并改名为 renderer（通用渲染）/ aiResponse（AI 回复处理），消除歧义

### 可扩展性
- **业务域自包含**：每个 views/<域> 可独立携带自己的 components/ composables/ utils/，新增页面只需建目录 + 注册路由，不动全局
- **公共区就位**：components/ composables/ 已预留并写明准入规则，跨域复用出现时有明确去处
- **状态管理可生长**：stores/ 目录约定使后续新增 user/、practice/ 等 store 有统一位置

### 团队协作
- **命名规范文档化**：目录/文件/资源三类命名规则成文，新成员无需猜测"该放哪、叫什么"
- **git 重命名历史保留**：全部迁移使用 `git mv`，`git log --follow` 可追溯文件演变
- **零功能回归验证**：迁移前后 CDP 端到端 TOTAL=10 FAIL=0，各页面渲染文本长度逐字节一致，可放心在同一分支协作
