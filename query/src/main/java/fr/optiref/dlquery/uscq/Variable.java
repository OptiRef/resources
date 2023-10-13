package fr.optiref.dlquery.uscq;

import java.util.Arrays;

public class Variable {
	String tableName;
	String colName;
	String uri;

	public Variable(String t, String c) {
		tableName = t.trim().replace("\"", "");
		uri = tableName;
		if(tableName.contains("#")) {
			tableName = tableName.split("#")[1];
		}else {
			tableName = tableName.substring(tableName.lastIndexOf("/")+1, tableName.length());
		}
		colName = c.trim();
	}
	public Variable(String varStr) {

		tableName = varStr.trim().replace("\"", "");
		if(tableName.contains("#")) {
			tableName = tableName.split("#")[1];
		}else {
			tableName = tableName.substring(tableName.lastIndexOf("/")+1, tableName.length());
		}
		tableName = tableName.substring(0, tableName.indexOf("."));
		colName = varStr.substring(varStr.lastIndexOf(".")+1).trim();
	}
	public static Variable[] buildSelectVariablesList(String[] selectVariables) {
		Variable[] output = new Variable[selectVariables.length];

		for(int i=0; i<selectVariables.length;i++) {
			if(selectVariables[i].contains(".")) {
				String temp1 = selectVariables[i].substring(0,selectVariables[i].lastIndexOf("."));
				String temp2 = selectVariables[i].substring(selectVariables[i].lastIndexOf(".")+1);
				output[i] = new Variable(temp1,temp2);
			}

		}

		return output;
	}

	public String getTableName() {

		return tableName;
	}
	public String getUri() {
		return uri;
	}
	public String getColName() {
		return colName;
	}

	public String toString() {
		return "table:"+ tableName+", variable:"+colName;
	}
}
