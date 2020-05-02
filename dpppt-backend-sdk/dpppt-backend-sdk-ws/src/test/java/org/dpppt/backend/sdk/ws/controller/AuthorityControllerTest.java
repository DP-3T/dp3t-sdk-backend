package org.dpppt.backend.sdk.ws.controller;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.crypto.params.RSABlindingParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.dpppt.backend.sdk.model.*;
import org.dpppt.backend.sdk.ws.util.BlindSignatureHelper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import static java.util.Base64.getEncoder;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"ws.app.jwt.publickey=classpath://generated_pub.pem",
		"ws.app.authority.infection.signing.rsa.public.key=classpath://generated_pub.pem",
		"ws.app.authority.infection.signing.rsa.private.key=classpath://generated_private.pem"})
public class AuthorityControllerTest extends BaseControllerTest {

	private RSAKeyParameters bcPublicKeyVerifiesInfections;

	private PublicKey jcePublicKeyVerifiesInfections;

	@Before
	public void setup() throws Exception {
		super.setup();
		InputStream inputStream = new ClassPathResource("generated_pub.pem").getInputStream();
		byte[] publicKey = Base64.getDecoder().decode(IOUtils.toString(inputStream));
		bcPublicKeyVerifiesInfections = (RSAKeyParameters) PublicKeyFactory.createKey(publicKey);
		jcePublicKeyVerifiesInfections = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
	}

	@Test
	public void test() throws Exception {
		MockHttpServletResponse response;
		String token;

		response = mockMvc
				.perform(get("/v1/signingauthorizationcode/" + HealthCondition.INFECTED))
				.andExpect(status().is2xxSuccessful())
				.andReturn()
				.getResponse();
		AuthorizationCode authorizationCode = object(response.getContentAsString(), AuthorizationCode.class);

		RSABlindingParameters rsaBlindingParameters = BlindSignatureHelper
				.generateRSABlindingParameters(bcPublicKeyVerifiesInfections);
		byte[] id = BlindSignatureHelper.generateId();
		String base64BlindSignRequest = BlindSignatureHelper.generateBase64BlindSignRequest(id, rsaBlindingParameters);

		BlindSignRequest infectedSignatureRequest = new BlindSignRequest(authorizationCode, base64BlindSignRequest);
		response = mockMvc
				.perform(post("/v1/infectedsignature").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(infectedSignatureRequest)))
				.andExpect(status().is2xxSuccessful())
				.andReturn()
				.getResponse();
		InfectedSignature infectedSignature = object(response.getContentAsString(), InfectedSignature.class);
		// Cannot use the same authorization code twice.
		mockMvc
				.perform(post("/v1/infectedsignature").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(infectedSignatureRequest)))
				.andExpect(status().isForbidden())
				.andReturn()
				.getResponse();

		byte[] unblindSignResponse = BlindSignatureHelper.unblindSignResponse(infectedSignature.getBase64signature(),
				rsaBlindingParameters);
		assertTrue(BlindSignatureHelper.verifySignature(id, unblindSignResponse, bcPublicKeyVerifiesInfections));
		{
			// Verification with JCE
			Signature signature = Signature.getInstance("SHA256withRSA/PSS");
			signature.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
			signature.initVerify(jcePublicKeyVerifiesInfections);
			signature.update(id);
			assertTrue(signature.verify(unblindSignResponse));
		}

		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData(getEncoder().encodeToString(id), getEncoder()
				.encodeToString(unblindSignResponse)));
		exposeeRequest.setKeyDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
		exposeeRequest.setKey(Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.UTF_8)));
		exposeeRequest.setIsFake(0);

		token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		response = mockMvc
				.perform(post("/v1/exposed")
						.contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + token)
						.header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is2xxSuccessful())
				.andReturn()
				.getResponse();
		// Cannot use the same blind signature twice.
		token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		response = mockMvc
				.perform(post("/v1/exposed")
						.contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + token)
						.header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().isForbidden())
				.andReturn()
				.getResponse();
	}
}
