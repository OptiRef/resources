
# Query Optimization for Ontology-Mediated Query Answering
## Requirements & Dependencies

### Dependencies

* Java 14
* Maven
* One or more of the following DBMSs:
  * DB2 (v11.5.5)
  * MySQL Community (v8.0.34)
  * PostgreSQL (v14.2)

### Data
To run OptiRef, data in the .owl or .dlp format is required:
* One file containing the ontology.
* One or more files containing the data.

#### Example: LUBM Dataset
We used  the EUGen (v0.1b) data generator, provided with the extended LUBM benchmark to generate datasets in .owl format.

The ontology used is LUBM-ex-20.owl.

Both the generator and the ontology are available at: http://www.informatik.uni-bremen.de/~clu/combined/
### Queries
The queries used in experiments are in the files:
* `ressources/lubm-cc.queries`
* `ressources/lubm-qa.queries`

#### Example: ***Consistency Checking*** Queries for LUBM

- C0c|C0i(?0) <- lubm:Person(?0), lubm:Organization(?0)

- C1c|C1i(?0) <- lubm:Organization(?0), lubm:Student(?0)

- C2c|C2i(?0) <- lubm:Organization(?0), lubm:Publication(?0)

- C3c|C3i(?0) <- lubm:Professor(?0), lubm:Department(?0)

- C4c|C4i(?0) <- lubm:Professor(?0), lubm:Publication(?0)


#### Example:  ***Query Answering*** Queries for LUBM

- QA0(?0,?2) <- lubm:Student(?0), lubm:takesCourse(?0,?1), lubm:Subj1Course(?1), lubm:teacherOf(?2,?1), lubm:Professor(?2), lubm:headOf(?2,?3), lubm:Subj1Department(?3), lubm:memberOf(?0,?3)

- QA1(?0) <- lubm:Person(?0), lubm:worksFor(?0,?1), lubm:Department(?1), lubm:takesCourse(?0,?2), lubm:Course(?2)

- QA2(?0) <- lubm:Student(?0), lubm:publicationAuthor(?1,?0), lubm:Publication(?1), lubm:teachingAssistantOf(?0,?2), lubm:Course(?2)

- QA3(?0) <- lubm:Faculty(?0), lubm:degreeFrom(?0,?1), lubm:University(?1), lubm:subOrganizationOf(?2,?1), lubm:Subj10Department(?2), lubm:memberOf(?0,?2)

- QA4(?1,?4) <- lubm:Subj3Department(?1), lubm:Subj4Department(?4), lubm:Subj10Professor(?0), lubm:memberOf(?0,?1), lubm:publicationAuthor(?2,?0), lubm:Professor(?3), lubm:memberOf(?3,?4), lubm:publicationAuthor(?2,?3)

- QA5(?0,?1,?2) <- lubm:Professor(?0), lubm:teacherOf(?0,?1), lubm:worksFor(?0,?2), lubm:degreeFrom(?0,"http://www.University870.edu"), lubm:researchInterest(?0,"Research21"), lubm:name(?0,"AssociateProfessor2"), lubm:emailAddress(?0,"AssociateProfessor2@Department1.University0.edu"), lubm:telephone(?0,"xxx-xxx-xxxx")

- QA6(?0,?2) <- lubm:Student(?0), lubm:takesCourse(?0,?1), lubm:Course(?1), lubm:teacherOf(?2,?1), lubm:Faculty(?2), lubm:worksFor(?2,?3), lubm:Subj5Department(?3), lubm:memberOf(?0,?3)

- QA7(?0,?1) <- lubm:Professor(?0), lubm:teacherOf(?0,?1), lubm:degreeFrom(?0,"http://www.University870.edu"), lubm:researchInterest(?0,"Research21"), lubm:name(?0,"AssociateProfessor2"), lubm:telephone(?0,"xxx-xxx-xxxx"), lubm:emailAddress(?0,"AssociateProfessor2@Department1.University0.edu")

- QA8(?0) <- lubm:Faculty(?0), lubm:mastersDegreeFrom(?0,?1), lubm:University(?1), lubm:subOrganizationOf(?2,?1), lubm:Subj10Department(?2), lubm:memberOf(?0,?2)

- QA9(?1,?4) <- lubm:Subj3Department(?1), lubm:Subj4Department(?4), lubm:Subj3Professor(?0), lubm:memberOf(?0,?1), lubm:publicationAuthor(?2,?0), lubm:Subj5Professor(?3), lubm:memberOf(?3,?4), lubm:publicationAuthor(?2,?3)

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
