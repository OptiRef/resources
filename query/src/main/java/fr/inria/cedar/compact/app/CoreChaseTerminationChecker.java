package fr.inria.cedar.compact.app;



import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import fr.inria.cedar.compact.corechasefinitenesschecker.DerivationTree;
import fr.inria.cedar.compact.corechasefinitenesschecker.DerivationTreeExtender;
import fr.inria.cedar.compact.corechasefinitenesschecker.DerivationTreeSimplifier;
import fr.inria.cedar.compact.loader.RuleLoader;
import fr.inria.cedar.compact.mess.Util;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.RuleSet;
import fr.lirmm.graphik.graal.core.ruleset.IndexedByBodyPredicatesRuleSet;
import fr.lirmm.graphik.util.Apps;

public class CoreChaseTerminationChecker {

	public final static String PROGRAM_NAME = "CCTC-0.0.1";
	public final static String ontoRepo = "../../boundedness/trunk/experiments/TestCorpus/TestCorpus/nonMSA/OBO/chebi3.owl.dlp";



	public static boolean checkLinearity(RuleSet rules){
		for (Rule rule:rules){
			if (!Util.isLinear(rule)){
				return false;
			}
		}
		return true;
	}
	
	public static boolean checkTermination(IndexedByBodyPredicatesRuleSet rules){
		DerivationTree dt = new DerivationTree(rules);
		DerivationTreeExtender dte = new DerivationTreeExtender(dt,rules);
		dte.saturate();
		DerivationTreeSimplifier dts = new DerivationTreeSimplifier(dt);
		dts.simplify();
		return !(new fr.inria.cedar.compact.corechasefinitenesschecker.CycleDetector(dt).isCyclicByTarjan());
	}
	
	public static void main(String[] args){
		CoreChaseTerminationChecker options = new CoreChaseTerminationChecker();

		JCommander commander = null;
		try {
			commander = new JCommander(options);
			commander.parse(args);
		} catch (com.beust.jcommander.ParameterException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}

		if (options.version) {
			Apps.printVersion(PROGRAM_NAME);
			System.out.println("");
			System.exit(0);
		}
		
		String pathRules = "";
		if (options.input_filepath.equals("-")){
			System.out.println("A set of rules should be given: -r");
			System.exit(1);
		}
		else{
			pathRules = options.input_filepath;
		}

		IndexedByBodyPredicatesRuleSet rules = new IndexedByBodyPredicatesRuleSet();
		
		RuleLoader rl = new RuleLoader(rules,pathRules);
		rl.load();
		if (!checkLinearity(rules)){
			System.out.println("Only Linear Rules are supported. Exit.");
			System.exit(1);
		}
		System.out.println("The core chase terminates on all instances: " + checkTermination(rules));

	}

	@Parameter(names = { "-r", "--rule-file" },
			description = "Ontology file")
	private String input_filepath = "-";


	@Parameter(names = { "-V", "--version" }, description = "Print version information")
	private boolean version = false;



}
