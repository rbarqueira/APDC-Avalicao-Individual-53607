package pt.unl.fct.di.apdc.firstwebapp.util;

public class UpdateData {
	public String username;
	public String name;
	public String email;	
	public String password;
	public String confirmation;
	public String street;
	public String place;
	public String country;
	public String token;
	
	public UpdateData() {
		
	}
	
	public UpdateData(String username, String name, String email, String password, String confirmation, String street, String place, String country,String token) {
		
			this.username = username;
			this.name = name;
			this.email = email;
			this.password = password;
			this.confirmation = confirmation;
			this.street = street;
			this.place = place;
			this.country = country;
			this.token = token;
	}
	
	private boolean validField(String value) {
		return value != null && !value.equals("");
	}
	
	public boolean validRegistration() {
		return validField(username) &&
			   validField(name) &&
			   validField(email) &&
			   validField(password) &&
			   validField(confirmation) &&
			   password.equals(confirmation) &&
			   email.contains("@");
	}
}
