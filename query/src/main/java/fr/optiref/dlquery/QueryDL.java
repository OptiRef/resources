package fr.optiref.dlquery;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import edu.ntua.isci.common.dl.LoadedOntology;
import fr.optiref.dlquery.costmodel.CostModel;
import fr.optiref.dlquery.jucq.Atomic;
import fr.optiref.dlquery.jucq.Cover;
import fr.optiref.dlquery.jucq.JUCQ;
import fr.optiref.dlquery.pruning.JUCQPruner;
import fr.optiref.dlquery.pruning.Pruner;
import fr.optiref.dlquery.pruning.UCQPruner;
import fr.optiref.dlquery.pruning.USCQPruner;
import fr.optiref.dlquery.ucq.UCQ;
import fr.optiref.dlquery.uscq.SelectQuery;
import fr.optiref.dlquery.uscq.USCQ;
import fr.optiref.dlquery.uscq.View;
import me.tongfei.progressbar.ProgressBar;
/**
 * Hello world!
 *
 */
public class QueryDL
{
	private Properties properties;
	private String queriesFile, approach;
	private int nexp;
	private static String ontofile;
	private static Connector connector;
	private static LoadedOntology ontRef;
	private long TTOTAL, TREF, TPRUN, EXEC_TIME;
	private long NANS, NREF, NPRUN;
	private String dbname;
	private String debugger;
	private USCQ compact;
	private UCQ rapid;
	private JUCQ jucq;
	private String engine;
	private int timeout;
	private int maxref;
	private Pruner pruner;
	private Map<String, String> prefixes;
	public QueryDL(Properties _properties, String _dbname) {
		this.TTOTAL  = 0;
		this.TREF    = 0;
		this.TPRUN   = 0;
		this.EXEC_TIME = 0;
		this.NANS    = 0;
		this.NREF    = 0;
		this.NPRUN   = 0;
		this.properties = _properties;
		this.queriesFile = (String) properties.get("database.queries");
		this.approach = (String) properties.get("reformulation.approach");
		this.debugger = properties.get("debugger.log_level").toString();
		this.nexp = Integer.parseInt(properties.get("exps.runs").toString());
		this.engine = properties.get("database.engine").toString();
		maxref = Integer.parseInt(properties.get("query.ref.max").toString());
		this.timeout = Integer.parseInt(properties.get("database.timeout").toString());
		ontofile = "file:"+System.getProperty("user.dir") + "/"+properties.get("database.ontology").toString();
		connector = new Connector(properties, _dbname);
		connector.clean_explain_statement();
		this.dbname = _dbname;
		compact = null;
		rapid = null;
		try {
			ontRef =  LoadedOntology.createFromPath(ontofile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(!this.debugger.equals("OFF")){
			System.out.println(String.format("Pruning enabled: %s", properties.get("database.pruning").toString()));
			System.out.println(String.format("Pruning on the summary: %s", properties.get("pruning.summary").toString()));
			System.out.println(String.format("Reformulation technique: %s", this.approach));
		}
		if(properties.get("database.pruning").toString().equals("true") && this.approach.equals("USCQ")) {
			this.pruner = new USCQPruner(properties.get("database.summary_table_alis").toString(), connector, properties);
		}
		else if(properties.get("database.pruning").toString().equals("true") && this.approach.equals("UCQ")) {
			this.pruner = new UCQPruner(properties.get("database.summary_table_alis").toString(), connector, properties);
		}
		else if(properties.get("database.pruning").toString().equals("true") && this.approach.equals("JUCQ")) {
			this.pruner = new JUCQPruner(properties.get("database.summary_table_alis").toString(), connector, properties);
		}
		else {
			this.pruner = null;
		}

		if(this.approach.equals("JUCQ")) {
			System.out.println("Creating all deps for Croot...");
			JUCQ.createDepOptim();
		}

	}

	List<String> getQueries(){
		List<String>queries = new ArrayList<String>();
		String line ="";
		BufferedReader reader;
		//System.out.println("we are in get queries");
		this.prefixes = QueryUtils.getPrefix(this.queriesFile);
		DL2SQL.getInstace().setPrefixes(this.prefixes);
		try {
			reader = new BufferedReader(new FileReader(this.queriesFile));
			line = reader.readLine();
			while(line != null) {
				if(line.startsWith("q")) {
					queries.add(line);
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(!this.debugger.equals("OFF")){
			if(queries.size() == 0) {
				System.out.println(String.format("Check your queries syntax example: q(?0) <- C(?0), R(?1, ?0)"));
			}else {
				System.out.println(String.format("Found %d queries in %s", queries.size(), this.queriesFile));
			}
		}

		return queries;
	}



	public int getUSCQTime(String qname) {
		int res = 0;
		String querypath    = System.getProperty("user.dir") + "/"+properties.get("compact.queryDir").toString();


		try {
			String qline;
			BufferedReader qbf = new BufferedReader(new FileReader(querypath+"/"+qname+".time.txt"));
			while ((qline = qbf.readLine()) != null) {
				res = Integer.parseInt(qline);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File file = new File(querypath+"/"+qname+".time.txt");

        file.delete();


		return res;
	}


	public void  eval() throws SQLException{

		System.out.println("Timeout: "+this.connector.getquerytimeout());
		Cover cbest = null;
		String sqlCode, prunedSQL, countQuery;
		long result ;
		long start;
		List<String> queries = getQueries();
		Set<Atomic> atoms ;
		FileWriter writer = null;

		String out = System.getProperty("user.dir") + "/"+properties.get("exps.output").toString();
		out += String.format("%s.%s.", this.dbname, this.approach);


		List<String> rowExps ;
		String qname;
		String names = "query,EXEC_TIME,NANS,NREF,TREF,NPRUN,TPRUN,TOTAL\n";
		List<Integer> pExps;
		int bestNPRUN;
		double cost, bestcost, worsecost;
		String bestQuery;

		boolean stats, docost;
		stats  = this.properties.get("query.savesubcq").toString().equals("true");
		docost = false;
		boolean countviews = this.properties.get("uscq.count_views").toString().equals("true");

		//this.connector.enable_seqscan("off");

		if(properties.get("exps.stats").toString().equals("true")) {
			out += "stats.csv";

			try {
				writer = new FileWriter(out);
				writer.write("query,NREF,NPRUN,BNPRUN,WCOST,COST,BCOST\n");

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else {
			if(properties.get("database.pruning").toString().equals("true") && properties.get("pruning.summary").toString().equals("true")) {
				out += "prune.csv";
			}
			else if(properties.get("database.pruning").toString().equals("true") && properties.get("pruning.summary").toString().equals("false")) {
				out += "db.prune.csv";
			}
			else{
				out += "ref.csv";
			}

			try {
				writer = new FileWriter(out);
				writer.write(names);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		for (String query : queries) {
			//System.out.println("we are in loop queries");
			qname = QueryUtils.getQName(query);
			atoms = JUCQ.parseDLPQuery(query);


			if(!this.debugger.equals("OFF")){
				System.out.println(String.format("The query %s has atoms %s", qname, atoms.toString()));
			}
			this.EXEC_TIME = this.NREF = this.TREF = this.NPRUN = this.TPRUN = this.TTOTAL = 0;

			switch (this.approach){
				case "UCQ": {

					//cbest = JUCQ.getBestCover(atoms,connector, 0);
					rapid = new UCQ(properties);
					break;
				}

				case "USCQ": {
					//cbest = JUCQ.getBestCover(atoms,connector, 1);
					compact = new USCQ(properties);

					break;
				}

				case "JUCQ": {
					jucq = new JUCQ(properties);
					jucq.resetTime();
					//jucq.getCrootOpti(atoms);
					cbest = jucq.GCOV(atoms, connector);

					break;
				}
				default:
					throw new IllegalArgumentException("Unexpected APPROACH value: " + this.approach);
			}
			try {
				if(compact != null) {
					//start = System.nanoTime();
					sqlCode = compact.reformulate(query);

					String[] tmpArray = sqlCode.split("\\n--#####\\n");
					String withquery = tmpArray[0].replace("WITH ", "");


					if(countviews) {
						System.out.println("withquery: "+withquery);
						System.out.println(String.format("Max view size: %d", QueryUtils.getMaxViewSize(connector, withquery)));
						System.exit(0);

					}
					this.TREF  = compact.getTimer(); //(System.nanoTime()- start)/1000000;
				}else if(rapid != null) {
					//start = System.nanoTime();
					sqlCode = rapid.reformulate(query);
					//System.out.println(sqlCode);
					this.TREF = rapid.getTimer(); //(System.nanoTime()- start)/1000000;
				}else {
					sqlCode = cbest.toSQL();
					this.TREF = cbest.getTimer() + jucq.getTotalTime();
				}
			}
			catch( Exception e){

				this.TPRUN = this.NPRUN = this.TTOTAL = this.TREF = this.TPRUN = this.EXEC_TIME = this.NANS = -2;
				rowExps = new ArrayList<String>();
				//"query	EXEC_TIME	NANS	NREF	TREF	NPRUN	TPRUN	TOTAL"
				rowExps.add(qname);
				rowExps.add(this.EXEC_TIME+"");
				rowExps.add(this.NANS+"");
				rowExps.add(this.NREF+"");
				rowExps.add(this.TREF+"");
				rowExps.add(this.NPRUN+"");
				rowExps.add(this.TPRUN+"");
				rowExps.add(this.TTOTAL+"");
				try {
					writer.write(String.format("%s\n", String.join(",", rowExps)));
					writer.flush();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}


				continue;
			}

			/*if(this.debugger.equals("DEBUG")){
				String queryfilename = System.getProperty("user.dir") + "/"+properties.get("exps.output").toString();
				queryfilename += String.format("%s_%s.%s.sql", qname, this.dbname, this.approach);
				storeQuery(sqlCode, queryfilename);
				// System.out.println(String.format("The query refomulated: \n %s", sqlCode));
			}*/

			if(countviews) {
				System.exit(0);
			}
			pExps = new ArrayList<Integer>();
			for (int i = 0; i<this.nexp; i++) {
				pExps.add(i);
			}

			this.NREF = QueryUtils.countMatches(sqlCode, "UNION");
			for(Integer r : ProgressBar.wrap(pExps, String.format("Running exps for %s...", qname))) {
				rowExps = new ArrayList<String>();
				this.NPRUN = this.TPRUN = this.EXEC_TIME = this.NANS = 0;
				this.NANS = -1;
				if(r == 0) {

					if(this.pruner != null &&  this.NREF < maxref) {

						// start = System.nanoTime();
						int timeout =(int) (this.connector.getTimeout()/(this.NREF +1));
						this.connector.settimeout(Math.max(timeout/1000, 1));
						System.out.println("Timeout: "+this.connector.getquerytimeout());
						System.out.println("prunning: "+qname);
						this.pruner.setTimer(0);
						prunedSQL = pruner.pruneQuery(sqlCode, query, stats, docost);
						// this.TPRUN  = (System.nanoTime()- start)/1000000;
						this.TPRUN  = this.pruner.getTimer();
						System.out.println("end prunning: "+qname);
						this.connector.settimeout(this.connector.getOtimeout());
					}else {
						prunedSQL = sqlCode;
					}

					this.NPRUN = QueryUtils.countMatches(prunedSQL, "UNION");
					//System.out.println("Query : "+prunedSQL);

					countQuery = prunedSQL.toLowerCase();
					countQuery = QueryUtils.normalize(countQuery);
					//System.out.println(countQuery);


					if(!countQuery.contains("count(*)")) {
						//System.out.println("No count aggregator found!!");
						countQuery = String.format(" with qf as (%s) select count(*) from qf", countQuery);
					}
					//System.out.println("count query "+countQuery);
					/*if(this.engine.equals("MYSQL")){
						countQuery = countQuery.replace("select count(*)", String.format("select + MAX_EXECUTION_TIME(%d) count(*) ", timeout));
					}*/
					countQuery = QueryUtils.clean(countQuery);

					if(this.properties.get("query.store").equals("ALL")){
						String queryfilename = System.getProperty("user.dir") + "/"+properties.get("exps.output").toString();
						queryfilename += String.format("%s_%s.%s.sql", qname, this.dbname, this.approach);
						QueryUtils.storeQuery(sqlCode, queryfilename);
						queryfilename = System.getProperty("user.dir") + "/"+properties.get("exps.output").toString();
						queryfilename += String.format("%s_pruned_%s.%s.sql", qname, this.dbname, this.approach);
						QueryUtils.storeQuery(countQuery, queryfilename);
					}
					else if(this.properties.get("query.store").equals("QUERY")){
						String queryfilename = System.getProperty("user.dir") + "/"+properties.get("exps.output").toString();
						queryfilename += String.format("%s_%s.%s.sql", qname, this.dbname, this.approach);
						QueryUtils.storeQuery(sqlCode, queryfilename);

					}
					else if(this.properties.get("query.store").equals("SUMARRY")){
						String queryfilename ;
						queryfilename = System.getProperty("user.dir") + "/"+properties.get("exps.output").toString();
						queryfilename += String.format("%s_pruned_%s.%s.sql", qname, this.dbname, this.approach);
						QueryUtils.storeQuery(countQuery, queryfilename);
					}

					start = System.nanoTime();
					if(properties.get("exps.stats").toString().equals("true")) {
						// n union is n +1 sub-queries
						if(this.NREF>0)this.NREF++;
						if(this.NPRUN>0)this.NPRUN++;
						List<String> cqs;
						List<String> bestcqs = new ArrayList<String>();
						String queryfilename = System.getProperty("user.dir") + "/"+properties.get("exps.output").toString();
						queryfilename += String.format("%s.%s.sql", qname, this.approach);
						bestNPRUN=0;
						cost = 0;
						cqs = QueryUtils.loadFile(queryfilename);
						int cqlidx = 2;
						if(!this.approach.equals("UCQ")) {
							cqlidx = 3;
						}
						for (String cq : cqs) {
							if(cq.startsWith("3")) {
								bestNPRUN++;
								bestcqs.add(StringUtils.split(cq, "^^")[cqlidx]);
							}
						}

						bestQuery = String.join(" union ", bestcqs);
						if(prunedSQL.isEmpty()) {
							cost = 0;
						}else {
							cost = connector.eval(prunedSQL.toLowerCase());
						}

						if(bestcqs.isEmpty()) {
							bestcost = 0;
						}else {
							bestcost = connector.eval(bestQuery);
						}

						worsecost = connector.eval(sqlCode);

						rowExps.add(qname);
						rowExps.add(this.NREF+"");
						rowExps.add(this.NPRUN+"");
						rowExps.add(bestNPRUN+"");
						rowExps.add(worsecost+"");
						rowExps.add(cost+"");
						rowExps.add(bestcost+"");

						try {
							//System.out.println("new rows : "+String.join(",", rowExps));
							writer.write(String.format("%s\n", String.join(",", rowExps)));
							writer.flush();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}else {
						if(prunedSQL.isEmpty()) {
							this.NANS = 0;
						}else {
//							int n = Integer.parseInt((String) this.properties.get("database.timeout"))/1000;
//							connector.settimeout(n);
							this.NANS = connector.getCount(countQuery);
						}

					}
					this.EXEC_TIME  = (System.nanoTime()- start)/1000000;

					this.TTOTAL = this.TREF + this.TPRUN + this.EXEC_TIME;




				}else {

					if(this.pruner != null && this.NREF>0 &&  this.NREF < maxref) {
						int timeout =(int) (this.connector.getTimeout()/(this.NREF +1));
						this.connector.settimeout(Math.max(timeout/1000, 1));
						System.out.println("prunning: "+qname);
						this.pruner.setTimer(0);
						prunedSQL = pruner.pruneQuery(sqlCode, query, stats, docost);
						// this.TPRUN  = (System.nanoTime()- start)/1000000;
						this.TPRUN  = this.pruner.getTimer();
						System.out.println("end prunning: "+qname);
						this.connector.settimeout(this.connector.getOtimeout());
					}else {
						prunedSQL = sqlCode;
					}

					this.NPRUN = QueryUtils.countMatches(prunedSQL, "UNION");


					countQuery = prunedSQL.toLowerCase();
					countQuery = QueryUtils.normalize(countQuery);
					if(!countQuery.contains("count(*)")) {
						//System.out.println("No count aggregator found!!");
						countQuery = String.format(" with qf as (%s) select count(*) from qf", countQuery);
					}

					countQuery = QueryUtils.clean(countQuery);
					//int n = Integer.parseInt((String) this.properties.get("database.timeout"))/1000;
					//connector.settimeout(n);
					start = System.nanoTime();
					if(prunedSQL.isEmpty()) {
						this.NANS = 0;
					}else {
						this.NANS = connector.getCount(countQuery);
					}
					this.EXEC_TIME  = (System.nanoTime()- start)/1000000;

					this.TTOTAL = this.TREF + this.TPRUN + this.EXEC_TIME;
					if(this.NANS < 0) {
						//this.EXEC_TIME = this.NREF = this.TREF = this.NPRUN = this.TPRUN = this.TTOTAL = this.NANS;
						this.EXEC_TIME = this.TTOTAL = this.NANS;
					}
					//"query	EXEC_TIME	NANS	NREF	TREF	NPRUN	TPRUN	TOTAL"
					rowExps.add(qname);
					rowExps.add(this.EXEC_TIME+"");
					rowExps.add(this.NANS+"");
					rowExps.add(this.NREF+"");
					rowExps.add(this.TREF+"");
					rowExps.add(this.NPRUN+"");
					rowExps.add(this.TPRUN+"");
					rowExps.add(this.TTOTAL+"");

					try {
						writer.write(String.format("%s\n", String.join(",", rowExps)));
						writer.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(this.NANS < 0) {
						break;
					}
				}

			}


		}

		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//this.connector.set_random_page_cost(4);
		this.connector.close();
	}



	public void  teval() throws SQLException{

		Cover cbest = null;
		String sqlCode, prunedSQL;

		List<String> queries = getQueries();

		Set<Atomic> atoms ;
		FileWriter writer = null;

		String out = System.getProperty("user.dir") + "/"+properties.get("exps.output").toString();
		out += String.format("%s.%s.", this.dbname, this.approach);
		boolean stats, docost;
		stats  = this.properties.get("query.savesubcq").toString().equals("true");
		docost = this.properties.get("exps.method").toString().equals("theoric");

		List<String> rowExps ;
		String qname;
		List<Integer> pExps;

		double cost, cost2, tcost, scost;

		if(properties.get("database.pruning").toString().equals("true") && properties.get("pruning.summary").toString().equals("true")) {
			out += "stats.prune.csv";
		}
		else if(properties.get("database.pruning").toString().equals("true") && properties.get("pruning.summary").toString().equals("false")) {
			out += "stats.db.prune.csv";
		}
		else{
			out += "stats.ref.csv";
		}

		try {
			writer = new FileWriter(out);
			writer.write("query,NREF,COST\n");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CostModel cm;
		for (String query : queries) {

			qname = QueryUtils.getQName(query);
			atoms = JUCQ.parseDLPQuery(query);
			rowExps = new ArrayList<String>();

			switch (this.approach){
				case "UCQ": {

					//cbest = JUCQ.getBestCover(atoms,connector, 0);
					rapid = new UCQ(properties);
					break;
				}

				case "USCQ": {
					//cbest = JUCQ.getBestCover(atoms,connector, 1);
					compact = new USCQ(properties);
					break;
				}

				case "JUCQ": {
					jucq = new JUCQ(properties);
					jucq.resetTime();
					//jucq.getCrootOpti(atoms);
					cbest = jucq.GCOV(atoms, connector);
					break;
				}
				default:
					throw new IllegalArgumentException("Unexpected APPROACH value: " + this.approach);
			}

			try {
				if(compact != null) {
					sqlCode = compact.reformulate(query);

				}else if(rapid != null) {
					sqlCode = rapid.reformulate(query);
				}else {
					sqlCode = cbest.toSQL();
				}
			}
			catch( Exception e){

				try {
					rowExps.add(qname);
					rowExps.add(this.NREF+"");
					rowExps.add(this.NPRUN+"");
					rowExps.add("-2");
					writer.write(String.format("%s\n", String.join(",", rowExps)));
					writer.flush();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}


				continue;
			}

			if(this.nexp >1) {
				this.nexp = 1;
			}
			pExps = new ArrayList<Integer>();
			for (int i = 0; i<this.nexp; i++) {
				pExps.add(i);
			}
			String[] tmp;
			String tcq;

			for(Integer r : ProgressBar.wrap(pExps, String.format("Running exps for %s...", qname))) {
				rowExps = new ArrayList<String>();

				cost = 0;

				tcost = 0;
				scost = 0;
				sqlCode = rapid.reformulate(query);


				for (String cq  : sqlCode.split("UNION")) {
					tmp = cq.split("FROM");
					tcq = String.format("Select exists(select 1 from %s limit 1)", tmp[1]);
					cost = connector.eval(tcq);
					tcost += cost;
					System.out.println("Sur base: "+cost);

					cost2 = connector.eval(tcq.replace("table", "tablesum"));
					System.out.println("Sur summary: "+cost2);
					if(cost2 > cost + 10) {
						System.out.println("C'est pas bon bon ça");
						System.out.println(String.format("cq cost on DB: %f", cost));
						System.out.println(String.format("cq cost on summary: %f", cost2));
						System.out.println("cq: "+cq);
						System.out.println("cq: "+tcq);
					}

					scost += cost2;
				}


				System.out.println("Total  cost on DB: "+tcost);
				System.out.println("Total  cost on summary: "+scost);
				System.exit(0);

				if(this.pruner != null) {
					pruner.setCost(0);
					prunedSQL = pruner.pruneQuery(sqlCode, query, stats, docost);

					if(prunedSQL.isEmpty()) {
						cost = 0;
					}else {
						//cost = connector.eval(prunedSQL.toLowerCase());
						cm = new CostModel(connector, prunedSQL);
						cost = cm.getCost();
					}
					cost += pruner.getCost();
				}else {
					prunedSQL = sqlCode;
					//cost = connector.eval(prunedSQL.toLowerCase());
					cm = new CostModel(connector, query);
					cost = cm.getCost();
				}

				System.out.println("Pruner cost: "+pruner.getCost());
//				System.out.println("Total  cost: "+tcost);
//				System.out.println("Pruner cost: "+pruner.getCost());
//				System.exit(0);
				this.NPRUN = QueryUtils.countMatches(prunedSQL, "UNION");
				if(this.NPRUN>0)this.NPRUN++;

				rowExps.add(qname);
				rowExps.add(this.NPRUN+"");
				rowExps.add(cost+"");

				try {
					//System.out.println("new rows : "+String.join(",", rowExps));
					writer.write(String.format("%s\n", String.join(",", rowExps)));
					writer.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
}

    public static void main( String[] args )
    {
    	if (args.length == 0) {
            System.out.println("you need to provide a config file and a database name. Ex java -jar dlssummary.jar config.properties");
            System.exit(0);
        } else if (args.length == 2) {


            String propertiesPath = System.getProperty("user.dir") + "/" + args[0];
			String debugger;
			InputStream input;
			try {
				input = new FileInputStream(propertiesPath);
				Properties prop = new Properties();
				prop.load(input);
				debugger = prop.get("debugger.log_level").toString();
				if(!debugger.equals("OFF")){
					System.out.println("Running exps with config file: " + args[0]);
					System.out.println("Running exps using database: " + args[1]);
					System.out.println("Using RDBMS: "+prop.get("database.engine"));
				}
				QueryDL querydl ;
				if(prop.get("exps.method").toString().equals("theoric")) {
					System.out.println("Running queries evaluation with theorical model");
					querydl = new QueryDL(prop, args[1]);
					querydl.teval();
				}else {
					if(prop.get("exps.stats").toString().equals("true")) {
						prop.setProperty("exps.runs", "1");
						prop.setProperty("query.savesubcq", "true");
						System.out.println("Time to query the data base for stats");
					}
					querydl = new QueryDL(prop, args[1]);
					querydl.eval();
				}



			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

        }else {
        	System.out.println("you gave too many options");
            System.exit(0);
        }

    }



	public static LoadedOntology getOntRef() {
		return ontRef;
	}


    public static Connector getConnector() {
		return connector;
	}

    public static String getOntofile() {
		return ontofile;
	}
}
