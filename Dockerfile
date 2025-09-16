# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src

# 先拷 pom.xml 并缓存依赖（加速后续构建）
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# 再拷源码并打包
COPY src ./src
RUN mvn -q -DskipTests clean package

# ---- run ----
FROM eclipse-temurin:17-jre

# 装排查工具：ss/netstat/curl/lsof 等（Debian/Ubuntu 基础）
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
      iproute2 net-tools curl lsof procps ca-certificates; \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
# 复制构建产物
COPY --from=build /src/target/*SNAPSHOT.jar app.jar

# 可用环境变量覆盖 application.yml
ENV APP_REDIS_URL=redis://redis:6379 \
    APP_SOCKET_HOST=0.0.0.0 \
    APP_SOCKET_PORT=9092 \
    SERVER_PORT=8080 \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"

EXPOSE 8080 9092

# 可选：如果你有 /actuator/health，就启用这个健康检查
# HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD curl -fsS "http://localhost:${SERVER_PORT:-8080}/actuator/health" || exit 1

# 用 exec 形式，支持 JAVA_OPTS
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]
