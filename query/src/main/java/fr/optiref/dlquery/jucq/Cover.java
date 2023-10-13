package fr.optiref.dlquery.jucq;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.optiref.dlquery.Connector;
import fr.optiref.dlquery.DL2SQL;
import fr.optiref.dlquery.QueryUtils;
public class Cover {

	Set<Fragment> fragments;
	DL2SQL cover2sql;
	String sqlquery="";
	long startTime, endTime, timer;
	long cid;
	static long ID = 0;
	public Cover() {
		fragments = new HashSet<Fragment>();
		cover2sql = DL2SQL.getInstace();
		cid = ID;
		ID++;
	}
	public Cover(Set<Fragment> _fragments) {
		this.fragments = _fragments;
		cover2sql =  DL2SQL.getInstace();
		cid = ID;
		ID++;
	}

	public void add(Fragment f) {
		this.fragments.add(f);
	}

	public void addAll(Collection<Fragment> fs) {
		this.fragments.clear();
		for (Fragment fragment : fs) {
			this.fragments.add(fragment.copy());
		}

	}

	public void clear() {
		this.fragments.clear();
	}
	public void remove(Fragment f) {
		this.fragments.remove(f);
	}
	public int size() {
		return this.fragments.size();
	}

	public long getTimer() {
		return timer;
	}

	/*public Fragment get(int i) {
		return this.fragments.get(i);
	}*/
	public Cover copy() {
		Set<Fragment> tmp;
		tmp = new HashSet<Fragment>();
		for (Fragment f : this.fragments) {
			tmp.add(f);
		}
		return new Cover(tmp);
	}
	public Set<Fragment> getFragments() {
		return fragments;
	}
	public long getCid() {
		return cid;
	}
	public String toString() {
		return String.join("\n", this.fragments.toString());
	}

	public boolean hasFragment(Fragment f) {
		for (Fragment fr : fragments) {
			if(f.equals(fr)) {
				return true;
			}
		}
		return false;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Cover other = (Cover) obj;
		/*Set<Fragment> ofragments = other.getFragments();
		if(ofragments.size() != this.fragments.size())
			return false;
		for (Fragment f : ofragments) {
			if(!hasFragment(f)) {
				return false;
			}
		}

		return true;*/
		return other.getCid() == this.cid;

	}
	public boolean isMinimal(Set<Atomic> atoms) {
		Set<Atomic> _atoms;
		int j=0;

		for (int i=0; i<fragments.size(); i++) {
			_atoms =  new HashSet<Atomic>();
			j=0;
			for (Fragment fragmentj : fragments) {
			 if(i != j) {
				 _atoms.addAll(fragmentj.getatoms());
			 }
			 j++;
			}
			//System.out.println("without the fragamt  "+fragments.toArray()[i] + " we get "+_atoms);
			//System.out.println("origin atoms : "+atoms);
			if(_atoms.equals(atoms)) {
				//System.out.println("And they are equals");
				return false;
			}
		}
		return true;

	}
	public void removeIncludedFragments(Fragment newFragment) {
		Set<Fragment> _fragments = new HashSet<Fragment>();
		//System.out.println("newf "+newFragment);
		for (Fragment fragment : fragments) {
			if(newFragment.include(fragment)) {
				_fragments.add(fragment);
			}
		}
		//System.out.println("this : "+this);
		//System.out.println("removed "+_fragments);
		fragments.removeAll(_fragments);
		//System.out.println("this : "+this);

	}

	/*public String toDLP() {
		List<String> dlpatoms = new ArrayList<String>();
		for (Fragment f : fragments) {
			dlpatoms.add(f.toDLP());
			//System.out.println(f +" => vars : "+f.head());
		}

		return String.join(";", dlpatoms);
	}*/
	public Set<String> joinVars(List<String> varsi, List<String> varsj){
		Set<String> headvars = new HashSet<String>();
		headvars.addAll(varsi);
		headvars.retainAll(varsj);
		return headvars;
	}
	public String toSQL() {

		if(this.sqlquery.length()>0) {
			return this.sqlquery;
		}
		timer = 0;
		String body, head, qfi, curVar;
		Set<String> fiJoinfj, headvarsi ;
		List<String> headvarsli, headvarslj, freeVarsi ;

		Set<String> qheadVars = new HashSet<String>() ;
		int indexfi = 0, indexfj=0;
		List<String> cond = new ArrayList<String>();
		List<String> dlpFragments = new ArrayList<String>();
		Set<String> sqlFragments = new HashSet<String>() ;
		List<String> fragmentsNames = new ArrayList<String>();
		List<String> selectvars =  new ArrayList<String>();
		List<String> refromulatedFragments =  new ArrayList<String>();
		int finalVars = 0;


		if(fragments.size() == 1){
			this.sqlquery = "";

			startTime = System.nanoTime();
			cover2sql.setaQuery(JUCQ.getJUCQquery());
			cover2sql.reformulate();
			timer += (System.nanoTime()- startTime)/1000000;
			refromulatedFragments = cover2sql.getReformulatedQueries();
			for(int i=0; i<refromulatedFragments.size()-1;i++) {
				this.sqlquery += refromulatedFragments.get(i) + " \nUNION\n";
			}
			this.sqlquery += refromulatedFragments.get(refromulatedFragments.size()-1);
			//this.sqlquery = String.join(" \nUNION\n", refromulatedFragments);

			return this.sqlquery;
		}

		//Compute the fragment head and body
		for(Fragment f : fragments) {

			headvarsi = new HashSet<String>() ;


			//Get free vars in fi, retains the intersection with the free vars of the initial query
			freeVarsi =  f.freeVariables();
			headvarsi.addAll(freeVarsi);
			headvarsi.retainAll(JUCQ.headVars);
			//System.out.println("query head: "+JUCQ.headVars);
			//System.out.println("fq"+indexfi+" free vars: "+freeVarsi);

			indexfj = 0;
			for (Fragment fj : fragments) {
				if(indexfj == indexfi) {
					indexfj++;
					continue;
				}
				fiJoinfj = joinVars(freeVarsi, fj.freeVariables());
				//System.out.println(String.format("fq%d join with fq%d on %s ",indexfi, indexfj, fiJoinfj.toString()));
				//Add the
				headvarsi.addAll(fiJoinfj);
				indexfj++;
			}
			f.setHeadvars(headvarsi);
			indexfi++;
		}
		indexfi = 0;
		for(Fragment f : fragments) {

			headvarsli = new ArrayList<String>(f.getHeadvars());
			body = f.bodyToDLP();
			head = "qf"+indexfi+"("+String.join(",", headvarsli) + ")";
			qfi = head+" <- " + body;
			dlpFragments.add(qfi);
			fragmentsNames.add(String.format("qf%d", indexfi));
			//System.out.println("We are processing fragment: "+qfi);
			startTime = System.nanoTime();
			cover2sql.setaQuery(qfi);
			cover2sql.reformulate();
			timer += (System.nanoTime()- startTime)/1000000;
			refromulatedFragments = cover2sql.getReformulatedQueries();


			//System.out.println("We got the refomulations: "+refromulatedFragments.size());
			sqlFragments.add(String.format("qf%d as(\n%s\n)\n", indexfi, QueryUtils.join("\nUNION\n", refromulatedFragments)));
			//System.out.println("fq"+indexfi+" free vars: "+headvarsli);
			for(int  vari=0; vari<headvarsli.size();vari++) {
				//compute the select vars, we will get the free vars of the initial query that we meet first in the different fragments!
				curVar = headvarsli.get(vari);
				if(JUCQ.headVars.contains(curVar) && !qheadVars.contains(curVar)) {
					qheadVars.add(curVar);
					selectvars.add(String.format("qf%d.h%d as h%d",indexfi,vari, finalVars) );
					finalVars++;
				}
				indexfj = 0;
				for (Fragment fj : fragments) {
					if(indexfi <= indexfj) {
						indexfj++;
						continue;
					}
					headvarslj = new ArrayList<String>(fj.getHeadvars());
					for(int  varj=0; varj<headvarslj.size();varj++) {
						if(headvarsli.get(vari).equals(headvarslj.get(varj))){
							// The join condition of the different fragments
							cond.add(String.format("qf%d.h%d=qf%d.h%d", indexfi, vari, indexfj, varj ));
						}
					}
					indexfj++;
				}
			}
			indexfi++;
		}
		//System.out.println("The select vars are: "+selectvars.toString());
		//System.out.println("The clause where cond is : "+cond.toString());


		this.sqlquery = "WITH\n-- ##############\n"+String.join(",\n-- ##############\n", sqlFragments)+" ,\n-- ##############\nqf as ( select DISTINCT " + String.join(", ",selectvars)+ " FROM " + String.join(", ", fragmentsNames);

		if(cond.size()>0) {
			this.sqlquery += " WHERE "+String.join(" and ", cond);
		}
		this.sqlquery += "\n-- ##############\n )select count(*) from qf;";
		//System.out.println("The final sql code : "+this.sqlquery);
		//System.exit(0);

		return this.sqlquery;
	}

	public void dump(String filename, String query) {

		FileWriter writer;
		try {
			writer = new FileWriter(filename);
			if(query == null) {
				query = toSQL();
			}
			writer.write(query);

			writer.close();
		}
		catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}


}
