#!/bin/bash

PROP_FILE=summary.properties
# summary jar file 
SUM_JAR="summary/target/dlsummary-jar-with-dependencies.jar"
CMD="java -Xmx100G -jar $SUM_JAR $PROP_FILE $1"

echo $CMD
 
$CMD
 
exit 0