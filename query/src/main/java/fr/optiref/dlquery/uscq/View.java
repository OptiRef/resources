package fr.optiref.dlquery.uscq;

import java.util.ArrayList;
import java.util.Arrays;

public class View {
	String viewName;
	String viewVariables;
	ArrayList<SelectQuery> selectStatements;
	public View(String viewStr) {
		selectStatements = new ArrayList<SelectQuery>();
		viewStr = viewStr.trim();

		String headPart = viewStr.substring(0, viewStr.indexOf("AS SELECT"));
		headPart = headPart.replace("CREATE TEMPORARY VIEW ", "");
		headPart = headPart.trim();
		viewName = headPart.substring(0,headPart.indexOf(" "));
		viewVariables = headPart.substring(headPart.indexOf("(")+1,headPart.indexOf(")"));

		String unionPart = viewStr.substring(viewStr.indexOf("AS SELECT")+3, viewStr.indexOf(";"));
		unionPart = unionPart.trim();
		String unionList[] = unionPart.split(" UNION ");

		for(int i=0; i<unionList.length;i++) {

			selectStatements.add(new SelectQuery(unionList[i]));
		}

	}

	public String toString() {
		return "viewName: "+ viewName+" viewVariables: "+viewVariables;
	}

	public String getName() {
		return viewName;
	}
	public String getVars() {
		return viewVariables;
	}
	public ArrayList<SelectQuery> getSelectQueries(){
		return selectStatements;
	}

}
