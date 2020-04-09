package pt.unl.fct.di.apdc.firstwebapp.util;

public class DeleteData {
	public String username;
	public String toDelete;
	public String token;
	
	public DeleteData() {
		
	}
	
	public DeleteData(String username, String token,String toDelete) {
		this.username = username;
		this.token = token;
		this.toDelete = toDelete;
	}
}
