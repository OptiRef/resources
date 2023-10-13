package fr.inria.cedar.compact.loader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;

import fr.lirmm.graphik.graal.api.io.ParseException;
import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.io.dlp.DlgpParser;

/**
 * 
 * @author Michael Thomazo (INRIA)
 *
 */
public class QueryLoader {

	Collection<ConjunctiveQuery> queries;
	String path;

	public QueryLoader(Collection<ConjunctiveQuery> q, String p){
		this.queries = q;
		this.path = p;
	}

	/**
	 * Parse the file located at path and load all the queries present in that file to queries.
	 */
	
	public void load(){
		DlgpParser parser = null;
		
		try {
			parser = new DlgpParser(new FileInputStream(this.path));
		} catch (FileNotFoundException e) {
			System.err.println("Could not open file: " + this.path);
			System.err.println(e);
			e.printStackTrace();
			System.exit(1);
		}
		
		try {
			while (parser.hasNext()) {
				Object o = parser.next();
				if (o instanceof ConjunctiveQuery) {
					ConjunctiveQuery q = (ConjunctiveQuery)o;
					this.queries.add(q);
				}
			}
		} catch (ParseException e) {
			System.err.println("An error occurred while parsing this file: " + this.path);
			System.err.println(e);
			e.printStackTrace();
			System.exit(1);
		}
	}
}	
