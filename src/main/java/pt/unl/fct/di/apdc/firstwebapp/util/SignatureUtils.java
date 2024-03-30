package pt.unl.fct.di.apdc.firstwebapp.util;

import java.util.Base64;
import java.util.UUID;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;

public class SignatureUtils {

	// Settings that must be in the database
	public static final String ADMIN = "Admin";
	public static final String BACKOFFICE = "Backoffice";
	public static final String REGULAR = "Regular";
	private static final String key = "isto e uma chave muito segura que ninguem consegue adivinhar porque e secreta e segura";

	private static final Logger LOG = Logger.getLogger(SignatureUtils.class.getName());

	public static final String ALGORITHM = "HmacSHA256";

	public static String calculateHMac(String key, String data) {
		try {
			SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), ALGORITHM);

			Mac sha256_HMAC = Mac.getInstance(ALGORITHM);
			sha256_HMAC.init(secret_key);

			return base64Enconder(sha256_HMAC.doFinal(data.getBytes("UTF-8")));
		} catch (Exception e) {
			LOG.severe("Error while signing. " + e.toString());
		}

		return null;
	}

	public static String byteArrayToHex(byte[] str) {
		StringBuilder sb = new StringBuilder(str.length * 2);
		for (byte b : str)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}

	public static String base64Enconder(byte[] str) {
		String encodedString = Base64.getEncoder().encodeToString(str);
		return encodedString;
	}

	public static NewCookie letsBake(String username) {
		String id = UUID.randomUUID().toString();
		long currentTime = System.currentTimeMillis();
		String fields = username + "." + id + "." + currentTime + "." + 1000 * 60 * 60 * 2;

		String signature = SignatureUtils.calculateHMac(key, fields);

		if (signature == null) {
			return null;
		}

		String value = fields + "." + signature;
		NewCookie cookie = new NewCookie("session::apdc", value, "/", null, "comment", 1000 * 60 * 60 * 2, false, true);
		return cookie;
	}

	public static boolean checkPermissions(Cookie cookie, String role) {
		if (cookie == null || cookie.getValue() == null) {
			return false;
		}

		String value = cookie.getValue();
		String[] values = value.split("\\.");

		String signatureNew = SignatureUtils.calculateHMac(key,
				values[0] + "." + values[1] + "." + values[2] + "." + values[3] + "." + values[4]);
		String signatureOld = values[5];

		if (!signatureNew.equals(signatureOld)) {
			return false;
		}

		int neededRole = convertRole(role);
		int userInSessionRole = convertRole(values[2]);

		if (userInSessionRole < neededRole) {
			return false;
		}
		if (System.currentTimeMillis() > (Long.valueOf(values[3]) + Long.valueOf(values[4]) * 1000)) {

			return false;
		}
		return true;
	}

	public static String checkUser(Cookie cookie) {
		if (cookie == null || cookie.getValue() == null) {
			return null;
		}

		String value = cookie.getValue();
		String[] values = value.split("\\.");

		String signatureNew = SignatureUtils.calculateHMac(key,
				values[0] + "." + values[1] + "." + values[2] + "." + values[3]);
		String signatureOld = values[4];

		if (!signatureNew.equals(signatureOld)) {
			return null;
		}

		if (System.currentTimeMillis() > (Long.valueOf(values[2]) + Long.valueOf(values[3]) * 1000)) {

			return null;
		}

		return values[0];
	}

	private static int convertRole(String role) {
		int result = 0;

		switch (role) {
		case "Editor":
			result = 1;
			break;
		case "Admin":
			result = 2;
			break;
		case "Viewer":
			result = 0;
			break;
		default:
			result = 0;
			break;
		}
		return result;
	}
}
