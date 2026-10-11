# 单体版构建镜像
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml pom.xml
RUN mvn dependency:go-offline -DskipTests -q 2>/dev/null || true

COPY src src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# 安全：JWT 签名密钥必须在运行时通过环境变量注入，切勿写死在镜像里。
# 启动示例：docker run -e JWT_SECRET="<不少于32字节的强随机字符串>" -p 8080:8080 app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
