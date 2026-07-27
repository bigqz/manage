# 多阶段构建 - 先用 Maven 编译，再用 JRE 运行
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
# Force cache bust - v5
RUN echo "cache-bust-5" > /tmp/cache-bust
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache ca-certificates
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
