package edu.optiref.sql;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.Term;
import edu.ntua.isci.common.lp.Tuple;
import fr.optiref.dlquery.Connector;
import fr.optiref.dlquery.QueryDL;

public class SqlConverter {
	private Map<String, String> tablesDictionary;
	private Map<String, String> tablesDictionaryShort;
	private Map<String, String> predicatesDictionary;
	private Map<String, String> predicatesDictionaryShort;

	Connector connector;

	long highestIndex = 0;

	private String arg;
	private String arg1;
	private String arg2;



	public SqlConverter() {
		connector = QueryDL.getConnector();

		setupDictionary();
		try {
			setupPredicates();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		arg=".c0";
		arg1=".c0";
		arg2=".c1";

	}

	private void setupDictionary() {
		tablesDictionary = new HashMap<String, String>();
		tablesDictionaryShort = new HashMap<String, String>();
		try {
			//System.out.println("Connected to PostgreSQL database!");
			Statement statement = connector.getStmt();
			//System.out.println("Reading table names");
			ResultSet resultSet = statement.executeQuery("SELECT * FROM relation_vocabulary");
			while (resultSet.next()) {
				String uri = resultSet.getString("relation_name");
				String encoding = resultSet.getString("relation_encoding");
				//if(uri.contains("npd-v2-ptl"))continue;
				tablesDictionary.put(uri, encoding);
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
		try {
			//System.out.println("Connected to PostgreSQL database!");
			Statement statement = connector.getStmt();
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
		System.out.println();

	}

	public String getPostgresQuery(Clause clause, Map<String, String> prefixes) {
		List<String> selectVariables = clause.getHead().getArguments().toStringList(); // Corresponds to the DLP version of the head variables
		List<String> selectVariablesToSQL; // Corresponds to the SQL version of the head variables
		Map<String, String> fromTables; // Corresponds to the SQL version of the table names
		List<String> whereConstraints; // Corresponds to the SQL version of the constraints

		Map<String, String> atomsToAlias = new HashMap<String, String>();

		List<String> allVariables;

		String selectPart = "";
		String fromPart = "";
		String wherePart = "";
		boolean hasWhere = false;

		//System.out.println(clause.toString());
		//Atoms of the query
		Atom[] queryAtoms= new Atom[clause.bodySize()]; // An array of all the atoms contained in the body of the query
		for (int i = 0; i < clause.bodySize(); i++){
			Atom bodyAtom = clause.getBodyAtomAt(i);//i th atom of the query
			queryAtoms[i]=bodyAtom;//add atom to conjunction
			//System.out.println("Body Atom at "+i+" is: "+bodyAtom.toString());
		}

		// Construction of FROM part -- table names
		fromTables = constructFromTables(queryAtoms, atomsToAlias);

		// Construct SQL version of Select vars
		selectVariablesToSQL = constructSelectVarsSQL(queryAtoms, selectVariables, atomsToAlias);

		// Construct a list of all the variables in dlp version
		allVariables = constructAllVariables(queryAtoms, selectVariables, atomsToAlias);

		// Construct a list of all the where constraints in sql version
		whereConstraints = constructWhereConstraints(queryAtoms, allVariables, atomsToAlias);

		hasWhere  = !whereConstraints.isEmpty();

		//WRITING THE SELECT PART
		selectPart="SELECT  ";
		for(int i=0;i<selectVariablesToSQL.size();i++){
			selectPart+=selectVariablesToSQL.get(i)+" AS h"+i+" , ";
		}
		selectPart = selectPart.substring(0,selectPart.lastIndexOf(" , "));

		//WRITING THE FROM PART
		fromPart=fromPart+"FROM ";
		Iterator<String> itr = fromTables.keySet().iterator();
		while (itr.hasNext()) {
			String val = itr.next();
			String val2 = fromTables.get(val);
			String key = tablesDictionary.get(val2);
			if(key==null) {
				System.out.println(" val : "+val);
				System.out.println(" val2 : "+val2);
				System.out.println(" table name table"+key);
				System.out.println("URI not in the dic, check your query ");
				System.exit(0);
			}

			if(tablesDictionary.containsKey(val2)) {
				fromPart+="table"+(tablesDictionary.get(val2))+" AS "+atomsToAlias.get(val)+" , ";
			}else {
				fromPart+=val2+ " AS "+atomsToAlias.get(val)+" , ";
			}
		}
		//System.exit(0);
		fromPart = fromPart.substring(0,fromPart.lastIndexOf(" , "));

		if(hasWhere){
			wherePart += " WHERE ";
			for(int i = 0; i < whereConstraints.size(); i++){
				wherePart+=whereConstraints.get(i);
				if(i != whereConstraints.size()-1) {
					wherePart+= " AND ";
				}else {
					wherePart+= " ";
				}
			}
		}


		return selectPart+" "+fromPart+" "+wherePart;

	}

	private List<String> constructWhereConstraints(Atom[] queryAtoms, List<String> allVariables, Map<String, String> atomsToAlias) {
		List<String> whereConst = new ArrayList<String>();
		String[] columnName = new String[]{arg,arg1,arg2};

		for(int i=0; i<queryAtoms.length;i++) {

			Tuple args1 = queryAtoms[i].getArguments();
			Term[] term1= args1.getTerms();//term of the atom

			//construction of column name from atom
			String tableName1 = atomsToAlias.get( queryAtoms[i].toString());


			boolean loopCheck = true; //Checking if the atom has variables: true if it has variables
			boolean loopCheck2 = true; //Checking if the atom has constants: true if it has constants
			//The two are not mutually exclusive when the atom has more than one term.

			if(term1.length==1) {
				loopCheck = term1[0].isVariable();
				loopCheck2 = term1[0].isConstant();
			}else {
				if(term1[0].isConstant() && term1[1].isConstant()) {
					loopCheck = false;
				}
				if(term1[0].isVariable() && term1[1].isVariable()) {
					loopCheck2 = false;
				}
			}


			//NEED TO CHECK IF BOTH VARIABLES ARE LINKED BEFRORE
			if(loopCheck) {
				//	The atom has a variable
				//	We compare it with other atoms to find links on this variable

				for(int j=0; j<queryAtoms.length; j++) {
					if(i!=j) {
						Tuple args2 = queryAtoms[j].getArguments();
						Term[] term2= args2.getTerms();//term of the atom

						//construction of column name from atom
						String tableName2 = atomsToAlias.get( queryAtoms[j].toString());

						for(int v1=0; v1<term1.length;v1++) {
							for(int v2=0;v2<term2.length;v2++) {
								if(term1[v1].isVariable() && term2[v2].isVariable()) {
									//both terms are variables
									if(term1[v1].toString().equals(term2[v2].toString())) {

										String constStr = "";
										String constStrOpp = "";
										String col1 = "";
										String col2 = "";

										if(term1.length ==1) {
											//term1 is a concept
											col1 = columnName[0];
										}else {
											//term1 is a role
											col1 = columnName[v1+1];
										}

										if(term2.length ==1) {
											//term1 is a concept
											col2 = columnName[0];
										}else {
											//term1 is a role
											col2 = columnName[v2+1];
										}
										constStr=tableName1+col1;
										constStr+=" = ";
										constStr+=tableName2+col2;

										constStrOpp = tableName2+col2+" = "+tableName1+col1;

										if(!(whereConst.contains(constStrOpp) || whereConst.contains(constStr))) {
											whereConst.add(constStr);
										}


									}
								}



							}
						}

					}

				}
			}
			if (loopCheck2){
				for(int v=0; v<term1.length;v++) {
					if(term1[v].isConstant()) {
						String constStr = "";
						String col = "";
						String constVal = "";
						String tempName = term1[v].toString();
						if(term1.length ==1) {
							//term1 is a concept
							col = columnName[0];
						}else {
							//term1 is a role
							col = columnName[v+1];
						}

						if(predicatesDictionary.containsKey(tempName)) {
							constVal = predicatesDictionary.get(tempName);
						}else if(predicatesDictionary.containsKey(tempName.replace("\"", ""))) {
							constVal = predicatesDictionary.get(tempName.replace("\"", ""));
						}else if(predicatesDictionaryShort.containsKey(tempName.substring(tempName.lastIndexOf("/")+1))){
							constVal=predicatesDictionaryShort.get(tempName.substring(tempName.lastIndexOf("/")+1));
						}else {
							constVal=""+(highestIndex+10);
						}

						constStr+=tableName1+col;
						constStr+=" = ";
						constStr+=constVal;

						whereConst.add(constStr);
					}
				}
			}
		}
		/*System.out.println("whereConst");
		System.out.println(whereConst);
		System.out.println();*/
		return whereConst;
	}

	private List<String> constructAllVariables(Atom[] queryAtoms, List<String> selectVariables, Map<String, String> atomsToAlias) {
		List<String> allVariables = new ArrayList<String>();

		for(int i=0;i<selectVariables.size();i++) {
			allVariables.add(selectVariables.get(i));
		}
		for (int i = 0; i < queryAtoms.length; i++){
			Tuple args = queryAtoms[i].getArguments();
			Term[] term= args.getTerms();//terms of the atom
			for(int j=0;j<term.length;j++) {
				if(!allVariables.contains(term[j].toString())) {
					allVariables.add(term[j].toString());
				}
			}
		}
		return allVariables;
	}

	private List<String> constructSelectVarsSQL(Atom[] queryAtoms, List<String> selectVariables, Map<String, String> atomsToAlias) {
		List<String> selVarsSQL = new ArrayList<String>();

		for(int i=0; i<selectVariables.size();i++) {
			for(int j=0; j<queryAtoms.length; j++) {
				Tuple args = queryAtoms[j].getArguments();
				Term[] term= args.getTerms();//term of the atom

				String tableName = atomsToAlias.get(queryAtoms[j].toString());


				if(queryAtoms[j].isConcept()) {

					if(selectVariables.get(i).equals(term[0].toString())) {
						selVarsSQL.add(tableName+arg);
						break;

					}
				}else {// It's a role
					if(selectVariables.get(i).equals(term[0].toString())) {
						selVarsSQL.add(tableName+arg1);
						break;
					}

					if(selectVariables.get(i).equals(term[1].toString())) {
						selVarsSQL.add(tableName+arg2);
						break;

					}

				}

			}
		}
		return selVarsSQL;
	}

	public Map<String, String> constructFromTables(Atom[] queryAtoms, Map<String, String> atomsToAlias){

		Map<String, Integer> atomCounter = new HashMap<String, Integer>();
		Map<String, String> fromTables = new HashMap<String, String>();
		for (int i = 0; i < queryAtoms.length; i++){
			//construction of column name from atom
			String tableName = queryAtoms[i].getPredicate().toString();
			if(tableName.startsWith("<") && tableName.endsWith(">")){
				tableName= tableName.substring(1,tableName.length()-1);
			}
			else if(tableName.startsWith("<")){
				tableName= tableName.substring(1,tableName.length());
			}
			else {
				tableName= tableName.substring(0,tableName.length()-1);
			}

			if(atomCounter.containsKey(queryAtoms[i].getPredicate().toString())) {
				int temp = atomCounter.get(queryAtoms[i].getPredicate().toString());

				atomCounter.remove(queryAtoms[i].getPredicate().toString());
				atomCounter.put(queryAtoms[i].getPredicate().toString(), temp+1);
			}else {
				atomCounter.put(queryAtoms[i].getPredicate().toString(), 1);
			}
			String finalename;
			if(tableName.contains("#")) {
				finalename = tableName.split("#")[1];
			}else {
				finalename = tableName.substring(tableName.lastIndexOf("/")+1, tableName.length()-1);
			}
			atomsToAlias.put(queryAtoms[i].toString(), finalename+"_"+atomCounter.get( queryAtoms[i].getPredicate().toString()));

			fromTables.put(queryAtoms[i].toString(), tableName);


		}
		return fromTables;
	}

}
