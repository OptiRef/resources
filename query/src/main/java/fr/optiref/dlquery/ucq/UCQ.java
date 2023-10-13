package fr.optiref.dlquery.ucq;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import fr.optiref.dlquery.DL2SQL;
import fr.optiref.dlquery.uscq.USCQConverter;

public class UCQ {

	private Properties properties;
	private String ontologypath;
	private String querypath;
	private DL2SQL rapid;

	long timer;
	public UCQ(Properties _properties) {
		properties = _properties;
		ontologypath = System.getProperty("user.dir") + "/"+properties.get("database.ontology").toString();
		rapid = DL2SQL.getInstace();
		//rapid.setPrefixes(prefixes);
	}
	public long getTimer() {
		return timer;
	}

	public String reformulate(String query){
		String ucqQuery = "";
		timer = 0;
		//System.out.println(query);
		long start ;


		start = System.nanoTime();
		rapid.setaQuery(query);
		rapid.reformulate();
		timer += (System.nanoTime()- start)/1000000;
		List refs = rapid.getRewritingQueries();
		if(properties.get("reformulation.log").equals("true")){
			System.out.println("USCQ reformulations : "+refs);
		}
		ucqQuery = String.join(" \nUNION\n", rapid.getReformulatedQueries());
		//System.out.println(ucqQuery);

		return ucqQuery;
	}

}
