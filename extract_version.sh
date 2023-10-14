cd target && ls
JAR_FILE=$(find . -maxdepth 1 -type f -name "qa.commons-[0-9].[0-9].[0-9].jar")
JAR_VERSION=$(echo $JAR_FILE | grep -oP 'qa.commons-\K\d+\.\d+\.\d+')
echo $JAR_VERSION > /app/qanaryVersion