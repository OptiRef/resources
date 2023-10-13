#!/bin/bash
# config file
PROP_FILE=query.properties

# database
db=$1
# summary jar file
SUM_JAR="query/target/query-jar-with-dependencies.jar"
CMD="java -Xmx100G -XX:MaxPermSize -jar $SUM_JAR $PROP_FILE $db"

echo $CMD

$CMD

exit 0
