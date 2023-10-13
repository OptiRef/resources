package fr.optiref.dlquery.pruning;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import fr.optiref.dlquery.Connector;
import fr.optiref.dlquery.DL2SQL;
import fr.optiref.dlquery.QueryUtils;

public class USCQPruner extends Pruner  {

	List<String>USCQs;
	int icq;
	public USCQPruner(String _alias, Connector _conn, Properties _properties) {
		// TODO Auto-generated constructor stub
		this.alias = _alias;
		this.conn = _conn;
		this.properties = _properties;
		pruning_summary = properties.get("pruning.summary").equals("true");
	}
	public List<String> purneView(String view, String currViewName, boolean stats, boolean docost) throws SQLException {

		List<String> viewsPrunedAtoms = new ArrayList<String>();
		boolean hasAnswer;
		long start;
		String pquery;
		String[] reserved = QueryUtils.getSqlReserved();
		long x;
		boolean limit1 = properties.get("database.use_limit1").toString().equals("true");
		boolean exists = properties.get("database.use_exists").toString().equals("true");
		//icq = 0;
		//System.out.println("actual time :"+timer);
		this.conn.settimeout(conn.getSumtimeout());
		for(String atom:view.split(" UNION ")) {
			for(String keyword: reserved) {
				keyword = keyword.substring(0, 1).toUpperCase() + keyword.substring(1);
				if(atom.contains(" "+keyword)) {
					atom = atom.replace(" "+keyword, String.format(" %s_1", keyword));
				}
			}

			start = System.nanoTime();
			pquery = pruneBody(atom);
			timer += (System.nanoTime()- start)/1000000;

			if(limit1) {
				pquery = String.format("select 1 from %s limit 1", pquery.toLowerCase().split(" from ")[1]);
			}else {
				pquery = String.format("select 1 from %s ", pquery.toLowerCase().split(" from ")[1]);
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
			if(docost) {
				cost += conn.eval(pquery);
			}
			/*if(x>300) {
				this.conn.setLimit1(false);
				System.out.println("hasAnswer");
				System.out.println(x);
				System.out.println(pquery);
			}*/
			if(hasAnswer) {
				viewsPrunedAtoms.add(atom);
				if(stats){
					if(this.conn.hasAnswer(atom)){
						USCQs.add(String.format("3^^%s^^%d^^%s", currViewName,icq, atom.replace("\n", "")));
					}else{
						USCQs.add(String.format("2^^%s^^%d^^%s", currViewName,icq, atom.replace("\n", "")));
					}
				}
			}else{
				if(stats){
					USCQs.add(String.format("1^^%s^^%d^^%s", currViewName,icq, atom.replace("\n", "")));
				}
			}
			icq++;
		}

		this.conn.settimeout(this.conn.getOtimeout());

		//System.out.println("end time :"+timer);
		return viewsPrunedAtoms;
	}

	public String cleanVars(String[] vars, String emptyView) {
		List<String> cleanedVars = new ArrayList<String>();
		//System.out.println("We have empty view : "+Arrays.asList(emptyView));

		for(String var: vars) {
			//System.out.println("We have vars : "+var);
			if(!var.contains(emptyView)) {
				cleanedVars.add(var);
			}
		}
		//System.out.println("We have cleanedVars : "+cleanedVars);
		return String.join(", ", cleanedVars);
	}

	public String cleanfROM(String[] from, String emptyView) {
		List<String> cleanedWhhere = new ArrayList<String>();

		for(String table: from) {
			if(!table.contains(emptyView)) {
				cleanedWhhere.add(table);
			}
		}
		//System.out.println("The emptyView: "+emptyView);
		//System.out.println("The cleaned from: "+cleanedWhhere);
		return String.join(", ", cleanedWhhere);
	}

	public String cleanWhere(String[] whereCond, String emptyView) {
		List<String> cleanedWhere = new ArrayList<String>();

		for(String cond: whereCond) {
			if(!cond.contains(emptyView)) {
				cleanedWhere.add(cond);
			}
		}
		return String.join(" AND ", cleanedWhere);
	}
	boolean checkEmpty(List<String> emptyViews, String select) {
		boolean join;
		List<String> cleanedSelect = new ArrayList<String>();
		for(String mSelect: select.split("\\n UNION \\n\\n")) {
			join = false;
			for(String view: emptyViews) {
				if(mSelect.contains(view)) {
					join = true;
					break;
				}
			}
			if(!join) {
				cleanedSelect.add(mSelect);
			}
		}
		return cleanedSelect.size() == 0;
	}
	public String cleanMainSelect(List<String> emptyViews, String select) {
		String res = "";
		List<String> cleanedSelect = new ArrayList<String>();
		String[] selectArray ;
		String tmpMS="";
		//System.out.println("Main select: "+select);

		for(String mSelect: select.split("\\n UNION \\n\\n")) {
			tmpMS = mSelect;
			for(String view: emptyViews) {
				res = "";
				if(tmpMS.contains(view)) {
					//System.out.println("We have empty views in this select : "+mSelect);
					selectArray = tmpMS.split("SELECT DISTINCT ")[1].split(" FROM ");
					//System.out.println("The vars : "+selectArray[0]);
					res += "SELECT DISTINCT "+cleanVars(selectArray[0].split(","), view);
					selectArray = selectArray[1].split(" WHERE ");
					//System.out.println("The from : "+selectArray[0]);
					res += " FROM "+cleanfROM(selectArray[0].split(","), view);
					//System.out.println("The where : "+selectArray[1]);
					res += " WHERE "+cleanWhere(selectArray[1].split("AND"), view);
				}
				tmpMS =res;
				//System.out.println("Curr mselec : "+tmpMS);
			}
			if(tmpMS.length() > 0) {
				cleanedSelect.add(tmpMS);
			}
		}


		return String.join("\n UNION \n\n", cleanedSelect);
	}


	public  String pruneQuery(String query, String dlpquery, boolean stats, boolean docost) throws SQLException {

		String res="";
		List<String> views = new ArrayList<String>();
		List<String> emptyViews = new ArrayList<String>();
		String[] tmpArray;
		String with;
		String currView = "";
		String currViewName = "";
		String select;
		String replaceVar;
		List<String> prunedView;
		String viewSelectVar = "";
		USCQs = new ArrayList<String>();


		icq = 0;

		tmpArray = query.split("\\n--#####\\n");
		with = tmpArray[0];
		select = tmpArray[1];
		if(stats){
			USCQs.add(String.format("origin^^main^^-1^^%s", select.replace("\n", "")));
		}
		setTimer(0);
		for(String v: with.replace("WITH ", "").split("\\n--##\\n")) {
			if(v.startsWith("disj")) {

				tmpArray = v.replace(",  ", "").split("AS \\(");
				currViewName = tmpArray[0].replace(" ", "");
				currView = tmpArray[1].split("\\)")[0];

				prunedView = purneView(currView, currViewName, stats, docost);
				if(prunedView.isEmpty()) {
					emptyViews.add(currViewName);
				}else {
					viewSelectVar = currView.split("SELECT ")[1].split(" FROM ")[0].split("\\.")[1];
					if(!prunedView.get(0).contains(viewSelectVar) && !viewSelectVar.contains(",")) {
						prunedView.set(0, prunedView.get(0).replace("FROM", " AS "+viewSelectVar+" FROM "));

					}
					views.add(String.format("%s AS (%s)", currViewName, String.join(" UNION ", prunedView)));
				}
			}
		}

		if(stats){
			String qid = QueryUtils.getQName(dlpquery);
			String queryfilename = System.getProperty("user.dir") + "/"+properties.get("exps.output").toString();
			queryfilename += String.format("%s.USCQ.sql", qid);
			QueryUtils.storeQuery(String.join("\n\n", USCQs), queryfilename);
		}
		res = "";
		if(views.size() > 0){
			res = "WITH  "+ String.join(",\n", views);
		}
		if(emptyViews.size()>0) {
			if(checkEmpty(emptyViews, select)) {
				return "";
			}
			res += "\n\n"+cleanMainSelect(emptyViews, select);
		}else {
			res += "\n\n"+ select;
		}
		return res;

	}

}
