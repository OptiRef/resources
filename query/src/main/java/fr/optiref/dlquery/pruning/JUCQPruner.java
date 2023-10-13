package fr.optiref.dlquery.pruning;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.optiref.dlquery.Connector;
import fr.optiref.dlquery.jucq.Cover;
import fr.optiref.dlquery.DL2SQL;
import fr.optiref.dlquery.QueryUtils;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Properties;
import java.util.Queue;

public class JUCQPruner extends Pruner {


	List<String>JUCQs;
	int icq;
	public JUCQPruner(String _alias, Connector _conn, Properties _properties) {
		// TODO Auto-generated constructor stub
		this.alias = _alias;
		this.conn = _conn;
		this.properties = _properties;
		pruning_summary = this.properties.get("pruning.summary").equals("true");
	}

	public String pruneUCQ(String query, String dlpquery, boolean stats , boolean docost) throws SQLException {
		List<String>prunedUCQ = new ArrayList<String>();
		List<String>UCQs = new ArrayList<String>();
		boolean limit1 = properties.get("database.use_limit1").toString().equals("true");
		boolean exists = properties.get("database.use_exists").toString().equals("true");

		icq = 0;

		String pquery;
		long start;
		boolean hasAnswer, empty;
		if(stats){
			String origin = DL2SQL.getInstace().getSQL(dlpquery);
			UCQs.add(String.format("origin^^main^^-1^^%s", origin));
		}
		setTimer(0);
		this.conn.settimeout(this.conn.getSumtimeout());
		for(String cq: query.split("\\nUNION\\n")) {



			start = System.nanoTime();
			pquery = pruneBody(cq);
			if(docost) {
				cost += conn.eval(pquery);
			}
			timer += (System.nanoTime()- start)/1000000;
			start = System.nanoTime();
			empty = isEmpty(cq);
			timer += (System.nanoTime()- start)/1000000;
			/*if(empty) {
				if(stats){
					UCQs.add(String.format("0^^qf0^^%d^^%s", icq, cq));
				}
				icq++;
				continue;
			}*/
			//Select exists(
			if(limit1) {
				pquery = String.format("select distinct 1 from %s limit 1", pquery.toLowerCase().split(" from ")[1]);
			}else {
				pquery = String.format("Select 1 from %s", pquery.toLowerCase().split(" from ")[1]);
			}
			if(exists) {
				if(this.conn.getEngine().equals("DB2")){
					pquery = String.format("select 1 from table%s99 where exists( %s )", alias, pquery);
				}else {
					pquery = String.format("select exists( %s )", pquery);
				}
			}
			start = System.nanoTime();
			hasAnswer = this.conn.hasAnswer(pquery);
			timer += (System.nanoTime()- start)/1000000;
			/*long x = (System.nanoTime()- start)/1000000;
			if(x>300) {
				System.out.println("hasAnswer takes: "+x);
				System.out.println(cq);
			}*/
			if(hasAnswer) {
				prunedUCQ.add(cq);

				hasAnswer = this.conn.hasAnswer(cq);
				if(stats){
					if(hasAnswer) {
						UCQs.add(String.format("3^^qf0^^%d^^%s", icq, cq));
					}else{
						UCQs.add(String.format("2^^qf0^^%d^^%s", icq, cq));
					}
				}
			}else{
				if(stats){
					UCQs.add(String.format("1^^qf0^^%d^^%s", icq, cq));
				}
			}
			icq++;




		}
		this.conn.settimeout(this.conn.getOtimeout());
		if(stats){
			String qid = QueryUtils.getQName(dlpquery);
			String queryfilename = System.getProperty("user.dir") + "/"+properties.get("exps.output").toString();
			queryfilename += String.format("%s.JUCQ.sql", qid);
			QueryUtils.storeQuery(String.join("\n\n", UCQs), queryfilename);
		}
		return String.join("\nUNION\n", prunedUCQ);

	}

	public  String pruneQuery(String query, String dlpquery, boolean stats, boolean docost) throws SQLException {

		Set<String> emptyViews = new HashSet<String>();
		Queue<String> moves = new ArrayDeque<String>();
		Set<String> analyzed = new HashSet<String>();
		Set<String> removeViews = new HashSet<String>();
		List<String> views = new ArrayList<String>();
		List<String> selectConds = new ArrayList<String>();
		List<String> finalSelect = new ArrayList<String>();
		List<String> finalFrom = new ArrayList<String>();
		String[] tmpArray ;
		String[] tmpArray2 ;
		String[] cond;
		String pview="", select="", count="", res = "";
		String tmpv, vid, viewName;
		Map<String,List<String>>  viewmap = new HashMap<>();
		HashMap<String, String> selectmap = new HashMap<>();
		List<String>maps = new ArrayList<String>();
		int nviews = 0;
		String[] tmpJucqs;
		JUCQs = new ArrayList<String>();
		String tmpViewMap;
		icq = 0;




		if(!query.contains("-- ##############")){
			return pruneUCQ(query, dlpquery, stats, docost);
		}
		if(stats){
			//String origin = DL2SQL.getInstace().getSQL(dlpquery);
			tmpJucqs = query.split("-- ##############");
			JUCQs.add(String.format("origin^^main^^-1^^%s)", tmpJucqs[tmpJucqs.length - 2].replace("\n","")));
		}
		tmpArray = query.split("-- ##############");

		select = tmpArray[tmpArray.length - 2];
		count = tmpArray[tmpArray.length - 1];
		res = tmpArray[0];

		setTimer(0);
		for (String view : tmpArray) {
			if(!view.contains("as") || view.equals(select) || view.equals(count)) continue;

			pview = pruneView(view, dlpquery, stats, docost);

			if(!pview.contains("as")) {
				//System.out.println("This an empty view :"+pview);
				emptyViews.add(pview);
			}else {
				views.add(pview);
				nviews++;
			}


		}
		if(stats){
			String qid = QueryUtils.getQName(dlpquery);
			String queryfilename = System.getProperty("user.dir") + "/"+properties.get("exps.output").toString();
			queryfilename += String.format("%s.JUCQ.sql", qid);
			QueryUtils.storeQuery(String.join("\n\n", JUCQs), queryfilename);
		}
		if(nviews == 0) {
			//System.out.println("empty ");
			return "";
		}
		res += String.join(",\n", views)+ ",";
		if(select.contains("=")) {
			tmpArray = select.split(" WHERE ");
			select = tmpArray[0];
			for(String and: tmpArray[1].split(" and ")){
				cond = and.split("=");
				tmpViewMap = cond[0].split("\\.")[0];
				if(viewmap.containsKey(tmpViewMap)) {
					maps = viewmap.get(tmpViewMap);
				}else {
					maps = new ArrayList<String>();
				}
				maps.add(cond[1].split("\\.")[0]);
				viewmap.put(tmpViewMap, maps);
				tmpViewMap = cond[1].split("\\.")[0];
				if(viewmap.containsKey(tmpViewMap)) {
					maps = viewmap.get(tmpViewMap);
				}else {
					maps = new ArrayList<String>();
				}
				maps.add(cond[0].split("\\.")[0]);
				viewmap.put(tmpViewMap, maps);
				if(cond[1].contains(".")){
					selectmap.put(cond[0].replace("\n",""), cond[1].replace("\n",""));
					selectmap.put(cond[1].replace("\n",""), cond[0].replace("\n",""));
				}
			}
			moves.addAll(emptyViews);
			//System.out.println("empty views: "+moves);

			while(!moves.isEmpty()) {
				tmpViewMap = moves.poll();
				//System.out.println("view name: "+tmpViewMap);
				if(viewmap.containsKey(tmpViewMap) && !analyzed.contains(tmpViewMap)){
					moves.addAll(viewmap.get(tmpViewMap));
					emptyViews.addAll(viewmap.get(tmpViewMap));
					analyzed.add(tmpViewMap);
				}
			}

			for(String sview: views){
				viewName = sview.split(" ")[0];
				if(emptyViews.contains(viewName)) {
					removeViews.add(sview);
				}
			}

			//System.out.println("final query : " + res);

			views.removeAll(removeViews);
			//System.out.println("final empty views: "+emptyViews);
			//System.out.println("final views size: "+views.size());

			//The non  empty views join with empty views
			if(views.size() == 0) {
				//System.out.println("empty ");
				return "";
			}
			//System.out.println(selectmap);
			//System.out.println(select);
			tmpArray2 = select.split(" FROM ");
			select = " qf as ( select DISTINCT ";
			for(String s: tmpArray2[0].replace("qf as ( select DISTINCT ","").split(", ")){
				tmpv = s.replace("\n","");
				vid = tmpv.split("\\.")[0];
				while(emptyViews.contains(vid)){
					tmpv = selectmap.get(tmpv);
					vid = tmpv.split("\\.")[0];
				}
				finalSelect.add(tmpv);
			}
			//System.out.println("empty views : "+String.join(" ; ",emptyViews));
			for(String s: tmpArray2[1].split(", ")){
				//System.out.println(String.format("#%s#", s));
				if(!emptyViews.contains(s)){
					finalFrom.add(s);
				}
			}
			//System.out.println("finalFrom views : "+String.join(" ; ",finalFrom));
			select += String.join(", ", finalSelect)+ " FROM " + String.join(", ", finalFrom);
			//System.out.println("select : " + select);

			if(emptyViews.size() != 0) {
				for (String and : tmpArray[1].split(" and ")) {
					tmpArray2 = and.split("=");

					if(!emptyViews.contains(tmpArray2[0].split("\\.")[0]) && !emptyViews.contains(tmpArray2[1].split("\\.")[0])){
						selectConds.add(and);
					}

				}
				if(selectConds.size()>0){
					select += " WHERE " + String.join(" AND ", selectConds);
				}

			}else {
				select += " WHERE " + tmpArray[1];
			}
		}

		//System.out.println("select : " + select);

		res += select + count;

		return res;
	}


	/*public String pruneBody(String line){
		String[] wherearray ;
		String[] andarray ;
		String query = line.replace("table", "table"+alias);
		String prunedQuery = "";
		String[] cond;
		List<String> andList = new ArrayList<String>();
		int graphnode, summarynode;
		if(!query.contains("WHERE")){
			return query;
		}
		wherearray = query.split(" WHERE ");
		prunedQuery += wherearray[0] + " WHERE ";
		andarray = wherearray[1].split(" AND ");
		for(String and : andarray){
			cond = and.split(" = ");
			try {
				graphnode   = Integer.parseInt(cond[1].replace(" ",""));
				summarynode = getSNode(graphnode);
				andList.add(cond[0] + " = "+summarynode);
			} catch (NumberFormatException nfe) {
				System.out.println(" This is normal string "+cond[1]);
				andList.add(and);
			}
		}
		prunedQuery += String.join(" AND ", andList);

		return prunedQuery;

	}*/
	public String pruneView(String view, String dlpquery, boolean stats, boolean docost) throws SQLException {
		String res ="";
		String[] lines = view.split("\n") ;
		String qid = "", viewname="";
		String q="";
		List<String> qsum = new ArrayList<String>();
		boolean hasAnswer, empty;
		long start;
		double x ;
		boolean limit1 = properties.get("database.use_limit1").toString().equals("true");
		boolean exists = properties.get("database.use_exists").toString().equals("true");
		//icq = 0;
		this.conn.settimeout(Integer.parseInt((String) this.properties.get("summary.timeout")));
		for (String line : lines) {
			if(line.contains("as(")) {
				qid = line;
				viewname = qid.split(" ")[0];
			}
			if(line.contains("SELECT ")) {

				//start = System.nanoTime();
				empty = isEmpty(line);
				/*x = (System.nanoTime()- start)/1000000;
				if(x>300) {
					System.out.println("isEmpty");
					System.out.println(x);
					System.out.println(line);
				}
				timer += (System.nanoTime()- start)/1000000;
				if(empty) {
					if(stats){
						JUCQs.add(String.format("0^^%s^^%d^^%s", viewname, icq, line));
					}
					icq++;
					continue;
				}*/
				q = pruneBody(line);
				if(docost) {
					cost += conn.eval(q);
				}
				//Select exists(
				if(limit1) {
					q = String.format("Select distinct 1 from %s limit 1 ", q.toLowerCase().split(" from ")[1]);
				}else {
					q = String.format("Select 1 from %s ", q.toLowerCase().split(" from ")[1]);
				}
				if(exists) {
					if(this.conn.getEngine().equals("DB2")){
						q = String.format("select 1 from table%s99 where exists( %s )", alias, q);
					}else {
						q = String.format("select exists( %s )", q);
					}
				}
				start = System.nanoTime();
				hasAnswer = this.conn.hasAnswer(q);
				timer += (System.nanoTime()- start)/1000000;
				/*x = (System.nanoTime()- start)/1000000;
				if(x>300) {
					System.out.println("hasAnswer takes: "+x);
					System.out.println(q);
					System.out.println(line);
				}*/

				//System.out.println(line);
				if(hasAnswer) {
					qsum.add(line);

					if(stats){
						hasAnswer = this.conn.hasAnswer(line);
						if(hasAnswer) {
							JUCQs.add(String.format("3^^%s^^%d^^%s", viewname, icq, line));
						}else{
							JUCQs.add(String.format("2^^%s^^%d^^%s", viewname, icq, line));
						}
					}
				}else{
					if(stats){
						JUCQs.add(String.format("1^^%s^^%d^^%s", viewname, icq, line));
					}
				}
				icq++;



			}
		}
		this.conn.settimeout(this.conn.getOtimeout());
		if(qsum.size() == 0) {
			qid = qid.split(" ")[0];
		}

		res = qid + String.join("\nUNION\n", qsum)+ " )";
		if(!res.contains("SELECT ")) {
			res = qid;
		}
		return res;
	}
}
