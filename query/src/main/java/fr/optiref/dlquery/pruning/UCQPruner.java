package fr.optiref.dlquery.pruning;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import fr.optiref.dlquery.Connector;
import fr.optiref.dlquery.DL2SQL;
import fr.optiref.dlquery.QueryUtils;

public class UCQPruner extends Pruner{

	public UCQPruner(String _alias, Connector _conn, Properties _properties) {
		// TODO Auto-generated constructor stub
		this.alias = _alias;
		this.conn = _conn;
		this.properties = _properties;
		pruning_summary = properties.get("pruning.summary").equals("true");
		cost = 0;
	}

	@Override
	public String pruneQuery(String query, String dlpquery, boolean stats, boolean docost) throws SQLException {
		List<String>prunedUCQ = new ArrayList<String>();
		List<String>UCQs = new ArrayList<String>();
		//System.out.println("query : "+dlpquery);
		String qid = QueryUtils.getQName(dlpquery);
		String pquery;
		long start, x;
		boolean limit1 = properties.get("database.use_limit1").toString().equals("true");
		boolean exists = properties.get("database.use_exists").toString().equals("true");

		boolean hasAnswer, empty;
		setTimer(0);
		int icq = 0;
		if(stats){
			String origin = DL2SQL.getInstace().getSQL(dlpquery);
			UCQs.add(String.format("origin^^-1^^%s", origin));
		}
		//this.conn.settimeout(this.conn.getSumtimeout());

		for(String cq: query.split("\\nUNION\\n")) {

			start = System.nanoTime();
			/*empty = isEmpty(cq);
			timer += (System.nanoTime()- start)/1000000;
			//System.out.println("isEmpty finished!!");
			//this.conn.settimeout(this.conn.getSumtimeout());
			if(empty) {
				if(stats){
					UCQs.add(String.format("0^^%d^^%s", icq, cq));
				}
				icq++;
				continue;
			}*/
			pquery = pruneBody(cq);
			//System.out.println("cq : "+cq);
			if(docost) {
				double c = this.conn.eval(pquery);
				//System.out.println("cost : "+c);
				cost += c;
			}
			//Select exists(
			if(limit1) {
				pquery = String.format("Select distinct 1 from %s limit 1", pquery.toLowerCase().split(" from ")[1]);
			}
			else {
				pquery = String.format("Select 1 from %s ", pquery.toLowerCase().split(" from ")[1]);
			}
			if(exists) {
				if(this.conn.getEngine().equals("DB2")){
					pquery = String.format("select 1 from table%s99 where exists( %s )", alias, pquery);
				}else {
					pquery = String.format("select exists( %s )", pquery);
				}
			}
			 //}
			//System.out.println(query);
			this.conn.clearstmt();
			start = System.nanoTime();
			//cost = this.conn.eval(pquery);
			hasAnswer = this.conn.hasAnswer(pquery);
			timer += (System.nanoTime()- start)/1000000;

			//System.out.println("hasAnswer finished!!");

			if(hasAnswer) {
				//System.out.println(cq);
				prunedUCQ.add(cq);

				if(stats){
					hasAnswer = this.conn.hasAnswer(cq);
					if(hasAnswer) {
						UCQs.add(String.format("3^^%d^^%s", icq, cq));
					}else{
						UCQs.add(String.format("2^^%d^^%s", icq, cq));
					}
				}
			}else{
				if(stats){
					UCQs.add(String.format("1^^%d^^%s", icq, cq));
				}
			}
			icq++;

		}

		if(stats){
			String queryfilename = System.getProperty("user.dir") + "/"+properties.get("exps.output").toString();
			queryfilename += String.format("%s.UCQ.sql", qid);
			QueryUtils.storeQuery(String.join("\n\n", UCQs), queryfilename);
		}

		//this.conn.settimeout(this.conn.getOtimeout());
		//System.out.println("prunning: "+qid);

		return String.join("\nUNION\n", prunedUCQ);

	}

}
