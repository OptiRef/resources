package fr.optiref.dlsummary;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.Lists;
import me.tongfei.progressbar.ProgressBar;

public class DlQuotient {

	private String dbname ;
	private Connection con;
	private Statement stmt;
	private int nbTables;
	private Set<Integer> dictionary;
	private DisjointUnionSets ds;
	private Properties properties;
	private String outputFile;
	private long summaryTime;
	private String engine;
	private int maxChunk;
	public DlQuotient(Properties _properties, String _dbname) {
		// TODO Auto-generated constructor stub
		this.properties = _properties;
        this.dbname = _dbname;
        String expsPath = System.getProperty("user.dir") + "/"+properties.get("stats.output").toString();
        this.outputFile = String.format("%s/%s.summary.stats.txt", expsPath, this.dbname);
		this.engine = this.properties.getProperty("database.engine");
        summaryTime = 0;
		init();
	}


	public void init() {

        String user = this.properties.getProperty("database.user");
        String password = this.properties.getProperty("database.password");
        String host = this.properties.getProperty("database.host");
        String port = this.properties.getProperty("database.port");

        String url = "";

        try {
            switch (this.engine) {
            case "POSTGRESQL": {
                Class.forName("org.postgresql.Driver");
                url = String.format("jdbc:postgresql://%s:%s/", host, port);
								this.con = DriverManager.getConnection(url + this.dbname + "?characterEncoding=latin1", user, password);
                break;
            }
            case "MYSQL": {
                Class.forName("com.mysql.jdbc.Driver");
                url = String.format("jdbc:mysql://%s:%s/", host, port);
								this.con = DriverManager.getConnection(url + this.dbname + "?characterEncoding=latin1", user, password);
                break;
            }
            case "DB2": {
                Class.forName("com.ibm.db2.jcc.DB2Driver");
                url = String.format("jdbc:db2://%s:%s/", host, port);
				this.con = DriverManager.getConnection(url + this.dbname, user, password);
				// this.con.setAutoCommit(false);
                break;
            }
            default:
                throw new IllegalArgumentException("Unexpected value: " + engine);
            }
            //this.con = DriverManager.getConnection(url + this.dbname + "?characterEncoding=latin1", user, password);
            this.stmt = con.createStatement();

        } catch (Exception e) {
            e.printStackTrace();
        }
        this.nbTables = getNtables();
        long startTime = System.nanoTime();
        dictionary = new HashSet<Integer>();
        fillDictionary();
        int maxValue = dictionary.stream().reduce((n1, n2) -> Math.max(n1, n2)).get() + 1;
        ds = new DisjointUnionSets(maxValue);
        summaryTime += (System.nanoTime() - startTime)/1000000;
        System.out.println("Ntables: "+this.nbTables);
		if(this.engine.equals("DB2")){
			maxChunk = Integer.parseInt(properties.getProperty("database.maxChunk").toString());
		}

    }

	private void update(String query){
		try {
			stmt.executeUpdate(query);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getNtables(){
		String query;
		if(this.engine.contains("DB2")){
			query = "Select tabname as table_name from syscat.tables where tabname like \'TABLE%\' and tabname not like \'%SUM%\' and type = \'T\' ";
			//Select tabname as table_name from syscat.tables where tabname like 'TABLE%' and type = 'T'
		}
		else if(this.engine.contains("POSTGRESQL")){
			query = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' and position('table' in table_name)>0; ";
			//Select tabname as table_name from syscat.tables where tabname like 'TABLE%' and type = 'T'
		}
		else{
			query = String.format("SELECT table_name FROM information_schema.tables WHERE table_schema = '%s' and position('table' in table_name)>0;", this.dbname);

		}


		//String query = String.format("SELECT table_name FROM information_schema.tables WHERE table_schema = '%s' and position('table' in table_name)>0;", this.dbname);
		List<String> tables = new ArrayList<String>();
		String name = "";
		System.out.println("we are in getNtables");
		try {
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
				name = rs.getString(1);
				//System.out.println("name: "+name);
                if(!name.contains("sum")){
					tables.add(name);
				}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
		return tables.size();
	}
	public void fillDictionary() {
        String dictionary_table = this.properties.getProperty("database.dictionary_table_name");
		System.out.println(String.format("Dictionary name : %s", dictionary_table));
        try {
            ResultSet rs = stmt.executeQuery(String.format("select predicate_encoding from %s ", dictionary_table));
            while (rs.next()) {
                this.dictionary.add(rs.getInt(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	public void cluster() throws SQLException {

		String query;
		query = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' and position('table' in table_name)>0; ";
		String t;
		List<String> tables = new ArrayList<String>();

        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()) {
        	t = rs.getString(1);
        	if(t.contains("sum")) {
        		tables.add( t);
        	}
        }


        for(String name : ProgressBar.wrap(tables, "Clustering the tables...")) {
			rs=stmt.executeQuery("select * from "+name +" limit 1");
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnsNumber = rsmd.getColumnCount();
			rs = stmt.executeQuery("select * from "+name);
			rsmd = rs.getMetaData();
			columnsNumber = rsmd.getColumnCount();
			if(columnsNumber == 2) {
				//System.out.println("Role");
				update(String.format("alter table %s add primary key (c0,c1)", name));
				update(String.format("CLUSTER VERBOSE %s USING index_tuple_fs_%s",name, name));
			}else {
				//System.out.println("Concept: "+String.format("alter table %s add primary key (c0)", name));
				update(String.format("alter table %s add primary key (c0)", name));
				update(String.format("CLUSTER VERBOSE %s USING index_param_%s", name, name));
			}
        }
	}
	public void computeQuotient() throws SQLException {

		long startTime = System.nanoTime();
		Integer firstValue = null;
		Integer value = null;

        // for each class, we iterate over their content.
		for(int i=0; i<this.nbTables; i++) {
			ResultSet rs=stmt.executeQuery("select * from table"+i);
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnsNumber = rsmd.getColumnCount();
			//Only concept tables
			if(columnsNumber == 1) {
				//Must be null to say we changed the class :-)
				firstValue = null;
				while (rs.next()) {
					value = rs.getInt(1);
					if (firstValue == null) {
                        // the representative is the first value
                        firstValue = value;
                    } else {
                        // all the others values of the class are equivalent to the representative
                        ds.union(firstValue, value);
                    }
	            }
			}

		}
		summaryTime += (System.nanoTime() - startTime)/1000000;

    }

	public void createConcept(int id) {
		//String query = "";
		String summary_vocabulary = this.properties.getProperty("database.summary_dictionary_table_name");
        String alias = this.properties.getProperty("database.summary_table_alis");


		if(this.engine.contains("DB2")){
			update(String.format("DROP TABLE IF EXISTS table%s%d ;\n",alias, id));

			//if(true)continue;
			update(String.format(
	                "create table table%s%d as (select distinct d.summarynode as c0 from table%d as c, %s as d "
	                        + "where c.c0 = d.graphnode) WITH DATA\n",
	                        alias, id, id, summary_vocabulary));


		}else{
			update(String.format("DROP TABLE IF EXISTS table%s%d CASCADE;\n", alias, id));
			//if(true)continue;
			update(String.format(
	                "create table table%s%d as select distinct d.summarynode as c0 from table%d as c, %s as d "
	                        + "where c.c0 = d.graphnode;\n",
	                        alias, id, id, summary_vocabulary));
		}



		update(String.format("CREATE INDEX index_param_table%s%d ON table%s%d(c0);\n", alias, id, alias, id));
		//return query;
	}

	public void createRole(int id) {
		//String query = "";
		String summary_vocabulary = this.properties.getProperty("database.summary_dictionary_table_name");
        String alias = this.properties.getProperty("database.summary_table_alis");

		if(this.engine.contains("DB2")){
			update(String.format("DROP TABLE IF EXISTS table%s%d ;\n", alias, id));
			update(String.format("create table table%s%d as (select distinct d1.summarynode as c0, d2.summarynode as c1 from table%d as r, %s as d1, %s as d2  "
							+ "where r.c0 = d1.graphnode and r.c1 = d2.graphnode) WITH DATA \n",
							alias, id, id, summary_vocabulary, summary_vocabulary));
		}else{
			update(String.format("DROP TABLE IF EXISTS table%s%d CASCADE;\n", alias, id));
			update(String.format("create table table%s%d as select distinct d1.summarynode as c0, d2.summarynode as c1 from table%d as r, %s as d1, %s as d2  "
							+ "where r.c0 = d1.graphnode and r.c1 = d2.graphnode;\n",
							alias, id, id, summary_vocabulary, summary_vocabulary));
		}
		update(String.format("CREATE INDEX index_firstparam_table%s%d ON table%s%d (c0);\n", alias, id, alias, id));
		update(String.format("CREATE INDEX index_secondparam_table%s%d ON table%s%d (c1);\n", alias, id, alias, id));
		update(String.format("CREATE INDEX index_tuple_fs_table%s%d ON table%s%d (c0,c1);\n", alias, id , alias, id));
		update(String.format("CREATE INDEX index_tuple_sf_table%s%d ON table%s%d (c1,c0);\n", alias, id, alias, id));

		//if(true)continue;



		//return query;
	}

	private void update(List<String> inserts){

		String query;
		String summary_vocabulary = this.properties.getProperty("database.summary_dictionary_table_name");
		for(List<String> batch : ProgressBar.wrap(Lists.partition(inserts, maxChunk), "Storing the dictionary...")){
			query = String.format("INSERT INTO  %s VALUES %s", summary_vocabulary, String.join(",", batch));
			try {
				stmt.executeUpdate(query);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}
	public void createDump() throws SQLException {

    	long startTime;
        String summary_vocabulary = this.properties.getProperty("database.summary_dictionary_table_name");

        List<String> inserts = new ArrayList<String>();


        startTime = System.nanoTime();

        //String query = "";
        //update(String.format("DROP TABLE IF EXISTS %s CASCADE;\n", summary_vocabulary));
		if(this.engine.contains("DB2")){
			update(String.format("DROP TABLE IF EXISTS %s ;\n", summary_vocabulary));
		}else{
			update(String.format("DROP TABLE IF EXISTS %s CASCADE;\n", summary_vocabulary));
		}


        update(String.format("CREATE Table %s(graphnode int NOT NULL, summarynode int NOT NULL);\n", summary_vocabulary));
        update(String.format("CREATE INDEX index_i_graphnode ON %s(graphnode);\n", summary_vocabulary));
        update(String.format("CREATE INDEX index_i_summarynode ON %s(summarynode);\n", summary_vocabulary));
        update(String.format("CREATE INDEX index_summary_tuple_gs ON %s(graphnode, summarynode);\n", summary_vocabulary));
		update(String.format("CREATE INDEX index_summary_tuple_sg ON %s(summarynode,graphnode);\n", summary_vocabulary));
		if(this.engine.equals("MYSQL")){
			update("set autocommit = 0;");
			update("START TRANSACTION;");
		}

        for (int key : ProgressBar.wrap(dictionary, "Storing the dictionary...")) {
			if(this.engine.equals("MYSQL")){
				update(String.format("INSERT INTO %s VALUES(%d,%d)", summary_vocabulary, key, ds.find(key)));
			}else{
				inserts.add(String.format("(%d,%d)",key, ds.find(key)));
			}
        }
		if(this.engine.equals("DB2")){
			//use chunks for db2
			update(inserts);
		}
		else if(this.engine.equals("MYSQL")){
			//use transactions for mysql
			update("COMMIT;");
		}else{
			//use brute force for postgres
			update(String.format("INSERT INTO %s VALUES %s;\n", summary_vocabulary, String.join(",", inserts)));
		}
		summaryTime += (System.nanoTime() - startTime)/1000000;

        // try {
        //     stmt.executeUpdate(query);
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }
		List<Integer> tables = new ArrayList<Integer>();
		for(int i=0; i<this.nbTables; i++) {
			tables.add(i);
		}
		startTime = System.nanoTime();
        for(int i : ProgressBar.wrap(tables, "Storing the tables...")) {
			ResultSet rs=stmt.executeQuery("select * from table"+i +" limit 1");
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnsNumber = rsmd.getColumnCount();
			// query = "";
			//Only concept tables
			if(columnsNumber == 1) {
				createConcept(i);
			}else {
				createRole(i);
			}
			// try {
	        //     stmt.executeUpdate(query);
	        // } catch (Exception e) {
			// 	System.out.println("table"+i);
			// 	System.out.println("columns: "+columnsNumber);
	        //     e.printStackTrace();
	        // }
        }

        summaryTime += (System.nanoTime() - startTime)/1000000;
		System.out.println(String.format("Creating the summary tables Time : %d (ms)",summaryTime));
		if(this.engine.equals("POSTGRESQL")) {
			cluster();
		}

    }

	public void updateStats() {
		String alias = this.properties.getProperty("database.summary_table_alis");
		FileWriter writer = null;
		int nbConcepts=0, nbRoles=0, emptyConcepts=0, emptyRoles=0;
		// for each class, we iterate over their content.
		int nbConstants = 0, nbClasses, nbt = 0;
		int dbTuples = 0, summaryTuples = 0;
		Set<Integer> classes = new HashSet<Integer>();
		for (int key : dictionary) {
			classes.add(ds.find(key));
			nbConstants++;
        }
		nbClasses = classes.size();
		for(int i=0; i<this.nbTables; i++) {
			//System.out.println("Processing table"+i);
			ResultSet rs;
			try {
				rs = stmt.executeQuery("select * from table"+i+" limit 1");
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnsNumber = rsmd.getColumnCount();

				rs = stmt.executeQuery("select count(*) from table"+i);
				if(rs.next()) {
					nbt = rs.getInt(1);
				}
				//Only concept tables
				if(columnsNumber == 1) {
					nbConcepts++;
					if(nbt == 0) {
						emptyConcepts++;
					}
				}else {
					nbRoles++;
					if(nbt == 0) {
						emptyRoles++;
					}
				}

				//System.out.println("has tuples: "+nbt);
				dbTuples += nbt;
				//System.out.println(String.format("select count(*) from table%s%d", alias,i));
				rs = stmt.executeQuery(String.format("select count(*) from table%s%d", alias,i));
				if(rs.next()) {
					nbt = rs.getInt(1);

				}
				//System.out.println("has tuples after summary: "+nbt);
				summaryTuples += nbt;


			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		try {
			writer = new FileWriter(this.outputFile);
			writer.write(String.format("Statistics for data base: %s \n", dbname));
			writer.write(String.format("Number of concepts: %d \n", nbConcepts));
			writer.write(String.format("Number of roles: %d \n", nbRoles));
			writer.write(String.format("Number of empty concepts: %d \n", emptyConcepts));
			writer.write(String.format("Number of empty roles: %d \n", emptyRoles));
			writer.write(String.format("Number of constants: %d \n", nbConstants));
			writer.write(String.format("Number of classes: %d \n", nbClasses));
			writer.write(String.format("Initial data base tuples: %d \n", dbTuples));
			writer.write(String.format("Summary tuples: %d \n", summaryTuples));
			writer.write(String.format("Compression ratio: %,.003f \n", 100.0*summaryTuples/dbTuples));
			writer.write(String.format("Time to create the summary: %d \n", summaryTime));
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void close() {
		try{
			con.close();
		}
		catch(Exception e){
			System.out.println(e);
		}

	}


	public static void main(String[] args) {
		if (args.length == 0) {
            System.out.println("you need to provide a config file and a database name. Ex java -jar dlssummary.jar config.properties");
            System.exit(0);
        } else if (args.length == 2) {
            System.out.println("Computing summary with config file : " + args[0]);
			System.out.println("Computing summary for database : " + args[1]);
            String propertiesPath = System.getProperty("user.dir") + "/" + args[0];
			InputStream input;
			try {
				input = new FileInputStream(propertiesPath);
				Properties prop = new Properties();
		        prop.load(input);
		        System.out.println(prop.get("database.engine"));

				DlQuotient dlquotient = new DlQuotient(prop, args[1]);
				dlquotient.computeQuotient();
				if(prop.get("database.summarize").toString().equals("true")){
					System.out.println("creating the summary ...");
					dlquotient.createDump();
				}

				dlquotient.updateStats();
			} catch (IOException | SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

        }else {
        	System.out.println("you gave too many options");
            System.exit(0);
        }
	}
}
