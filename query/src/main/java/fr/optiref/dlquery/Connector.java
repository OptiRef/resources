package fr.optiref.dlquery;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mysql.cj.jdbc.exceptions.MySQLTimeoutException;

import me.tongfei.progressbar.ProgressBar;

public class Connector {

	private String host;
	private String databasename ;
	private Connection con;
	private Statement stmt;
	private String user;
	private String password;
	private String engine;
	private String explain;
	private int timeout, otimeout;
	private int sumtimeout;
	private boolean limit1;
	private boolean exists;
	private long start, timer;
	static long queryno = 0;
	private double cdb = 0.00321368181;
	private double cl  = 0.00018806125;
	private double ck  = 0.00046167385;
	private double cj  = 0.001285472726;
	private double ct  = 0.00053436378;
	private double cm  = 0.00039235885;
	private int query_level;
	private Map<Integer, Integer> atomsMap;
	Map<String, Integer> summaryMap ;


	Connector(Properties prop, String _databasename) {
		// TODO Auto-generated constructor stub
		String _engine,  _user,  _password;
		_engine       = prop.get("database.engine").toString();
		_user         = prop.get("database.user").toString();
		_password     = prop.get("database.password").toString();
		timeout       = Integer.parseInt(prop.get("database.timeout").toString());
		otimeout       = Integer.parseInt(prop.get("database.timeout").toString())/1000;
		System.out.println("otimeout: "+otimeout);
		sumtimeout    = Integer.parseInt(prop.get("summary.timeout").toString());
		limit1 = prop.get("database.use_limit1").toString().equals("true");
		exists = prop.get("database.use_exists").toString().equals("true");
		query_level = Integer.parseInt(prop.get("query.opt_level").toString());

		this.databasename = _databasename;
		this.user = _user;
		this.password = _password;
		this.engine = _engine;
		open();

		if(prop.get("cost.model").equals("RDBMS")) {
			clean_explain_statement();
		}else {
			initMap();
		}

		timer = 0;
	}

	public int getMap(String s){
		return summaryMap.get(s);
	}
	public String getEngine() {
		return engine;
	}
	public int getOtimeout() {
		return otimeout;
	}
	public void setLimit1(boolean limit1) {
		this.limit1 = limit1;
	}
	public int getTimeout() {
		return timeout;
	}
	public int getSumtimeout() {
		return sumtimeout;
	}
	public void initMap(){
		String query;
		atomsMap = new HashMap<>();
		if(this.engine.contains("DB2")){
			query = "Select tabname as table_name from syscat.tables where tabname like \'TABLE%\' and tabname not like \'%SUM%\' and type = \'T\' ";
			//Select tabname as table_name from syscat.tables where tabname like 'TABLE%' and type = 'T'
		}
		else if(this.engine.contains("POSTGRESQL")){
			query = String.format("SELECT table_name FROM information_schema.tables WHERE table_schema = '%s' and position('table' in table_name)>0;", "public");
		}
		else{
			query = String.format("SELECT table_name FROM information_schema.tables WHERE table_schema = '%s' and position('table' in table_name)>0;", this.databasename);
		}

		String name = "";
		System.out.println("we are in init map");
		ResultSet  rs;
		List<String> tables = new ArrayList<String>();
		int tid;
		try {
            rs = stmt.executeQuery(query);
            while (rs.next()) {
				name = rs.getString(1);
				//System.out.println("name: "+name);
                if(!name.contains("sum")){
                	tables.add(name);
				}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

		for(String table: tables) {
			tid = Integer.parseInt(table.replace("table", ""));
			try {
				rs = stmt.executeQuery(String.format("select count(*) from %s", table));
				if(rs.next()){
					atomsMap.put(tid, rs.getInt(1));
					//System.out.println("table: "+table+ " , rows: "+atomsMap.get(tid));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		System.out.println("init map done!");

	}
	public long getTimer() {
		return timer;
	}
	public double getCdb() {
		return cdb;
	}
	public double getCj() {
		return cj;
	}
	public double getCk() {
		return ck;
	}
	public double getCl() {
		return cl;
	}
	public double getCt() {
		return ct;
	}
	public double getCm() {
		return cm;
	}
	public void setCl(double cl) {
		this.cl = cl;
	}
	public void setCm(double cm) {
		this.cm = cm;
	}
	public void setCt(double ct) {
		this.ct = ct;
	}
	public void setCdb(double cdb) {
		this.cdb = cdb;
	}
	public void setCk(double ck) {
		this.ck = ck;
	}
	public void setCj(double cj) {
		this.cj = cj;
	}

	public int getRows(String tableName){
		Integer tid = Integer.parseInt(tableName.replace(" ","").replace("table", ""));
		Integer c = atomsMap.get(tid);
		if(c == null) {
			System.out.println("Table: "+tableName);
		}
		return c;
	}
	public void open() {

		try{
			switch (this.engine) {
			case "POSTGRESQL": {
				Class.forName("org.postgresql.Driver");
				host = "jdbc:postgresql://localhost:5432/";
				explain = "EXPLAIN (FORMAT JSON) ";
				break;
			}
			case "MYSQL": {
				Class.forName("com.mysql.jdbc.Driver");
				host = "jdbc:mysql://localhost:3306/";
				explain = "EXPLAIN FORMAT=JSON" ;
				break;
			}
			case "DB2": {
				Class.forName("com.ibm.db2.jcc.DB2Driver");
				host = String.format("jdbc:db2://localhost:50000/");
				break;
			}
			default:
				throw new IllegalArgumentException("Unexpected value: " + this.engine);
			}
			if(this.engine.equals("DB2")){
				this.con = DriverManager.getConnection(host+this.databasename,this.user, this.password);
			}else{
				this.con = DriverManager.getConnection(host+this.databasename+"?characterEncoding=latin1",this.user, this.password);
			}
			this.stmt=con.createStatement();

			this.host += this.databasename;
			settimeout(this.otimeout);


		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public void settimeout(int timeout) {
		try {
			stmt.setQueryTimeout(timeout);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getquerytimeout() {
		try {
			return stmt.getQueryTimeout();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	public void nestedloopoff() {

		try {
			if(this.engine.equals("MYSQL")){
				stmt.executeUpdate("set optimizer_switch='block_nested_loop=off';");

			}else if(this.engine.equals("POSTGRESQL")){
				stmt.executeUpdate("SET enable_nestloop = off;");
			}
			else if(this.engine.equals("DB2")) {
				stmt.executeUpdate(String.format("SET CURRENT QUERY OPTIMIZATION = %d;", query_level));

			}



		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	public void nestedloopon() {

		try {
			if(this.engine.equals("MYSQL")){
				stmt.executeUpdate("set optimizer_switch='block_nested_loop=on';");
			}
			else if(this.engine.equals("POSTGRESQL")){
				stmt.executeUpdate("SET enable_nestloop = ON;");
			}
			else if(this.engine.equals("DB2")) {
				// 0 is the best for indexed tables, 9 is the best in general
				stmt.executeUpdate(String.format("SET CURRENT QUERY OPTIMIZATION = %d;", query_level));

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	public double  evalTime(String query) {
		double cost = 0;
		String sjson = "";
		String scost = "";
		JSONArray queryPlan;
		JSONObject plan;

		try {
			System.out.println("Let compute the cost\n");
			ResultSet rs=stmt.executeQuery(String.format("%s %s;", this.explain, query));
			while (rs.next())
			{
				scost = rs.getString(1);

				queryPlan = (JSONArray) new JSONParser().parse(scost);
				System.out.println(queryPlan);
				System.exit(0);
				plan = (JSONObject) queryPlan.get(0);
				//
				cost = (double) plan.get("Execution Time");
				return cost;

			}

		} catch (SQLException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cost;
	}
	public double  eval_psql(String query) {
		double cost = Double.MAX_VALUE;
		String sjson = "";
		String scost = "";
		JSONArray joe;
		JSONObject plan;

		try {
			timer = 0;
			start = System.nanoTime();
			ResultSet rs;
			rs =stmt.executeQuery(String.format("%s %s;", this.explain, query));
			//}
			while (rs.next())
			{
				scost = rs.getString(1);
				//sjson += scost;


				joe = (JSONArray) new JSONParser().parse(scost);
				plan = (JSONObject) joe.get(0);
				plan = (JSONObject) plan.get("Plan");
				cost = (double) plan.get("Total Cost");
				//System.out.println("the cost:" +cost);
				timer = System.nanoTime() - start;
				return cost;

			}

		} catch (Exception e) {
			if(e instanceof SQLException) {
				System.out.println("timeout: "+ ((SQLException)e).getSQLState());
			}

		}
		return cost;
	}
	public void clean_explain_statement(){
		int t = this.getTimeout();
		this.settimeout(0);
		if(!this.engine.equals("DB2")) return;
		try {
			stmt.executeUpdate("delete from EXPLAIN_STATEMENT ");
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		this.settimeout(t);
	}
	public double  eval_db2(String query) {
		double cost = Double.MAX_VALUE;
		String db2query = "";
		double scost = Double.MAX_VALUE;
		ResultSet rs;

		try {
			timer = 0;
			queryno++;
			start = System.nanoTime();
			db2query =  String.format("EXPLAIN PLAN SET QUERYNO = %d FOR %s ", queryno, query);
			stmt.executeUpdate(db2query);
			rs=stmt.executeQuery(String.format("select TOTAL_COST from EXPLAIN_STATEMENT where QUERYNO = %d ", queryno));

			if (rs.next())
			{
				scost = rs.getDouble(1);
				//System.out.println("Score : "+scost);
			}
			timer = System.nanoTime() - start;
			//System.exit(0);
			clean_explain_statement();
			return scost;

		} catch (SQLException e) {
			System.out.println("timeout: "+e.getSQLState());

		}
		return cost;
	}
	public double  eval_mysql(String query) {
		double cost = Double.MAX_VALUE;
		String sjson = "";
		String scost = "";

		JSONObject plan, block = null, cost_info = null;
		JSONArray query_specifications;

		try {
			timer = 0;

			ResultSet rs;
			start = System.nanoTime();
			if(query.toLowerCase().contains("count(*)")) {
				rs =stmt.executeQuery(String.format("%s %s;", this.explain, query));
			}else {
				rs=stmt.executeQuery(String.format("%s with qf as (%s) select count(*) from qf;", this.explain, query));
			}
			if (rs.next())
			{

				scost = rs.getString(1);
				timer = System.nanoTime() - start;

				plan = (JSONObject) new JSONParser().parse(scost);


				plan = (JSONObject) new JSONParser().parse(scost);
				plan = (JSONObject) plan.get("query_block");
				plan = (JSONObject) plan.get("cost_info");
				scost = plan.get("query_cost").toString();

				cost = Double.parseDouble(scost);
				return cost;

			}

		} catch (Exception e) {
			System.out.println("error: "+e.getMessage());

		}
		return cost;
	}
	public double  eval(String query) {

		double cost = Double.MAX_VALUE;

		switch (this.engine) {

			case "POSTGRESQL": {
				cost = eval_psql(query);

				break;
			}
			case "MYSQL": {
				cost = eval_mysql(query);
				break;
			}
			case "DB2": {
				cost = eval_db2(query);
				break;
			}
		}
		return cost;
	}

	public double  eval(List<String> queries) {
		double s = 0;
		for (String cq : queries) {
			s+= eval(cq);
		}
		return s;

	}
	public int getCount(String query) {

		if(query.isEmpty()){
			return 0;
		}

		int res = -1;
		ResultSet rs;
		try {
			rs = stmt.executeQuery(query);
			if (rs.next())
			{

				res = rs.getInt(1) ;
				return res;
			}
		} catch (SQLException e) {
			return processSQLException(e);
		}

		return res;

	}

	public int getAnswer(String query) throws SQLException  {


		int res = -1;

		ResultSet rs;
		rs = stmt.executeQuery(query);
		if (rs.next())
		{
			res = rs.getInt(1) ;
			return res;
		}
		return res;

	}
	public void freePostgres() {
		String query = String.format("with pids as (SELECT * FROM pg_stat_activity WHERE usename = %s) SELECT pg_cancel_backend(pid) from pids", getUser());
		//System.out.println(query);
		ResultSet rs;
		try {
			rs = stmt.executeQuery(query);
		} catch (SQLException e) {
			processSQLException(e);
		}
	}

	public void freeRDBMS() {
		switch (this.engine) {
		case "POSTGRESQL": {
			freePostgres();
			break;
		}
		case "MYSQL": {
			System.out.println("Free not implemented for MYSQL");
			break;
		}
		case "DB2": {
			System.out.println("Free not implemented for DB2");
			break;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + this.engine);
		}
	}
	public List<Integer> getAllAnswers(String query) {

		List<Integer>  res = new ArrayList<Integer>();
		ResultSet rs;
		try {
			rs = stmt.executeQuery(query);
			if (rs.next())
			{
				res .add(rs.getInt(1)) ;
			}
		} catch (SQLException e) {
			processSQLException(e);
		}
		return res;

	}
	private int processSQLException(SQLException e) {
		String state = e.getSQLState();
		if (e instanceof MySQLTimeoutException || state.equals("57014") ||state.equals("HY000") )
	    {
		   System.out.println("The query timeout");
	       return -1;
	    }
	    else
	    {
	    	System.out.println("ERROR");
	    	System.out.println("SQL State: "+state);
	        return -2;
	    }
	}

	public int evluateView(String query) {

		if(query.isEmpty()){
			return 0;
		}
		String vname = query.split("AS \\(")[0];
		int res = -1;
		System.out.println(vname);
		ResultSet rs;
		try {
			query = String.format("with %s select count(*) from %s", query, vname);
			System.out.println("the final viewe: "+query);
			rs = stmt.executeQuery(query);
			if (rs.next())
			{

				res = rs.getInt(1) ;
				//System.out.println(res);
				return res;
			}
		} catch (SQLException e) {
			return processSQLException(e);
		}

		return res;
	}


	public Statement getStmt() {
		return stmt;
	}
	public void clearstmt() {
		try {
			this.stmt.clearBatch();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void enable_seqscan(String s) throws SQLException {
		if(this.engine.equals("POSTGRESQL")) {
			stmt.executeUpdate(String.format("SET enable_seqscan = %s;", s));
		}

	}
	public void set_random_page_cost(double cost) throws SQLException {
		if(this.engine.equals("POSTGRESQL")) {
			stmt.executeUpdate(String.format("SET random_page_cost = %f;", cost));
		}
	}
	public boolean hasAnswer(String query) {


		ResultSet rs;
		String s ;

		try {
			rs = stmt.executeQuery(query);
			if (rs.next())
			{
				if(query.contains("exists") && !this.engine.equals("DB2")){
					s = rs.getString(1);
					return s.equals("1") || s.equals("t");
				}
				else{
					return true;
				}
			}
		} catch (SQLException e) {
			System.out.println("sql ERROR: "+e.getSQLState());
		}


		return false;

	}

	public void createSummaryMap() throws SQLException {

		String query;
		query = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' and position('tablesum' in table_name)>0; ";

		List<String> tables = new ArrayList<String>();

        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()) {
			tables.add( rs.getString(1));
        }


        for(String name : ProgressBar.wrap(tables, "Storing summary tables in summaryMap ...")) {
			rs=stmt.executeQuery("select * from "+name +" limit 1");
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnsNumber = rsmd.getColumnCount();
			rs = stmt.executeQuery("select * from "+name);
			rsmd = rs.getMetaData();
			columnsNumber = rsmd.getColumnCount();
			summaryMap.put(name, -1);
			if(columnsNumber == 1) {
				if (rs.next())
				{
					summaryMap.put(name, rs.getInt(1));
				}

			}
        }
	}

	public int isConcept(String tablename)  {

        ResultSet rs ;
		ResultSetMetaData rsmd;
		int columnsNumber;
		try {
			rs = stmt.executeQuery("select * from "+tablename+ " limit 1");
			rsmd = rs.getMetaData();
			rsmd = rs.getMetaData();
			columnsNumber = rsmd.getColumnCount();
			if(columnsNumber == 2) {
				return 0;
			}
			if (rs.next())
			{
				return rs.getInt(1) ;
			}else {
				return 0;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}

	}
	public boolean isEmpty(String table) {
		String query = String.format("SELECT 1 FROM %s LIMIT 1", table);
		try {
			ResultSet rs = stmt.executeQuery(query);
			if (!rs.next())
			{
				return true;
			}
		} catch (SQLException e) {
			processSQLException(e);
		}
		return false;
	}


	public String getDatabasename() {
		return databasename;
	}
	public String getHost() {
		return host;
	}
	public String getUser() {
		return user;
	}
	public String getPassword() {
		return password;
	}


	public void close() {
		try{
			con.close();
		}
		catch(Exception e){
			System.out.println(e);
		}

	}

}
