#!/bin/bash
#Edit the passwords according to your configurations before using the script!

if [ $# -ne 7 ] ; then
	echo "Usage: $0 DBNAME OWLONTOLOGY DLPONTOLOGY QUERYTAG POSTGRE(O/N) MYSQL(O/N) DB2(O/N)"
	exit 10
fi

dbname=$1
owlontology=$2
dlpontology=$3
queriesGroup=$4
shift; shift; shift; shift

echo database.ontology = $owlontology  >> query.properties
echo compact.ontology  = $dlpontology  >> query.properties


inputdb=$dbname

for nb in `seq 1 3` ; do
	dbname=$inputdb
	if [[ "$1" == "O" ]] ; then
		case $nb in
			1)
			# POSTGRES
			echo POSTGRES

			echo database.engine = POSTGRESQL  >> query.properties
			echo database.port = 5433  >> query.properties
			echo database.password = [PASSWORD]  >> query.properties
			echo exps.output = exps-final/exps-psql-$dbname/$queriesGroup-cc-inconsistant/  >> query.properties
			mkdir -p  exps-final/exps-psql-$dbname/$queriesGroup-cc-inconsistant/
			;;
			2)
			# MYSQL
			echo MYSQL
			echo database.engine = MYSQL  >> query.properties
			echo database.port = 3306  >> query.properties
			echo database.password = [PASSWORD]  >> query.properties
			echo exps.output = exps-final/exps-mysql-$dbname/$queriesGroup-cc-inconsistant/  >> query.properties
			mkdir -p  exps-final/exps-mysql-$dbname/$queriesGroup-cc-inconsistant/
			;;
			3)
			# BD2
			echo BD2
			echo database.engine = DB2  >> query.properties
			echo database.port = 50000  >> query.properties
			echo database.password = [PASSWORD]  >> query.properties
			echo exps.output = exps-final/exps-db2-$dbname/$queriesGroup-cc-inconsistant/  >> query.properties
			mkdir -p  exps-final/exps-db2-$dbname/$queriesGroup-cc-inconsistant/
			;;
		esac
		# # Separate QA and CC
		echo database.queries  = ressources/lubm-$queriesGroup.queries    >> query.properties

		# UCQ USCQ JUCQ

		for approach in UCQ USCQ
		do
			echo reformulation.approach = $approach  >> query.properties

			echo database.use_limit1 = false >> query.properties
			echo database.use_exists = false >> query.properties
			echo database.pruning = true  >> query.properties
			echo pruning.summary  = true  >> query.properties
			./run-exp.sh $dbname
			echo pruning.summary  = false  >> query.properties
			./run-exp.sh $dbname
			echo database.pruning = false  >> query.properties
			echo pruning.summary  = false  >> query.properties
			./run-exp.sh $dbname
		done

	fi

	shift
done
