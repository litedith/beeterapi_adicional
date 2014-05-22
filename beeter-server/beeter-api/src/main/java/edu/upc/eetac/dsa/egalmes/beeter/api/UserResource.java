package edu.upc.eetac.dsa.egalmes.beeter.api;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.sql.DataSource;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import edu.upc.eetac.dsa.egalmes.beeter.api.model.Sting;
import edu.upc.eetac.dsa.egalmes.beeter.api.model.User;
import edu.upc.eetac.dsa.egalmes.beeter.api.model.UserCollection;
//consume -> tipo de medyatype que va a RECIBIR el metodo
@Path ("/users")
public class UserResource {

private DataSource ds = DataSourceSPA.getInstance().getDataSource();
@Context
private SecurityContext security;
@PUT
@Path ("/{username}")
@Consumes (MediaType.BEETER_API_USER)
@Produces (MediaType.BEETER_API_USER)

public User updateUser (@PathParam ("username") String username, User user)
{
	validateUser(username);
	validateUpdateUser(user);
	Connection conn = null;
	try {
		conn = ds.getConnection();
	} catch (SQLException e) {
		throw new ServerErrorException("Could not connect to the database",
				Response.Status.SERVICE_UNAVAILABLE);
	}
	

	PreparedStatement stmt = null;
	try {
		String sql = buildUpdateUser();
		stmt = conn.prepareStatement(sql);
		stmt.setString(1, user.getUsername());
		stmt.setString(2, user.getEmail());
		stmt.setString(3, username);
		System.out.println("despues de meter parametros en la query");

		int rows = stmt.executeUpdate();
		if (rows == 1)
			user = getUserFromDatabase(username);
		else {
			throw new NotFoundException("There's no sting with stingid="
					+ username);
			// Updating inexistent sting
		}
		System.out.println("despues de meter parametros en la query1");

	} catch (SQLException e) {
		throw new ServerErrorException(e.getMessage(),
				Response.Status.INTERNAL_SERVER_ERROR);
	} finally {
		try {
			if (stmt != null)
				stmt.close();
			conn.close();
		} catch (SQLException e) {
		}
	}

	return user;
}

private void validateUser(String username) {
	User currentuser = getUserFromDatabase(username);
	//if (!username.equals (security.getUserPrincipal().getName()))
	if(!security.getUserPrincipal().getName().equals(currentuser.getUsername()))
		throw new ForbiddenException(
				"You are not allowed to modify this sting.");
}


private void validateUpdateUser(User user) {
	if (user.getUsername() != null && user.getUsername().length() > 100)
		throw new BadRequestException(
				"Subject can't be greater than 100 characters.");
	if (user.getEmail() != null && user.getEmail().length() > 500)
		throw new BadRequestException(
				"Content can't be greater than 500 characters.");
}


private User getUserFromDatabase(String username) {
	User user = new User();

	Connection conn = null;
	try {
		conn = ds.getConnection();
	} catch (SQLException e) {
		throw new ServerErrorException("Could not connect to the database",
				Response.Status.SERVICE_UNAVAILABLE);
	}

	PreparedStatement stmt = null;
	try {
		stmt = conn.prepareStatement(buildGetUserByIdQuery());
		stmt.setString(1, username);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			
			user.setUsername(rs.getString ("username"));
			user.setName(rs.getString("name"));
			user.setEmail(rs.getString("email"));
			
			
		} else {
			throw new NotFoundException("There's no user with username="
					+ username);
		}

	} catch (SQLException e) {
		throw new ServerErrorException(e.getMessage(),
				Response.Status.INTERNAL_SERVER_ERROR);
	} finally {
		try {
			if (stmt != null)
				stmt.close();
			conn.close();
		} catch (SQLException e) {
		}
	}

	return user;
}
private String buildGetUserByIdQuery() {
	return "select s.*, u.name from stings s, users u where u.username=s.username ";
}

private String buildUpdateUser() {
	return "update users set username=ifnull(?, username), email=ifnull(?, email) where username=?";
}


///////////////////////////////


@GET
@Path("/{username}")
// subrecurso del padre
@Produces(MediaType.BEETER_API_USER)
public Response getUser(@PathParam("username") String username,
		@Context Request request) {
	// Create CacheControl
	CacheControl cc = new CacheControl();

	User user = getUserFromDatabase(username);

	// Calculate the ETag on last modified date of user resource lo crea con
	// el lastmodified
	String s = user.getName() + " " + user.getEmail();
	EntityTag eTag = new EntityTag(Long.toString(s.hashCode()));

	// Verify if it matched with etag available in http request
	// mira si coincide con el etag
	Response.ResponseBuilder rb = request.evaluatePreconditions(eTag);

	// If ETag matches the rb will be non-null;
	// Use the rb to return the response without any further processing
	if (rb != null) {
		return rb.cacheControl(cc).tag(eTag).build();
	}

	// If rb is null then either it is first time request; or resource is
	// modified
	// Get the updated representation and return with Etag attached to it
	rb = Response.ok(user).cacheControl(cc).tag(eTag);

	return rb.build();
}

}
