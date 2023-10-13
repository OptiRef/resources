package fr.optiref.dlquery.uscq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SelectQuery {

	private static String SELECT = "SELECT";
	private static String FROM = "FROM";
	private static String WHERE = "WHERE";

	//Select Part building
	String  selectPart;
	String[] selectVariablesList;
	public Variable[] selectVariables;

	//From part
	String fromPart;
	public String[] fromTables;
	public Map<String, String> fromTableAliases = new HashMap<>();

	//Where part
	String wherePart;
	ArrayList<Constraint> constraints;
	public boolean hasWherePart;

	public SelectQuery(String selectStr) {
		if(selectStr.contains(SELECT)) {
			selectPart = selectStr.substring(selectStr.indexOf(SELECT), selectStr.indexOf(FROM)).trim();
			String temp = selectPart.substring(selectStr.indexOf(" ")+1);
			selectVariablesList = temp.split(", ");
			selectVariables = Variable.buildSelectVariablesList(selectVariablesList);

		}else {
			selectPart = "";
		}
		if(selectStr.contains(WHERE)) {
			hasWherePart=true;
			fromPart = selectStr.substring(selectStr.indexOf(FROM), selectStr.indexOf(WHERE)).trim();
			wherePart = selectStr.substring(selectStr.indexOf(WHERE));

		}else {
			hasWherePart=false;
			fromPart = selectStr.substring(selectStr.indexOf(FROM)).trim();
			wherePart="";
		}
		fromTables = fromPart.substring(fromPart.indexOf(" ")+1).split(",");
		for(int i=0; i<fromTables.length;i++) {
			String temp1;
			String temp2;
			if(fromTables[i].contains("AS")) {
				temp1 = fromTables[i].substring(0, fromTables[i].indexOf("AS"));
				temp2 = fromTables[i].substring(fromTables[i].indexOf("AS")+2);
			}else {
				temp1 = fromTables[i].trim();
				temp2  = fromTables[i].trim();
			}
			if(temp2.contains("#")) {
				temp2 = temp2.split("#")[1];
			}else {
				temp2 = temp2.substring(temp2.lastIndexOf("/")+1, temp2.length());
			}

			fromTableAliases.put(temp2.trim().replace("\"", ""), temp1.trim().replace("\"", ""));
		}

		constraints = Constraint.buildWhereConstraints(wherePart);


	}




	public String toString() {
		if(hasWherePart) {
			return Arrays.deepToString(selectVariables) +"\n"+ Arrays.deepToString(fromTables)+"\n"+Arrays.deepToString(constraints.toArray());
		}else {
			return Arrays.deepToString(selectVariables) +"\n"+ Arrays.deepToString(fromTables);
		}

	}
	public ArrayList<Constraint> getConstraints(){
		return constraints;
	}

	public static void main(String[] args) {
		SelectQuery test = new SelectQuery("SELECT \"Subj4Course\".c0, \"Subj4Course\".c1 FROM \"Subj4Course\" AS \"Subj4Course\", \"teacherOf\" AS  \"teacherOf_3\", disj_0_0 WHERE disj_0_5.c1 = disj_0_6.c0 AND disj_0_5.c1 = disj_0_7.c1 ");

		System.out.println(Arrays.deepToString(test.constraints.toArray()));
		System.out.println(test);
	}
}
