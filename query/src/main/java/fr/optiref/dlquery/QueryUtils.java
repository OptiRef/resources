package fr.optiref.dlquery;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import java.util.stream.Collectors;
public class QueryUtils {

	/* Checks if a string is empty ("") or null. */
    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }
    static final String[] SQL_RESERVED = {"group", "function"};
    public static String[] getSqlReserved() {
		return SQL_RESERVED;
	}
    public static String clean(String query){
        String countQuery = query.replace("#", "");
        countQuery = countQuery.replace("-", "");
        /*if(!query.contains("UNION") && !query.contains("distinct")  ) {
        	countQuery = countQuery.replace("select ", "select distinct ");
        }*/
        return countQuery;
    }

    public static int getMaxViewSize(Connector connector, String uscq) {
    	String s;
    	int m = -1;

    	for(String v: uscq.split("\\n--##\\n")) {
    		System.out.println("v:" +v);
			s = v.replace("),", ")");
			m = Math.max(m, connector.evluateView(s));
    	}
    	return m;
    }

    public static String normalize(String query) {
    	String q = query;
    	for(String reserved: SQL_RESERVED) {
    		if(q.contains(" as "+reserved+" ")) {
    			q = q.replace(" "+reserved, " "+reserved+"0");
    		}
    	}
    	return q;
    }
	/* Counts how many times the substring appears in the larger string. */
    public static int countMatches(String text, String str)
    {
        if (isEmpty(text) || isEmpty(str)) {
            return 0;
        }

        int index = 0, count = 0;
        while (true)
        {
            index = text.indexOf(str, index);
            if (index != -1)
            {
                count ++;
                index += str.length();
            }
            else {
                break;
            }
        }

        return count;
    }

    public static void storeQuery(String query, String filename){
		FileWriter writer = null;
		try {
			writer = new FileWriter(filename);
			writer.write(query);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    public static List<String> loadFile(String filename){
    	List<String> cqs = new ArrayList<String>();
    	String cq = null;
    	BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(filename));
			cq = reader.readLine();
			while(cq != null) {
				cqs.add(cq);
				cq = reader.readLine();
			}

			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cqs;
	}
     /* join large string list*/
    public static String join(String joinstring, List<String> queries) {
        return  queries.stream().collect(Collectors.joining(joinstring));
        //StringUtils.join(queries, joinstring);
        /*String re ="";
    	try{
            res = String.join(joinstring, queries);
        }
        catch(Exception e){
            System.out.println("List size: "+queries.size());
            e.printStackTrace();
            for(int i=0; i<queries.size()-1; i++){
                res += queries.get(i)+joinstring;
            }
            res += queries.get(queries.size()-1);
        }
        return res;*/
    }

    /* get the name of a query */
    public static String getQName(String query) {
    	int i = query.indexOf('(');

    	return query.substring(0,i);
    }

    public static String toUSCQ(String query, Map<String, String> prefixes){
        Map<String, Integer> varsmap = new HashMap<String, Integer>();
        String head, fhead, name;
        String[] atoms, avars;

        String header="%%%%% ATOMIC HEAD %%%%\n@prefix : <>\n@prefix xsd: <http://www.w3.org/2001/XMLSchema#>\n@top top\n";
        for(String key: prefixes.keySet()) {
        	header+= String.format("@prefix %s: <%s>\n",key, prefixes.get(key));
        }
        head = query.split("<-")[0];
        System.out.println(head);
        head = head.substring(head.indexOf('(')+1, head.indexOf(')'));
        atoms = query.split("<-")[1].split((", "));
        char[] currentAlphabet = "abcdefghijklmnopqrstuvwxyz".toUpperCase().toCharArray();
        String tmp, vars;
        int kvar = 0, cvar;
        String finalQuery = " :- ";
        fhead = "? (";
        for(String atom: atoms){
            name = atom.substring(0, atom.indexOf('(')).replace(" ","");
            vars = atom.substring(atom.indexOf('(')+1, atom.indexOf(')'));
            avars = vars.split(",");
            finalQuery += String.format("%s(", name);
            for(String var: avars){
                if(!varsmap.containsKey(var) && var.startsWith("?")){
                    varsmap.put(var, kvar);
                    kvar++;
                }
                if(var.startsWith("?")) {
                	cvar = varsmap.get(var);
                    finalQuery += String.format("%c,", currentAlphabet[cvar]);
                }else {
                	finalQuery += String.format("%s,", var);
                }

            }
            finalQuery+=")";
            finalQuery = finalQuery.replace(",)", "");
            finalQuery+="), ";
        }
        finalQuery+=")";
        finalQuery = finalQuery.replace(", )", "");
        avars = head.split(",");
        for(String var: avars){
            cvar = varsmap.get(var);
            fhead += String.format("%c,", currentAlphabet[cvar]);
        }
        fhead+=")";
        fhead = fhead.replace(",)", "");
        fhead+=")";

        return header+fhead + finalQuery+".\n";
    }

    public static Map<String, String> getPrefix(String filename){
        Map<String, String> prefixes = new HashMap<String, String>();
		String line ="";
        String[] pref;
		BufferedReader reader;


		try {
			reader = new BufferedReader(new FileReader(filename));
			line = reader.readLine();
			while(line != null) {
				if(line.startsWith("PREFIX")) {
					pref = line.replace("PREFIX ","").split(": ");
                    prefixes.put(pref[0], pref[1]);
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return prefixes;
    }

}
