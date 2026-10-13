# ScholarPal 后端

学生学科智能问答平台的服务端：Spring Boot 3.5.7 / Java 17 / MySQL 8 / Redis /
MyBatis-Plus 3.5.9 / LangChain4j（DeepSeek 文本 + 视觉），单体架构。

15 个 `@RestController`、约 106 个端点，按领域分包：
`user`（认证与用户）、`learning`（书架/书籍/章节/学习进度/笔记）、
`exercise`（练习与错题本）、`qa`（问答、学长答疑、会话）、`knowledge`（知识图谱）、
`ai`（模型接入）、`common`（跨域共享的配置、DTO、工具）。

## 本地运行

需要同时具备 **MySQL** 和 **Redis**。Redis 不是可选项：JWT 吊销名单存在 Redis 里，
且按 fail-closed 设计——查不到名单就当令牌无效，所以没有 Redis 时所有鉴权请求都会
返回 401。

```bash
# 1) Redis（本机已有则直接起）
redis-server --port 6379 --bind 127.0.0.1 --save "" --appendonly no

# 2) MySQL：默认连 jdbc:mysql://localhost:3306/student_platform，用户 root、空口令
#    建库用 docs/sql/schema-baseline.sql（由开发库导出的结构基线）

# 3) 启动。注意先 unset SERVER__PORT：机器上残留的该环境变量会引发端口冲突
unset SERVER__PORT
export JWT_SECRET="<不少于 32 字节的强随机串>"     # 缺失时只给一次性随机密钥，重启即失效
export DEEPSEEK_API_KEY="<DeepSeek 密钥>"
mvn -o package -DskipTests
java -jar target/student-platform-backend-1.0.0.jar --server.port=8080
```

`.env` 里的 `SPRING_DATASOURCE_URL` / `REDIS_HOST` 指向的是**不可达的远程主机**，
本地运行需要显式覆盖为 localhost；Spring Boot 本身不会自动读取 `.env`。

健康检查：`GET /api/actuator/health` → `200 {"status":"UP"}`。
上下文路径是 `/api`，所以所有端点都要带这个前缀。

## 构建验证的两个坑

- **`mvn compile` 会假绿**。源码 mtime 早于 `target/classes` 里的 class 时，Maven
  判定"无需编译"直接跳过，即使那个文件已经改坏。验证结论前请
  `touch` 相关源文件或 `mvn clean compile`，确认真的看到 `Compiling N source files`。
- **编译通过 ≠ 能启动**。Mapper XML 里的 `resultType` 是字符串，类被删掉后 Java
  侧没有悬空 import、编译照过，但 `SqlSessionFactory` 创建失败，应用起不来。
  改动实体或 Mapper 后务必实际启动一次。

## 测试与联调

```bash
mvn test                          # 单元测试 + jacoco 覆盖率报告
bash scripts/api-smoke.sh          # 需服务已启动：38 项全量接口联调
```

单元测试覆盖鉴权链（`JwtAuthFilterTest`、`JwtBlacklistTest`）、刷新令牌轮换与吊销
（`UserServiceImplRefreshRevocationTest`）、书籍目录的管理员门禁与部分更新
（`BookshelfServiceImplUpdateBookTest`）。整体覆盖率仍很低（约 3%），新增业务分支
请顺带补测试——`pom.xml` 已配好 jacoco，CI 会上传报告。

`scripts/api-smoke.sh` 里有两条值得知道的约定：

- **中文载荷不要写在命令行里**。Git Bash 会按 GBK 发出，后端报
  `Invalid UTF-8 middle byte`。脚本统一把中文 JSON 落到文件再用
  `--data-binary @file`。
- `book_id` 有外键指向 `book.id`，测试数据必须取库里真实存在的 id。

## 关键配置项（`src/main/resources/application.yml`）

| 键 | 说明 |
|---|---|
| `jwt.secret` / 环境变量 `JWT_SECRET` | 无兜底默认值；弱密钥或短于 32 字节会拒绝启动 |
| `jwt.expiration` | 访问令牌 15 分钟；过期后由前端静默续期 |
| `jwt.refresh-expiration` | 刷新令牌 7 天，一次性使用（换新即作废旧的） |
| `app.cors.allowed-origins` | 显式来源白名单，默认只放本机 Vite 端口 |
| `API_DOCS_ENABLED` | 生产应设为 `false`，否则 Swagger 会公开全部端点清单 |
| `spring.mvc.async.request-timeout` | 200s，必须大于 AI 生成耗时，否则异步请求被掐断 |

## 生产部署注意

`Dockerfile` 是多阶段构建，但**不会**注入 `JWT_SECRET`，必须由编排层传入环境变量，
否则容器每次重启都会生成新密钥、使既有令牌全部失效。

Redis 应设置密码并做网络隔离：缓存值使用带类型信息的 JSON，虽已限制可实例化类型白名单，
但无密码的 Redis 本身仍是可被写入的存储。
