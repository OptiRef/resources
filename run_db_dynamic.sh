#!/bin/bash
#Edit the passwords according to your configurations before using the script!


if [ $# -ne 6 ] ; then
	echo "Usage: $0 DBNAME ONTOLOGY DATAPATH POSTGRE(O/N) MYSQL(O/N) DB2(O/N)"
	exit 10
fi

dbname=$1
ontology=$2
datapath=$3
postgres=$4
mysql=$5
db2=$6

echo database.name = "${dbname}" >> load.properties

echo database.ontologyDlp = "${ontology}"  >> load.properties

echo dlp.filesDirectory = "${datapath}" >> load.properties


# POSTGRES
if [[ "$postgres" == 'O' ]] ; then
	echo "Delete POSTGRE DB $dbname ? (y/n)"
	read del
	if [[ "$del" == 'y' ]] ; then
		dropdb $dbname
		createdb $dbname || exit 21
	fi

	mkdir -p exps-final/exps-psql-$dbname
	echo database.engine = POSTGRESQL  >> load.properties
	echo database.port = 5432  >> load.properties
	echo database.password = [PASSWORD]  >> load.properties
	./load_data.sh  || exit 22

	echo database.engine = POSTGRESQL  >> summary.properties
	echo database.port = 5432  >> summary.properties
	echo database.password = [PASSWORD]  >> summary.properties
	echo stats.output = exps-final/exps-psql-$dbname/  >> summary.properties

	./run_summary.sh $dbname  || exit 23
	# Load stats
	vacuumdb --analyze $dbname  || exit 24

fi

# MYSQL
if [[ "$mysql" == 'O' ]] ; then
	echo "Delete MYSQL DB $dbname ? (y/n)"
	read del
	if [[ "$del" == 'y' ]] ; then
		mysqladmin --defaults-extra-file=mysql.conf -f  drop $dbname
		mysqladmin --defaults-extra-file=mysql.conf create $dbname	 || exit 31
	fi

	mkdir -p exps-final/exps-mysql-$dbname
	echo database.engine = MYSQL  >> load.properties
	echo database.port = 3306  >> load.properties
	echo database.password = [PASSWORD]  >> load.properties
	./load_data.sh

	echo database.engine = MYSQL  >> summary.properties
	echo database.port = 3306  >> summary.properties
	echo database.password = [PASSWORD]  >> summary.properties
	echo stats.output = exps-final/exps-mysql-$dbname/  >> summary.properties


	./run_summary.sh $dbname || exit 32
	# Load stats
	mysqlcheck --defaults-extra-file=mysql.conf -o $dbname >logs/mysql_$dbname.log || exit 33
fi

# # DB2
if [[ "$db2" == 'O' ]] ; then
	echo "Delete DB2 DB $dbname ? (y/n)"
	read del
	export DB2_INST=/local/ibm/db2/V11.5
	export PATH=$PATH:$DB2_INST/bin:$DB2_INST/adm
	. /home/db2inst1/sqllib/db2profile

	if [[ "$del" == 'y' ]] ; then
		db2 drop database $dbname
		db2 create database $dbname on '/local/data/' || exit 41
		mkdir -p exps-final/exps-db2-$dbname
	fi

	# db2 connect to $dbname
	db2 update db cfg for $dbname using LOGPRIMARY 128
	db2 update db cfg for $dbname using LOGSECOND 128
	db2 update db cfg for $dbname using LOGFILSIZ 4096
	db2 "CALL SYSPROC.SYSINSTALLOBJECTS('EXPLAIN','C',NULL,CURRENT SCHEMA)"
	db2 connect reset

	echo database.engine = DB2  >> load.properties
	echo database.port = 50000  >> load.properties
	echo database.password = [PASSWORD]  >> load.properties
	./load_data.sh || exit 42

	echo database.engine = DB2  >> summary.properties
	echo database.port = 50000  >> summary.properties
	echo database.password = [PASSWORD]  >> summary.properties
	echo stats.output = exps-final/exps-db2-$dbname/  >> summary.properties
	mkdir -p  exps-final/exps-db2-$dbname/
	./run_summary.sh $dbname || exit 43

	db2 connect to $dbname

	db2 -x "select 'reorg table',substr(rtrim(tabschema)||'.'||rtrim(tabname),1,50),';'from syscat.tables where type = 'T' " > logs/reorg_$dbname.out
	db2 -tvf  logs/reorg_$dbname.out > logs/db2_out_$dbname.out

	db2 -x "select 'reorgchk update statistics on table',substr(rtrim(tabschema)||'.'||rtrim(tabname),1,50),';' from syscat.tables where type = 'T' " > logs/reorgchk_$dbname.out
	db2 -tvf logs/reorgchk_$dbname.out >> logs/db2_out_$dbname.out

	db2 -x "select 'runstats on table',substr(rtrim(tabschema)||'.'||rtrim(tabname),1,50),' and indexes all;'from syscat.tables where type = 'T' " > logs/runstats_$dbname.out
	db2 -tvf logs/runstats_$dbname.out  >> logs/db2_out_$dbname.out

	db2rbind $dbname -l logs/db2_$dbname.log all -u user -p [PASSWORD]

	db2 connect reset

fi
