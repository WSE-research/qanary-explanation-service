#Build Stage - Frontend
FROM node:latest AS client_build
WORKDIR /app
COPY ./client ./client
WORKDIR /app/client
RUN npm install && npm run build


#Build Stage
FROM maven:latest AS build
WORKDIR /app
COPY ./src ./src
COPY ./pom.xml ./pom.xml
COPY --from=client_build /app/client/build/* src/main/resources/static
# build the app
WORKDIR /app
RUN mvn clean install

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/qanary-explanation-service-*.jar qanary-explanation-service.jar
ENTRYPOINT ["java","-jar","qanary-explanation-service.jar"]
