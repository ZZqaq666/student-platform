# 后端单体化迁移说明（2026-10-05）

## 概述

应用户要求，将后端由「Spring Cloud Gateway + 5 个业务微服务 + common 公共库」合并为**单一 Spring Boot 应用**（端口 8080，context-path `/api`），前端无需任何改动。

原微服务代码完整归档于 `_archive-microservices/`，确认单体稳定运行后可自行删除。

## 新结构

```
student-platform-backend/
├── pom.xml                     # 单模块 Spring Boot 工程（packaging: jar）
├── Dockerfile                  # 单体镜像（多阶段构建）
├── src/
│   ├── main/java/com/student/platform/
│   │   ├── StudentPlatformApplication.java   # 唯一启动类
│   │   ├── common/             # 原 common 模块（dto/exception/util/result/event/config）
│   │   ├── user/ qa/ learning/ exercise/ knowledge/   # 原五个服务的业务代码
│   └── main/resources/
│       ├── application.yml     # 合并后的配置（单库 student_platform）
│       └── mapper/{user,qa,learning,exercise,knowledge}/*.xml
├── docker-compose.yml          # 基础设施（mysql/redis/rabbitmq，未变）
├── init.sql
└── _archive-microservices/     # 原微服务工程归档（含旧 Dockerfile、seata-config）
```

## 关键改动

1. **对外路径不变**：原网关对 `/api/**` 路由并 `StripPrefix=1`，单体改用
   `server.servlet.context-path: /api` 等价实现，控制器映射保持 `/auth`、`/qa` 等
   原样。仅修正两个硬编码了 `/api` 前缀的控制器：
   `QaController#ImageProxyController`（`/api/image-proxy` → `/image-proxy`）、
   `CourseRecommendationController`（`/api/course-recommendations` → `/course-recommendations`）。

2. **鉴权移植**：网关 `JwtAuthGatewayFilterFactory` 移植为 Servlet 过滤器
   `common/config/JwtAuthFilter`，白名单与 401 语义一致；校验通过后注入
   `X-User-Id` / `X-User-Name` 请求头，原有 `UserIdInterceptor` 继续生效。
   HTTP 层由统一 `SecurityConfig`（permitAll + stateless）放行，BCrypt Encoder 保留。

3. **配置类去重**：原各服务重复复制的 `RedisConfig`、`JacksonConfig`、`OpenApiConfig`、
   `CacheConfig`、`CharacterEncodingFilterConfig`、`GlobalExceptionHandler` 合并为一份，
   移入 `common/config`；`WebMvcConfig`（4 份相同）、`SecurityConfig`（2 份）、
   `UserIdInterceptor`（5 份）各保留一份（位于 `user/config`）。

4. **同名 bean 冲突处理**：跨包同名类（如 `AiServiceImpl`、`RedisUtil`、各服务的
   `UserMapper`）通过 `FullyQualifiedAnnotationBeanNameGenerator`（启动类与
   `@MapperScan` 均已指定）以全限定类名注册 bean，按类型注入不受影响。

5. **移除微服务组件**：Nacos 注册发现、Sentinel、OpenFeign、Seata、Spring Cloud BOM
   全部移除（Feign 接口在业务代码中本就无任何注入点）。

6. **AI 视觉模型优雅降级**：`AiServiceImpl` 的 `visionStreamingChatModel` 改用
   `ObjectProvider` 注入，`GLM_API_KEY` 未配置时为 null（业务侧已有判空逻辑，
   图片问答功能自动禁用，不再阻断启动）。

7. **MyBatis 别名冲突**：不再配置 `type-aliases-package`（多服务存在同名实体类如
   `User`），所有 Mapper XML 均使用全限定类名，无需别名。

## 运行方式

```bash
# 依赖环境：MySQL(student_platform 库) / Redis / RabbitMQ（可用根目录 docker-compose.yml 启动）
mvn -DskipTests package
java -jar target/student-platform-backend-1.0.0.jar    # 默认 8080

# 环境变量（与原微服务一致）
# SPRING_DATASOURCE_URL / SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD
# REDIS_HOST / REDIS_PASSWORD / RABBIT_* / JWT_SECRET / DEEPSEEK_API_KEY / GLM_API_KEY
```

## 验证记录（本地无数据库环境）

- `mvn -DskipTests package` → BUILD SUCCESS（73MB fat jar）
- `GET /api/actuator/health` → 200 `{"status":"DOWN"}`（DB 未启动，预期）
- `GET /api/qa/sessions`（无 token）→ 401 `Missing JWT token`（鉴权过滤器生效）
- `POST /api/auth/login` → 503 统一异常「数据库连接失败」，日志确认请求已正确
  路由至 `user.mapper.UserMapper.findByUsername`（路由 + MyBatis 装配正常）
