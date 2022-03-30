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

package edu.ntua.isci.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SetUtils {

	public static <U> boolean areEqual(Collection<U> s1, Collection<U> s2) {
		
		if (s1 == null && s2 == null) {
			return true;
		} else if (s1 == null || s2 == null) {
			return false;
		} 
		
		for (Object o : s1) {
			if (!s2.contains(o)) {
				return false;
			}
		}

		for (Object o : s2) {
			if (!s1.contains(o)) {
				return false;
			}
		}
		
		return true;
	}	
	
	public static <U> Set<U> intersection(Collection<U> s1, Collection<U> s2) {
		
		Set<U> res = new HashSet<U>();
		
		for (U o : s1) {
			if (s2.contains(o)) {
				res.add(o);
			}
		}
		return res;
	}	

	public static <U> Set<U> difference(Collection<U> s1, Collection<U> s2) {
		
		Set<U> res = new HashSet<U>();
		
		for (U o : s1) {
			if (!s2.contains(o)) {
				res.add(o);
			}
		}
		
		return res;
	}		


}
