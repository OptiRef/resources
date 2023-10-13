# Summary Creation Module


## Setup
To execute this module, you must set up the parameters in the file `./summary.properties` from the main directory.

## Build

```bash
mvn clean compile assembly:single
```
## Execute
To generate the summary tables (one table per concept and role) in the database <DATABASE_NAME>, run the following script from the main directory:
```bash
./run_db.sh <DATABASE_NAME>
```
