package fr.irisa.shaman.dlquery.uscq;

import java.util.ArrayList;
import java.util.Arrays;

public class View {
	String viewName; 
	String viewVariables;
	ArrayList<SelectQuery> selectStatements;
	public View(String viewStr) {
		selectStatements = new ArrayList<SelectQuery>();
		//System.out.println("Begin View");
		viewStr = viewStr.trim();
		//System.out.println("viewStr---");
		//System.out.println(viewStr);
		//System.out.println("viewStr---");
		
		String headPart = viewStr.substring(0, viewStr.indexOf("AS SELECT"));
		headPart = headPart.replace("CREATE TEMPORARY VIEW ", "");
		headPart = headPart.trim();
		viewName = headPart.substring(0,headPart.indexOf(" "));
		viewVariables = headPart.substring(headPart.indexOf("(")+1,headPart.indexOf(")"));
		
		String unionPart = viewStr.substring(viewStr.indexOf("AS SELECT")+3, viewStr.indexOf(";"));
		unionPart = unionPart.trim();
		//System.out.println(unionPart);
		String unionList[] = unionPart.split(" UNION ");
		//System.out.println(Arrays.toString(unionList));
		
		for(int i=0; i<unionList.length;i++) {
			
			selectStatements.add(new SelectQuery(unionList[i]));
			//System.out.println(i);
			//System.out.println(selectStatements.get(i));
		}
		//System.out.println("Done View");
		
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
//	public static void main(String[] args) {
//		View test = new View("CREATE TEMPORARY VIEW disj_0_4 (c0, c1) AS SELECT \"Faculty\".c0 FROM \"Faculty\" AS \"Faculty\" UNION SELECT \"hasFaculty\".c1 FROM \"hasFaculty\" AS \"hasFaculty\" UNION SELECT \"teacherOf\".c0 FROM \"teacherOf\" AS \"teacherOf\" UNION SELECT \"isPartOfUniversity\".c0 FROM \"isPartOfUniversity\" AS \"isPartOfUniversity\" UNION SELECT \"Lecturer\".c0 FROM \"Lecturer\" AS \"Lecturer\" UNION SELECT \"PostDoc\".c0 FROM \"PostDoc\" AS \"PostDoc\" UNION SELECT \"Professor\".c0 FROM \"Professor\" AS \"Professor\" UNION SELECT \"advisor\".c1 FROM \"advisor\" AS \"advisor\" UNION SELECT \"tenured\".c0 FROM \"tenured\" AS \"tenured\" UNION SELECT \"Subj13Professor\".c0 FROM \"Subj13Professor\" AS \"Subj13Professor\" UNION SELECT \"Subj6Professor\".c0 FROM \"Subj6Professor\" AS \"Subj6Professor\" UNION SELECT \"FullProfessor\".c0 FROM \"FullProfessor\" AS \"FullProfessor\" UNION SELECT \"VisitingProfessor\".c0 FROM \"VisitingProfessor\" AS \"VisitingProfessor\" UNION SELECT \"Subj8Professor\".c0 FROM \"Subj8Professor\" AS \"Subj8Professor\" UNION SELECT \"Subj19Professor\".c0 FROM \"Subj19Professor\" AS \"Subj19Professor\" UNION SELECT \"Subj2Professor\".c0 FROM \"Subj2Professor\" AS \"Subj2Professor\" UNION SELECT \"Subj16Professor\".c0 FROM \"Subj16Professor\" AS \"Subj16Professor\" UNION SELECT \"Subj12Professor\".c0 FROM \"Subj12Professor\" AS \"Subj12Professor\" UNION SELECT \"Subj17Professor\".c0 FROM \"Subj17Professor\" AS \"Subj17Professor\" UNION SELECT \"Subj7Professor\".c0 FROM \"Subj7Professor\" AS \"Subj7Professor\" UNION SELECT \"Subj1Professor\".c0 FROM \"Subj1Professor\" AS \"Subj1Professor\" UNION SELECT \"Subj4Professor\".c0 FROM \"Subj4Professor\" AS \"Subj4Professor\" UNION SELECT \"AssistantProfessor\".c0 FROM \"AssistantProfessor\" AS \"AssistantProfessor\" UNION SELECT \"Chair\".c0 FROM \"Chair\" AS \"Chair\" UNION SELECT \"Subj18Professor\".c0 FROM \"Subj18Professor\" AS \"Subj18Professor\" UNION SELECT \"Subj15Professor\".c0 FROM \"Subj15Professor\" AS \"Subj15Professor\" UNION SELECT \"ExDean\".c0 FROM \"ExDean\" AS \"ExDean\" UNION SELECT \"Subj3Professor\".c0 FROM \"Subj3Professor\" AS \"Subj3Professor\" UNION SELECT \"Subj20Professor\".c0 FROM \"Subj20Professor\" AS \"Subj20Professor\" UNION SELECT \"Subj14Professor\".c0 FROM \"Subj14Professor\" AS \"Subj14Professor\" UNION SELECT \"AssociateProfessor\".c0 FROM \"AssociateProfessor\" AS \"AssociateProfessor\" UNION SELECT \"Subj11Professor\".c0 FROM \"Subj11Professor\" AS \"Subj11Professor\" UNION SELECT \"Subj9Professor\".c0 FROM \"Subj9Professor\" AS \"Subj9Professor\" UNION SELECT \"Subj5Professor\".c0 FROM \"Subj5Professor\" AS \"Subj5Professor\" UNION SELECT \"Dean\".c0 FROM \"Dean\" AS \"Dean\" UNION SELECT \"Subj10Professor\".c0 FROM \"Subj10Professor\" AS \"Subj10Professor\" ;");
//		
//		System.out.println(Arrays.deepToString(test.selectStatements.toArray()));
//	}
}
