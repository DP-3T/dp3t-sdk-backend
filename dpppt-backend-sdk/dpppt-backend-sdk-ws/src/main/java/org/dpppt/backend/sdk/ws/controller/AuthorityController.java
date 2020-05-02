package org.dpppt.backend.sdk.ws.controller;

import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.dpppt.backend.sdk.data.DPPPTDataService;
import org.dpppt.backend.sdk.model.AuthorizationCode;
import org.dpppt.backend.sdk.model.BlindSignRequest;
import org.dpppt.backend.sdk.model.HealthCondition;
import org.dpppt.backend.sdk.model.InfectedSignature;
import org.dpppt.backend.sdk.ws.util.AuthorizationCodeHelper;
import org.dpppt.backend.sdk.ws.util.BlindSignatureHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;

/**
 * Due to the independence of this controller, it may be run on a different machine than the {@link DPPPTController}).
 */
@Controller
@RequestMapping("/v1")
public class AuthorityController {

	private final byte[] infectedSigningAuthorizationCodeScryptSalt;
	private final DPPPTDataService dppptDataService;
	private final RSAPrivateCrtKeyParameters privateKeySignsInfections;

	public AuthorityController(DPPPTDataService dppptDataService, byte[] infectedSigningAuthorizationCodeScryptSalt,
							   RSAPrivateCrtKeyParameters infectionSigningPrivateKey) {
		assert dppptDataService != null;
		assert infectedSigningAuthorizationCodeScryptSalt != null && infectedSigningAuthorizationCodeScryptSalt.length > 0;
		assert infectionSigningPrivateKey != null;
		this.dppptDataService = dppptDataService;
		this.infectedSigningAuthorizationCodeScryptSalt = infectedSigningAuthorizationCodeScryptSalt;
		this.privateKeySignsInfections = infectionSigningPrivateKey;
	}

	/**
	 * Only authority members should be allowed to request an authorization code. The code is then communicated to an
	 * individual which then can generate a signature (see e.g. {@link #getInfectedSignature(BlindSignRequest)}).
	 */
	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@GetMapping(value = "/signingauthorizationcode/{status}", produces = "application/json")
	// TODO Protect the method with an authentication mechanism.
	public @ResponseBody ResponseEntity<AuthorizationCode> getSigningAuthorizationCode(@PathVariable HealthCondition status) {
		byte[] authorizationCode;
		{
			byte[] scryptedAuthorizationCode;

			do {
				authorizationCode = AuthorizationCodeHelper.generateAuthorizationCode();
				scryptedAuthorizationCode = AuthorizationCodeHelper.scrypt(authorizationCode, infectedSigningAuthorizationCodeScryptSalt);
			} while (dppptDataService.insertSigningAuthorizationCode(scryptedAuthorizationCode, status) == false);
		}

		return ResponseEntity.ok().body(new AuthorizationCode(AuthorizationCodeHelper.getBaseXAuthorizationCode(authorizationCode)));
	}

	/**
	 * @return A infected blind signature which allows an individual to make is verified infection public anonymously.
	 *
	 * Note: Such a signature is infinite valid but it can only be used once (ensured by the method
	 * {@link DPPPTDataService#insertBlindSignatureId(byte[])}). In order to limit the validity period of the signature,
	 * the server keys must be revoked.
	 */
	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@PostMapping(value = "/infectedsignature", produces = "application/json")
	public @ResponseBody ResponseEntity<InfectedSignature> getInfectedSignature(
			@Valid @RequestBody BlindSignRequest infectedBlindSignRequest) {
		byte[] scryptAuthorizationCode = AuthorizationCodeHelper.scrypt(infectedBlindSignRequest.getAuthorizationCode(),
				infectedSigningAuthorizationCodeScryptSalt);
		// If the signing fails somehow, the individual must acquire an new authorization code.
		if (dppptDataService.updateSigningAuthorizationCode(scryptAuthorizationCode, HealthCondition.INFECTED,
				LocalDateTime.now().minusHours(24))) {
			byte[] blindSignedData = BlindSignatureHelper.blindSign(infectedBlindSignRequest.getBase64BlindSignRequest(),
					privateKeySignsInfections);
			return ResponseEntity.ok().body(new InfectedSignature(blindSignedData));
		}

		return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
	}

	/**
	 * It is thinkable that an authority wants that just virus exposed individuals without being tested also provide
	 * their keys. Such an approach safes the time testing requires which may be necessary in periods with a high
	 * reproduction number R.
	 *
	 * This is implemented in a separate method in order it can be overridden and to avoid probably a bug-prone if else
	 * cascade.
	 */
	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@PostMapping(value = "/exposedsignature", produces = "application/json")
	// TODO Implement.
	public @ResponseBody ResponseEntity<InfectedSignature> getExposedSignature(
			@Valid @RequestBody BlindSignRequest exposedSignatureRequest) {
		throw new UnsupportedOperationException();
	}
}
