# ---------- 阶段 1：构建 ----------
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 先拷贝 pom.xml 单独拉依赖，利用 Docker 层缓存
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- 阶段 2：运行 ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8233

ENTRYPOINT ["java", "-jar", "app.jar"]
