#!/bin/bash
# cc qa
# for queriesGroup in cc
# do
#     ./run_queries_dynamic.sh lubm75M ressources/LUBM-ex-20.owl ressources/LUBM-ex-20.dlp $queriesGroup N O N
# done
DB=lubm130M


# echo BD2
# echo database.engine = DB2  >> summary.properties
# echo database.port = 50000  >> summary.properties
# echo database.password = [PASSWORD]  >> summary.properties

# echo database.summary_table_alis = sum  >> summary.properties
# echo stats.output = exps-final/exps-db2-$DB/inconsistancy/  >> summary.properties
# mkdir -p exps-final/exps-db2-$DB/inconsistancy/
# ./add_inconsistancy_db2.sh $DB O 10000 Department exps-final/exps-db2-$DB/inconsistancy/$DB\_Department.add Person Professor Student Work Publication Employee
# ./run_summary.sh $DB

# echo database.summary_table_alis = sum  >> query.properties
# for queriesGroup in cc
# do
#     ./run_queries_dynamic.sh $DB ressources/LUBM-ex-20.owl ressources/LUBM-ex-20.dlp $queriesGroup O N N
# done

# psql -d $DB -c "insert into table137 (c0) select c0 from table21 limit 10000;"
# psql -d $DB -c "insert into table129 (c0) select c0 from table21 limit 5000;"
# psql -d $DB -c "insert into table123 (c0) select c0 from table21 limit 15000;"
# psql -d $DB -c "insert into table120 (c0) select c0 from table21 limit 13000;"
# psql -d $DB -c "insert into table111 (c0) select c0 from table21 limit 17000;"
# psql -d $DB -c "insert into table5 (c0) select c0 from table21 limit 5000;"
echo database.engine = POSTGRESQL  >> summary.properties
echo database.port = 5432  >> summary.properties
echo database.password = [PASSWORD]  >> summary.properties

echo database.summary_table_alis = sumi  >> summary.properties
echo stats.output = exps-final/exps-psql-$DB/inconsistancy/  >> summary.properties
mkdir -p exps-final/exps-psql-$DB/inconsistancy/

./run_summary.sh $DB

echo database.summary_table_alis = sumi  >> query.properties
for queriesGroup in cc
do
    ./run_queries_dynamic.sh $DB ressources/LUBM-ex-20.owl ressources/LUBM-ex-20.dlp $queriesGroup O N N
done
# psql -d $DB -c "delete from table137 where c0 in (select c0 from table21);"
# psql -d $DB -c "delete from table129 where c0 in (select c0 from table21);"
# psql -d $DB -c "delete from table123 where c0 in (select c0 from table21);"
# psql -d $DB -c "delete from table120 where c0 in (select c0 from table21);"
# psql -d $DB -c "delete from table111 where c0 in (select c0 from table21);"
# psql -d $DB -c "delete from table5 where c0 in (select c0 from table21);"


# echo database.engine = MYSQL  >> summary.properties
# echo database.port = 3306  >> summary.properties
# echo database.password = [PASSWORD]  >> summary.properties

# echo database.summary_table_alis = sumi  >> summary.properties
# echo stats.output = exps-final/exps-mysql-$DB/inconsistancy/  >> summary.properties
# mkdir -p exps-final/exps-mysql-$DB/inconsistancy/
# ./add_inconsistancy_mysql.sh $DB O 10000 Department exps-final/exps-mysql-$DB/inconsistancy/$DB\_Department.add Person Professor Student Work Publication Employee
# ./run_summary.sh $DB

# echo database.summary_table_alis = sumi  >> query.properties

# for queriesGroup in cc
# do
#     ./run_queries_dynamic.sh $DB ressources/LUBM-ex-20.owl ressources/LUBM-ex-20.dlp $queriesGroup N O N
# done

# ./add_inconsistancy_mysql.sh $DB N 10000 Department exps-final/exps-mysql-$DB/inconsistancy/$DB\_Department.add Person Professor Student Work Publication Employee
