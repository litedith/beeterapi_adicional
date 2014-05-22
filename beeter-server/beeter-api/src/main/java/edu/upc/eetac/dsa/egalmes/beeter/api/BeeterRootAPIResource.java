package edu.upc.eetac.dsa.egalmes.beeter.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
 
import edu.upc.eetac.dsa.egalmes.beeter.api.model.BeeterRootAPI;
 
@Path("/")//raiz
public class BeeterRootAPIResource {
	@GET
	public BeeterRootAPI getRootAPI() {
		BeeterRootAPI api = new BeeterRootAPI();
		return api;
	}
}
