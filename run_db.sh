#!/bin/bash

printf "y\ny\ny\n" | ./run_db_dynamic.sh lubm140M ressources/LUBM-ex-20.dlp /local/data/DLP/Base_1000U/ N N O
# dbname=$1

# echo database.name = $dbname >> load.properties
# echo database.ontologyDlp = ressources/LUBM-ex-20.dlp  >> load.properties
# echo dlp.filesDirectory = /local/data/DLP/Base_1000U/ >> load.properties
# # echo database.ontologyDlp = ressources/npd-v2-ql.dlp  >> load.properties
# # echo dlp.filesDirectory = /local/disable-reasoning/npd100.nt  >> load.properties


# # POSTGRES
# dropdb $dbname
# createdb $dbname
# mkdir -p exps-final/exps-psql-$dbname
# echo database.engine = POSTGRESQL  >> load.properties
# echo database.port = 5432  >> load.properties
# echo database.password = [PASSWORD]  >> load.properties
# ./load_data.sh

# echo database.engine = POSTGRESQL  >> summary.properties
# echo database.port = 5432  >> summary.properties
# echo database.password = [PASSWORD]  >> summary.properties
# echo stats.output = exps-final/exps-psql-$dbname/  >> summary.properties

# ./run_summary.sh $dbname
# # Load stats
# vacuumdb --analyze $dbname

# ./run_queries.sh >>results/lubm130.run0

# # MYSQL
# mysqladmin --defaults-extra-file=mysql.conf -f  drop $dbname
# # BACK_PID=$!
# # wait $BACK_PID
# mysqladmin --defaults-extra-file=mysql.conf create $dbname

# mkdir -p exps-final/exps-mysql-$dbname
# echo database.engine = MYSQL  >> load.properties
# echo database.port = 3306  >> load.properties
# echo database.password = [PASSWORD]  >> load.properties
# ./load_data.sh

# echo database.engine = MYSQL  >> summary.properties
# echo database.port = 3306  >> summary.properties
# echo database.password = [PASSWORD]  >> summary.properties
# echo stats.output = exps-final/exps-mysql-$dbname/  >> summary.properties


# ./run_summary.sh $dbname
# # Load stats
# mysqlcheck --defaults-extra-file=mysql.conf -o $dbname >logs/mysql_$dbname.log

# ./run_queries.sh $dbname

# # DB2
# export DB2_INST=/local/ibm/db2/V11.5
# export PATH=$PATH:$DB2_INST/bin:$DB2_INST/adm
# . /home/db2inst1/sqllib/db2profile

# db2 drop database $dbname
# db2 create database $dbname on '/local/data/'
# mkdir -p exps-final/exps-db2-$dbname

# db2 connect to $dbname
# db2 update db cfg for $dbname using LOGPRIMARY 128
# db2 update db cfg for $dbname using LOGSECOND 128
# db2 update db cfg for $dbname using LOGFILSIZ 4096
# db2 "CALL SYSPROC.SYSINSTALLOBJECTS('EXPLAIN','C',NULL,CURRENT SCHEMA)"
# db2 connect reset

# echo database.engine = DB2  >> load.properties
# echo database.port = 50000  >> load.properties
# echo database.password = [PASSWORD]  >> load.properties
# ./load_data.sh

# echo database.engine = DB2  >> summary.properties
# echo database.port = 50000  >> summary.properties
# echo database.password = [PASSWORD]  >> summary.properties
# echo stats.output = exps-final/exps-db2-$dbname/  >> summary.properties
# mkdir -p  exps-final/exps-db2-$dbname/
# ./run_summary.sh $dbname

# db2 connect to $dbname

# db2 -x "select 'reorg table',substr(rtrim(tabschema)||'.'||rtrim(tabname),1,50),';'from syscat.tables where type = 'T' " > logs/reorg_$dbname.out
# db2 -tvf  logs/reorg_$dbname.out > logs/db2_out_$dbname.out

# db2 -x "select 'reorgchk update statistics on table',substr(rtrim(tabschema)||'.'||rtrim(tabname),1,50),';' from syscat.tables where type = 'T' " > logs/reorgchk_$dbname.out
# db2 -tvf logs/reorgchk_$dbname.out >> logs/db2_out_$dbname.out

# db2 -x "select 'runstats on table',substr(rtrim(tabschema)||'.'||rtrim(tabname),1,50),' and indexes all;'from syscat.tables where type = 'T' " > logs/runstats_$dbname.out
# db2 -tvf logs/runstats_$dbname.out  >> logs/db2_out_$dbname.out

# db2rbind $dbname -l logs/db2_$dbname.log all -u [USERNAME] -p [PASSWORD]

# db2 connect reset

# ./run_queries.sh $dbname
