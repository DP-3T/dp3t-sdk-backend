package org.dpppt.backend.sdk.ws.util;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.digests.GeneralDigest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.RSABlindingEngine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSABlindingFactorGenerator;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.RSABlindingParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.signers.PSSSigner;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Base64;

import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;

/**
 * See also: {@link org.bouncycastle.crypto.test.PSSBlindTest#testSig(int, RSAKeyParameters, RSAKeyParameters, byte[], byte[], byte[])}
 */
public class BlindSignatureHelper {

	/**
	 * {@link SecureRandom} is thread safe.
	 */
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	/**
	 * When changing, update the "V0_6__blind_signature_id.sql" file as well.
	 */
	// TODO Review
	private static final int ID_LENGTH = 32;

	/**
	 * See the note in the {@link PSSSigner} class comment.
	 */
	// TODO Review
	private static final int SALT_LENGTH = getSHADigest().getDigestSize();

	// TODO Review
	private static GeneralDigest getSHADigest() {
		return new SHA256Digest();
	}

	public static RSABlindingParameters generateRSABlindingParameters(RSAKeyParameters serverPublicKey) {
		RSABlindingFactorGenerator blindingFactorGenerator = new RSABlindingFactorGenerator();
		blindingFactorGenerator.init(serverPublicKey);
		BigInteger blindingFactor = blindingFactorGenerator.generateBlindingFactor();
		return new RSABlindingParameters(serverPublicKey, blindingFactor);
	}

	public static byte[] generateId() {
		byte[] result = new byte[ID_LENGTH];
		SECURE_RANDOM.nextBytes(result);
		return result;
	}
	
	public static byte[] generateBlindSignRequest(byte[] id, RSABlindingParameters rsaBlindingParameters) throws CryptoException {
		PSSSigner pssSigner = new PSSSigner(new RSABlindingEngine(), getSHADigest(), SALT_LENGTH);
		pssSigner.init(true, new ParametersWithRandom(rsaBlindingParameters, SECURE_RANDOM));
		pssSigner.update(id, 0, id.length);
		return pssSigner.generateSignature();
	}

	public static String generateBase64BlindSignRequest(byte[] id, RSABlindingParameters rsaBlindingParameters) throws CryptoException {
		return getEncoder().encodeToString(generateBlindSignRequest(id, rsaBlindingParameters));
	}

	public static byte[] blindSign(byte[] blindSignRequest, RSAKeyParameters serverPrivateKey) {
		RSAEngine engine = new RSAEngine();
		engine.init(true, serverPrivateKey);
		return engine.processBlock(blindSignRequest, 0, blindSignRequest.length);
	}

	public static byte[] blindSign(String base64BlindSignRequest, RSAKeyParameters serverPrivateKey) {
		return blindSign(Base64.getDecoder().decode(base64BlindSignRequest), serverPrivateKey);
	}

	public static byte[] unblindSignResponse(byte[] signResponse, RSABlindingParameters rsaBlindingParameters) {
		RSABlindingEngine rsaBlindingEngine = new RSABlindingEngine();
		rsaBlindingEngine.init(false, rsaBlindingParameters);
		return rsaBlindingEngine.processBlock(signResponse, 0, signResponse.length);
	}

	public static byte[] unblindSignResponse(String base64SignResponse, RSABlindingParameters rsaBlindingParameters) {
		return unblindSignResponse(getDecoder().decode(base64SignResponse), rsaBlindingParameters);
	}

	public static boolean verifySignature(byte[] id, byte[] signature, RSAKeyParameters serverPublicKey) {
		PSSSigner pssSigner = new PSSSigner(new RSAEngine(), getSHADigest(), SALT_LENGTH);
		pssSigner.init(false, serverPublicKey);
		pssSigner.update(id, 0, id.length);
		return pssSigner.verifySignature(signature);
	}

	public static boolean verifySignature(String base64Id, String base64Signature, RSAKeyParameters serverPublicKey) {
		return verifySignature(getDecoder().decode(base64Id), getDecoder().decode(base64Signature), serverPublicKey);
	}
}
