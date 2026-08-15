# =============================================
# 后端 Dockerfile — 直接运行预构建的 JAR
# 构建产物已在本地打好：permission-api/target/permission-api-1.0.0.jar
# =============================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装 curl 用于 HEALTHCHECK，并设置时区
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 复制预构建的可执行 JAR
COPY permission-api/target/permission-api-1.0.0.jar app.jar

# 非 root 用户运行
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/auth/captcha -o /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
