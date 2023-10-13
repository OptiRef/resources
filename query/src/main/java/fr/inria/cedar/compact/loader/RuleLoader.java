package fr.inria.cedar.compact.loader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import fr.lirmm.graphik.graal.api.core.NegativeConstraint;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.RuleSet;
import fr.lirmm.graphik.graal.api.io.ParseException;
import fr.lirmm.graphik.graal.io.dlp.DlgpParser;

public class RuleLoader {

	private final RuleSet rules;
	private final RuleSet constraints;
	private final String path;

	public RuleLoader(RuleSet r, String p){
		this.rules = r;
		this.path = p;
		this.constraints = null;
	}

	public RuleLoader(RuleSet positiveRules, RuleSet constraints, String p){
		this.rules = positiveRules;
		this.constraints = constraints;
		this.path = p;
	}


	/**
	 * Parse the file located at path and load all the rules present in that file to rules.
	 */

	public void load(){
		int currentRuleID = 0;
		DlgpParser parser = null;
		try { parser = new DlgpParser(new FileInputStream(this.path));}
		catch (FileNotFoundException e) {
			System.err.println("Could not open file: " + this.path);
			System.err.println(e);
			e.printStackTrace();
			System.exit(1);
		}

		try {
			while (parser.hasNext()) {
				Object o = parser.next();
				if (o instanceof Rule && !(o instanceof NegativeConstraint)) {
					Rule r = (Rule)o;
					if (r.getLabel() == null || r.getLabel().equals(""))
						r.setLabel("R" + currentRuleID++);
					this.rules.add(r);
				}
				else
					if (o instanceof NegativeConstraint && this.constraints!=null){
						NegativeConstraint c = (NegativeConstraint) o;
						this.constraints.add(c);
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
