cd target && ls
#JAR_FILE=$(find . -maxdepth 1 -type f -name "qa.commons-[0-9].[0-9].[0-9].jar")
#JAR_VERSION=$(echo $JAR_FILE | grep -oP 'qa.commons-\K\d+\.\d+\.\d+')
JAR_FILE=$(find . -maxdepth 1 -type f -name "qa.commons-[0-9].[0-9].[0-9].jar")
JAR_VERSION=$(echo $JAR_FILE | grep -oP 'qa.commons-\K\d+\.\d+\.\d+')
mv qa.commons-$JAR_VERSION.jar qa.commons.jar # Rename commons jar
cp qa.commons.jar /app
cd /app
echo $JAR_VERSION > jar_version # Create file involving jar-file