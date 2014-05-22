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
import edu.upc.eetac.dsa.egalmes.beeter.api.model.StingCollection;

@Path("/stings")
public class StingResource {
	private DataSource ds = DataSourceSPA.getInstance().getDataSource();// referencia
																		// a
																		// datasrouce
																		// via
																		// acceso
																		// al
																		// unico
																		// punto
																		// definido
																		// (datasourcespa)
	@Context
	private SecurityContext security;

	@GET
	// recurso @path este caso subrecurso pq no tiene @path
	@Produces(MediaType.BEETER_API_STING_COLLECTION)
	// la respuesta es de este mediatype
	public StingCollection getStings(@QueryParam("length") int length, // mas
																		// alla
																		// del
																		// interrogante
																		// son
																		// los
																		// queryparams
			@QueryParam("before") long before, @QueryParam("after") long after) {
		StingCollection stings = new StingCollection();

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			boolean updateFromLast = after > 0;
			stmt = conn.prepareStatement(buildGetStingsQuery(updateFromLast));
			if (updateFromLast) {// 1 es el primer valor ? interrogante de la
									// query, para que rellene ese valor
				stmt.setTimestamp(1, new Timestamp(after));
			} else {
				if (before > 0)
					stmt.setTimestamp(1, new Timestamp(before));
				else
					stmt.setTimestamp(1, null); // si es null valor 5
				length = (length <= 0) ? 20 : length; // if else reducido
				stmt.setInt(2, length);
			}
			ResultSet rs = stmt.executeQuery();
			boolean first = true;
			long oldestTimestamp = 0;
			while (rs.next()) {
				Sting sting = new Sting();
				sting.setId(rs.getString("stingid"));
				sting.setUsername(rs.getString("username"));
				sting.setAuthor(rs.getString("name"));
				sting.setSubject(rs.getString("subject"));
				oldestTimestamp = rs.getTimestamp("last_modified").getTime();
				sting.setLastModified(oldestTimestamp);
				if (first) {
					first = false;
					stings.setNewestTimestamp(sting.getLastModified());
				}
				stings.addSting(sting);
			}
			stings.setOldestTimestamp(oldestTimestamp);
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

		return stings;
	}

	private Sting getStingFromDatabase(String stingid) {
		Sting sting = new Sting();

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(buildGetStingByIdQuery());
			stmt.setInt(1, Integer.valueOf(stingid));
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				sting.setId(rs.getString("stingid"));
				sting.setUsername(rs.getString("username"));
				sting.setAuthor(rs.getString("name"));
				sting.setSubject(rs.getString("subject"));
				sting.setContent(rs.getString("content"));
				sting.setLastModified(rs.getTimestamp("last_modified")
						.getTime());
			} else {
				throw new NotFoundException("There's no sting with stingid="
						+ stingid);
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

		return sting;
	}

	private String buildGetStingsQuery(boolean updateFromLast) {
		if (updateFromLast)
			return "select s.*, u.name from stings s, users u where u.username=s.username and s.last_modified > ? order by last_modified desc";
		else
			// si es null coge el de ahora
			return "select s.*, u.name from stings s, users u where u.username=s.username and s.last_modified < ifnull(?, now())  order by last_modified desc limit ?";
	} // ifnull (?, now()) si es nullo coge el valor now

	@GET
	@Path("/{stingid}")
	// subrecurso del padre
	@Produces(MediaType.BEETER_API_STING)
	public Response getSting(@PathParam("stingid") String stingid,
			@Context Request request) {
		// Create CacheControl
		CacheControl cc = new CacheControl();

		Sting sting = getStingFromDatabase(stingid);

		// Calculate the ETag on last modified date of user resource lo crea con
		// el lastmodified
		EntityTag eTag = new EntityTag(Long.toString(sting.getLastModified()));

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
		rb = Response.ok(sting).cacheControl(cc).tag(eTag);

		return rb.build();
	}

	private String buildGetStingByIdQuery() {
		return "select s.*, u.name from stings s, users u where u.username=s.username and s.stingid=?";
	}

	@POST
	@Consumes(MediaType.BEETER_API_STING)
	// lo qe se envie tiene qe ser en formato beeter_api_sting
	@Produces(MediaType.BEETER_API_STING)
	public Sting createSting(Sting sting) {

		validateSting(sting);

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			String sql = buildInsertSting();// query
			stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);// te
																				// vuelve
																				// la
																				// clave
																				// primaria
																				// que
																				// se
																				// ha
																				// generado
			// parametrizados 1,2,3
			// el auto no hay que pasarlo, esta en la tabla de usuarios, ni id
			// ni lastmod pq son autogenerados
			// stmt.setString(1, sting.getUsername());
			stmt.setString(1, security.getUserPrincipal().getName());
			stmt.setString(2, sting.getSubject());
			stmt.setString(3, sting.getContent());
			stmt.executeUpdate();// consulta

			ResultSet rs = stmt.getGeneratedKeys();// recojo respuesta
			if (rs.next()) {
				int stingid = rs.getInt(1);

				sting = getStingFromDatabase(Integer.toString(stingid));
			} else {
				// Something has failed...
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

		return sting;
	}

	private void validateSting(Sting sting) {
		if (sting.getSubject() == null)
			throw new BadRequestException("Subject can't be null.");
		if (sting.getContent() == null)
			throw new BadRequestException("Content can't be null.");
		if (sting.getSubject().length() > 100)
			throw new BadRequestException(
					"Subject can't be greater than 100 characters.");
		if (sting.getContent().length() > 500)
			throw new BadRequestException(
					"Content can't be greater than 500 characters.");
	}

	private void validateUser(String stingid) {
		Sting currentSting = getStingFromDatabase(stingid);
		if (!security.getUserPrincipal().getName()
				.equals(currentSting.getUsername()))
			throw new ForbiddenException(
					"You are not allowed to modify this sting.");
	}

	private String buildInsertSting() {
		return "insert into stings (username, subject, content) value (?, ?, ?)";
	}

	@DELETE
	@Path("/{stingid}")
	public void deleteSting(@PathParam("stingid") String stingid) {
		validateUser(stingid);
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		PreparedStatement stmt = null;
		try {
			String sql = buildDeleteSting();
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, Integer.valueOf(stingid));

			int rows = stmt.executeUpdate();
			if (rows == 0)// te has intentado cargar algo que no existia
				throw new NotFoundException("There's no sting with stingid="
						+ stingid);// Deleting inexistent sting
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {// cierro conexiones
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
	}

	private String buildDeleteSting() {
		return "delete from stings where stingid=?";
	}

	@PUT
	@Path("/{stingid}")
	@Consumes(MediaType.BEETER_API_STING)
	@Produces(MediaType.BEETER_API_STING)
	public Sting updateSting(@PathParam("stingid") String stingid, Sting sting) {
		validateUser(stingid);
		validateUpdateSting(sting);
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			String sql = buildUpdateSting();
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, sting.getSubject());
			stmt.setString(2, sting.getContent());
			stmt.setInt(3, Integer.valueOf(stingid));

			int rows = stmt.executeUpdate();
			if (rows == 1)
				sting = getStingFromDatabase(stingid);
			else {
				throw new NotFoundException("There's no sting with stingid="
						+ stingid);
				// Updating inexistent sting
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

		return sting;
	}

	private void validateUpdateSting(Sting sting) {
		if (sting.getSubject() != null && sting.getSubject().length() > 100)
			throw new BadRequestException(
					"Subject can't be greater than 100 characters.");
		if (sting.getContent() != null && sting.getContent().length() > 500)
			throw new BadRequestException(
					"Content can't be greater than 500 characters.");
	}

	private String buildUpdateSting() {
		return "update stings set subject=ifnull(?, subject), content=ifnull(?, content) where stingid=?";
	}

	@GET
	@Path("/search")
	@Produces(MediaType.BEETER_API_STING_COLLECTION)
	// devuelve sting collection
	public StingCollection searchByContentandSubject(
			@QueryParam("subject") String subject,
			@QueryParam("content") String content,
			@QueryParam("length") int length) {
		StingCollection stings = new StingCollection();
		
		// conectamos a bbdd
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		PreparedStatement stmt = null;
		try {
			String sql = searchbyContentadSubjectQuery(subject, content);
			stmt = conn.prepareStatement(sql);
			if (subject != null && content == null)
			{
				stmt.setString(1,"%"+ subject+ "%");
					if (length == 0)
					{
						length = 3;
						stmt.setInt(2, length);
					}
					else
					{
						stmt.setInt(2, length);
					}
			}
			if (subject == null && content != null)
			{
				stmt.setString(1, "%"+ content+ "%");
					if (length == 0)
					{
						length = 3;
						stmt.setInt(2, length);
					}
					else
					{
						stmt.setInt(2, length);
					}
			}
			
			if (subject != null && content != null)
			{
				stmt.setString(1,"%"+ subject+ "%");
				stmt.setString(2, "%"+ content+ "%");
					if (length == 0)
					{
						length = 3;
						stmt.setInt(3, length);
					}
					else
					{
						stmt.setInt(3, length);
					}
			}
		
			
			
			//recogemos consulta
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				
				Sting sting = new Sting();
				sting.setId(rs.getString("stingid"));
				sting.setUsername(rs.getString("username"));
				sting.setAuthor(rs.getString("name"));
				sting.setSubject(rs.getString("subject"));
				sting.setContent(rs.getString ("content"));
				stings.addSting(sting);
				
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
		// query select where [clausulajoin u.] and subject = like %?% (% % este
		// patron para si esta en alguna de las cadenas)or content like %?%
		// limit ?
		return stings;

	}

	private String searchbyContentadSubjectQuery(String s, String c) {
		if (s == null & c == null) {
			throw new BadRequestException("Subject and Content can't be null. ");
		}

		if (s == null & c != null) {
			
			return "select s.*, u.name from stings s, users u where s.username= u.username and content like ? limit ? ;";
		}
		if (s != null & c == null) {
			return "select s.*, u.name from stings s, users u where s.username= u.username and subject like ? limit ? ;";
		}
		if (s != null & c != null) {
			return "select s.*, u.name from stings s, users u where s.username= u.username and subject like ? and content like ? limit ?;";
		}

		return null;
	}// si los dos son nulos badrquest

}
