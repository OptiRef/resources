package fr.lri.lahdak.cqapri.db.results;

import java.util.Set;


public class Cause {
	private Set<Integer> c;

	public Cause(Set<Integer> cause){
		c=cause;
	}

	public Set<Integer> getasrtns(){
		return c;
	}
}
