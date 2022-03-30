# DL-SUM4QA

# Summaries for Query Answering in DL

## Installation

### Loading data

```bash
cd loadDL
mvn clean compile assembly:single
```
### Summary 

```bash
cd ../summary
mvn clean compile assembly:single
```

### Query

```bash
cd ../query
mvn clean compile assembly:single
```

## Usage

### Loading

General command (check `load.properties` for login parameters):
```bash
./load_data.sh 
```
Example: the database in `Base_1MT` is loaded into the `lubm1M` database in Postgres using the following command. 
```bash
./load_data.sh 
```

### Creating the summary :
General command (check `summary.properties` for login parameters), this script will creates a summary tables for each concept and each role:
```bash
./run_summary.sh <DATABASE_NAME>
```

### Querying
General command (check `query.properties` for login parameters):
```bash
./run-exp.sh <DATABASE_NAME>
```
1. `DB` defines the database name of the graph ,
with the params in query.properties:
2. `database.queries` defines the file containing the queries,
3. `reformulation.approach` defines the Query Answering approach among:
   - `UCQ` reformulation-based query answering  using RAPID
   - `SCQ` reformulation-based query answering using COMPACT
   - `JUCQ` reformulation-based query answering with using greedy optimization of query covers
4. `database.pruning` specifies that the pruning is activated.
5. `exps.output` the output folder, example ./myfolder/ 

Example from the loaded lubm1M database with UCQ (RAPID) change the params from the query.properties file:
```bash
./run-exp.sh lubm1MDL
```

