#Bauen des externen repos
FROM ubuntu:20.04 AS qanary_commons
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update
RUN apt-get install -y maven wget #git #openjdk-17-jre
WORKDIR /app
#RUN wget https://dlcdn.apache.org/maven/maven-3/3.9.4/binaries/apache-maven-3.9.4-bin.tar.gz
#RUN tar xzvf apache-maven-3.9.4-bin.tar.gz
# old RUN git clone https://github.com/WDAqua/Qanary.git
RUN wget https://github.com/WDAqua/Qanary/archive/refs/tags/v3.5.2.tar.gz
RUN tar -xzvf v3.5.2.tar.gz
WORKDIR /app/Qanary-3.5.2/qanary_commons
RUN mvn clean install -DskipTests
WORKDIR /app
RUN cp Qanary-3.5.2/qanary_commons/target/qa.commons-3.5.2.jar .

#Build Stage
FROM maven:latest AS build
WORKDIR /app
COPY src .
COPY pom.xml .
COPY --from=qanary_commons /app/qa.commons-3.5.2.jar .
# Installing the qa_commons dependency
RUN mvn install:install-file -Dfile=qa.commons-3.5.2.jar -DgroupId=eu.wdaqua.qanary -DartifactId=qa.commons -Dversion=3.5.2 -Dpackaging=jar
# build the app
RUN mvn clean install

FROM openjdk:17.0.2-jdk
WORKDIR /app
COPY --from=build /app/target/webservice_for_componentExplanation-*.jar webservice_for_componentExplanation.jar
ENTRYPOINT ["java", "-jar", "webservice_for_componentExplanation.jar"]
