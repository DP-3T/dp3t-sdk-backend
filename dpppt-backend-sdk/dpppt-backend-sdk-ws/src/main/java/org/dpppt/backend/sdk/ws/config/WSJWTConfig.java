/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.dpppt.backend.sdk.data.DPPPTDataService;
import org.dpppt.backend.sdk.ws.security.JWTClaimSetConverter;
import org.dpppt.backend.sdk.ws.security.JWTValidateRequest;
import org.dpppt.backend.sdk.ws.security.JWTValidator;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.ws.util.KeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@EnableWebSecurity
@Profile(value = "jwt")
public class WSJWTConfig extends WebSecurityConfigurerAdapter {

	@Value("${ws.app.jwt.publickey}")
	String publicKey;

	@Autowired
	@Lazy
	DPPPTDataService dataService;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
	// @formatter:off
		http.cors()
        .and()
          .authorizeRequests()
            .antMatchers(HttpMethod.POST, "/v1/exposed")
            .authenticated()
            .anyRequest()
			.permitAll()
        .and()
          .oauth2ResourceServer()
          .jwt();
	// @formatter:on
	}

	@Bean
	public JwtDecoder jwtDecoder() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
		X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(loadPublicKey()));
		KeyFactory kf = KeyFactory.getInstance("RSA");
		RSAPublicKey pubKey = (RSAPublicKey) kf.generatePublic(keySpecX509);
		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(pubKey).build();
		jwtDecoder.setClaimSetConverter(claimConverter());

		OAuth2TokenValidator<Jwt> defaultValidators = JwtValidators.createDefault();
		jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidators, jwtValidator()));
		return jwtDecoder;
	}

	@Bean
	public JWTValidator jwtValidator() {
		return new JWTValidator(dataService);
	}

	@Bean
	public ValidateRequest requestValidator() {
		return new JWTValidateRequest();
	}

	@Bean
	public JWTClaimSetConverter claimConverter() {
		return new JWTClaimSetConverter();
	}

	private String loadPublicKey() throws IOException {
		if (publicKey.startsWith("keycloak:")) {
			String url = publicKey.replace("keycloak:/", "");
			return KeyHelper.getPublicKeyFromKeycloak(url);
		}

		InputStream in = null;
		if (publicKey.startsWith("classpath:/")) {
			in = new ClassPathResource(publicKey.substring(11)).getInputStream();
			return IOUtils.toString(in);
		} else if (publicKey.startsWith("file:/")) {
			in = new FileInputStream(publicKey);
			return IOUtils.toString(in);
		}
		return publicKey;
	}
}
