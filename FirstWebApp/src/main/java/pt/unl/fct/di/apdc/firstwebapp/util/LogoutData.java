package pt.unl.fct.di.apdc.firstwebapp.util;

public class LogoutData {
	
	public String username;
	public String token;
	
	public LogoutData() {
		
	}
	
	public LogoutData(String username, String token) {
		this.username = username;
		this.token = token;
	}
}
