
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
To run OptiRef, data in the .owl or .dlp format is required:
* One file containing the ontology.
* One or more files containing the data.

#### Example: LUBM Dataset
We used  the EUGen (v0.1b) data generator, provided with the extended LUBM benchmark to generate datasets in .owl format.

The ontology used is LUBM-ex-20.owl.

Both the generator and the ontology are available at: http://www.informatik.uni-bremen.de/~clu/combined/

The queries used are in the file [FILE_NAME]

## Installation

The following modules must be compiled separately as follows:

### Data Loader

```bash
cd loadDL
mvn clean compile assembly:single
```
### Summary Generator

```bash
cd ../summary
mvn clean compile assembly:single
```

### Query Executor

```bash
cd ../query
mvn clean compile assembly:single
```
## Configuration Setup
Prior to running the scripts, the following files must be modified to fit your setup: 
* `load.properties`
* `summary.properties`
* `query.properties`

## Usage

### Loading the data:

First, to load the data from the .owl  or .dlp format into a database, run: 

```bash
./run_db.sh
```
This script uses the parameters set in `load.properties`.


### Creating the summary :
Next, to generate the summary tables (one table per concept and role) in the database <DATABASE_NAME>, run:

```bash
./run_summary.sh <DATABASE_NAME>
```
This script uses the parameters set in `summary.properties`.

### Querying the data:
Finally, to execute the queries found in [FILE_NAME] on the database <DATABASE_NAME>, and generate the results, run:
```bash
./run_queries.sh <DATABASE_NAME>
```
This script uses the parameters set in `queries.properties`.
