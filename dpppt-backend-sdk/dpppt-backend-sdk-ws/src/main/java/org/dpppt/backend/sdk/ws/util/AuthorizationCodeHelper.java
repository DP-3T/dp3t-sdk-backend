package org.dpppt.backend.sdk.ws.util;

import org.apache.commons.codec.binary.Base32;
import org.bouncycastle.crypto.generators.SCrypt;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;

import java.security.SecureRandom;

/**
 * This authentication code is short (communicable by voice), simple (alphanumeric), fault-tolerant (case-insensitive)
 * and secure ({@link #NUMBER_OF_BYTES} with a short validity period).
 */
public class AuthorizationCodeHelper {

	// TODO Review
	public static final int SCRYPT_SALT_LENGTH = 64;

	/**
	 * When changing, update the "V0_5__signing_authorization_code.sql" file as well.
	 */
	// TODO Review
	public static final int SCRYPT_CODE_LENGTH = 32;

	// TODO Review
	private static final int NUMBER_OF_BYTES = Long.BYTES;

	/**
	 * {@link Base32} is thread safe.
	 */
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	/**
	 * {@link SecureRandom} is thread safe.
	 */
	private static final Base32 BASE_X = new Base32();

	public static byte[] generateAuthorizationCode() {
		byte[] result = new byte[NUMBER_OF_BYTES];
		SECURE_RANDOM.nextBytes(result);
		return result;
	}

	public static String getBaseXAuthorizationCode(byte[] authorizationCode) {
		assert authorizationCode.length == NUMBER_OF_BYTES;
		byte[] tmp = BASE_X.encode(authorizationCode);
		return new String(tmp, 0, tmp.length - 3);
	}

	public static byte[] getAuthorizationCode(String base64AuthorizationCode) {
		return BASE_X.decode(base64AuthorizationCode);
	}

	/**
	 * Scrypt parameters selected like in {@link SCryptPasswordEncoder}.
	 */
	// TODO Review: Same salt for every scrypt code generation.
	public static byte[] scrypt(byte[] authorizationCode, byte[] salt) {
		assert salt.length == SCRYPT_SALT_LENGTH;
		return SCrypt.generate(authorizationCode, salt, 16384, 8, 1, SCRYPT_CODE_LENGTH);
	}

	public static byte[] scrypt(String baseXAuthorizationCode, byte[] salt) {
		return scrypt(getAuthorizationCode(baseXAuthorizationCode), salt);
	}
}
