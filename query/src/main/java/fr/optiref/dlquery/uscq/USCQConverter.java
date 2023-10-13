package fr.optiref.dlquery.uscq;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import fr.optiref.dlquery.Connector;
import fr.optiref.dlquery.DL2SQL;
import fr.optiref.dlquery.QueryDL;


public class USCQConverter {
	private static Map<String, String> tablesDictionary;
	private static Map<String, String> tablesDictionaryShort;
	private static Map<String, String> predicatesDictionary;
	private static Map<String, String> predicatesDictionaryShort;
	private Properties properties;
	static long highestIndex = 0;
	private String querypath;

	static String columnsEncodings[] = {".c0",".c0",".c1"};
	static ArrayList<String> rolesList;

	Connector connector;
	static USCQConverter myUnique = null;
	public USCQConverter(Properties _properties) {

		properties = _properties;
		querypath    = System.getProperty("user.dir") + "/"+properties.get("compact.queryDir").toString();
		connector = QueryDL.getConnector();
		setupDictionary();
		try {
			setupPredicates();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static USCQConverter getInstace(Properties _properties) {

		if(myUnique == null) {
			myUnique = new USCQConverter(_properties);
		}
		return myUnique;
	}

	public String getUSCQ(String query) {

		//String inputFileName = querypath+"/"+queryName+".sql";
		//System.out.println("query file: "+inputFileName);
		CompactQuery cqeury;
		String queryOuptut = "";
		try {
			cqeury = new CompactQuery(query);
			queryOuptut = convertCompactToPsql(cqeury);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}




		return queryOuptut;
	}

	private void setupDictionary() {

		tablesDictionary = new HashMap<String, String>();
		tablesDictionaryShort = new HashMap<String, String>();
		try(Connection connection = DriverManager.getConnection(connector.getHost(), connector.getUser(), connector.getPassword())) {
			//System.out.println("Connected to PostgreSQL database!");
			Statement statement = connection.createStatement();
			//System.out.println("Reading table names");
			ResultSet resultSet = statement.executeQuery("SELECT * FROM relation_vocabulary");
			while (resultSet.next()) {
				String uri = resultSet.getString("relation_name");
				String encoding = resultSet.getString("relation_encoding");
				tablesDictionary.put(uri.trim(), encoding);
				tablesDictionaryShort.put(uri.substring(uri.lastIndexOf('#')+1).trim(), encoding);
				if(Integer.parseInt(encoding.trim())>highestIndex) {
					highestIndex = Integer.parseInt(encoding.trim());
				}
			}

		}catch(SQLException e){
			System.out.println("Connection failure.");
			e.printStackTrace();
		}


	}
	private void setupPredicates() throws IOException {

		predicatesDictionary = new HashMap<String, String>();
		predicatesDictionaryShort = new HashMap<String, String>();
		try(Connection connection = DriverManager.getConnection(connector.getHost(), connector.getUser(), connector.getPassword())) {
			//System.out.println("Connected to PostgreSQL database!");
			Statement statement = connection.createStatement();
			//System.out.println("Reading table names");
			ResultSet resultSet = statement.executeQuery("SELECT * FROM predicate_vocabulary");
			while (resultSet.next()) {
				String value = resultSet.getString("predicate_name");
				String encoding = resultSet.getString("predicate_encoding");


				predicatesDictionary.put(value.trim(), encoding);
				predicatesDictionaryShort.put(value.substring(value.lastIndexOf('/')+1).trim(), encoding);

				if(Integer.parseInt(encoding.trim())>highestIndex) {
					highestIndex = Integer.parseInt(encoding.trim());
				}

			}

		}catch(SQLException e){
			System.out.println("Connection failure.");
			e.printStackTrace();
		}

	}



	private String viewToPSQL(View view) {
		String viewStr = "";
		viewStr = view.getName();
		viewStr += " AS (";
		ArrayList<SelectQuery> selectStatements = view.getSelectQueries();
		viewStr+= selectQueryToPSQL(selectStatements.get(0));
		for(int i=1;i<selectStatements.size();i++) {
			viewStr+= " UNION ";
			viewStr+= selectQueryToPSQL(selectStatements.get(i));
		}
		viewStr+=")";

		return viewStr;
	}
	private String selectQueryToPSQL(SelectQuery query) {
		String queryStr="SELECT ";
		String tempTable = "";
		String tempVar = "";
		String tempTableEnc = "";
		Variable currentVar = query.selectVariables[0];
		tempTable = currentVar.getTableName();

		tempVar = currentVar.getColName();


		queryStr+=tempTable+"."+tempVar+" ";
		for(int i=1; i< query.selectVariables.length;i++) {
			currentVar = query.selectVariables[i];
			tempTable = currentVar.getTableName();
			tempVar = currentVar.getColName();
			queryStr+=", "+tempTable+"."+tempVar+" ";

		}

		//System.exit(0);
		queryStr+=" FROM ";

		tempTable = "";
		tempTableEnc="";
		tempTable ="";

		Map<String, String> fromTableAliasesMap =  query.fromTableAliases;
		Iterator<String> itr = fromTableAliasesMap.keySet().iterator();
		int count=0;
		while (itr.hasNext()) {
			String val = itr.next();
			//System.out.println(val+" "+fromTableAliasesMap.get(val));
			String encoding = tablesDictionary.get(fromTableAliasesMap.get(val));
			//System.out.println("table"+encoding);
			if(encoding != null) {
				tempTableEnc = "table"+encoding;
			}else {
				tempTableEnc = val;
			}
			if(count==0) {
				queryStr+=tempTableEnc+" AS "+val;
			}else {
				queryStr+=", "+tempTableEnc+" AS "+val;
			}

			count++;
		}

		if(query.hasWherePart) {
			queryStr+="\n WHERE \n";
			ArrayList<Constraint> constraintsList = query.getConstraints();

			for(int i=0; i<constraintsList.size();i++) {

				Constraint currentConst = constraintsList.get(i);
				Variable first = currentConst.getFirstVar();
				String tempConst = "  ";
				String tempVal = first.getTableName();
				tempVar = first.getColName();
				tempConst=tempVal;

				tempConst+="."+tempVar;
				tempConst+=  currentConst.getSign();

				//Right SIDE
				if(currentConst.hasDoubleVars()) {
					Variable second = (Variable) currentConst.getSecondVar();
					tempVal = second.getTableName();
					tempVar = second.getColName();
					tempConst+=tempVal+"."+tempVar;
				}else {
					String constantVal = currentConst.getSecondVar()+"";

					constantVal = constantVal.replace("\'", "");
					constantVal = constantVal.replace("\"", "");


					if(predicatesDictionary.containsKey(constantVal)) {
						tempConst += predicatesDictionary.get(constantVal);
					}else if(predicatesDictionaryShort.containsKey(constantVal.substring(constantVal.lastIndexOf("/")+1))){
						tempConst+=predicatesDictionaryShort.get(constantVal.substring(constantVal.lastIndexOf("/")+1));
					}else {
						tempConst+=""+(highestIndex+10);

					}



				}
				queryStr+=tempConst;
				if(i!=constraintsList.size()-1) {
					queryStr+=" AND \n";
				}

			}
		}




		return queryStr;
	}

	public  String convertCompactToPsql(CompactQuery query) {
		String queryStr = "";

		String viewsStr="WITH ";
		View currentView = query.getViews().get(0);
		viewsStr+= viewToPSQL(currentView);
		if(query.getViews().size()>1) {
			for(int i=1;i<query.getViews().size();i++) {
				currentView = query.getViews().get(i);
				viewsStr+= ", \n--##\n"+ viewToPSQL(currentView);
			}

		}
		queryStr+=viewsStr;
		queryStr+="\n--#####\n";
		queryStr+=mainSelectQueryToPSQL(query.getMainQuery());

		return queryStr;

	}

	private  String mainSelectQueryToPSQL(ArrayList<SelectQuery> mainQueries) {
		String queryStr="";
		queryStr+= mainSelectQueryToPSQL(mainQueries.get(0));

		for(int i=1;i<mainQueries.size();i++) {
			queryStr+= "\n UNION \n\n";
			queryStr+= mainSelectQueryToPSQL(mainQueries.get(i));
		}


		return queryStr;

	}

	private String mainSelectQueryToPSQL(SelectQuery mainQuery) {
		String queryStr="SELECT DISTINCT ";
		String tempTable = "";
		String tempVar = "";
		String tempTableEnc = "";
		String tempVarEnc = "";
		Variable currentVar = mainQuery.selectVariables[0];
		tempTable = currentVar.getTableName();
		Map<String, String> fromTableAliasesMap =  mainQuery.fromTableAliases;


		tempVar = currentVar.getColName();
		String encoding =  tablesDictionary.get(currentVar.getUri());
		//System.out.println("uri : "+currentVar.getUri());
		//System.out.println("table"+encoding);
		if(encoding != null) {
			tempTableEnc = "table"+encoding;
			if(tempVar.equals("c0")) {
				tempVarEnc = columnsEncodings[0];
			}else {
				tempVarEnc = columnsEncodings[2];
			}
		}else {
			tempTableEnc=tempTable;
			tempVarEnc="."+tempVar;
		}

		queryStr+=tempTableEnc+tempVarEnc+" as c0 ";


		for(int i=1; i< mainQuery.selectVariables.length;i++) {
			currentVar = mainQuery.selectVariables[i];
			//System.out.println("uri : "+currentVar.getUri());
			tempTable = currentVar.getTableName();
			tempVar = currentVar.getColName();
			encoding = tablesDictionary.get(currentVar.getUri());
			if(encoding != null) {
				tempTableEnc = "table"+encoding;
				if(tempVar.equals("c0")) {
					tempVarEnc = columnsEncodings[0];
				}else {
					tempVarEnc = columnsEncodings[2];
				}
			}else {
				tempTableEnc=tempTable;
				tempVarEnc="."+tempVar;
			}

			queryStr+=", "+tempTableEnc+tempVarEnc+" as c"+i+" ";

		}
		queryStr+="\n FROM ";

		tempTable = "";
		tempTableEnc="";
		tempTable ="";

		Iterator<String> itr = fromTableAliasesMap.keySet().iterator();
		int count=0;

		//System.out.println("in the from clause ==========>\n");
		while (itr.hasNext()) {
			String val = itr.next();
			//System.out.println(val+" "+fromTableAliasesMap.get(val));
			encoding = tablesDictionary.get(fromTableAliasesMap.get(val));
			if(encoding != null) {
				tempTableEnc = "table"+encoding;
				if(count==0) {
					queryStr+=tempTableEnc +" AS "+val;
				}else {
					queryStr+=", "+tempTableEnc +" AS "+val;
				}

			}else {
				tempTableEnc = fromTableAliasesMap.get(val);
				if(count==0) {
					queryStr+=tempTableEnc+" AS "+val;
				}else {
					queryStr+=", "+tempTableEnc+" AS "+val;
				}
			}
			count++;
		}
		if(mainQuery.hasWherePart) {
			queryStr+="\n WHERE \n";
			ArrayList<Constraint> constraintsList = mainQuery.getConstraints();

			for(int i=0; i<constraintsList.size();i++) {

				Constraint currentConst = constraintsList.get(i);
				Variable first = currentConst.getFirstVar();
				String tempConst = "  ";
				String tempVal = first.getTableName();
				tempVar = first.getColName();

				tempConst+=tempVal;
				tempVarEnc="."+tempVar;

				tempConst+=tempVarEnc;
				tempConst+=  currentConst.getSign();

				//Right SIDE
				if(currentConst.hasDoubleVars()) {
					Variable second = (Variable) currentConst.getSecondVar();
					tempVal = second.getTableName();
					tempVar = second.getColName();
					tempConst+=tempVal+"."+tempVar;

				}else {
					String constantVal = currentConst.getSecondVar()+"";
					//System.out.println(" Cond "+i+" "+constantVal);
					constantVal = constantVal.replace("\'", "");
					//constantVal = constantVal.replace("\"", "");
					//System.out.println(" Cond "+i+" "+constantVal);
					if(predicatesDictionary.containsKey(constantVal)) {
						tempConst += predicatesDictionary.get(constantVal);
					}else if(predicatesDictionaryShort.containsKey(constantVal.substring(constantVal.lastIndexOf("/")+1))){
						tempConst+=predicatesDictionaryShort.get(constantVal.substring(constantVal.lastIndexOf("/")+1));
					}else {
						tempConst += highestIndex+"";
						predicatesDictionary.put(constantVal, highestIndex+"");
						predicatesDictionaryShort.put(constantVal.substring(constantVal.lastIndexOf("/")+1), highestIndex+"");
						highestIndex++;
						if(properties.get("debugger.log_level").toString().equals("DEBUG")){
							System.out.println(String.format("Constant %s not in the DB", constantVal));
						}

						//System.exit(0);
					}

				}
				queryStr+=tempConst;
				if(i!=constraintsList.size()-1) {
					queryStr+=" AND \n";
				}

			}
		}
		//		String test = "\n"
		//				+ "SELECT \"takesCourse_1\".c0, \"teacherOf_3\".c0 "
		//				+ "FROM \"takesCourse\" AS  \"takesCourse_1\","
		//				+ " \"teacherOf\" AS  \"teacherOf_3\", disj_0_0,"
		//				+ " disj_0_2,"
		//				+ " disj_0_4,"
		//				+ " disj_0_5,"
		//				+ " disj_0_6, "
		//				+ "disj_0_7 "
		//				+ "WHERE disj_0_5.c1 = disj_0_6.c0 "
		//				+ "AND disj_0_5.c1 = disj_0_7.c1 "
		//				+ "AND \"takesCourse_1\".c0 = disj_0_0.c0 "
		//				+ "AND \"takesCourse_1\".c0 = disj_0_7.c0 "
		//				+ "AND \"takesCourse_1\".c1 = \"teacherOf_3\".c1 "
		//				+ "AND \"takesCourse_1\".c1 = disj_0_2.c0 "
		//				+ "AND \"teacherOf_3\".c0 = disj_0_4.c0 "
		//				+ "AND \"teacherOf_3\".c0 = disj_0_5.c0;";
		return queryStr;
	}
}
