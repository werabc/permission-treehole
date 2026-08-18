FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制预构建的 JAR
COPY permission-api-1.0.0.jar app.jar

# 上传文件目录
RUN mkdir -p /app/uploads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
