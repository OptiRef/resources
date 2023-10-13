package fr.lri.lahdak.cqapri.db;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.Term;
import edu.ntua.isci.common.lp.Tuple;
import edu.optiref.sql.DbConnectionParams;
import fr.optiref.dlquery.Connector;
import fr.optiref.dlquery.QueryDL;


public class CQ2SQL{
	protected Map<Atom, List<Integer>> asrt;
	protected Map<String, List<Integer>> asrtString;
	protected List<String> selectpart;
	private List<Integer> selectansvars;

	private boolean retrieval;
	private boolean usepriority;

	boolean encodedtriples;

	long highestIndex = 0;

	private String arg;
	private String arg1;
	private String arg2;
	private Map<String, String> tablesDictionary;
	private Map<String, String> tablesDictionaryShort;
	private Map<String, String> predicatesDictionary;
	private Map<String, String> predicatesDictionaryShort;
	private DbConnectionParams dbCnx;
	private Connector connector;



	public CQ2SQL(boolean priority, boolean eval, boolean encodedData) {
		retrieval= eval;
		usepriority=priority;
		encodedtriples=encodedData;

		//dbCnx = new DbConnectionParams();
		connector = QueryDL.getConnector();
		setupDictionary();
		setupPredicates();


		if(encodedtriples){
			arg=".s";
			arg1=".s";
			arg2=".o";
		}
		else{
			arg=".param";
			arg1=".firstparam";
			arg2=".secondparam";
		}

//		arg=".c0";
//		arg1=".c0";
//		arg2=".c1";

		//		else{
		//			arg=".term";
		//			arg1=".term1";
		//			arg2=".term2";
		//		}

	}

	private void setupDictionary() {
		tablesDictionary = new HashMap<String, String>();
		tablesDictionaryShort = new HashMap<String, String>();
		try(Connection connection = DriverManager.getConnection(connector.getHost(), connector.getUser(), connector.getPassword())) {
			System.out.println("Connected to PostgreSQL database!");
			Statement statement = connection.createStatement();
			System.out.println("Reading table names");
			ResultSet resultSet = statement.executeQuery("SELECT * FROM public.relation_vocabulary");
			while (resultSet.next()) {
				String uri = resultSet.getString("relation_name");
				String encoding = resultSet.getString("relation_encoding");

				//                System.out.println("relation_name: "+uri);
				//                System.out.println("relation_encoding: "+encoding);

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
		/*Iterator<String> itr = tablesDictionaryShort.keySet().iterator();
		while (itr.hasNext()) {
			String val = itr.next();
			System.out.println(val+" "+tablesDictionaryShort.get(val));
		}*/

	}
	private void setupPredicates(){
		predicatesDictionary = new HashMap<String, String>();
		predicatesDictionaryShort = new HashMap<String, String>();

		try(Connection connection = DriverManager.getConnection(connector.getHost(), connector.getUser(), connector.getPassword())) {
			System.out.println("Connected to PostgreSQL database!");
			Statement statement = connection.createStatement();
			System.out.println("Reading table names");
			ResultSet resultSet = statement.executeQuery("SELECT * FROM public.predicate_vocabulary");
			while (resultSet.next()) {
				String value = resultSet.getString("predicate_name");
				String encoding = resultSet.getString("predicate_encoding");

				//                System.out.println("relation_name: "+uri);
				//                System.out.println("relation_encoding: "+encoding);

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
		/*System.out.println("Predicates Dictionary: ");
		Iterator<String> itr = predicatesDictionary.keySet().iterator();
		while (itr.hasNext()) {
			String val = itr.next();
			System.out.println(val+" "+predicatesDictionary.get(val));

		}*/

	}

	public String getQuery(Clause clause) throws IOException {
		//Atoms of the query
		Atom[] queryAtoms= new Atom[clause.bodySize()]; // An array of all the atoms contained in the body of the query
		for (int i = 0; i < clause.bodySize(); i++){
			Atom bodyAtom = clause.getBodyAtomAt(i);//i th atom of the query
			queryAtoms[i]=bodyAtom;//add atom to conjunction
			//System.out.println("Body Atom at "+i+" is: "+bodyAtom.toString());
		}

		//Variables of the head (answers)
		Tuple queryAnswerVars=clause.getHead().getArguments();
		Tuple queryAnswerVars2=clause.getHead().getArguments();
		selectansvars= new ArrayList<Integer>();
		for(int i=0; i<queryAnswerVars.size();i++){//prepare array var->corresponding column
			selectansvars.add(-1);
		}
		//System.out.println("queryAnswerVars "+queryAnswerVars);

		//construction of frompart and selectpart and vars ((term) -> list of predicate in which it appears) and asrt (atom->[clmn of vars]) (for now, atom[0,0] or atom[0,0,0]
		List<String> frompart= new ArrayList<String>();
		selectpart=new ArrayList<String>();

		Map<Term, List<String>> vars= new HashMap<Term, List<String>>();//vars ((term) -> list of predicate in which it appears)

		asrt=new HashMap<Atom, List<Integer>>();//atom->[clmn of vars]
		asrtString=new HashMap<String, List<Integer>>();//atom->[clmn of vars]

		int plcounter=0;//for alias name
		int column=1;//counter: number of column for asrt
		constructSelectVarsSQL(queryAtoms,clause.getHead().getArguments().toStringList());

		for (int i = 0; i < queryAtoms.length; i++){

			Tuple args = queryAtoms[i].getArguments();
			Term[] term= args.getTerms();//term of the atom

			//construction of column name from atom
			String tableName = queryAtoms[i].getPredicate().toString();


			//MODIF ENCODAGE
			if(tableName.contains("#")){//case without encoding
				int ibegin=tableName.indexOf('#');
				if (!tableName.startsWith("<")) {
					tableName = "<" + tableName + ">";
				}
				int iend=tableName.length();
				tableName= tableName.substring(ibegin+1,iend-1);
			}

			//construction of frompart
			if  (frompart.contains(tableName)){//pl=column name, construction of alias
				//				frompart.add(tableName+" "+tableName+plcounter);
				//				frompart.add(tableName);
				tableName=tableName+plcounter;
				plcounter++;
			}else{
				frompart.add(tableName);
			}

			//selectpart and construction of vars ((term) -> list of predicate in which it appears) and asrt (atom->[clmn of vars])

			// args represents the list of variables/const in an atom
			// ex: degreeFrom(?0,?1) has ?0 and ?1 as args
			// ex: Professor(?0) has ?0 as args

			if (args.size() == 1) {
				//concept
				if (vars.get(term[0])!=null){
					//this term has already been met (in another atom for instance)
					List<String> predlist=vars.get(term[0]);
					predlist.add(tableName+arg);
					vars.put(term[0], predlist);

					List<Integer> vlist= new ArrayList<Integer>();//will contain column nb corresponding to the term of the atom
					String firstpred=vars.get(term[0]).get(0);//first predicate in which the term appears
					int col=-1;//will contain the number of the column corresponding to the term
					if(firstpred.contains(arg2)){
						//term is the second term of firstpredicate
						firstpred=firstpred.substring(0, firstpred.indexOf(arg2));
						col=asrtString.get(firstpred).get(1);
					} else if(firstpred.contains(arg1)){
						//term is the first term of firstpredicate
						firstpred=firstpred.substring(0, firstpred.indexOf(arg1));
						col=asrtString.get(firstpred).get(0);
					}
					else if(firstpred.contains(arg)){
						//term is the first term of firstpredicate
						firstpred=firstpred.substring(0, firstpred.indexOf(arg));
						col=asrtString.get(firstpred).get(0);
					}
					vlist.add(col);

					if(usepriority){
						//add priority column
						selectpart.add(tableName+".priority");
						vlist.add(column);
						column++;
					}
					//System.out.println("IF "+tableName);
					asrt.put(queryAtoms[i], vlist);
					asrtString.put(tableName, vlist);
				}else{
					//first time the term is met-> add to select and new column
					if(queryAnswerVars2.toString().contains(term[0].toString())) {
						List<String> predlist=new ArrayList<String>();
						predlist.add(tableName+arg);
						selectpart.add(tableName+arg);
						//System.out.println("ELSE "+tableName+arg);
						vars.put(term[0], predlist);

						List<Integer> vlist= new ArrayList<Integer>();//will contain column nb corresponding to the term of the atom
						vlist.add(column);//new column
						column++;
						if(usepriority){
							//add priority column
							selectpart.add(tableName+".priority");
							vlist.add(column);
							column++;
						}
						asrt.put(queryAtoms[i], vlist);
						asrtString.put(tableName, vlist);
					}
				}
			}else{
				//role
				List<Integer> vlist;
				if(asrtString.get(tableName)!=null){//for second term
					vlist=asrtString.get(tableName);
				}else{
					vlist= new ArrayList<Integer>();
				}
				//System.out.println("ROLE TERM LIST "+Arrays.deepToString(term));
				//System.out.println(args);
				for (int v=0; v< term.length; v++){
					//same as before but for role
					if (vars.get(term[v])!=null ){
						//this term has already been met
						List<String> predlist=vars.get(term[v]);
						String numterm=(v==0)? arg1:arg2;
						predlist.add(tableName+numterm);
						vars.put(term[v], predlist);

						String firstpred=vars.get(term[v]).get(0);
						int col=-1;
						if(firstpred.contains(arg2)){
							firstpred=firstpred.substring(0, firstpred.indexOf(arg2));
							col=asrtString.get(firstpred).get(1);
						}else if(firstpred.contains(arg1)){
							firstpred=firstpred.substring(0, firstpred.indexOf(arg1));
							col=asrtString.get(firstpred).get(0);
						}
						else if(firstpred.contains(arg)){
							firstpred=firstpred.substring(0, firstpred.indexOf(arg));
							col=asrtString.get(firstpred).get(0);
						}
						vlist.add(col);
					}else{
						//first time the term is met
						//System.out.println("queryAnswerVars.toString() _"+queryAnswerVars.toString()+"_");
						//System.out.println("queryAnswerVars.toString() ARR _"+Arrays.deepToString(queryAnswerVars.toStringArray())+"_");
						//System.out.println("term[v].toString() _"+term[v].toString()+"_");
						if( queryAnswerVars2.toString().contains(term[v].toString())) {
							//System.out.println("YESS");


							List<String> predlist=new ArrayList<String>();
							String numterm=(v==0)? arg1:arg2;
							predlist.add(tableName+numterm);
							selectpart.add(tableName+numterm);
							//System.out.println("ELSE2 "+tableName+numterm);
							vars.put(term[v], predlist);

							vlist.add(column);//new column
							column++;
							asrtString.put(tableName, vlist);
							asrt.put(queryAtoms[i], vlist);
						}
					}
				}
				if(usepriority){
					//add priority column
					selectpart.add(tableName+".priority");
					vlist.add(column);
					column++;
				}
				asrtString.put(tableName, vlist);
				asrt.put(queryAtoms[i], vlist);
			}
		}


		//Construct where part
		List<String> wherepart=new ArrayList<String>();
		Set<Term> termset=vars.keySet();//all terms of the query
		Term[] termtable= new Term[termset.size()];
		termtable=termset.toArray(termtable);//all terms of the query (array)

		int where =0;//>0 if there is a where part: same variable in several atoms or constant

		for (int vt=0; vt<termtable.length; vt++){//for each term of query
			Term t= termtable[vt];

			if (vars.get(t)!=null){
				//construct WHERE part
				if (t.isVariable() && vars.get(t).size()>1){
					//t is a variable appearing in several atoms
					where++;
					List<String> vlist= vars.get(t);	//list of atoms in which t appears
					for (int i=0; i<vlist.size()-1;i++){
						wherepart.add(vlist.get(i)+" = "+vlist.get(i+1));//table1.term=table2.term
					}
				}else if (t.isConstant()){
					where++;
					String indi=t.toString();//const
					List<String> vlist= vars.get(t);//list of atoms in which t appears
					for (int i=0; i<vlist.size();i++){
						wherepart.add(vlist.get(i)+" = "+ indi);//table.term=const
					}
				}
			}
		}

		if(retrieval){
			//construct selectansvars: maps answer variables to columns
			for (int vt=0; vt<termtable.length; vt++){//for each term of query
				Term t= termtable[vt];
				for (int j=0; j<queryAnswerVars.size(); j++){
					if (queryAnswerVars.getTerm(j).toString().equals(t.toString())){
						//t is in j th position in answer variables
						String pred=vars.get(t).get(0);//first predicate in which the term appear
						if(pred.contains(arg2)){
							pred=pred.substring(0, pred.indexOf(arg2));
							selectansvars.remove(j);
							selectansvars.add(j, asrtString.get(pred).get(1));
						}else if(pred.contains(arg1)){
							pred=pred.substring(0, pred.indexOf(arg1));
							selectansvars.remove(j);
							selectansvars.add(j, asrtString.get(pred).get(0));
						}else{
							pred=pred.substring(0, pred.indexOf(arg));
							selectansvars.remove(j);
							selectansvars.add(j, asrtString.get(pred).get(0));
						}
					}
				}
			}
		}

		//Write the query:
		String temp = "";
		String updatedTableName = "";
		String SQLQuery="";//query
		SQLQuery=SQLQuery+"SELECT DISTINCT ";
		for(int i=0;i<selectpart.size()-1;i++){
			temp = selectpart.get(i);
			updatedTableName = temp.substring(0,temp.indexOf('.'));

			if(tablesDictionaryShort.containsKey(updatedTableName)) {
				SQLQuery+="table"+(tablesDictionaryShort.get(updatedTableName))+(temp.substring(temp.indexOf('.')))+" , ";

			}else {
				SQLQuery+=temp+" , ";
			}
		}
		temp = selectpart.get(selectpart.size()-1);
		updatedTableName = temp.substring(0,temp.indexOf('.'));
		if(tablesDictionaryShort.containsKey(updatedTableName)) {
			SQLQuery+="table"+(tablesDictionaryShort.get(updatedTableName))+(temp.substring(temp.indexOf('.')))+" ";

		}else {
			SQLQuery+=temp+" ";
		}
		//		SQLQuery+=selectpart.get(selectpart.size()-1)+" ";

		SQLQuery=SQLQuery+"FROM ";
		for(int i=0;i<frompart.size()-1;i++){
			//			SQLQuery+=frompart.get(i)+" , ";
			if(tablesDictionaryShort.containsKey(frompart.get(i))) {
				SQLQuery+="table"+(tablesDictionaryShort.get(frompart.get(i)))+" , ";
			}else {
				SQLQuery+=frompart.get(i)+" , ";
			}
		}
		//		SQLQuery+=frompart.get(frompart.size()-1)+" ";

		temp = frompart.get(frompart.size()-1);
		//System.out.println(temp);
		if(tablesDictionaryShort.containsKey(temp)) {
			SQLQuery+="table"+(tablesDictionaryShort.get(temp))+" ";
		}else {
			SQLQuery+=frompart.get(frompart.size()-1)+" ";
		}

		if(where>0){
			SQLQuery += " WHERE ";
			String part1 = "";
			String part2 = "";
			String tableName1="";
			String tableName2="";
			String updatedPart1="";
			String updatedPart2="";
			for(int i = 0; i < wherepart.size(); i++){
				temp = wherepart.get(i);
				updatedTableName = temp.substring(0,temp.indexOf('.'));

				part1 = temp.substring(0, temp.indexOf(" = "));
				part2 = temp.substring(temp.indexOf(" = ")+3);
				//System.out.println(part1);
				//System.out.println(part2);

				tableName1 = part1.substring(0,part1.indexOf('.')).trim();
				tableName2 = part2.substring(0,part2.indexOf('.')).trim();

				if(Character.isDigit(tableName1.charAt(tableName1.length()-1))) {
					tableName1 = tableName1.substring(0, tableName1.length()-1);
				}
				if(Character.isDigit(tableName2.charAt(tableName2.length()-1))) {
					tableName2 = tableName2.substring(0, tableName2.length()-1);
					//System.out.println("WAFAA: ");
					//System.out.println(tableName2);
				}
				//				table120.param = table1.firstparam AND table1.secondparam = table54.param
				//				select * from table120, table1, table54 where table120.param = table1.firstparam AND table1.secondparam = table54.param
				if(tablesDictionaryShort.containsKey(tableName1)) {
					updatedPart1 = "table"+(tablesDictionaryShort.get(tableName1))+(part1.substring(part1.indexOf('.')));
				}else {
					updatedPart1 = part1;
				}

				if(tablesDictionaryShort.containsKey(tableName2)) {
					updatedPart2 = "table"+(tablesDictionaryShort.get(tableName2))+(part2.substring(part2.indexOf('.')));
				}else {

					updatedPart2 = part2;
				}


				//				if(tablesDictionaryShort.containsKey(updatedTableName)) {
				//					SQLQuery+="table"+(tablesDictionaryShort.get(updatedTableName))+(temp.substring(temp.indexOf('.')))+" AND ";
				//
				//				}else {
				//					SQLQuery+=temp+" AND ";
				//				}


				SQLQuery+=updatedPart1 +" = "+updatedPart2;

				if(i!= wherepart.size()-1) {
					SQLQuery+= " AND ";
				}else {
					SQLQuery+= " ";
				}


				//				SQLQuery += wherepart.get(i)+ " AND ";
			}
			//			SQLQuery+=wherepart.get(wherepart.size()-1)+" ";



		}
		/*System.out.println("Select Part:");
		System.out.println(Arrays.toString(selectpart.toArray()));
		System.out.println("From Part:");
		System.out.println(Arrays.toString(frompart.toArray()));

		System.out.println("Where Part:");
		System.out.println(Arrays.toString(wherepart.toArray()));*/


		return SQLQuery;
	}


	public String getPostgresQuery(Clause clause) {
		String sqlQuery = "";

		List<String> selectVariables = clause.getHead().getArguments().toStringList(); // Corresponds to the DLP version of the head variables
		List<String> selectVariablesToSQL; // Corresponds to the SQL version of the head variables
		List<String> fromTables; // Corresponds to the SQL version of the table names
		List<String> whereConstraints; // Corresponds to the SQL version of the constraints

		List<String> allVariables;

		String selectPart = "";
		String fromPart = "";
		String wherePart = "";
		boolean hasWhere = false;


		Map<Term, List<String>> varsToPredicatesMap= new HashMap<Term, List<String>>();//vars ((term) -> list of predicate in which it appears)

		//Atoms of the query
		Atom[] queryAtoms= new Atom[clause.bodySize()]; // An array of all the atoms contained in the body of the query
		for (int i = 0; i < clause.bodySize(); i++){
			Atom bodyAtom = clause.getBodyAtomAt(i);//i th atom of the query
			queryAtoms[i]=bodyAtom;//add atom to conjunction
			//System.out.println("Body Atom at "+i+" is: "+bodyAtom.toString());
		}

		// Construction of FROM part -- table names
		fromTables = constructFromTables(queryAtoms);

		// Construct SQL version of Select vars
		selectVariablesToSQL = constructSelectVarsSQL(queryAtoms, selectVariables);

		// Construct a list of all the variables in dlp version
		allVariables = constructAllVariables(queryAtoms, selectVariables);

		// Construct a list of all the where constraints in sql version
		whereConstraints = constructWhereConstraints(queryAtoms, allVariables);

		hasWhere  = !whereConstraints.isEmpty();

		//Converting the query to our pgsql format:
		String temp = "";
		String updatedTableName = "";

		//WRITING THE SELECT PART
		selectPart="SELECT DISTINCT ";
		for(int i=0;i<selectVariablesToSQL.size()-1;i++){
			temp = selectVariablesToSQL.get(i);
			updatedTableName = temp.substring(0,temp.indexOf('.'));

			if(tablesDictionaryShort.containsKey(updatedTableName)) {
				selectPart+="table"+(tablesDictionaryShort.get(updatedTableName))+(temp.substring(temp.indexOf('.')))+" AS h"+i+", ";

			}else {
				selectPart+=temp+" AS h"+i+" , ";
			}
		}
		/*System.out.println(selectPart);
		System.out.println(selectVariables);
		System.out.println("selectVariablesToSQL: "+selectVariablesToSQL);*/
		temp = selectVariablesToSQL.get(selectVariablesToSQL.size()-1);
		updatedTableName = temp.substring(0,temp.indexOf('.'));
		if(tablesDictionaryShort.containsKey(updatedTableName)) {
			selectPart+="table"+(tablesDictionaryShort.get(updatedTableName))+(temp.substring(temp.indexOf('.')))+" AS h"+(selectVariablesToSQL.size()-1);

		}else {
			selectPart+=temp+" AS h"+(selectVariablesToSQL.size()-1);
		}
		//		SQLQuery+=selectpart.get(selectpart.size()-1)+" ";


		//WRITING THE FROM PART

		fromPart=fromPart+"FROM ";
		for(int i=0;i<fromTables.size()-1;i++){
			//			SQLQuery+=frompart.get(i)+" , ";
			if(tablesDictionaryShort.containsKey(fromTables.get(i))) {
				fromPart+="table"+(tablesDictionaryShort.get(fromTables.get(i)))+" , ";
			}else {
				fromPart+=fromTables.get(i)+" , ";
			}
		}
		//		SQLQuery+=frompart.get(frompart.size()-1)+" ";

		temp = fromTables.get(fromTables.size()-1);
		//System.out.println(temp);
		if(tablesDictionaryShort.containsKey(temp)) {
			fromPart+="table"+(tablesDictionaryShort.get(temp))+" ";
		}else {
			fromPart+=fromTables.get(fromTables.size()-1)+" ";
		}

		if(hasWhere){
			wherePart += " WHERE ";
			String part1 = "";
			String part2 = "";
			String tableName1="";
			String tableName2="";
			String updatedPart1="";
			String updatedPart2="";
			for(int i = 0; i < whereConstraints.size(); i++){
				//NEED TO UPDATE TO TAKE INTO CONSIDERATION CONSTANTS
				temp = whereConstraints.get(i);
				updatedTableName = temp.substring(0,temp.indexOf('.'));

				part1 = temp.substring(0, temp.indexOf(" = "));
				part2 = temp.substring(temp.indexOf(" = ")+3);
				//System.out.println(part1);
				//System.out.println(part2);


				tableName1 = part1.substring(0,part1.indexOf('.')).trim();
				if(Character.isDigit(tableName1.charAt(tableName1.length()-1))) {
					tableName1 = tableName1.substring(0, tableName1.length()-1);
				}

				if(tablesDictionaryShort.containsKey(tableName1)) {
					updatedPart1 = "table"+(tablesDictionaryShort.get(tableName1))+(part1.substring(part1.indexOf('.')));
				}else {
					updatedPart1 = part1;
				}
				if(part2.toLowerCase().contains("param") && part2.contains(".")) {
					//part two is a variable
					tableName2 = part2.substring(0,part2.indexOf('.')).trim();
					if(Character.isDigit(tableName2.charAt(tableName2.length()-1))) {
						tableName2 = tableName2.substring(0, tableName2.length()-1);
						//System.out.println("WAFAA: ");
						//System.out.println(tableName2);
					}

				}else {
					//part two is a constant
					tableName2 = part2.trim();

					String tempName = tableName2.replace("\"", "");
					/*System.out.println("_"+tempName.substring(tempName.lastIndexOf("/")+1).trim()+"_");
					System.out.println("tempName.substring(tempName.lastIndexOf(\"/\")+1)");
					System.out.println(predicatesDictionaryShort.get("AssociateProfessor2"));
					System.out.println("predicatesDictionaryShort");
					System.out.println();*/
					if(predicatesDictionary.containsKey(tempName)) {
						tableName2 = predicatesDictionary.get(tempName);
					}else if(predicatesDictionaryShort.containsKey(tempName.substring(tempName.lastIndexOf("/")+1))){
						tableName2=predicatesDictionaryShort.get(tempName.substring(tempName.lastIndexOf("/")+1));
					}else {
//						tableName2=tableName2.replace("\"", "\'");
						tableName2=""+(highestIndex+10);
					}

				}

				if(tablesDictionaryShort.containsKey(tableName2)) {
					updatedPart2 = "table"+(tablesDictionaryShort.get(tableName2))+(part2.substring(part2.indexOf('.')));
				}else {

					updatedPart2 = tableName2;
				}





				wherePart+=updatedPart1 +" = "+updatedPart2;

				if(i!= whereConstraints.size()-1) {
					wherePart+= " AND ";
				}else {
					wherePart+= " ";
				}

			}

		}


		sqlQuery+=selectPart+" "+fromPart+" "+wherePart;

		sqlQuery= sqlQuery.replace("secondparam", "c1");
		sqlQuery= sqlQuery.replace("firstparam", "c0");
		sqlQuery= sqlQuery.replace("param", "c0");

		return sqlQuery;
	}

	private List<String> constructWhereConstraints(Atom[] queryAtoms, List<String> allVariables) {
		List<String> whereConst = new ArrayList<String>();
		String[] columnName = new String[]{arg,arg1,arg2};

		for(int i=0; i<queryAtoms.length;i++) {

			Tuple args1 = queryAtoms[i].getArguments();
			Term[] term1= args1.getTerms();//term of the atom

			//construction of column name from atom
			String tableName1 = queryAtoms[i].getPredicate().toString();
			//MODIF ENCODAGE
			if(tableName1.contains("#")){//case without encoding
				int ibegin=tableName1.indexOf('#');
				if (!tableName1.startsWith("<")) {
					tableName1 = "<" + tableName1 + ">";
				}
				int iend=tableName1.length();
				tableName1= tableName1.substring(ibegin+1,iend-1);
			}


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
			List<String> checkedVariables = new ArrayList<String>();
			if(loopCheck) {
				//The atom has a variable
				//We compare it with other atoms to find links on this variable

				for(int j=i+1; j<queryAtoms.length; j++) {
					Tuple args2 = queryAtoms[j].getArguments();
					Term[] term2= args2.getTerms();//term of the atom

					//construction of column name from atom
					String tableName2 = queryAtoms[j].getPredicate().toString();
					//MODIF ENCODAGE
					if(tableName2.contains("#")){//case without encoding
						int ibegin=tableName2.indexOf('#');
						if (!tableName2.startsWith("<")) {
							tableName2 = "<" + tableName2 + ">";
						}
						int iend=tableName2.length();
						tableName2= tableName2.substring(ibegin+1,iend-1);
					}
					for(int v1=0; v1<term1.length;v1++) {
						for(int v2=0;v2<term2.length;v2++) {

							if(term1[v1].isVariable() && term2[v2].isVariable()) {
								//both terms are variables
								if(term1[v1].toString().equals(term2[v2].toString())) {

									boolean newUnlinkedVars = true;
									if(checkedVariables.contains(term1[v1].toString()) &&
											checkedVariables.contains(term2[v2].toString())) {
										newUnlinkedVars = false;

									}
									if(newUnlinkedVars) {
										String constStr = "";
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
										constStr+=tableName1+col1;
										constStr+=" = ";
										constStr+=tableName2+col2;

										whereConst.add(constStr);

										if(!checkedVariables.contains(term1[v1].toString())) {
											checkedVariables.add(term1[v1].toString());
										}
										if(!checkedVariables.contains(term2[v2].toString())) {
											checkedVariables.add(term2[v2].toString());
										}
									}

								}
							}



						}
					}



				}
			}
			if (loopCheck2){
				//there's a constant in the Atom
				//System.out.println("loopCheck2 ENTER");
//				System.out.println(term1[0].toString());
//				System.out.println(term1[1].toString());

				for(int v=0; v<term1.length;v++) {
					if(term1[v].isConstant()) {
						String constStr = "";
						String col = "";

						if(term1.length ==1) {
							//term1 is a concept
							col = columnName[0];
						}else {
							//term1 is a role
							col = columnName[v+1];
						}
						constStr+=tableName1+col;
						constStr+=" = ";
						constStr+=term1[v].toString();

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

	private List<String> constructAllVariables(Atom[] queryAtoms, List<String> selectVariables) {
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

	private List<String> constructSelectVarsSQL(Atom[] queryAtoms, List<String> selectVariables) {
		List<String> selVarsSQL = new ArrayList<String>();
//		List<String> parsedSelVars = new ArrayList<String>();
		/*System.out.println("constructSelectVarsSQL ENTER");
		System.out.println("selectVariables "+selectVariables);
		System.out.println("");
		System.out.println("");
		System.out.println("");*/
		for(int i=0; i<selectVariables.size();i++) {
			for(int j=0; j<queryAtoms.length; j++) {
				Tuple args = queryAtoms[j].getArguments();
				Term[] term= args.getTerms();//term of the atom

				//construction of column name from atom
				String tableName = queryAtoms[j].getPredicate().toString();
				//MODIF ENCODAGE
				if(tableName.contains("#")){//case without encoding
					int ibegin=tableName.indexOf('#');
					if (!tableName.startsWith("<")) {
						tableName = "<" + tableName + ">";
					}
					int iend=tableName.length();
					tableName= tableName.substring(ibegin+1,iend-1);
				}


				if(queryAtoms[j].isConcept()) {
					//					System.out.println(selectVariables);
					//					System.out.println("term[0].toString()");
					//					System.out.println(term[0].toString());
					//					System.out.println((selectVariables.toString().contains(term[0].toString())));
					//					System.out.println();
					if(selectVariables.get(i).equals(term[0].toString())) {
//						if(!selVarsSQL.contains(tableName+arg)) {
							selVarsSQL.add(tableName+arg);
							break;
//						}

					}
				}else {// It's a role
					//					System.out.println(selectVariables);
					//					System.out.println("term[1].toString()");
					//					System.out.println(term[1].toString());
					//					System.out.println((selectVariables.contains(term[1].toString())));
					//					System.out.println();



					if(selectVariables.get(i).equals(term[0].toString())) {
						//if(!selVarsSQL.contains(tableName+arg1)) {
							selVarsSQL.add(tableName+arg1);
							break;
						//}
					}

					if(selectVariables.get(i).equals(term[1].toString())) {
						//if(!selVarsSQL.contains(tableName+arg2)) {
							selVarsSQL.add(tableName+arg2);
							break;
						//}
					}

				}

			}
		}
		/*System.out.println("selVarsSQL");
		System.out.println(selVarsSQL);
		System.out.println();*/

		return selVarsSQL;
	}

	public List<String> constructFromTables(Atom[] queryAtoms){
		List<String> fromTables = new ArrayList<String>();
		for (int i = 0; i < queryAtoms.length; i++){
			//construction of column name from atom
			String tableName = queryAtoms[i].getPredicate().toString();
			//MODIF ENCODAGE
			if(tableName.contains("#")){//case without encoding
				int ibegin=tableName.indexOf('#');
				if (!tableName.startsWith("<")) {
					tableName = "<" + tableName + ">";
				}
				int iend=tableName.length();
				tableName= tableName.substring(ibegin+1,iend-1);
			}

			//construction of frompart
			if(!fromTables.contains(tableName)){
				fromTables.add(tableName);
			}

		}
		return fromTables;
	}


	public int getnumOfcolumns(){
		if(selectpart!=null){
			return selectpart.size();
		}
		return 0;
	}

	public Map<Atom, List<Integer>> getCauseMask(){
		return asrt;
	}

	public List<Integer> getselectansvars(){
		return selectansvars;
	}



}
