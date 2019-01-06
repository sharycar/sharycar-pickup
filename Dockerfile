FROM openjdk:8-jre-alpine
RUN mkdir /app
WORKDIR /app
ADD ./sharycar-pickup-api/target/sharycar-pickup-api-1.0-SNAPSHOT.jar /app
EXPOSE 8082
CMD ["java", "-jar", "sharycar-pickup-api-1.0-SNAPSHOT.jar"]
