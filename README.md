
# OptiRef: Optimization for data management in KBs
## Requirements & Dependencies

### Dependencies

* Java 14
* Maven
* One or more of the following DBMSs:
  * DB2 (v11.5)
  * MySQL Community (v8.0.25)
  * PostgreSQL (v14.2)

### Data
To run OptiRef, data in the .owl or .dlp format is needed:
* One file containing the ontology.
* One or more files containing the data.

#### Example: LUBM Dataset
We used  the EUGen (v0.1b) data generator, provided with the extended LUBM benchmark to generate datasets in .owl format.

The ontology used is LUBM-ex-20.owl.

Both the generator and the ontology are available at: http://www.informatik.uni-bremen.de/~clu/combined/

The queries used are in the file [FILE_NAME]

## Installation

### Module Compilation

#### Data Loader

```bash
cd loadDL
mvn clean compile assembly:single
```
#### Summary Generator

```bash
cd ../summary
mvn clean compile assembly:single
```

#### Query Executor

```bash
cd ../query
mvn clean compile assembly:single
```
### Configuration Setup
Prior to running the script, the following files must be modified to fit your setup: 
* `load.properties`
* `summary.properties`
* `query.properties`

## Usage

### Loading the data:

This script loads the data from the .owl  or .dlp format into a database according to the parameters set in `load.properties`.

```bash
./run_db.sh
```

### Creating the summary :
This script creates a summary table for each concept and each role in the database <DATABASE_NAME>, according to the parameters set in `summary.properties`.

```bash
./run_summary.sh <DATABASE_NAME>
```

### Querying the data:

This script executes the queries from [FILE_NAME] on the database <DATABASE_NAME>, according to the parameters set in `queries.properties`, and generates the performance results.

```bash
./run_queries.sh <DATABASE_NAME>
```
