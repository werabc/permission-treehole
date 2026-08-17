# =============================================
# 后端 Dockerfile - 多阶段构建（Maven + Java 17）
# =============================================

# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 复制 POM 文件
COPY pom.xml .
COPY permission-common/pom.xml permission-common/
COPY permission-framework/pom.xml permission-framework/
COPY permission-system/pom.xml permission-system/
COPY permission-api/pom.xml permission-api/

# 复制源码
COPY permission-common/src permission-common/src
COPY permission-framework/src permission-framework/src
COPY permission-system/src permission-system/src
COPY permission-api/src permission-api/src

# 构建可执行 JAR（跳过 dependency:go-offline 以加快构建）
RUN mvn clean package -DskipTests -q

# =============================================
# 生产阶段
# =============================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装 curl 用于 HEALTHCHECK，设置时区
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 从构建阶段复制 JAR
COPY --from=builder /build/permission-api/target/*.jar app.jar

# 非 root 用户运行
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -f http://localhost:8080/api/auth/captcha -o /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "-Dfile.encoding=UTF-8", "app.jar"]
