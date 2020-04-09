package pt.unl.fct.di.apdc.firstwebapp.util;

public class RegisterData {

	public String username;
	public String name;
	public String email;	
	public String password;
	public String confirmation;
	public String street;
	public String place;
	public String country;
	public String role;
	
	public RegisterData() {
		
	}
	
	public RegisterData(String username, String name, String email, String password, String confirmation, String street, String place, String country,String role) {
		
			this.username = username;
			this.name = name;
			this.email = email;
			this.password = password;
			this.confirmation = confirmation;
			this.street = street;
			this.place = place;
			this.country = country;
			this.role = role;
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
			   email.contains("@") &&
			   validRole(role);
	}

	private boolean validRole(String role2) {
			return role2.equals("USER") || role2.equals("BACKEND");
	}

}
