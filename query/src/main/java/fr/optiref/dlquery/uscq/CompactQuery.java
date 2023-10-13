package fr.optiref.dlquery.uscq;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CompactQuery {

	private ArrayList<View> viewsList;
	private ArrayList<SelectQuery> mainQueries;

	BufferedReader qbf;


	public CompactQuery(String query) throws IOException{

		//qbf = new BufferedReader(new FileReader(queryFile));

		viewsList = new ArrayList<View>();
		mainQueries=new ArrayList<SelectQuery>();


		//String qline;
		int countView=0;
		for(String qline : query.split("\\\n")) {

			if (qline.startsWith("CREATE TEMPORARY VIEW ")) {
				countView++;
				//System.out.println("Current view count: "+countView);
				//System.out.println(qline);
				viewsList.add(new View(qline));

			}
			if (qline.startsWith("SELECT ")) {
				qline = qline.replace(";", "");
				if(qline.contains("UNION SELECT")) {
					String linesList[] = qline.split("UNION");
					for(int i=0;i<linesList.length;i++) {
						mainQueries.add(new SelectQuery(linesList[i].trim()));
					}
				}else {
					//System.out.println(qline.trim());
					mainQueries.add(new SelectQuery(qline.trim()));
				}
			}
		}

	}


	public ArrayList<View> getViews() {
		return viewsList;
	}

	public ArrayList<SelectQuery> getMainQuery() {
		return mainQueries;
	}


}
