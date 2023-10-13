#Bauen des externen repos
FROM ubuntu:20.04 AS qanary_commons
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update
RUN apt-get install -y maven wget #git #openjdk-17-jre
RUN apt-get install -y git
WORKDIR /app

ENV JAR_VERSION=""

RUN git clone https://github.com/WDAqua/Qanary.git
WORKDIR /app/Qanary/qanary_commons
RUN mvn clean install -DskipTests
COPY extract_version.sh /extract_version.sh
RUN chmod +x /extract_version.sh
RUN /extract_version.sh
WORKDIR /app
RUN cp Qanary/qanary_commons/target/qa.commons-"$JAR_VERSION".jar .

#RUN wget https://github.com/WDAqua/Qanary/archive/refs/tags/v3.5.2.tar.gz
#RUN tar -xzvf v3.5.2.tar.gz
#WORKDIR /app/Qanary-3.5.2/qanary_commons
#RUN mvn clean install -DskipTests
#WORKDIR /app
#RUN cp Qanary-3.5.2/qanary_commons/target/qa.commons-3.5.4.jar .

#Build Stage
FROM maven:latest AS build
WORKDIR /app
COPY ./src ./src
COPY ./pom.xml ./pom.xml
COPY --from=qanary_commons /app/qa.commons-"$JAR_VERSION".jar .
# Installing the qa_commons dependency
RUN mvn install:install-file -Dfile=qa.commons-"$JAR_VERSION".jar -DgroupId=eu.wdaqua.qanary -DartifactId=qa.commons -Dversion="$JAR_VERSION" -Dpackaging=jar
# build the app
RUN mvn clean install

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/qanary-explanation-service-*.jar qanary-explanation-service.jar
ENTRYPOINT ["java","-jar","qanary-explanation-service.jar"]
