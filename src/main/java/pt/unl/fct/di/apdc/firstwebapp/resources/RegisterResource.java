package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;
import com.google.gson.Gson;
import com.google.cloud.Timestamp;

import pt.unl.fct.di.apdc.firstwebapp.util.*;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource {

	/**
	 * Logger Object
	 */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	private final Gson g = new Gson();

	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	public RegisterResource() {
	} // Nothing to be done here

	@POST
	@Path("/v1")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doRegister(LoginData data) {
		LOG.fine("Attemp to register user: " + data.username);
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Entity person = Entity.newBuilder(userKey).set("password", DigestUtils.sha512Hex(data.password))
				.set("timeOfCreation", Timestamp.now()).build();
		try {
			datastore.add(person);
		} catch (DatastoreException e) {
			if ("ALREADY_EXISTS".equals(e.getReason())) {
				// entity.getKey() already exists
				return Response.status(Status.FORBIDDEN).entity("Username already in use.").build();
			}
		}
		AuthToken at = new AuthToken(data.username);
		return Response.ok(g.toJson(at)).build();
	}

	@POST
	@Path("/v2")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doRegisterV2(LoginDataV2 data) {
		LOG.fine("Attemp to register user: " + data.username);

		String status = isDataValid(data);
		if (!status.equals(Utils.SUCCESS)) {
			return Response.status(Status.BAD_REQUEST).entity(status).build();
		}

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Entity person = Entity.newBuilder(userKey).set("password", DigestUtils.sha512Hex(data.password))
				.set("timeOfCreation", Timestamp.now()).set("email", data.email).set("name", data.name).build();

		try {
			datastore.add(person);
		} catch (DatastoreException e) {
			if ("ALREADY_EXISTS".equals(e.getReason())) {
				// entity.getKey() already exists
				return Response.status(Status.FORBIDDEN).entity("Username already in use.").build();
			}
		}
		AuthToken at = new AuthToken(data.username);
		return Response.ok(g.toJson(at)).build();
	}

	@PUT
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doUpdate(LoginDataV2 data, @CookieParam("session::apdc") Cookie cookie) {
		LOG.fine("Attemp to update user: " + data.username);

		if (!SignatureUtils.checkUser(cookie).equals(data.username)) {
			return Response.status(Status.FORBIDDEN).build();
		}

		String status = isDataValid(data);
		if (!status.equals(Utils.SUCCESS)) {
			return Response.status(Status.BAD_REQUEST).entity(status).build();
		}

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Entity user = datastore.get(userKey);
		if (user == null) {
			return Response.status(Status.FORBIDDEN).build();
		}
		Entity person = Entity.newBuilder(user).set("password", DigestUtils.sha512Hex(data.password))
				.set("email", data.email).set("name", data.name).build();

		try {
			datastore.update(person);
		} catch (DatastoreException e) {

			// user to update doesn't exist
			return Response.status(Status.FORBIDDEN).build();

		}
		NewCookie newCookie = SignatureUtils.letsBake(data.username);
		return Response.ok().cookie(newCookie).build();
	}

	private String areParamsNull(LoginDataV2 data) {
		String status = Utils.SUCCESS;
		if (Utils.isFieldNull(data.username) || Utils.isFieldNull(data.password) || Utils.isFieldNull(data.confirmation)
				|| Utils.isFieldNull(data.email) || Utils.isFieldNull(data.name))
			status = Utils.FIELDS_NULL;
		return status;
	}

	private String areParamsEmpty(LoginDataV2 data) {
		String status = Utils.SUCCESS;
		if (Utils.isFieldEmpty(data.username) || Utils.isFieldEmpty(data.password)
				|| Utils.isFieldEmpty(data.confirmation) || Utils.isFieldEmpty(data.email)
				|| Utils.isFieldEmpty(data.name)) {
			status = Utils.FIELDS_EMPTY;

		}
		return status;

	}

	private String arePasswordsEqual(String password1, String confirmation) {
		String status = Utils.SUCCESS;
		if (!Utils.areFieldsEqual(password1, confirmation)) {
			status = Utils.PW_NO_MATCH;
		}
		return status;
	}

	private String isDataValid(LoginDataV2 data) {

		String status = areParamsNull(data);
		if (!status.equals(Utils.SUCCESS)) {
			return status;
		}

		status = areParamsEmpty(data);
		if (!status.equals(Utils.SUCCESS)) {
			return status;
		}

		status = Utils.isEmailValid(data.email);
		if (!status.equals(Utils.SUCCESS)) {
			return status;
		}
		status = Utils.isNameValid(data.name);
		if (!status.equals(Utils.SUCCESS)) {
			return status;
		}
		status = Utils.isUsernameValid(data.username);
		if (!status.equals(Utils.SUCCESS)) {
			return status;
		}

		status = Utils.isPasswordValid(data.password);
		if (!status.equals(Utils.SUCCESS)) {
			return status;
		}

		status = arePasswordsEqual(data.password, data.confirmation);
		if (!status.equals(Utils.SUCCESS)) {
			return status;
		}
		return Utils.SUCCESS;

	}
}
