FROM eclipse-temurin:21-jdk-alpine
LABEL authors="Daniele"
COPY ./ $HOME/mas
WORKDIR $HOME/mas
RUN ["./gradlew","build"]
ENTRYPOINT ["./gradlew","runhelloworldMAS"]