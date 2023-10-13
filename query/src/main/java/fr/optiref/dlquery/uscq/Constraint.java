package fr.optiref.dlquery.uscq;

import java.util.ArrayList;
import java.util.Arrays;


public class Constraint {
	Variable firstVar;
	Variable secondVar;
	String secondVarCte;
	boolean doubleVars;

	String sign;

	public Constraint(String c) {
		//System.out.println("Constraint : "+c);
		c=c.trim();
		String temp = c.substring(c.indexOf(" "));
		temp = temp.trim();
		temp = temp.substring(0,temp.indexOf(" "));
		sign = temp.trim();

		temp = c.substring(0,c.indexOf(sign)).trim();
		firstVar = new Variable(temp);
		temp = c.substring(c.indexOf(sign)+sign.length()).trim();

		if(temp.contains(".c0")||temp.contains(".c1")) {
			secondVar = new Variable(temp);
			doubleVars = true;
		}else {
			secondVarCte = temp;
			doubleVars = false;
 		}


	}
	public String toString() {
		if(doubleVars) {
			return firstVar +" sign:"+sign+" "+secondVar;
		}else {
			return firstVar +" sign:"+sign+" "+secondVarCte;
		}
	}

	public static ArrayList<Constraint> buildWhereConstraints(String wherePart) {
		//System.out.println("Clause where : "+wherePart);
		wherePart = wherePart.replace("WHERE", "").trim();
		ArrayList<Constraint> constraints = new ArrayList<Constraint>();
		if(wherePart.contains("AND")) {
			String constList[] = wherePart.split("AND");
			for(int i=0;i<constList.length;i++) {
				constraints.add(new Constraint(constList[i]));
			}

		}else if(!wherePart.equals("")){
			constraints.add(new Constraint(wherePart));
		}

		return constraints;
	}


	public Variable getFirstVar() {
		return firstVar;
	}
	public Object getSecondVar() {
		if(hasDoubleVars()) {
			return secondVar;
		}

		return secondVarCte;
	}
	public String getSign() {
		return sign;
	}
	public boolean hasDoubleVars() {
		return doubleVars;
	}


}
