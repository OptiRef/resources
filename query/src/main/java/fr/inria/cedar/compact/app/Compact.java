
package fr.inria.cedar.compact.app;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedList;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import fr.inria.cedar.compact.loader.QueryLoader;
import fr.inria.cedar.compact.loader.RuleLoader;
import fr.inria.cedar.compact.localsaturator.HashLocalSaturator;
import fr.inria.cedar.compact.localsaturator.LocalSaturator;
import fr.inria.cedar.compact.nonlocalrewriter.NaiveNonLocalRewriter;
import fr.inria.cedar.compact.nonlocalrewriter.NonLocalRewriter;
import fr.inria.cedar.compact.query.SemiConjunctiveQuery;
import fr.inria.cedar.compact.query.SemiConjunctiveQueryLinkedList;
import fr.inria.cedar.compact.query.UnionSemiConjunctiveQuery;
import fr.inria.cedar.compact.query.UnionSemiConjunctiveQueryLinkedList;
import fr.inria.cedar.compact.sqltranslator.UnionSemiConjunctiveQueryTranslator;
import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.VariableGenerator;
import fr.lirmm.graphik.graal.core.DefaultVariableGenerator;
import fr.lirmm.graphik.graal.core.ruleset.IndexedByHeadPredicatesRuleSet;
import fr.lirmm.graphik.util.Apps;

/**
 * 
 * @author Michael Thomazo (INRIA)
 *
 */
public class Compact{

	//TODO: logging
	public final static String PROGRAM_NAME = "Compact 0.0.1";
	public final static String viewName = "disj";
	public static UnionSemiConjunctiveQueryLinkedList solve(ConjunctiveQuery query, IndexedByHeadPredicatesRuleSet rules){
		SemiConjunctiveQuery scquery = new SemiConjunctiveQueryLinkedList(query);
		VariableGenerator varGenerator = new DefaultVariableGenerator("fresh_var");//
		UnionSemiConjunctiveQueryLinkedList result = new UnionSemiConjunctiveQueryLinkedList();
		Collection<SemiConjunctiveQuery> toExplore = new LinkedList<>();

		result.add(scquery);
		toExplore.add(scquery);
		while (!toExplore.isEmpty()){
			Collection<SemiConjunctiveQuery> created = new LinkedList<>();
			for (SemiConjunctiveQuery current:toExplore){
				//Saturate current with respect to local rewritings
				LocalSaturator saturator = null;
				saturator = new HashLocalSaturator(current,rules,varGenerator);
				saturator.saturateInPlace();
				//Generates non local rewritings	
				NonLocalRewriter NLRewriter = new NaiveNonLocalRewriter(current, rules,varGenerator);
				Collection<SemiConjunctiveQuery> rewritings = NLRewriter.rewrites();
				if (rewritings != null)
				{
					created.addAll(rewritings);	
				}
			}

			toExplore = new LinkedList<>();
			//check that created queries are indeed novel, and if that is the case, queue them for exploration
			for (SemiConjunctiveQuery potential:created){
				if (!result.entails(potential)){
					result.addAndRemoveRedundant(potential);
					toExplore.add(potential);
				}
			}
		}
		result.clean("_ans");
		return result;
	}

	public static void main(String[] args){
		Compact options = new Compact();

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

		String pathQueries = "";
		if (options.query_filepath.equals("-")){
			System.out.println("A set of queries should be given: -q");
			System.exit(1);
		}
		else{
			pathQueries = options.query_filepath;
		}

		String outfolder = options.outputPath;

		long start = System.nanoTime();
		long timer = 0;
		IndexedByHeadPredicatesRuleSet rules = new IndexedByHeadPredicatesRuleSet();

		Collection<ConjunctiveQuery> queries = new LinkedList<>();
		RuleLoader rl = new RuleLoader(rules,pathRules);
		rl.load();

		QueryLoader ql = new QueryLoader(queries, pathQueries);
		ql.load();
		timer += (System.nanoTime()- start)/1000000;
		String outputFileName = options.querfile;;
		int count = -1;
		for(ConjunctiveQuery query : queries){
			count++;
			start = System.nanoTime();
			UnionSemiConjunctiveQuery uscq = solve(query,rules);
			if (options.uscqOutput){
				System.out.println("Rewriting query " + count + ":\n" + uscq);
			}
			UnionSemiConjunctiveQueryTranslator uscqt = new UnionSemiConjunctiveQueryTranslator(uscq);
			String result = uscqt.generateSQL(viewName);
			timer += (System.nanoTime()- start)/1000000;

			// if (query.getLabel() != ""){
			// 	outputFileName = query.getLabel(); 
			// 			}
			// else
			// 	outputFileName = "query" + count;

			
			try (Writer writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outfolder + "/" + outputFileName + ".sql"), "utf-8"))) 
			{
				writer.write(result);
			}
			catch (Exception e){
				e.printStackTrace();
			}
			
		}
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outfolder+ "/" + outputFileName+".time.txt"), "utf-8"))) 
		{
			writer.write(timer+"");
		}
		catch (Exception e){
			e.printStackTrace();
		}
		System.out.println("Terminated.");
	}

	@Parameter(names = { "-r", "--rule-file" },
			description = "Ontology file")
	private String input_filepath = "-";

	@Parameter(names = { "-q", "--query-file" },
			description = "Query file")
	private String query_filepath = "-";

	@Parameter(names = { "-V", "--version" }, description = "Print version information")
	private boolean version = false;

	@Parameter(names = { "-u", "--uscq" }, description = "Output the USCQ version on the standard output")
	private boolean uscqOutput = false;

	@Parameter(names = { "-o", "--output-folder" }, description = "Folder to place the rewritings")
	private String outputPath= "./output";
	@Parameter(names = { "-f", "--output-file" }, description = "Folder to place the rewritings")
	private String querfile = "query0";


}
