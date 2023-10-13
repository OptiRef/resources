package fr.optiref.dlquery.jucq;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import edu.ntua.isci.common.dl.LoadedOntology;
import fr.optiref.dlquery.Connector;
import fr.optiref.dlquery.QueryDL;
import fr.optiref.dlquery.costmodel.CostModel;

//EXPLAIN FORMAT=JSON select count(*) from table99;
public class JUCQ {


	static String head, body;
	static Set<String>headVars;
	static String JUCQquery;
	final Logger JUCQLOGGER =  Logger.getAnonymousLogger();
	final boolean verbose = false;
	final boolean debug = false;
	int MAXBUDGET ;
	long totalTime = 0;
	static private DisjointUnionSets ds;
	static private Map<String, Integer> encoder = new HashMap<>();
	static private Map<String, Set<Integer>> depEncoder = new HashMap<>();
	Properties properties;
	String debugger;

	public JUCQ(Properties _properties) {
		properties = _properties;
		if(properties.get("jucq.maxbudget") != null){
			MAXBUDGET = Integer.parseInt(properties.get("jucq.maxbudget").toString());
		}else{
			MAXBUDGET = 300;
		}
		this.debugger = properties.get("debugger.log_level").toString();
		//getCrootOpti2(new HashSet<Atomic>());

	}
	public static String getBody() {
		return body;
	}
	public static Set<String> getHeadVars() {
		return headVars;
	}

	public static String getJUCQquery() {
		return JUCQquery;
	}
	public long getTotalTime() {
		return totalTime;
	}

	public void resetTime() {
		totalTime = 0;
	}
	public Set<Atomic> parseQuery(String filename){
		Set<Atomic> triples = new HashSet<Atomic>();
		BufferedReader reader;
		String line;
		String body;
		try {
			reader = new BufferedReader(new FileReader(filename));
			line = reader.readLine();
			if(line != null) {
				body = line.split(":-")[1];
				//System.out.println(body);
				for(String t: body.split(",")) {
					triples.add(new Triple(t));
				}

			}
		} catch ( IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(triples);
		return triples;
	}


	public static Set<Atomic> parseDLPQuery(String query){
		JUCQquery = query;
		Set<Atomic> triples = new HashSet<Atomic>();
		String[] q;
		String tmp;
		q = query.split(" <- ");
		head = q[0];
		body = q[1];
		headVars = new HashSet<String>();
		tmp = head.split("\\(")[1].replace(")", "");
		for(String h: tmp.split(",")) {
			headVars.add(h);
		}
		//System.out.println(body);
		for(String t: body.split(", ")) {
			triples.add(new Triple(t));
		}

		return triples;
	}

	public boolean contains(Set<Cover> covers, Cover c) {
		for (Cover cover : covers) {
			if(cover.equals(c)) {
				return true;
			}
		}
		return false;
	}


	public Cover getSCQ(Set<Atomic> atoms) {
		Set<Atomic> tmp ;
		Cover C0 = new Cover();
		for (Atomic triple : atoms) {
			tmp = new HashSet<Atomic>();
			tmp.add(triple.copy());
			C0.add(new Fragment(tmp));
		}
		return C0;
	}

	public Cover getUCQ(Set<Atomic> atoms) {
		Set<Atomic> tmp = new HashSet<Atomic>();
		Cover C0 = new Cover();
		for (Atomic triple : atoms) {
			tmp.add(triple.copy());
		}
		C0.add(new Fragment(tmp));
		return C0;
	}

	public Cover getTMPCover(Set<Atomic> atoms) {
		int i=0;
		Set<Atomic> tmp = new HashSet<Atomic>();
		List<Atomic> toto =  new ArrayList<Atomic>();
		toto.addAll(atoms);
		Cover C0 = new Cover();
		tmp.add(toto.get(0));
		tmp.add(toto.get(1));
		tmp.add(toto.get(4));
		C0.add(new Fragment(tmp));
		tmp = new HashSet<Atomic>();
		tmp.add(toto.get(2));
		tmp.add(toto.get(3));
		C0.add(new Fragment(tmp));
		return C0;
	}



	public static void createDepOptim() {

		//String uri;
		//String ontofile = properties.get("jucq.maxbudget")
		LoadedOntology ontRef = QueryDL.getOntRef();
		OWLOntology ontology = ontRef.getMainOntology();
		//System.out.println(ontology.getAxiomCount());
		List<OWLAxiom> axioms = new ArrayList<OWLAxiom>();
		List<List<Integer>> pairs = new ArrayList<>();
		List<Integer> pair;
		Set<Integer> deps;

		axioms.addAll(ontology.getAxioms());
		String[] tmpAxiomArray;
		String[] tmps;
		int sencoding = 0, oencoding = 0;
		int encodingKey = 0;
		String tmp, s, o, l;
		Map<String, Set<Integer>> crmap = new HashMap<String, Set<Integer>>();
		for (OWLAxiom owlAxiom : axioms) {

				pair = new ArrayList<>();
				tmp = owlAxiom.getAxiomWithoutAnnotations().toString();



				if(!tmp.contains(" "))continue;
				if(tmp.contains("rdfs:label"))continue;
				if(tmp.contains("AnnotationAssertion"))continue;
				if(tmp.contains("Declaration"))continue;

				//if(tmp.contains("rdfs:label") && !tmp.contains("AnnotationAssertion"))continue;

				//System.out.println(tmp);

				// tmp = tmp.replce("AnnotationAssertion(", "");
				tmpAxiomArray = tmp.split(" ");
				l = "";
				if(tmpAxiomArray.length == 3) {
					//System.out.println(tmp);

					s = tmpAxiomArray[0];
					o = tmpAxiomArray[1];
					l = tmpAxiomArray[2];
				//	System.out.println(l);
				}else {
					s = tmpAxiomArray[0];
					o = tmpAxiomArray[1];

				}




				if(o.contains("#")){
					o = o.substring(o.indexOf("#")+1, o.indexOf(">"));
				}else if(o.contains("/")){
					o = o.substring(o.lastIndexOf("/")+1, o.indexOf(">"));
				}


				if(s.contains("#")){
					s = s.substring(s.indexOf("#")+1, s.indexOf(">"));
				}else if(s.contains("/")){
					s = s.substring(s.lastIndexOf("/")+1, s.indexOf(">"));
				}

				if(l.contains("#")){
					l = l.substring(l.indexOf("#")+1, l.indexOf(">"));
				}else if(s.contains("/")){
					l = l.substring(l.lastIndexOf("/")+1, l.indexOf(">"));
				}
				if(l.contains(")")){
					l = l.replace(")", "");
				}




				if(encoder.containsKey(o)){
					oencoding = encoder.get(o);
				}else{
					encoder.put(o, encodingKey);
					oencoding = encodingKey;
					encodingKey++;
				}
				if(encoder.containsKey(s)){
					sencoding = encoder.get(s);
				}else{
					encoder.put(s, encodingKey);
					sencoding = encodingKey;
					encodingKey++;
				}

				if(tmpAxiomArray.length == 3) {
					if(!encoder.containsKey(l)){
						encoder.put(l, encodingKey);
						encodingKey++;

					}
					if(crmap.containsKey(l)) {
						deps = crmap.get(l);
						deps.add(encoder.get(s));
						deps.add(encoder.get(o));
						crmap.put(l, deps);
					}else {
						deps = new HashSet<>();
						deps.add(encoder.get(s));
						deps.add(encoder.get(o));
						deps.add(encoder.get(l));
						crmap.put(l, deps);
					}
				}
				if(crmap.containsKey(o)) {
					deps = crmap.get(o);
					deps.add(encoder.get(s));
					crmap.put(o, deps);
				}else {
					deps = new HashSet<>();
					deps.add(encoder.get(s));
					deps.add(encoder.get(o));
					crmap.put(o, deps);
				}


				/*pair.add(oencoding);
				pair.add(sencoding);
				pairs.add(pair);*/
		}
		Map<Integer, String> reversedencoder = new HashMap<>();
		for (Entry<String, Integer> entry : encoder.entrySet()) {
			reversedencoder.put(entry.getValue(), entry.getKey());
		}
		Map<String, Set<String>> fcrmap = new HashMap<String, Set<String>>();
		Set<Integer> dset;
		Set<Integer>  fset;
		Set<Integer> atomes = new HashSet<>();
		for (Entry<String, Set<Integer>> entry : crmap.entrySet()) {
        	atomes.addAll(entry.getValue());
        	atomes.add(encoder.get(entry.getKey()));
        	//System.out.println(String.format("%s: %s", entry.getKey(), entry.getValue()) );
		}
		//System.out.println(atomes);
		for (Integer atom : atomes) {
			if(!crmap.containsKey(reversedencoder.get(atom))) {
				deps = new HashSet<>();
				deps.add(atom);
				crmap.put(reversedencoder.get(atom), deps);
			}else {
				deps = crmap.get(reversedencoder.get(atom));
				deps.add(atom);
				crmap.put(reversedencoder.get(atom), deps);
			}
		}
		int maxValue = encoder.size() + 1;
		int setkey = 0;
		Map<String, Integer> setencoder = new HashMap<>();
        ds = new DisjointUnionSets(maxValue);

        for (Entry<String, Set<Integer>> entry : crmap.entrySet()) {
        	dset = entry.getValue();
        	fset = new HashSet<Integer>();

        	while(dset.size() != fset.size()) {
        		fset.addAll(dset);
        		for (Integer atom : fset) {
        			dset.addAll(crmap.get(reversedencoder.get(atom)));
        		}
        	}
        	//System.out.println(String.format("%s: %s", entry.getKey(), dset) );
        	depEncoder.put(entry.getKey(), dset);
		}



	}

	public Cover getCrootOpti2(Set<Atomic> atoms) {

		if(atoms == null)return null;
		List<BitSet> uscq = new ArrayList<BitSet>();
		List<Atomic> listAtoms = new ArrayList<Atomic>(atoms);
		BitSet scq;
		int n = atoms.size();
		Cover Croot ;
		Set<Integer> p1, p2;
		Triple t1 , t2;
		DisjointUnionSets fragmentDS = new DisjointUnionSets(n);
		Set<Atomic> tatoms;
		Fragment ftmp;
		int key;
		//System.out.println(String.format("atomes: %s -> size: %d", atoms, n));
		for(int i=0; i<n; i++) {
			scq = new BitSet(n);
			scq.set(i);
			uscq.add(scq);
		}
		//System.out.println(String.format("bitset 0 : %s -> size: %d", uscq.get(0), uscq.size()));
		for (int i=0; i<n; i++){

			t1 = (Triple)listAtoms.get(i);
			//System.out.println(t1);
			if(t1.isConcept()) {
				p1 = depEncoder.get(t1.getO());
				if(p1 == null)continue;
			}else {
				p1 = depEncoder.get(t1.getP());
				if(p1 == null)continue;
			}

			for (int j=i+1; j<n; j++){
				//if(a1.equals(a2))continue;
				t2 = (Triple)listAtoms.get(i);
				if(t2.isConcept()) {
					p2 = depEncoder.get(t2.getO());
					if(p2 == null)continue;
				}else {
					p2 = depEncoder.get(t2.getP());
					if(p2 == null)continue;
				}

				p1.retainAll(p2);

				if(!p1.isEmpty()) {
					fragmentDS.union(j, i);
					//break;
				}
			}
		}

		for (int i=0; i<n; i++){
			//System.out.println(String.format("%s -> %d", f1, fkey));
			key = fragmentDS.find(i);
			//System.out.println(String.format("Dep %d -> %d", fkey, key));
			//if the fragment is alone in the joinunionset, so it will be alone at the end, we ignore it, or it will be the parent of other fragment, union will be done later
			if(i != key) {
				//System.out.println(String.format("Dep %d -> %d", i, key));
				uscq.get(key).or(uscq.get(i) );
				uscq.get(i).clear();
			}

		}
		//System.exit(0);
		Croot = new  Cover();
		boolean empty = true;
		for(BitSet fragment : uscq) {
			tatoms = new HashSet<Atomic>();
			empty = true;
			for(int i=0; i<n; i++) {
				if(fragment.get(i)) {
					tatoms.add(listAtoms.get(i));
					empty = false;
				}
			}
			if(!empty) {
				ftmp = new Fragment(tatoms);
				Croot.add(ftmp);
			}

		}
		return Croot;
	}

	public Cover getCrootOpti(Set<Atomic> atoms) {

		if(atoms == null)return null;
		Cover Croot = getSCQ(atoms);
		Set<Integer> p1, p2;
		//Integer crkey;
		Triple t1 , t2;
		DisjointUnionSets fragmentDS = new DisjointUnionSets(atoms.size() + 1);
		Map<Integer, Set<Atomic>> amaps = new HashMap<Integer, Set<Atomic>>();
		Map<Integer, Integer> fid2id = new HashMap<Integer,Integer>();
		Set<Atomic> tatoms;
		Fragment ftmp;
		int fid = 0;
		for (Fragment f1 : Croot.getFragments()){
			fid2id.put(f1.getFid(), fid);
			fid++;
		}
		for (Fragment f1 : Croot.getFragments()){
			amaps.put(fid2id.get(f1.getFid()), f1.getatoms());
			for (Atomic a1 : f1.getatoms()){
				t1 = (Triple)a1;
				//System.out.println(t1);
				if(t1.isConcept()) {
					p1 = depEncoder.get(t1.getO().split(":")[1]);
					if(p1 == null)continue;
					//p1 = ds.find(crkey);
					//System.out.println("atome1 :"+ t1.getO().split(":")[1]+"#");
				}else {
					p1 = depEncoder.get(t1.getP().split(":")[1]);
					if(p1 == null)continue;
					//System.out.println("atome1 :"+ t1.getP().split(":")[1]+"#");
					//if(crkey == null)continue;
					//p1 = ds.find(crkey);
				}
				for (Fragment f2 : Croot.getFragments()){
					if(f1.equals(f2))continue;
					for (Atomic a2 : f2.getatoms()){
						t2 = (Triple)a2;

						if(t2.isConcept()) {
							p2 = depEncoder.get(t2.getO().split(":")[1]);
							if(p2 == null)continue;
							///if(crkey == null)continue;
							//p2 = ds.find(crkey);
							//System.out.println("atome2 :"+ t2.getO().split(":")[1]+"#");
						}else {
							p2 = depEncoder.get(t2.getP().split(":")[1]);
							if(p2 == null)continue;
							//System.out.println("atome2 :"+ t2.getP().split(":")[1]+"#");
							//if(crkey == null)continue;
							//p2 = ds.find(crkey);
						}
						/*if(t1.toString().contains("Student") && t2.toString().contains("teacherOf")) {
							System.out.println("p1: "+p1);
							System.out.println("p2: "+p2);
						}*/
						p1.retainAll(p2);
						/*if(t1.toString().contains("Student") && t2.toString().contains("teacherOf")) {
							System.out.println("p1 and p2: "+p1);
							System.exit(0);
						}*/
						if(!p1.isEmpty()) {
							//System.out.println("Dep found!!!");
							fragmentDS.union(fid2id.get(f1.getFid()), fid2id.get(f2.getFid()));
							break;
						}
					}
				}
			}
		}
		int key, fkey;
		for (Fragment f1 : Croot.getFragments()){
			fkey = fid2id.get(f1.getFid());
			//System.out.println(String.format("%s -> %d", f1, fkey));
			key = fragmentDS.find(fkey);
			//System.out.println(String.format("Dep %d -> %d", fkey, key));
			//if the fragment is alone in the joinunionset, so it will be alone at the end, we ignore it, or it will be the parent of other fragment, union will be done later
			if(fkey != key) {
				tatoms = amaps.get(key);
				tatoms.addAll(f1.getatoms());
				amaps.put(key, tatoms);
				amaps.remove(fkey);
			}



		}
		//System.exit(0);
		Croot = new  Cover();
		for(int skey: amaps.keySet()) {
			ftmp = new Fragment(amaps.get(skey));
			Croot.add(ftmp);
		}
		return Croot;
	}


	public Cover getBestCover(Set<Atomic> atoms, Connector conn, int type) {
		Cover cbest = null;
		switch (type) {
	        case 0:
	        	cbest = getUCQ(atoms);
	            break;
	        case 1:
	        	cbest =  getSCQ(atoms);
	        	break;
	        case 2:
	        	cbest = GCOV(atoms,conn);
	        	System.out.println(cbest);
	        	break;
	        default:
	        	System.err.println("wrong args for type");

		}
        	return cbest;
	}

	public double getCost(Connector c, Cover cover){
		c.settimeout(Integer.parseInt(properties.get("cost.model.timeout").toString()) );
		double eval = Double.MAX_VALUE;
		if(properties.get("cost.model").equals("RDBMS")) {
			eval = c.eval(cover.toSQL());
		}else {
			CostModel cmodel = new CostModel(c, cover.toSQL() );
			eval = cmodel.getCost();
		}
		c.settimeout(Integer.parseInt(properties.get("database.timeout").toString())/1000);
		return eval;
	}
	public Cover GCOV(Set<Atomic> atoms, Connector conn){
		System.out.println("running GCOV");
		long costTime = 0;
		totalTime = 0;
		Cover C0;
		Cover C, Cbest, Cucq;
		double score, bestscore = Double.MAX_VALUE;
		double ucqscore = 0;
		long start, cstart;
		Set<Cover> analyzed = new HashSet<Cover>();
		Queue<Cover> moves = new ArrayDeque<Cover>();
		Fragment tmpf;
		boolean timeup = false, tmpbool;
		//Time to compute Croot and init gcov
		if(properties.get("cost.model").equals("RDBMS")) {
			conn.clean_explain_statement();
		}
		Cucq = getUCQ(atoms);
		//ucqscore = getCost(conn, Cucq);
		start = System.nanoTime();
        C0 = getCrootOpti(atoms);
        totalTime += (System.nanoTime() - start)/1000000;
        System.out.println("JUCQ Croot : "+C0);
        System.out.println("JUCQ Croot took: "+totalTime);
        if(C0.size() == 1) {
        	return C0;
        }
		if(properties.get("jucq.croot").equals("true") ){
			System.out.println("Croot only ");
			return C0;
		}
		Cbest = C0;
		moves.add(C0);
		conn.settimeout(Integer.parseInt(properties.get("cost.model.timeout").toString()) );
		cstart = System.nanoTime();
		bestscore = getCost(conn, C0);
		costTime =  System.nanoTime() - cstart;
		totalTime += (System.nanoTime() - costTime - start)/1000000;
		System.out.println("time to get getCost:  "+costTime/1000000000);
		conn.settimeout(Integer.parseInt(properties.get("database.timeout").toString())/1000);
		/*if(ucqscore < bestscore) {
			return Cucq;
		}*/
		//costTime += conn.getTimer();
		costTime = 0;
		start = System.nanoTime();
		//System.gc();

		while(!moves.isEmpty()) {
			C0 = moves.poll();
			timeup = (System.nanoTime() - costTime - start)/1000000 >= MAXBUDGET;
			if(timeup)break;
			//System.out.println("timeup : "+timeup);
			for (Fragment f : C0.getFragments()){
				for (Atomic t : atoms){

					if(!f.connected(t)){
						continue;
					}
					tmpf = f.copy();
					//System.out.println("copy fragment : "+tmpf);
					if(!tmpf.hasatom(t)) {
						tmpf.add(t);
					}
					C =  C0.copy();
					//start = System.nanoTime();
					C.removeIncludedFragments(tmpf);
					// System.out.println("removeIncludedFragments:  "+(System.nanoTime() - start)/1000000);
					C.add(tmpf);
					if(!C.isMinimal(atoms)) continue;
					if(!contains(analyzed, C)) {
						analyzed.add(C);
						cstart = System.nanoTime();
						score = getCost(conn, C);
						costTime +=  System.nanoTime() - cstart;
						//costTime += conn.getTimer();
						if(score < bestscore) {
							Cbest = C;
							bestscore = score;
							moves.add(C);
						}
					}
					timeup = (System.nanoTime()  - costTime - start)/1000000 >= MAXBUDGET;
					if(timeup)break;

				}

				if(timeup)break;
			}
			if(timeup)break;
		}

		totalTime += (System.nanoTime() - costTime - start)/1000000;
		if(!this.debugger.equals("OFF")){
			System.out.println("Final Cover : "+Cbest);
			System.out.println("Final best score : "+bestscore);
		}
//		if(ucqscore < bestscore) {
//			return Cucq;
//		}
		//System.gc();
		if(properties.get("reformulation.log").equals("true")){
			System.out.println("JUCQ reformulations : "+Cbest);
		}
		return Cbest;

	}


}
