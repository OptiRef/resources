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

package edu.ntua.isci.qa.utils;


import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;

import edu.ntua.isci.common.lp.ClauseWrapper;
import edu.ntua.isci.common.lp.Substitution;

import edu.ntua.isci.qa.lp.theory.LogicProgram;


public class Utils {

	private static NumberFormat nf2 = NumberFormat.getInstance();
	private static NumberFormat nf3 = NumberFormat.getInstance();
	
	static {
		nf2.setMinimumIntegerDigits(2);
		nf2.setMaximumIntegerDigits(2);
		nf3.setMinimumIntegerDigits(3);
		nf3.setMaximumIntegerDigits(3);
	}
	
	private static void printObject(PrintStream out, int size, int i, Object s) {
		char[] max = (size + "").toCharArray();
		char[] number = (i + "").toCharArray();
		
		String r = "";
		for (int j = number.length; j < max.length; j++) {
			r += " ";
		}
		
		r += i;
		
		out.println(r + ": " + s);
	}
	
	public static void printObjectArrayList(PrintStream out, String title, Collection<?> set) {
		out.println("-------------------------------------------------------------");
		out.println(title);
		out.println("-------------------------------------------------------------");
		int i = 0;
		for (Object c : set) {
			printObject(out, set.size(), i++, c);
		}
		out.println("-------------------------------------------------------------");
		out.println();
	}	
	
	public static void printClauses(PrintStream out, String title, Collection<ClauseWrapper> set) {
		out.println("-------------------------------------------------------------");
		out.println(title);
		out.println("-------------------------------------------------------------");
		int i = 0;
		for (ClauseWrapper c : set) {
			printObject(out, set.size(), i++, c.getClause());
		}
		out.println("-------------------------------------------------------------");
		out.println();
	}	

	public static void printClauses(PrintStream out, String title, LogicProgram lp) {
		printObjectArrayList(out, title, lp.getClauses());
	}
	
	public static String printArray(boolean[] arr) {
		if (arr == null) {
			return null;  
		}
		String s = "[";
		
		for (int i = 0; i < arr.length; i++) {
			if (i > 0) {
				s += ", ";
			}
			s += arr[i];
		}
		s += "]";
		
		return s;
	}
	
	public static String printArray(int[] arr) {
		if (arr == null) {
			return null;  
		}
		String s = "[";
		
		for (int i = 0; i < arr.length; i++) {
			if (i > 0) {
				s += ", ";
			}
			s += arr[i];
		}
		s += "]";
		
		return s;
	}

	public static String printArray(Object[] arr) {
		if (arr == null) {
			return null;  
		}
		String s = "[";
		
		for (int i = 0; i < arr.length; i++) {
			if (i > 0) {
				s += ", ";
			}
			s += arr[i];
		}
		s += "]";
		
		return s;
	}	
	
	public static String printArray(double[] arr) {
		if (arr == null) {
			return null;  
		}
		String s = "[";
		
		for (int i = 0; i < arr.length; i++) {
			if (i > 0) {
				s += ", ";
			}
			s += arr[i];
		}
		s += "]";
		
		return s;
	}	
	
	public static String millisToTimeString(long ms) {
		
		long ims = ms%1000;
		long sec = ms/1000;
		
		long isec = sec%60;
		long min = sec/60;
		
		long imin = min%24;
		long hr = min/24;
		
		return nf2.format(hr) + ":"  +  nf2.format(imin) + ":" + nf2.format(isec) + "." + nf3.format(ims);
	}
	
	public static int[] permutation(ArrayList<Integer> t, int num) {
		int[] s = new int[t.size()];
		for (int i = 0; i < t.size(); i++) {
			s[i] = t.get(i);
		}
		
		int factorial = factorial(s.length - 1); 
	 
		if (num/s.length >= factorial) {
			return null;
		}
	 
		for(int i = 0; i < s.length - 1; i++) {
			int tempi = (num / factorial) % (s.length - i);			
			int temp = s[i + tempi];
			for(int j = i + tempi; j > i; j--) {
				s[j] = s[j-1];
			}
	 
			s[i] = temp; 
	 
			factorial /= (s.length - (i + 1)); 
		}
	 
		return s;
	}
	
	public static int factorial(int k) {
		int f = 1;
		for (int i = 2; i <= k; i++) {
			f *= i;
		}
		return f;
	}
	
    public static int findIndexInArrayList(ArrayList<Integer> list, int k) {
    	for (int i = 0; i < list.size(); i++) {
    		if (list.get(i) == k) {
    			return i;
    		}
    	}
    	
    	return -1;
    }

    public static int findFirstIndex(int[] list, int k) {
    	for (int i = 0; i < list.length; i++) {
    		if (list[i] == k) {
    			return i;
    		}
    	}
    	
    	return -1;
    }    
	
	public static <T> boolean subsumesSome(ArrayList<? extends Collection<T>> list, Collection<T> set) {
		for (Collection<T> l : list) {
			if (set.containsAll(l)) {
				return true;
			}
		}
		
		return false;
	}

	
	public static ArrayList<Substitution> cloneArrayListSubstitution(ArrayList<Substitution> cover) {
		ArrayList<Substitution> res = new ArrayList<Substitution>();
		
		for (Substitution s : cover) {
			if (s != null) {
				res.add((Substitution)((Substitution)s).clone());
			} else {
				res.add(null);
			}
		}
		
		return res;
	}
}
