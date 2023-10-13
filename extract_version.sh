cd target
JAR_FILE=$(find . -maxdepth 1 -type f -name "*.jar")
export JAR_VERSION=$(echo "$JAR_FILE" | grep -oP 'java-\K[\d.]+')

echo JAR_VERSION