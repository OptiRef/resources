package fr.optiref.dlquery.pruning;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.mysql.cj.jdbc.exceptions.MySQLTimeoutException;

import fr.optiref.dlquery.Connector;

public abstract class Pruner {

	String alias;
	Properties properties;
	Connector conn;
	long timer;
	double cost;
	boolean pruning_summary;
	public Pruner() {
		// TODO Auto-generated constructor stub
		//timer = 0;
	}
	public void setCost(double cost) {
		this.cost = cost;
	}
	public double getCost() {
		return cost;
	}
	public void setTimer(long timer) {
		this.timer = timer;
	}
	public long getTimer() {
		return timer;
	}
	public int getSNode(int graphnode) {
		String query = String.format("select summarynode from %s where graphnode=%d", properties.get("database.summary_dictionary_table_name").toString(), graphnode);
		//System.out.println(query);
		int timeout = conn.getTimeout();
		conn.settimeout(conn.getOtimeout());
		int res = -1;

		try {
			//long start = System.nanoTime();
			res = conn.getAnswer(query);
			//timer += (System.nanoTime()- start)/1000000;
		} catch (SQLException e) {
			// TODO Auto-generated catch block

			String state = e.getSQLState();
			if (e instanceof MySQLTimeoutException || state.equals("57014") ||state.equals("HY000") )
		    {
			   System.out.println(String.format("computing equivalent class for %d timeout",graphnode));
		       return -1;
		    }
		    else
		    {
		    	e.printStackTrace();
		    }
		}
		conn.settimeout(timeout);
		//System.out.println(" const time "+timer);
		return res;
	}

	public boolean isEmpty(String cq) {
		List<String>tables = Arrays.asList(cq.split(" FROM ")[1].split("  WHERE ")[0].split("\\,"));

		boolean empty ;
		String query ;

		for(String table: tables) {
			if(pruning_summary) {
				query = table.split("AS")[0].replace(" table"," table"+alias);
			}else {
				query = table.split("AS")[0];
			}
			empty = this.conn.isEmpty(query);
			if(empty)return true;
		}

		return false;
	}
	public boolean isConcept(String name, Map<String, String> tablesNames) {
		if(tablesNames.get(name) != null) {
			//long start = System.nanoTime();
			return conn.getMap(tablesNames.get(name)) >0;
			//timer += (System.nanoTime()- start)/1000000;
		}
		return false;
	}
	public String pruneBody(String cq){
		String[] wherearray ;
		String[] andarray ;
		//System.out.println(cq);
		String query ;
		String namec, namer;
		int equi1, equi2;
		List<String> summaryProjections = new ArrayList<String>() ;
		Map<String, String> tablesNames = new HashMap<String,String>();
		Map<String, Integer> fromtable = new HashMap<String, Integer>();
		if(!pruning_summary) {
			return cq;
		}
		query = cq.replace(" table", " table"+alias);

		//System.out.println(query);

		String prunedQuery = "";
		String[] cond;
		String finalFrom = " FROM ";
		Set<String> andList = new HashSet<String>();
		int graphnode, summarynode;
		if(!query.contains("WHERE")){
			return query;
		}

		//System.out.println(tablesNames);
		/*if(properties.get("exps.use_summary_constants").equals("true")) {
			for(String t: query.split(" FROM ")[1].split(" WHERE")[0].split(" , ") ) {
				wherearray = t.split(" AS ");
				tablesNames.put(wherearray[1].replace(" ", ""), wherearray[0].replace(" ", ""));
			}
			wherearray = query.split(" WHERE ");
			andarray = wherearray[1].split(" AND ");
			for(String and : andarray){

				cond = and.replace(" ", "").split("=");
				//for(String joined : cond) {
				//System.out.println(joined);
				namec = cond[0].split("\\.")[0];
				namer = cond[1].split("\\.")[0];

				if(tablesNames.get(namec) != null &&  tablesNames.get(namer) != null) {
					equi1 = conn.getMap(tablesNames.get(namec));
					equi2 = conn.getMap(tablesNames.get(namer));

					if(equi1 < 0 && equi2>=0 ) { //namec is role and namer is concept
						query = query.replace(cond[1], cond[0]) + String.format(" AND %s = %d", cond[0], equi2); //replace all the
					}
					if(equi1 >= 0 && equi2<0 ) { //namec is role and namer is concept
						query = query.replace(cond[0], cond[1]) + String.format(" AND %s = %d", cond[1], equi1); //replace all
						//System.out.println(String.format("%s.c0 = %d", name, equivalent));
					}
				}

			}

			for(String t: query.split(" FROM ")[1].split(" WHERE")[0].split(" , ") ) {
				wherearray = t.split(" AS ");
				if(conn.getMap(tablesNames.get(wherearray[1].replace(" ", ""))) <0) {
					finalFrom += " "+t+", ";
				}
			}
			finalFrom += "#";
			finalFrom = finalFrom.replace(", #", "");
		}*/
		wherearray = query.split(" WHERE ");
		if(properties.get("exps.use_summary_constants").equals("true")) {
			prunedQuery += wherearray[0].split(" FROM ")[0] + finalFrom + " WHERE ";
		}else {
			prunedQuery += wherearray[0] + " WHERE ";
		}
		andarray = wherearray[1].split(" AND ");
		for(String and : andarray){
			cond = and.replace(" ", "").split("=");
			if(cond[0] == cond[1])continue;
			try {
				graphnode   = Integer.parseInt(cond[1].replace(" ",""));
				summarynode = getSNode(graphnode);
				andList.add(cond[0] + " = "+summarynode);
			}
			catch(ArrayIndexOutOfBoundsException e) {
				System.out.println(" This is not normal string "+and);
			}
			catch (NumberFormatException nfe) {
				andList.add(and);
			}
		}

		prunedQuery += String.join(" AND ", andList) ;
		/*System.out.println("prunebody finished!!");
		System.out.println(cq);
		System.out.println(prunedQuery);
		System.exit(0);*/
		return prunedQuery;

	}

	public abstract  String pruneQuery(String query, String qid, boolean stats, boolean cost) throws SQLException;


}
