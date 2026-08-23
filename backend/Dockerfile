FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --uid 10001 --create-home appuser
COPY build/libs/money-backend.jar /app/money-backend.jar
RUN chown appuser:appuser /app/money-backend.jar

USER appuser
EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "/app/money-backend.jar"]
