# Ontological Data Loading Module


## Setup
To execute this module, you must set up the parameters in the file `./load.properties` from the main directory.

## Build

```bash
mvn clean compile assembly:single
```

## Execute
To load the data from the .owl or .dlp format into a database, run the following script from the main directory:
```bash
./run_db.sh <DATABASE_NAME>
```
