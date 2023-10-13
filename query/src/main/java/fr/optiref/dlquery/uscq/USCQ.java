package fr.optiref.dlquery.uscq;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

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
import fr.optiref.dlquery.QueryUtils;


public class USCQ {

	private Properties properties;
	private String ontologypath;
	private String querypath;
	private String queryfile;
	long timer;
	private Map<String, String> prefixes;

	public long getTimer() {
		return timer;
	}
	public USCQ(Properties _properties) {
		properties = _properties;
		ontologypath = System.getProperty("user.dir") + "/"+properties.get("compact.ontology").toString();
		querypath    = System.getProperty("user.dir") + "/"+properties.get("compact.queryDir").toString();
		queryfile    = properties.get("database.queries").toString();
		prefixes = QueryUtils.getPrefix(queryfile);

	}

	public  UnionSemiConjunctiveQueryLinkedList solve(ConjunctiveQuery query, IndexedByHeadPredicatesRuleSet rules){
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
	public String reformulate(String oquery){
		USCQConverter usqcConverter = USCQConverter.getInstace(properties);
		String uscqQuery = "";
		String viewName = "disj";
		//String ucqQuery = "";
		timer = 0;

		long start = System.nanoTime();


		IndexedByHeadPredicatesRuleSet rules = new IndexedByHeadPredicatesRuleSet();

		Collection<ConjunctiveQuery> queries = new LinkedList<>();
		RuleLoader rl = new RuleLoader(rules,ontologypath);
		rl.load();
		String uquery = QueryUtils.toUSCQ(oquery, prefixes);

		String queryName = QueryUtils.getQName(oquery);
		//System.out.println("in USCQ queryName"+queryName);
		QueryUtils.storeQuery(uquery, querypath+"/"+queryName+".txt");
		QueryLoader ql = new QueryLoader(queries, querypath+"/"+queryName+".txt");

		ql.load();

		System.out.println("queries : "+queries);

		String result;

		for(ConjunctiveQuery query : queries){
			//System.out.println("processing query : "+query);
			UnionSemiConjunctiveQuery uscq = solve(query,rules);
			UnionSemiConjunctiveQueryTranslator uscqt = new UnionSemiConjunctiveQueryTranslator(uscq);
			timer += (System.nanoTime()- start)/1000000;
			result = uscqt.generateSQL(viewName);
			if(properties.get("debugger.log_level").toString().equals("DEBUG")){
				System.out.println("result : "+result);
			}
			if(properties.get("reformulation.log").equals("true")){
				System.out.println("USCQ reformulations : "+uscqt);
			}
			uscqQuery = usqcConverter.getUSCQ(result);
			if(properties.get("debugger.log_level").toString().equals("DEBUG")){
				System.out.println("result2:"+uscqQuery);
			}

		}



		return uscqQuery;
	}
}
