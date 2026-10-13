# Shared multi-stage build for every service.
#   docker build --build-arg SERVICE=payment-service -t payment-service .
ARG SERVICE

FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /workspace
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY order-service/pom.xml order-service/
COPY payment-service/pom.xml payment-service/
COPY notification-service/pom.xml notification-service/
RUN mvn -B -q -pl ${SERVICE} -am dependency:go-offline
COPY . .
RUN mvn -B -q -pl ${SERVICE} -am package -DskipTests

# Red Hat UBI runtime: supported on OpenShift and runs under an arbitrary non-root UID.
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.20
ARG SERVICE
LABEL org.opencontainers.image.title="${SERVICE}" \
      org.opencontainers.image.source="https://github.com/andrewsselvaraj/payment-gateway-microservices"
COPY --from=build --chown=185:0 /workspace/${SERVICE}/target/app.jar /deployments/app.jar
USER 185
EXPOSE 8080 8081 8082 8083
ENV JAVA_OPTS_APPEND="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS_APPEND -jar /deployments/app.jar"]
