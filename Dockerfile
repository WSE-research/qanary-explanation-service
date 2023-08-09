FROM openjdk:17.0.2-jdk
COPY target/webservice_for_componentExplanation-0.1.0.jar webservice_for_componentExplanation-0.1.0.jar
ENTRYPOINT ["java", "-jar", "webservice_for_componentExplanation-0.1.0.jar"]