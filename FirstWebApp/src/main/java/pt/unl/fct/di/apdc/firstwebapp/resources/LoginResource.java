package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Transaction;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.DeleteData;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;
import pt.unl.fct.di.apdc.firstwebapp.util.LogoutData;
import pt.unl.fct.di.apdc.firstwebapp.util.UpdateData;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

	/**
	 * Logger Object
	 */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
	private final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");

	private final Gson g = new Gson();

	public LoginResource() { }

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLogin(LoginData data) {
		LOG.fine("Attempt to login user: " + data.username);

		if(data.username.equals("jleitao") && data.password.equals("password")) {
			AuthToken token = new AuthToken(data.username);
			LOG.info("User '" + data.username + "' logged in sucessfully.");
			return Response.ok(g.toJson(token)).build();
		} 
		LOG.warning("Failed login attempt for username: " + data.username);
		return Response.status(Status.FORBIDDEN).entity(g.toJson("Incorrect username or password.")).build();
	}

	@POST
	@Path("/v1")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLogin1(LoginData data) {
		LOG.fine("Attempt to login user: " + data.username);

		Key userKey = userKeyFactory.newKey(data.username);
		Entity user = datastore.get(userKey);
		if( user != null ) {
			String hashedPWD = user.getString("user_pwd");
			if (hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {
				AuthToken token = new AuthToken(data.username);
				LOG.info("User '" + data.username + "' logged in sucessfully.");
				return Response.ok(g.toJson(token)).build();				
			} else {
				LOG.warning("Wrong password for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();				
			}
		}
		else {
			// Username does not exist
			LOG.warning("Failed login attempt for username: " + data.username);
			return Response.status(Status.FORBIDDEN).build();
		}
	}
	
	@POST
	@Path("/v2")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response doLogin2(LoginData data,@Context HttpServletRequest request,@Context HttpHeaders headers) {
		LOG.fine("Attempt to login user: " + data.username);
		Key userKey = userKeyFactory.newKey(data.username);
		Key logKey = datastore.allocateId(datastore.newKeyFactory().addAncestors(PathElement.of("User",data.username)).setKind("UserLog").newKey());
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			if(user == null || user.getBoolean("user_active") == true) {
				LOG.warning("Failed login attempt for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();
			}
			String hashedPWD = user.getString("user_pwd");
			if (hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {
				Entity log = Entity.newBuilder(logKey)
						.set("user_login_ip", request.getRemoteAddr())
						.set("user_login_host", request.getRemoteHost())
						.set("user_login_latlon", StringValue.newBuilder(headers.getHeaderString("X-AppEngine-CityLatLong")).setExcludeFromIndexes(true).build())
						.set("user_login_city", headers.getHeaderString("X-AppEngine-City"))
						.set("user_login_country", headers.getHeaderString("X-AppEngine-Country"))
						.set("user_login_time", Timestamp.now())
						.build();
				
				
				AuthToken token = new AuthToken(data.username);
				Key validateKey = tokenKeyFactory.newKey(token.username);
				Entity token2 = txn.get(validateKey);
				if(token2 == null) {
				Key newTokenKey = datastore.newKeyFactory().setKind("Token").newKey(token.username);
				Entity entToken = Entity.newBuilder(newTokenKey)
						.set("token_ID", token.tokenID)
						.set("token_creation_data",token.creationData)
						.set("token_expiration_data",token.expirationData)
						.build();
				txn.put(log);
				txn.add(entToken);
				txn.commit();
				}else {
					txn.commit();
					LOG.warning("User " + data.username + " already logged in.");
					return Response.status(Status.FORBIDDEN).build();	
				}
				
				LOG.info("User '" + data.username + "' logged in sucessfully.");
				return Response.ok(g.toJson(token)).build();	
			} else {
				LOG.warning("Wrong password for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();				
			}
		} catch(Exception e) {
			txn.rollback();
			LOG.severe(e.getMessage());	
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();	
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}
	
	@POST
	@Path("/logout")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response doLogout(LogoutData data) {
		LOG.fine("Attempt to logout user: " + data.username);
		Key userKey = userKeyFactory.newKey(data.username);
		Key tokenKey = tokenKeyFactory.newKey(data.token);
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			Entity token = txn.get(tokenKey);
			if(user == null || token == null) {
				LOG.warning("Failed logout attempt for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();
			}
			if(user.getKey().getName().equals(token.getKey().getName())) {
			txn.delete(tokenKey);
			txn.commit();
			return Response.ok("{}").build();
			}
			else {
				LOG.warning("Failed logout attempt for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();
			}
		}catch(Exception e) {
			txn.rollback();
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();	
		}
	}
	
	@POST
	@Path("/delete")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response deleteUser(DeleteData data) {
		LOG.fine("Attempt to delete user: " + data.toDelete);
		Key userKey = userKeyFactory.newKey(data.username);
		Key tokenKey = tokenKeyFactory.newKey(data.token);
		Key deleteKey = userKeyFactory.newKey(data.toDelete);
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			Entity token = txn.get(tokenKey);
			Entity delete = txn.get(deleteKey);
			if(user == null || token == null || delete == null) {
				LOG.warning("Failed delete attempt for username: " + data.toDelete);
				return Response.status(Status.FORBIDDEN).build();
			}
			if(user.getKey().getName().equals(token.getKey().getName())) {
				if(user.getKey().getName().equals(delete.getKey().getName()) || user.getString("user_role").equals("BACKEND")) {
					txn.delete(deleteKey);
					if(user.getKey().getName().equals(delete.getKey().getName()))
						txn.delete(tokenKey);
					txn.commit();
					return Response.ok("{}").build();
				}else {
				LOG.warning("Failed delete attempt for username: " + data.toDelete);
				return Response.status(Status.FORBIDDEN).build();
				}
			}else {
				LOG.warning("Failed delete attempt for username: " + data.toDelete);
				return Response.status(Status.FORBIDDEN).build();
				}
		}catch(Exception e) {
			txn.rollback();
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();	
		}
		
	}

	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response updateUser(UpdateData data) {
		LOG.fine("Attempt to update user: " + data.username);
		Key userKey = userKeyFactory.newKey(data.username);
		Key tokenKey = tokenKeyFactory.newKey(data.token);
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			Entity token = txn.get(tokenKey);
			if(user == null || token == null) {
				LOG.warning("Failed update attempt for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();
			}
			if(user.getKey().getName().equals(token.getKey().getName())) {
				if( ! data.validRegistration() ) {
					return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
				}
				Key updatedUserKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
				Entity newUser = Entity.newBuilder(updatedUserKey)
						.set("user_name", data.name)
						.set("user_pwd", DigestUtils.sha512Hex(data.password))
						.set("user_email", data.email)
						.set("user_address_street", data.street)
						.set("user_address_place", data.place)
						.set("user_address_country", data.country)			
						.set("user_creation_time", Timestamp.now())
						.build();
				txn.update(user,newUser);
				txn.commit();
				return Response.ok("{}").build();
			}else {
				LOG.warning("Failed update attempt for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();
				}
		}catch(Exception e) {
			txn.rollback();
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();	
		}
	}
	
	@GET
	@Path("/{username}")
	public Response checkUsernameAvailable(@PathParam("username") String username) {
		if(!username.equals("jleitao")) {
			return Response.ok().entity(false).build();
		} else {
			return Response.ok().entity(true).build();
		}
	}

}
