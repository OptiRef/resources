# Query Module

## Setup
To execute this module, you must set up the parameters in the file `./queries.properties`  from the main directory.

## Build

```bash
mvn clean compile assembly:single
```

## Execute 
To execute the queries found in [FILE_NAME] on the database <DATABASE_NAME>, and generate the results, run the following script from the main directory:
```bash
./run_queries.sh <DATABASE_NAME>
```
