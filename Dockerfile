FROM openjdk:17.0.2
COPY target/webservice_for_annotationsRequest-0.0.1-SNAPSHOT.jar webservice_for_annotationsRequest.jar
ENTRYPOINT ["java","-jar","webservice_for_annotationsRequest.jar"]