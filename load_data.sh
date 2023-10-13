PROP_FILE=load.properties
# summary jar file
LOAD_JAR="loadDL/target/loadDL-jar-with-dependencies.jar"
CMD="java -Xmx100G -jar $LOAD_JAR $PROP_FILE"

echo $CMD

$CMD

exit 0
