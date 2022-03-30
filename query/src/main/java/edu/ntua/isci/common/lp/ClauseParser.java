/*Copyright 2011, 2013, 2015 Alexandros Chortaras

 This file is part of Rapid.

 Rapid is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Rapid is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Rapid.  If not, see <http://www.gnu.org/licenses/>.*/

package edu.ntua.isci.common.lp;

import java.util.ArrayList;

public class ClauseParser {

	private String IMPLIES_SYMBOL;
	private String VARIABLE_SYMBOL;
	private String END_SYMBOL;
	
	public ClauseParser() {
		IMPLIES_SYMBOL = "<-";
		VARIABLE_SYMBOL = Variable.VARIABLE_PREFIX;
		END_SYMBOL = "";
	}

	public ClauseParser(String is, String vs, String es) {
		IMPLIES_SYMBOL = is;
		VARIABLE_SYMBOL = vs;
		END_SYMBOL = es;
	}

	public Clause parseClause(String s) {
		return parseClause(s, true);
	}
	
	public Clause parseClause(String s, boolean condense) {
		
		if (END_SYMBOL.length() > 0) {
			int p = s.lastIndexOf(END_SYMBOL);
			s = s.substring(0, p);
		}
		
		int pos = s.indexOf(IMPLIES_SYMBOL);

		String headString = s.substring(0,pos).trim();
		String bodyString = s.substring(pos + 2).trim();
		
		Atom head = parseAtom(headString);
		
		ArrayList<String> v = split(bodyString);

		ArrayList<Atom> body = new ArrayList<Atom>();
		
		for (int i = 0; i < v.size(); i++) {
			Atom newAtom = parseAtom(v.get(i));
			if (!body.contains(newAtom)) {
				body.add(newAtom);
			}
		}

		if (condense) {
			return (new Clause(head, body)).condense();
		} else {
			return (new Clause(head, body));
		}
	}
	
	public Atom parseAtom(String s) {

		int pos1 = s.indexOf("(");
		int pos2 = s.lastIndexOf(")");

		if (pos1 < 0) {
			return new Atom(new Predicate(s,0));
		} else {
			
			String pre = s.substring(0, pos1).trim();
			String arg = s.substring(pos1 + 1, pos2).trim();
						
			ArrayList<String> v = split(arg);
						
			ArrayList<Term> terms = new ArrayList<Term>();
			for (String c : v) {
				terms.add(parseTermString(c));
			}
			
			Predicate p;
			int arity = terms.size();
			if (pre.equals("q")) {
				p = new Predicate("Q", arity);
			} else {
				p = new Predicate(pre, arity);
			}
			
			return new Atom(p, (Term[])terms.toArray(new Term[] {}));
		}
	}	
	
	private Term parseTermString(String s) {
		
		int pos1 = s.indexOf("(");
		int pos2 = s.lastIndexOf(")");

		if (pos1 < 0) {
			if (!VARIABLE_SYMBOL.equals("") && s.startsWith(VARIABLE_SYMBOL) ) {
				return new Variable(s.substring(1));
			} else if (VARIABLE_SYMBOL.equals("")) {
				return new Variable(s);
			} else {
				return new Constant(s);
			}
		} else {
			
			String fun = s.substring(0, pos1).trim();
			String arg = s.substring(pos1 + 1, pos2).trim();
			
			ArrayList<String> v = split(arg);
			
			ArrayList<Term> terms = new ArrayList<Term>();
			for (String c : v) {
				terms.add(parseTermString(c));
			}
			
			Function ff = new Function(fun, terms.size());
			
			return new FunctionalTerm(ff, (Term[])terms.toArray(new Term[] {}));
		}
	}
	
	
	private ArrayList<String> split(String s) {
		ArrayList<String> res = new ArrayList<String>();
		
		char[] arr = s.toCharArray();
	
		int c = 0;
		int start = 0;
		
		for (int i = 0; i < arr.length; i++) {
			boolean close = false;
	
			if (arr[i] == '(') {
				c++;
			} else if (arr[i] == ')') {
				c--;
				close = true;
			}
		
			if (c == 0 && arr[i] == ',') {
				res.add(s.substring(start, i).trim());
				i = i + 1;
				
				while (i < arr.length && (arr[i] == ' ')) {
					i++;
				}
				start = i;
				
			}
			if (c == 0 && close) {
				res.add(s.substring(start, i + 1).trim());
				i = i + 1;
				
				while (i < arr.length && (arr[i] == ' ' || arr[i] == ',')) {
					i++;
				}
				start = i;
			}
				
		}
		
		if (start <= arr.length - 1) {
			res.add(s.substring(start));
		}
		
		return res;
		
	}
	
}
