package edu.optiref.sql;

public class DbConnectionParams {

//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//
//	}

	private String connectionString;
	private String connectionUsername;
	private String connectionPassword;


	public DbConnectionParams() {
		connectionString = "jdbc:postgresql://localhost:5432/dls";
		connectionUsername = "user";
		connectionPassword = "brexe";

	}

	public DbConnectionParams(String cnx, String user, String password) {
		connectionString = cnx;
		connectionUsername = user;
		connectionPassword = password;
	}

	public void setConnectionString(String str) {
		connectionString = str;
	}
	public void setConnectionUsername(String user) {
		connectionUsername = user;
	}
	public void setConnectionPassword(String pass) {
		connectionPassword = pass;
	}

	public String getConnectionString() {
		return this.connectionString;
	}
	public String getConnectionUsername() {
		return this.connectionUsername;
	}
	public String getConnectionPassword() {
		return this.connectionPassword;
	}

}
