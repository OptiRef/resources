package fr.optiref.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections4.ListUtils;



import me.tongfei.progressbar.ProgressBar;

/**
 * Load OWL data to RDBMS
 *
 */
public class Loader
{
	private String dlpFilesDirectory;
	private int cptRelation = 0;
	private int cptPredicate = 0;
	//private List<Map.Entry<String,Integer>> relationVocabulary;
	private HashMap<String, Integer> relationVocabulary;
	//	private List<Map.Entry<String,Integer>> predicateVocabulary ;
	private HashMap<String, Integer> predicateVocabulary;
	private Connection con;
	private Statement stmt;
	private String engine;
	private List<String> queries;
	private Map<Integer, List<String>> tablesQueries;
	private int maxChunk;
	String user, password, url, dbname;
	private int duplicate;


	public Loader(Properties props) {

		this.dlpFilesDirectory = props.getProperty("dlp.filesDirectory").toString();
		this.engine = props.getProperty("database.engine").toString();
		maxChunk = Integer.parseInt(props.getProperty("database.maxChunk").toString());

		this.relationVocabulary = new HashMap<String, Integer>();
		this.predicateVocabulary = new HashMap<String, Integer>();


		user = props.getProperty("database.user").toString();
		password = props.getProperty("database.password").toString();
		dbname = props.getProperty("database.name").toString();
		try {
			url = getURL(props.getProperty("database.engine").toString());
			/*if(this.engine.equals("DB2")){
				DB2ConnectionPoolDataSource db2ds = new DB2ConnectionPoolDataSource();
				db2ds.setDatabaseName( dbname );
				db2ds.setServerName( "localhost" );
				db2ds.setDriverType( 4 );
				db2ds.setPortNumber( 50000 );
				//db2ds.setDescription( sDescription );
				db2ds.setUser( user);
				db2ds.setPassword( password );
				db2ds.setAutoCommit(false);
				db2ds.setMaxConnCachedParamBufferSize(1024*1024*1024);
				this.con = db2ds.getPooledConnection().getConnection();

			}else {
				this.con = DriverManager.getConnection(url+dbname, user, password);
			}*/
			this.con = DriverManager.getConnection(url+dbname, user, password);
			this.stmt = con.createStatement();
			duplicate = 0;

		} catch (ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("Please check that you've created the database: "+dbname);
			System.exit(0);
			e.printStackTrace();
		}


	}
	public String getURL(String engine) throws ClassNotFoundException {
		String url;

		System.out.println("We got: "+engine);
		switch (engine) {
		case "POSTGRESQL": {
			Class.forName("org.postgresql.Driver");
			url = "jdbc:postgresql://localhost:5432/";
			break;
		}
		case "MYSQL": {
			Class.forName("com.mysql.jdbc.Driver");
			url = "jdbc:mysql://localhost:3306/";
			break;
		}
		case "DB2": {

			Class.forName("com.ibm.db2.jcc.DB2Driver");
			url = String.format("jdbc:db2://localhost:50000/");
			break;
		}
		default:{

			throw new IllegalArgumentException("Unexpected value: " + engine);
		}

		}
		return url;
	}


	// ################################################################################
	// ################################ CREATE ENCODING ###############################
	// ################################################################################

	private int CreateRelationEncoding(String relationName)
	{

		relationVocabulary.put(relationName, cptRelation);
		cptRelation++;
		return (cptRelation-1);
	}

	public int CreatePredicateEncoding(String predicateName)
	{

		predicateVocabulary.put(predicateName, cptPredicate);
		cptPredicate++;
		return (cptPredicate-1);
	}

	// ################################################################################
	// ############################# WRITING IN DATA DUMP #############################
	// ################################################################################

	public void AddTableInDataDump(int relationEncodingValue,
			boolean binaryRelation)
	{
		String tableName = "table" + relationEncodingValue;
		String columns[] = {"c0","c0", "c1"};

		if(this.engine.contains("DB2")){
			update("DROP TABLE IF EXISTS " + tableName + " ;\n");
		}else{
			update("DROP TABLE IF EXISTS " + tableName + " CASCADE;\n");
		}
		if (binaryRelation)
		{
			if(this.engine.equals("DB2")) {
				update(String.format("CREATE TABLE %s(%s integer not null, %s integer not null,  constraint pk_c0_c1 PRIMARY KEY (%s, %s))", tableName, columns[1], columns[2], columns[1], columns[2] ));
			}else {
				update(String.format("CREATE TABLE %s(%s integer not null, %s integer not null,  PRIMARY KEY (%s, %s))", tableName, columns[1], columns[2], columns[1], columns[2] ));
			}
			update("CREATE INDEX index_"+columns[1]+"_" + tableName + " ON " + tableName + " ("+columns[1]+");\n");
			update("CREATE INDEX index_"+columns[2]+"_" + tableName + " ON " + tableName + " ("+columns[2]+");\n");
			update("CREATE INDEX index_tuple1_" + tableName + " ON " + tableName + " ("+columns[1]+", "+columns[2]+");\n");
			update("CREATE INDEX index_tuple2_" + tableName + " ON " + tableName + " ("+columns[2]+", "+columns[1]+");\n");


		}
		else
		{
			if(this.engine.equals("DB2")) {
				update(String.format("CREATE TABLE %s(%s integer not null , constraint pk_c0 PRIMARY KEY (%s));", tableName, columns[1], columns[1] ));
			}else {
				update(String.format("CREATE TABLE %s(%s integer not null , PRIMARY KEY (%s));", tableName, columns[1], columns[1] ));
			}
			update("CREATE INDEX index_"+columns[0]+"_" + tableName + " ON " + tableName + " ("+columns[0]+");\n");

		}

	}

	public void AddValuesInDataDump(int relationNameEncoded,
			int firstPredicateNameEncoded,
			int secondPredicateNameEncoded)
	{

		List<String> tmp;
		String tableName = "table" + relationNameEncoded;
		String query = "INSERT INTO " + tableName + " VALUES (" + firstPredicateNameEncoded + "," + secondPredicateNameEncoded + ");\n";
		//update(query);
		//update(String.format("INSERT INTO  %s VALUES(?,?) with parameters: \n %d \n %d", tableName, firstPredicateNameEncoded, secondPredicateNameEncoded));
		if(tablesQueries.containsKey(relationNameEncoded)) {
			tmp = tablesQueries.get(relationNameEncoded);

		}else {
			tmp = new ArrayList<String>();
		}
		tmp.add(String.format(" (%d, %d)" , firstPredicateNameEncoded, secondPredicateNameEncoded));
		tablesQueries.put(relationNameEncoded, tmp);

	}

	public void AddValuesInDataDump(int relationNameEncoded,
			int firstPredicateNameEncoded)
	{
		String tableName = "table" + relationNameEncoded;
		List<String> tmp;
		String query = "INSERT INTO " + tableName + " VALUES (" + firstPredicateNameEncoded + ");\n";
		if(tablesQueries.containsKey(relationNameEncoded)) {
			tmp = tablesQueries.get(relationNameEncoded);

		}else {
			tmp = new ArrayList<String>();
		}
		tmp.add(String.format(" (%d)" , firstPredicateNameEncoded));
		tablesQueries.put(relationNameEncoded, tmp);

	}

	// ################################################################################
	// ############################ WRITING IN VOCAB DUMPS ############################
	// ################################################################################

	public void InitialisationOfRelationVocabularyTable()
	{

		if(this.engine.contains("DB2")){
			update("DROP TABLE IF EXISTS relation_vocabulary;\n");
		}else{
			update("DROP TABLE IF EXISTS relation_vocabulary CASCADE;\n");
		}
		update("CREATE TABLE relation_vocabulary (relation_name varchar(700), relation_encoding int);\n");
		update("CREATE INDEX index_relation_name ON relation_vocabulary (relation_name);\n");
		update("CREATE INDEX index_relation_encoding ON relation_vocabulary (relation_encoding);\n");
		update("CREATE UNIQUE INDEX index_relation_tuple ON relation_vocabulary (relation_name, relation_encoding);\n");

	}
	private void update(String query){
		try {
			stmt.executeUpdate(query);
		} catch (Exception e) {
			System.out.println(stmt);
			e.printStackTrace();
		}
	}

	private void update(){
		String tablename, query;
		int key;
		List<String> values;

		int ntables = 0;

		for(Entry<Integer, List<String>> mp: ProgressBar.wrap(tablesQueries.entrySet(), "Loading to db...")){
			key = mp.getKey();
			if(key == -1) {
				tablename = "relation_vocabulary";
			}
			else if(key == -2) {
				tablename = "predicate_vocabulary";
			}else {
				tablename = "table"+key;
			}

			values = mp.getValue();
			if(this.engine.equals("POSTGRESQLL")){
				query = String.format("INSERT INTO  %s VALUES %s", tablename, String.join(",", values));
				try {
					stmt.executeUpdate(query);
				} catch (Exception e) {
					duplicate++;
					System.out.println("Duplicate");

				}
			}else{
				List<List<String> > batchs = ListUtils.partition(values, maxChunk);
				for(List<String> batch : batchs){
					query = String.format("INSERT INTO  %s VALUES %s", tablename, String.join(",", batch));
					try {
						stmt.executeUpdate(query);
					} catch (Exception e) {
						duplicate++;
						System.out.println("Duplicate");

					}

				}
			}
			ntables++;

		}

	}
	public void InitialisationOfPredicateVocabularyTable()
	{

		if(this.engine.contains("DB2")){
			update("DROP TABLE IF EXISTS predicate_vocabulary ;\n");
		}else{
			update("DROP TABLE IF EXISTS predicate_vocabulary CASCADE;\n");
		}
		update("CREATE TABLE predicate_vocabulary (predicate_name varchar(700), predicate_encoding int);\n");
		update("CREATE INDEX index_predicate_name ON predicate_vocabulary (predicate_name);\n");
		update("CREATE INDEX index_predicate_encoding ON predicate_vocabulary (predicate_encoding);\n");
		update("CREATE UNIQUE INDEX index_predicate_tuple ON predicate_vocabulary (predicate_name, predicate_encoding);\n");
	}

	public void AddRelationEncodingInRelationVocabDump(String relationName,
			int relationEncodingValue)
	{
		List<String> tmp;
		String query = "INSERT INTO relation_vocabulary VALUES ('" + relationName + "'," + relationEncodingValue + ");\n";
		if(tablesQueries.containsKey(-1)) {
			tmp = tablesQueries.get(-1);

		}else {
			tmp = new ArrayList<String>();
		}
		tmp.add(String.format(" ('%s', %d)" , relationName, relationEncodingValue));
		tablesQueries.put(-1, tmp);
	}

	public void AddPredicateEncodingInPredicateVocabDump(String predicateName, int predicateEncodingValue)
	{
		List<String> tmp;
		String query = "INSERT INTO predicate_vocabulary VALUES ('" + predicateName + "'," + predicateEncodingValue + ");\n";
		if(tablesQueries.containsKey(-2)) {
			tmp = tablesQueries.get(-2);
		}else {
			tmp = new ArrayList<String>();
		}
		tmp.add(String.format(" ('%s', %d)" , predicateName, predicateEncodingValue));
		tablesQueries.put(-2, tmp);
	}

	// ################################################################################
	// ################################ SEARCH METHODS ################################
	// ################################################################################

	public int SearchRelationInVocabulary(String relationName,
			boolean binaryRelation )
	{

		if(!relationVocabulary.isEmpty()) {
			if(relationVocabulary.containsKey(relationName)) {
				return relationVocabulary.get(relationName);
			}
		}
		int relationEncodingValue = CreateRelationEncoding(relationName);
		AddRelationEncodingInRelationVocabDump(relationName, relationEncodingValue);
		AddTableInDataDump(relationEncodingValue, binaryRelation);

		return relationEncodingValue;
	}

	public int SearchPredicateInVocabulary(String predicateName )
	{


		if(!predicateVocabulary.isEmpty()) {
			if(predicateVocabulary.containsKey(predicateName)) {
				return predicateVocabulary.get(predicateName);
			}
		}

		int predicateEncodingValue = CreatePredicateEncoding(predicateName);
		AddPredicateEncodingInPredicateVocabDump(predicateName, predicateEncodingValue);

		return predicateEncodingValue;
	}

	private void addPredicate(String predicate, HashMap<String, String> prefixesMap) {
		Boolean binaryRelation = false;
		String currentPrefix = "";
		String relationName  = "";
		String currentRel;
		if(!predicate.startsWith("<")) {
			currentPrefix = predicate.substring(0, predicate.indexOf(":")+1).trim();
			relationName = prefixesMap.get(currentPrefix);
			currentRel = predicate.substring(predicate.indexOf(":")+1,predicate.indexOf("("));
		}else {
			currentRel = predicate.substring(1,predicate.indexOf("(")-1);
		}


		relationName += currentRel;
		if(relationName.contains("Disposition")) {
			System.out.println("currentPrefix "+currentPrefix);
			System.out.println(relationName);
		}

		if((predicate.indexOf(")") - predicate.indexOf("(X")) > 3) {
			binaryRelation = true;
		}

		SearchRelationInVocabulary(relationName, binaryRelation);
	}

	public void processOntology(String filepathOnto, HashMap<String, String> prefixesMap) throws IOException {
		File fileOnto = new File(filepathOnto);
		BufferedReader brOnto = new BufferedReader(new FileReader(fileOnto));
		String line = "";

		//Reading the ontology
		try {
			System.out.println("Reading second file.");

			while ((line = brOnto.readLine()) != null) {


				if(line.startsWith("@prefix")) {
					String prefixStr = line.substring(line.indexOf(" ")+1, line.indexOf(":")+1);
					String uriStr = line.substring(line.indexOf("<")+1, line.indexOf(">"));
					if(!prefixesMap.containsKey(prefixStr)) {
						prefixesMap.put(prefixStr,uriStr);
					}
				}

				else if(line.contains(":-") && !line.contains("!")){

					String currentLine = line;
					String firstPred = "";
					String secondPred = "";
					String thirdPred = "";

					firstPred = currentLine.substring(0, currentLine.indexOf(')')+1);

					currentLine = currentLine.substring(currentLine.indexOf(')')+1);

					if(currentLine.charAt(0)==','|| currentLine.charAt(1)==','|| currentLine.charAt(2)==',') {
						currentLine = currentLine.substring(currentLine.indexOf(", ")+2);
						secondPred = currentLine.substring(0, currentLine.indexOf(')')+1);
						currentLine = currentLine.substring(currentLine.indexOf(')')+1);
					}

					currentLine = currentLine.substring(currentLine.indexOf(":- ")+3);
					thirdPred = currentLine;

					if(firstPred!="") {
						addPredicate(firstPred, prefixesMap);
					}
					if(secondPred!="") {
						addPredicate(secondPred, prefixesMap);
					}
					if(thirdPred!="") {
						addPredicate(thirdPred, prefixesMap);
					}
				}
			}
			System.out.println("Finished ontology file.");

		}catch(IOException e){
			e.printStackTrace();


		}

		brOnto.close();
	}

	public void processRDF(String filepath) throws IOException {
		File fileDLP = new File(filepath);
		BufferedReader brDLP = new BufferedReader(new FileReader(fileDLP));
		String line = "";
		String[] triple;
		String s, p, o;
		boolean binaryRelation;
		int relationNameEncoded, firstPredicateNameEncoded, secondPredicateNameEncoded;
		try
		{

			while ((line = brDLP.readLine()) != null)
			{

				if(line.contains("^^")){
					line = line.substring(0, line.indexOf("^^"));
				}

				s = line.substring(line.indexOf("<")+1, line.indexOf(">"));
				line = line.substring(line.indexOf(" ")+1);
				p = line.substring(line.indexOf("<")+1, line.indexOf(">"));
				line = line.substring(line.indexOf(" ")+1);
				if(line.startsWith("<")){
					o = line.substring(line.indexOf("<")+1, line.indexOf(">"));
				}else{
					o = line.substring(0, line.length()-2);
				}
				o = o.replace("'", "");
				//to avoid mysql crying
				if(o.length() >700){
					o = o.substring(0, 500);
				}
				if(p.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")){
					binaryRelation = false;
					// search numeral encoding of the relation name, will create it if doesn't exist
					relationNameEncoded = SearchRelationInVocabulary(o, binaryRelation);
					firstPredicateNameEncoded = SearchPredicateInVocabulary(s);
					AddValuesInDataDump(relationNameEncoded, firstPredicateNameEncoded);

				}else{
					binaryRelation = true;
					// search numeral encoding of the relation name, will create it if doesn't exist
					relationNameEncoded = SearchRelationInVocabulary(p, binaryRelation);
					firstPredicateNameEncoded = SearchPredicateInVocabulary(s);
					secondPredicateNameEncoded = SearchPredicateInVocabulary(o);
					AddValuesInDataDump(relationNameEncoded, firstPredicateNameEncoded, secondPredicateNameEncoded);
				}

			}
		}catch (IOException e)
		{
			e.printStackTrace();

		}
		brDLP.close();
	}
	public void processFiles(String filepathDLP, HashMap<String, String> prefixesMap) throws IOException {

		File fileDLP = new File(filepathDLP);
		BufferedReader brDLP = new BufferedReader(new FileReader(fileDLP));
		String line = "";

		boolean skip = true;
		try
		{

			while ((line = brDLP.readLine()) != null)
			{
				//Process imports

				if(line.toLowerCase().contains("@prefix")) {
					String prefixStr = line.substring(line.indexOf(" ")+1, line.indexOf(":")+1);
					String uriStr = line.substring(line.indexOf("<")+1, line.indexOf(">"));
					prefixesMap.put(prefixStr,uriStr);
				}else if(line.contains("(") && !line.contains("!")){
					if(line.contains("FieldReserve")){
						System.out.println("in process file line "+line);
					}
					int i = 4; // skipping 'ub:'
					String currentLine = line;
					String[] contParse;
					//Added to handle constants correctly
					if(currentLine.contains("PlainLiteral")) {

						currentLine = currentLine.replace("^^<http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral>", "");
						contParse = currentLine.split("\\, ");
						currentLine = contParse[0] +", <"+contParse[1].split("\\)")[0]+">).";

					}
					line = currentLine;
					String currentPrefix;
					String relationName = "";
					String currentRel;

					if(currentLine.contains("http") && currentLine.startsWith("<")){

						relationName = line.substring(line.indexOf("<")+1,line.indexOf(">"));

					}else{
						currentPrefix = line.substring(0, line.indexOf(":")+1);
						relationName = prefixesMap.get(currentPrefix);
						currentRel = line.substring(line.indexOf(":")+1,line.indexOf("("));
						relationName += currentRel;
					}

					i= line.indexOf("(");
					currentLine= line.substring(i);

					boolean binaryRelation = false;
					String secondPredicate = "";

					String firstPredicate = "";

					if(currentLine.contains("<")&& currentLine.contains(">")) {
						//consider it a URI
						String currentSubstr = currentLine.substring(currentLine.indexOf('(')+1, currentLine.lastIndexOf(')'));

						firstPredicate = currentSubstr.substring(currentSubstr.indexOf('<')+1,currentSubstr.indexOf('>'));
						currentSubstr = currentSubstr.substring(currentSubstr.indexOf(">,")+1);

						if(currentSubstr.indexOf(", ")!=-1) {
							binaryRelation = true;
							secondPredicate= currentSubstr.substring(currentSubstr.indexOf('<')+1,currentSubstr.indexOf('>'));
						}

					}else {
						//consider it a prefix
						String currentSubstr = currentLine.substring(currentLine.indexOf('(')+1, currentLine.indexOf(')'));

						String firstPredPref= currentSubstr.substring(0,currentSubstr.indexOf(':')+1);
						String firstPredPrefRel = prefixesMap.get(firstPredPref);
						String firstPredName="";
						String secondPredPref= "";
						String secondPredPrefRel = "";
						String secondPredName="";
						if(currentSubstr.indexOf(", ")==-1) {
							firstPredName = currentSubstr.substring(currentSubstr.indexOf(':')+1);
						}else {
							binaryRelation = true;
							firstPredName = currentSubstr.substring(currentSubstr.indexOf(':')+1,currentSubstr.indexOf(", "));

							currentSubstr= currentSubstr.substring(currentSubstr.indexOf(", ")+2);
							secondPredPref= currentSubstr.substring(0,currentSubstr.indexOf(':')+1);
							secondPredPrefRel=prefixesMap.get(secondPredPref);
							secondPredName = currentSubstr.substring(currentSubstr.indexOf(':')+1);


						}
						firstPredicate = firstPredPrefRel+firstPredName;
						secondPredicate =secondPredPrefRel+secondPredName;
					}

					// search numeral encoding of the relation name, will create it if doesn't exist
					int relationNameEncoded = SearchRelationInVocabulary(relationName, binaryRelation);
					// search numeral encoding of the first predicate name, will create it if doesn't exist
					int firstPredicateNameEncoded = SearchPredicateInVocabulary(firstPredicate);
					if(line.contains("PlainLiteral")) {
						System.out.println(String.format(" We are storing: %s(%s, %s)", relationName, firstPredicate,secondPredicate));
					}
					if (binaryRelation) // if binary relation
					{
						// search numeral encoding of the second predicate name, will create it if doesn't exist
						int secondPredicateNameEncoded = SearchPredicateInVocabulary(secondPredicate);
						AddValuesInDataDump(relationNameEncoded, firstPredicateNameEncoded, secondPredicateNameEncoded);
					}
					else
					{
						AddValuesInDataDump(relationNameEncoded, firstPredicateNameEncoded);
					}
				}
			}

			//System.out.println("Finished.");
		}
		catch (IOException e)
		{
			e.printStackTrace();

		}
		brDLP.close();

	}


	// ################################################################################
	// ################################################################################
	// #################################### MAIN ######################################
	// ################################################################################
	// ################################################################################
	public void startTransaction() {

		try {
			this.con.setAutoCommit(false);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void commit() {

		try {
			this.con.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void convertToSQL(Properties props ) throws Exception
	{
		//System.out.println("Begin ");
		// creating filepaths from main attributes
		String filepathDLP = this.dlpFilesDirectory;
		//System.out.println(this.dlpFilesDirectory);
		String filepathOnto = props.getProperty("database.ontologyDlp").toString(); // ontology dlp file

		//startTransaction();
		if(this.engine.equals("POSTGRES")){
			update("START TRANSACTION;");
		}
		else if(this.engine.equals("MYSQL")) {
			update("SET autocommit=0;");
		}
		//queries =  new ArrayList<String>();
		tablesQueries = new HashMap<Integer, List<String>>();
		// writing first initialisation lines of relation vocabulary dump
		//System.out.println("Initialisation of relation vocabulary table with indexes\n");
		InitialisationOfRelationVocabularyTable();

		// writing first initialisation lines of predicate vocabulary dump
		//System.out.println("Initialisation of predicate vocabulary table with indexes\n");
		InitialisationOfPredicateVocabularyTable();

		// create buffer reader on input data file
		HashMap<String, String> prefixesMap = new HashMap<String, String>();

		if(filepathDLP.endsWith(".dlp")) {
			//startTransaction();
			processFiles(filepathDLP, prefixesMap);
			//commit();

		}
		else if(filepathDLP.endsWith(".nt")){
			System.out.println("Processing the data in RDF format");
			processRDF(filepathDLP);
		}
		else {
			//String folderPath = "18April2021/Base_10MT/";
			File folder = new File(filepathDLP);
			//File[] listOfFiles = folder.listFiles();
			List<File> listOfFiles = Arrays.asList(folder.listFiles());

			for (File file: ProgressBar.wrap(listOfFiles, "Preprocessing files...")) {
				if (file.isFile()) {
					String currentFileName = file.getAbsolutePath();

					//System.out.println();
					//System.out.println();
					//System.out.println(currentFileName);
					//System.out.println();
					//System.out.println();
					//startTransaction();
					processFiles(currentFileName, prefixesMap);
					//commit();
				}
			}
		}
		//startTransaction();
		processOntology(filepathOnto, prefixesMap);
		update();
		if(!this.engine.equals("DB2")){
			update("COMMIT;");
		}else {
			commit();
		}
		if(this.engine.equals("POSTGRESQL")) {
			cluster();
		}

	}

	public void cluster() throws SQLException {

		String query;
		query = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' and position('table' in table_name)>0; ";

		List<String> tables = new ArrayList<String>();

		ResultSet rs = stmt.executeQuery(query);
		while (rs.next()) {
			tables.add( rs.getString(1));
		}


		for(String name : ProgressBar.wrap(tables, "Clustering the tables...")) {
			rs=stmt.executeQuery("select * from "+name +" limit 1");
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnsNumber = rsmd.getColumnCount();
			rs = stmt.executeQuery("select * from "+name);
			rsmd = rs.getMetaData();
			columnsNumber = rsmd.getColumnCount();
			if(columnsNumber == 2) {
				if(name.contains("sum")) {
					update(String.format("CLUSTER VERBOSE %s USING index_tuple_fs_%s",name, name));
				}else {
					update(String.format("CLUSTER VERBOSE %s USING index_tuple1_%s",name ,name));
				}

			}else {
				if(name.contains("sum")) {
					update(String.format("CLUSTER VERBOSE %s USING index_param_%s", name, name));
				}else {
					update(String.format("CLUSTER VERBOSE %s USING index_c0_%s", name, name));
				}

			}
		}
	}
	public void alterDBaddPK() throws SQLException {


		String query;
		if(this.engine.contains("DB2")){
			query = "Select tabname as table_name from syscat.tables where tabname like \'TABLE%\' and type = \'T\' ";
		}
		else if(this.engine.contains("POSTGRESQL")){
			query = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' and position('table' in table_name)>0; ";
		}
		else{
			query = String.format("SELECT table_name FROM information_schema.tables WHERE table_schema = '%s' and position('table' in table_name)>0;", this.dbname);
		}


		//String query = String.format("SELECT table_name FROM information_schema.tables WHERE table_schema = '%s' and position('table' in table_name)>0;", this.dbname);
		List<String> tables = new ArrayList<String>();

		System.out.println("we are in alterDBaddPK");

		ResultSet rs = stmt.executeQuery(query);
		while (rs.next()) {
			tables.add( rs.getString(1));
		}

		System.out.println("Tables: "+tables.size());
		for(String name : ProgressBar.wrap(tables, "Altering the tables...")) {
			rs=stmt.executeQuery("select * from "+name +" limit 1");
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnsNumber = rsmd.getColumnCount();
			System.out.println("Altering table: "+name);
			rs = stmt.executeQuery("select * from "+name);
			rsmd = rs.getMetaData();
			columnsNumber = rsmd.getColumnCount();
			if(columnsNumber == 2) {
				System.out.println("Role");
				update(String.format("alter table %s add primary key (c0,c1)", name));
				if(name.contains("sum")) {
					update(String.format("CLUSTER VERBOSE %s USING index_tuple_fs_%s",name, name));
				}else {
					update(String.format("CLUSTER VERBOSE %s USING index_tuple1_%s",name ,name));
				}

			}else {
				System.out.println("Concept: "+String.format("alter table %s add primary key (c0)", name));
				update(String.format("alter table %s add primary key (c0)", name));
				if(name.contains("sum")) {
					update(String.format("CLUSTER VERBOSE %s USING index_param_%s", name, name));
				}else {
					update(String.format("CLUSTER VERBOSE %s USING index_c0_%s", name, name));
				}

			}
		}
	}

	public int getDuplicate() {
		return duplicate;
	}
	public static void main( String[] args )
	{

		String propertiesPath = System.getProperty("user.dir") + "/" + args[0];
		InputStream input;
		try {
			input = new FileInputStream(propertiesPath);
			Properties prop = new Properties();
			prop.load(input);
			Loader dlLoader = new Loader(prop);
			if(prop.get("database.load").equals("true")) {
				dlLoader.convertToSQL(prop);

				System.out.println("Nb duplicate: "+dlLoader.getDuplicate());
			}else {
				dlLoader.alterDBaddPK();
			}


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
}
