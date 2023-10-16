JAR_VERSION=$(cat jar_version | xargs)

# Install commons-dependency
mvn install:install-file -Dfile=qa.commons.jar -DgroupId=eu.wdaqua.qanary -DartifactId=qa.commons -Dversion=$JAR_VERSION -Dpackaging=jar
