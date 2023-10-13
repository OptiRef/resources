package fr.inria.cedar.compact.query;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * 
 * @author Michael Thomazo (INRIA)
 *
 */
public class UnionSemiConjunctiveQueryLinkedList implements UnionSemiConjunctiveQuery{

	LinkedList<SemiConjunctiveQuery> scqs;
	
	public UnionSemiConjunctiveQueryLinkedList(){
		this.scqs = new LinkedList<>();
	}
	
	@Override
	public LinkedList<SemiConjunctiveQuery> getSCQs(){
		return this.scqs;
	}
	
	@Override
	public void add(final SemiConjunctiveQuery scq){
		this.scqs.add(scq);
	}

	@Override
	public void addAndRemoveRedundant(final SemiConjunctiveQuery s) {
		List<SemiConjunctiveQuery> toRemove = new ArrayList<>();
		for (SemiConjunctiveQuery scq:this.scqs){
			if (s.entails(scq)){
				toRemove.add(scq);
			}
		}
		this.scqs.removeAll(toRemove);
		this.scqs.add(s);
		
	}

	@Override
	public boolean entails(final SemiConjunctiveQuery s) {
		for (SemiConjunctiveQuery scq:this.scqs){
			if (scq.entails(s)){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void clean(final String s){
		for (SemiConjunctiveQuery scq:this.scqs){
			scq.clean(s);
		}
	}

	@Override
	public String toString(){
		String res = "";
		for (final SemiConjunctiveQuery scq:this.scqs){
			res = res + scq.toString() + "\n";
		}
		return res;
	}
}
