package edu.optiref.sql;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.ClauseParser;

public class DlpToSqlParser {
	static ClauseParser clauseParser;
	static SqlConverter conv;

	public static void main(String[] args) throws IOException {
		String input;
		String output;
		String originalQuery;
		if (args.length < 2) {
			System.out.println("Syntax : DlpToSqlParser inputFile originalQueryFile outputDirectory");
			System.out.println("    inputFile  : full path to input file (txt) of UCQs");
			System.out.println("    originalQueryFile  : full path to input file (txt) of the original query");
			System.out.println("    outputDirectory  : full path to folder where output files will be printed ending with \"/\"");
			System.out.println();

			System.exit(0);
		}
		input = args[0];
		originalQuery = args[1];
		output = args[2];

		clauseParser = new ClauseParser();
		conv = new SqlConverter();

		//Example Wafaa
		// input/DLPQueries/q1UCQ.txt input/DLPQueries/q1UCQ_original.txt output/DLPQueries/q1UCQ_SQL_New.txt

		convertDlpQueryFileToSqlWriteToFile(input,originalQuery,output);


	}



	public static String convertDlpQueryToSql(String query) {
		Clause queryClause = clauseParser.parseClause(query); //new Clause();

		return conv.getPostgresQuery(queryClause, new HashMap<String, String>());
	}


	public static ArrayList<String> convertDlpQueryListToSql(ArrayList<String> queries) {
		ArrayList<String> sqlQueries = new ArrayList<String>();
		for(int i=0; i< queries.size();i++) {
			sqlQueries.add(convertDlpQueryToSql(queries.get(i)));
		}

		return sqlQueries;
	}

	public static ArrayList<String> convertDlpQueryFileToSql(String file) throws FileNotFoundException{
		//Read a file in dlp format for which each line is a separate query
		ArrayList<String> sqlQueries = new ArrayList<String>();
		File myObj = new File(file);
		Scanner myReader = new Scanner(myObj);
	    while (myReader.hasNextLine()) {
	    	String currentLine = myReader.nextLine();

	    	sqlQueries.add(convertDlpQueryToSql(currentLine.trim()));
	    }
		myReader.close();
		return sqlQueries;
	}

	public static void convertDlpQueryFileToSqlWriteToFile(String file, String orginalQ, String output) throws IOException{
		//Read a file in dlp format for which each line is a separate query
		int counter = 0;
		File outputFile = new File(output.replace(".txt", ".sql"));
		File outputFile2 = new File(output);
		FileWriter fileWriter;
		BufferedWriter writer;
		FileWriter fileWriter2;
		BufferedWriter writer2;

		fileWriter = new FileWriter(outputFile, true);
		writer = new BufferedWriter(fileWriter);

		fileWriter2 = new FileWriter(outputFile2, true);
		writer2 = new BufferedWriter(fileWriter2);

		File myObjOrg = new File(orginalQ);
		Scanner myReaderOrg = new Scanner(myObjOrg);
		String orgQ = myReaderOrg.nextLine();

		writer2.write("-------------------------------------------------------------");
		writer2.write("\n");
		writer2.write("Original Query Rewriting to SQL:");
		writer2.write("\n");
		String sqlq =convertDlpQueryToSql(orgQ);
		writer2.write(sqlq);
		writer2.write("\n");
		writer2.write("-------------------------------------------------------------");
		writer2.write("\n");
		ArrayList<String> sqlQueries = new ArrayList<String>();
		File myObj = new File(file);
		Scanner myReader = new Scanner(myObj);

		writer.write("SELECT DISTINCT count(*)");
		writer.write("\n");
		writer.write("FROM(");
		writer.write("\n");
		String currentLine = myReader.nextLine();

	    while (!currentLine.equals("")) {

	    	writer2.write(counter+": ");
	    	counter++;
	    	System.out.println("Current query number: "+counter);
	    	System.out.println(currentLine);
	    	String converted = convertDlpQueryToSql(currentLine.trim());
	    	sqlQueries.add(converted);
	    	writer.write(converted);
	    	writer.write("\n");
	    	writer2.write(converted);
	    	writer2.write("\n");
	    	if(myReader.hasNextLine()) {
	    		currentLine = myReader.nextLine();
	    		if(currentLine.contains("Q(?X")) {
	    			writer.write("UNION");
	    	    	writer.write("\n");
	    		}
	    	}else {
	    		break;
	    	}

	    }
	    writer.write(")x");
    	writer.write("\n");
		myReader.close();
		myReaderOrg.close();
		writer.close();
		writer2.close();

	}
}
