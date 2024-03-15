package pt.unl.fct.di.apdc.firstwebapp.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginDataV2;

import java.util.logging.Logger;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource {

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());

	private final Gson g = new Gson();

	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	public RegisterResource() {

	}

	@POST
	@Path("/v1")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doRegister(LoginData data) {
		LOG.fine("Registier attempt by user: " + data.username);
		Key userKey = datastore.newKeyFactory().setKind("username").newKey(data.username);
		Entity person = Entity.newBuilder(userKey).set("password", data.password)
				.set("timeOfCreation", System.currentTimeMillis()).build();

		try {
			datastore.add(person);
		} catch (DatastoreException ex) {

			return Response.status(Status.FORBIDDEN).entity("Username already in use.").build();
		}

		AuthToken at = new AuthToken(data.username);
		return Response.ok(g.toJson(at)).build();
	}

	@POST
	@Path("/v2")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doRegisterV2(LoginDataV2 data) {
		LOG.fine("Registier attempt by user: " + data.username);
		if (!data.password.equals(data.confirmation)) {

			return Response.status(Status.FORBIDDEN).entity("Password mismatch.").build();
		}

		if (data.username == null || data.password == null || data.confirmation == null || data.email == null
				|| data.name == null) {
			return Response.status(Status.BAD_REQUEST).entity("You forgot to fill at least a field.").build();
		}
		if (data.username.isEmpty() || data.password.isEmpty() || data.confirmation.isEmpty() || data.email.isEmpty()
				|| data.name.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("You must think you are really funny.").build();
		}
		Key userKey = datastore.newKeyFactory().setKind("username").newKey(data.username);
		Entity person = Entity.newBuilder(userKey).set("password", data.password)
				.set("timeOfCreation", System.currentTimeMillis()).set("confirmation", data.confirmation)
				.set("email", data.email).set("name", data.name).build();

		try {
			datastore.add(person);
		} catch (DatastoreException ex) {

			return Response.status(Status.FORBIDDEN).entity("Username already in use.").build();
		}

		AuthToken at = new AuthToken(data.username);
		return Response.ok(g.toJson(at)).build();
	}

	@GET
	@Path("/{username}")
	public Response checkUsernameAvailable(@PathParam("username") String username) {
		if (username.equals("jleitao")) {
			return Response.ok().entity(g.toJson(false)).build();
		} else {
			return Response.ok().entity(g.toJson(true)).build();
		}
	}

}
