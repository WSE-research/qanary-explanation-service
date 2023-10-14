#Bauen des externen repos
FROM ubuntu:20.04 AS qanary_commons
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update
RUN apt-get install -y maven wget #git #openjdk-17-jre
RUN apt-get install -y git

WORKDIR /app
RUN git clone https://github.com/WDAqua/Qanary.git

WORKDIR /app/Qanary/qanary_commons
RUN mvn clean install -DskipTests
COPY extract_commons_version.sh /app/extract_commons_version.sh
RUN chmod +x /app/extract_commons_version.sh
RUN /app/extract_commons_version.sh

WORKDIR /app
COPY Qanary/qa_commons/target/qa.commons.jar .

#Build Stage
FROM maven:latest AS build
WORKDIR /app
COPY ./src ./src
COPY ./pom.xml ./pom.xml
COPY --from=qanary_commons /app/qa.commons.jar .
COPY --from=qanary_commons /app/jar_version .
# Installing the qa_commons dependency
COPY install_commons_dependency.sh /app/install_commons_dependency.sh
RUN chmod +x /app/install_commons_dependency.sh
RUN /app/install_commons_dependency.sh
RUN mvn install:install-file -Dfile=qa.commons.jar -DgroupId=eu.wdaqua.qanary -DartifactId=qa.commons -Dversion="$JAR_VERSION" -Dpackaging=jar
# build the app
RUN mvn clean install

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/qanary-explanation-service-*.jar qanary-explanation-service.jar
ENTRYPOINT ["java","-jar","qanary-explanation-service.jar"]
