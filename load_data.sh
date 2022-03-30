#!/bin/bash
# dbdir=Base_1MT
# root=/local/data/LUBM
# for file in $root/$dbdir/*.owl 
# do 
#  java -jar loadDL/lib/owl2dlgp-1.1.0.jar -f $root/$dbdir/$file -o /local/data/LUBM-DLP/$dbdir/"$(basename "$file" .owl).dlp"

# done

 
PROP_FILE=load.properties
# summary jar file 
LOAD_JAR="loadDL/target/loadDL-jar-with-dependencies.jar"
CMD="java -Xmx100G -jar $LOAD_JAR $PROP_FILE"

echo $CMD
 
$CMD
 
exit 0