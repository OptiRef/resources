package fr.optiref.dlquery;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.ntua.isci.common.dl.LoadedOntology;
import edu.ntua.isci.common.dl.LoadedOntologyAccess;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.ClauseParser;
import edu.ntua.isci.qa.algorithm.ComputedRewriting;
import edu.ntua.isci.qa.algorithm.Engine;
import edu.ntua.isci.qa.algorithm.Stats;
import edu.ntua.isci.qa.algorithm.rapid.dllite.DLRapid;
import edu.ntua.isci.qa.algorithm.rapid.elhi.ETRapid;
import edu.ntua.isci.qa.owl.OWL2LogicTheory;
import edu.optiref.sql.SqlConverter;




public class DL2SQL {


	//private  FCTheory lp;
	ArrayList<Clause> queries;
	LoadedOntology ontRef;
	NumberFormat nf;
	String mode;
	String iri;
	String ontologyFile;
	String aQuery;
	SqlConverter cq2sql;
	Map<String, String> prefixes;
	Map<String, String> reversedprefixes;;


	Engine engine = null;
	LoadedOntologyAccess loa;

	Map<String, Object> props ;
	static DL2SQL myUnique = null;
	List<String> reformulatedQueries;
	List<Clause> rewritingQueries;
	private DL2SQL() {
		// TODO Auto-generated constructor stub
		props = new HashMap<>();
		cq2sql = new SqlConverter();//CQ2SQL(false, false, false);
		reformulatedQueries = new ArrayList<String>();
		rewritingQueries = new ArrayList<Clause>();
		nf = NumberFormat.getInstance(Locale.US);
		nf.setMinimumFractionDigits(2);
		nf.setMaximumFractionDigits(2);
		nf.setGroupingUsed(false);
		aQuery = "";
		mode = "DU";
		iri = "FULL";
		ontologyFile = QueryDL.getOntofile();
		if (iri.equals("FULL")) {
			props.put(LoadedOntologyAccess.PRESENTATION_TYPE, LoadedOntologyAccess.FULL_FORM);
		} else {
			props.put(LoadedOntologyAccess.PRESENTATION_TYPE, LoadedOntologyAccess.SIMPLE_SHORT_FORM);
		}

		if (mode.equals("DU")) {
			engine = DLRapid.createFastUnfoldRapid();
		} else {
			engine = ETRapid.createDatalogRapid();
		}



	}
	public static DL2SQL getInstace() {

		if(myUnique == null) {
			myUnique = new DL2SQL();
		}
		return myUnique;
	}



	private List<String> run(Engine engine, LoadedOntologyAccess loa, String mode) throws Exception {
		if (engine == null) {
			return null;
		}

		ArrayList<Clause>[] rewritings = new ArrayList[queries.size()];

		String engineName = engine.getEngineName();

		List<String> sqlQueries = new ArrayList<String>();
		if (mode.equals("DU") || mode.equals("DD")) {
			engine.importOntology(ontRef, loa, OWL2LogicTheory.DL_LITE);
		} else if (mode.equals("ED")) {
			engine.importOntology(ontRef, loa, OWL2LogicTheory.ELHI);
		} else if (mode.equals("HD")) {
			engine.importOntology(ontRef, loa, OWL2LogicTheory.HORN_SHIQ);
		}

		//lp = engine.getCurrentTheory();
		//Utils.printObjectArrayList(System.out, " PROGRAM (" + lp.getClauses().size() + ")", lp.getSortedClauses());
		String cclause;
		List<String>reformulations = new ArrayList<String>();
		for (int j = 0; j < queries.size(); j++) {
			Clause query = queries.get(j);

			query.reset();

			//System.out.println("Query " + (j + 1) + " : " + query);

			Stats st = new Stats();
			ComputedRewriting res = engine.computeRewritings(query, true);

			rewritings[j] = res.getFilteredRewritings();
			rewritingQueries = rewritings[j];

			Stats rst = res.getStatistics();

			st.addIterationTimes(rst.qrewriteTime, rst.qcheckTime, rst.qmatchTime);
			st.finishIteration(res.getAllComputedRewritings().size(), res.getFilteredRewritings().size(), 0);

			//System.out.println(String.join("\n",rewritings[j].toString()));
			/*for(Clause c: rewritings[j]) {
				cclause = c.toString();
				//System.out.println("avant : "+cclause);
				for(String key : reversedprefixes.keySet()) {
					cclause = cclause.replace(key, reversedprefixes.get(key)+":").replace("<", "").replace(">", "");
				}
				//System.out.println("apres : "+cclause);
				reformulations.add(cclause);

			}*/
			//System.exit(0);
			//printObjectArrayListV1(writer, engineName + " - NON REDUNDANT ", rewritings[j]);

			//writer.close();

			sqlQueries.addAll(getSQL(rewritings[j]));

		}

		//System.out.println("FINISHED.");
		if(sqlQueries.size() == 0)return null;
		return sqlQueries;
	}


	private String addPrefix(String query) {
		String head, body;
		head = query.split(" <- ")[0];
		body = query.split(" <- ")[1];
		List<String> atoms = new ArrayList<String>();
		String key;
		//System.out.println(aQuery);
		for(String atom: body.split(", ")){
			key = atom.split(":")[0];
			//System.out.println("cur atom :" +atom);
			atoms.add("<"+atom.replace(key+":", prefixes.get(key)).replace("(", ">("));
		}
		return head+" <- "+String.join(", ", atoms);

	}
	public void loadData(String ontologyFile, String aQuery) throws Exception {

		ontRef = LoadedOntology.createFromPath(ontologyFile);

		if (ontRef != null) {
			/*System.out.println("Ontology : " + ontologyFile);
			System.out.println("Query    : " + aQuery);
			System.out.println();*/

			//System.out.println("Ontology axioms : " + ontRef.getMainOntology().getLogicalAxiomCount());
		}
		queries = new ArrayList<Clause>();

		ClauseParser cp = new ClauseParser();
		aQuery = addPrefix(aQuery);
		//System.out.println(aQuery);
		queries.add(cp.parseClause(aQuery));

		//loadQueries(queryFile);
	}

	public void  reformulate() {
		reformulatedQueries.clear();
		try {
			loadData(ontologyFile, aQuery);
			loa = new LoadedOntologyAccess(ontRef, props);
			reformulatedQueries = run(engine, loa, mode);
			//System.out.println(reformulatedQueries.size());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//System.out.println(reformulatedQueries);
			e.printStackTrace();
		}
	}

	public String getSQL(String query){
		ArrayList<Clause> oqueries = new ArrayList<Clause>();
		System.out.println(query);
		query = addPrefix(query);
		System.out.println(query);
		ClauseParser cp = new ClauseParser();
		oqueries.add(cp.parseClause(query));

		return cq2sql.getPostgresQuery(oqueries.get(0), prefixes);
	}


	public List<String> getSQL(ArrayList<Clause> queriesList) throws IOException{
		List<String> res = new ArrayList<String>();

		for(int i=0;i<queriesList.size();i++) {
			if (queriesList.get(i)!=null) {
				String sqlq = cq2sql.getPostgresQuery(queriesList.get(i), prefixes);
				//System.out.println(i+": "+sqlq);
				res.add(sqlq);
			}
		}
		//query.terminate();
		return res;
	}

	public void dump(String filename) {
		FileWriter writer;
		try {
			writer = new FileWriter(filename);
			writer.write("the result");
			writer.write(String.format("\n", getReformulatedQueries()));
			//writer.write("The result : "+conn.getCount(toSQL())+"\n");
			writer.close();
		}
		catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	public void setaQuery(String aQuery) {
		this.aQuery = aQuery;
	}
	public void setPrefixes(Map<String, String> _prefixes) {
		this.prefixes = _prefixes;
		reversedprefixes = new HashMap<String, String>();
		for(String key : prefixes.keySet()) {
			reversedprefixes.put(prefixes.get(key), key);
		}
	}
	public Map<String, String> getPrefixes() {
		return prefixes;
	}
	public List<String> getReformulatedQueries() {
		return reformulatedQueries;
	}

	public List<Clause> getRewritingQueries() {
		return rewritingQueries;
	}
	public void terminate(){
		/*try {
			cq2sql..terminate();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
//	public static void main(String[] args) throws Exception {
//
//		Cover2SQL test = new Cover2SQL("ressources/lubm.queries");
//		//System.out.println(String.join("\nUNION\n", test.getReformulatedQueries()));
//		test.setaQuery("q5(?0) <- Publication(?0), publicationAuthor(?0,?1), Professor(?1), publicationAuthor(?0,?2), Student(?2)");
//		test.reformulate();
//		System.out.println(String.join("\nUNION\n", test.getReformulatedQueries()));
//
//		FileWriter writer;
//		try {
//			writer = new FileWriter("graphSum/workspace/JUCQ/src/q1res.txt");
//			writer.write(String.join("\nUNION\n", test.getReformulatedQueries()));
//			//writer.write("The result : "+conn.getCount(toSQL())+"\n");
//			writer.close();
//		}
//		catch (Exception e) {
//			// TODO: handle exception
//			e.printStackTrace();
//		}
//		//test.dump("graphSum/workspace/JUCQ/src/q1res.txt");
//
//	}

}
