FROM gradle:jdk17-alpine AS build
WORKDIR /build
COPY . .
RUN gradle clean build

FROM eclipse-temurin:17-jdk-alpine AS final
WORKDIR /app
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/app" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser
USER appuser
COPY --from=build /build/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar"]
EXPOSE 8080
CMD ["/app/app.jar"]
