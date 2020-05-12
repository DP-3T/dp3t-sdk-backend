/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.ws.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.dpppt.backend.sdk.data.DPPPTDataService;
import org.dpppt.backend.sdk.data.RedeemDataService;
import org.dpppt.backend.sdk.ws.security.JWTClaimSetConverter;
import org.dpppt.backend.sdk.ws.security.JWTValidateRequest;
import org.dpppt.backend.sdk.ws.security.JWTValidator;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.ws.util.KeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
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
public class MultipleJWTConfig {


	public static class CommonJWTBase extends WebSecurityConfigurerAdapter {
		@Value("${ws.app.jwt.publickey}")
		String publicKey;
	
		@Value("${ws.app.jwt.maxValidityMinutes: 60}")
		int maxValidityMinutes;
	
		@Autowired
		@Lazy
		DPPPTDataService dataService;
	
		@Autowired
		@Lazy
		RedeemDataService redeemDataService;


		protected String loadPublicKey() throws IOException {
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

	@Order(1)
	public static class WSJWTSecondConfig extends CommonJWTBase {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
	// @formatter:off
		http
		.antMatcher("/v1/gaen/exposednextday")
		.cors()
        .and()
          .authorizeRequests()
            .antMatchers(HttpMethod.POST, "/v1/gaen/exposednextday")
			.authenticated()
			.anyRequest()
			.permitAll()
        .and()
          .oauth2ResourceServer()
          .jwt().decoder(jwtDecoderSecondDay());
	// @formatter:on
		}
		@Autowired
		@Lazy
		KeyPairHolder secondDayKeyPair;

		@Bean
		public JWTValidator jwtValidatorGAEN() {
			return new JWTValidator(redeemDataService, Duration.ofDays(3));
		}

		@Bean
		public JWTClaimSetConverter claimConverterGAEN() {
			return new JWTClaimSetConverter();
		}

		@Bean
		public JwtDecoder jwtDecoderSecondDay() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
			// X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(loadPublicKey()));
			// KeyFactory kf = KeyFactory.getInstance("RSA");
			// RSAPublicKey pubKey = (RSAPublicKey) kf.generatePublic(keySpecX509);
			NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey)secondDayKeyPair.getKeyPair().getPublic()).build();
			jwtDecoder.setClaimSetConverter(claimConverterGAEN());

			OAuth2TokenValidator<Jwt> defaultValidators = JwtValidators.createDefault();
			jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidators, jwtValidatorGAEN()));
			return jwtDecoder;
		}
	}

	@Order(2)
	public static class WSJWTConfig extends CommonJWTBase {

		
		@Override
		protected void configure(HttpSecurity http) throws Exception {
	// @formatter:off
		http
		//.regexMatcher("/v1/(exposed|exposedlist|gaen/exposed)")
		.cors()
        .and()
          .authorizeRequests()
            .antMatchers(HttpMethod.POST, "/v1/exposed", "/v1/exposedlist", "/v1/gaen/exposed")
            .authenticated()
            .anyRequest()
			.permitAll()
        .and()
          .oauth2ResourceServer()
          .jwt();
	// @formatter:on
		}

		@Bean
		public JWTValidator jwtValidator() {
			return new JWTValidator(redeemDataService, Duration.ofMinutes(maxValidityMinutes));
		}

		@Bean
		public ValidateRequest requestValidator() {
			return new JWTValidateRequest();
		}

		@Bean
		public ValidateRequest gaenRequestValidator() {
			return new org.dpppt.backend.sdk.ws.security.gaen.JWTValidateRequest();
		}

		@Bean
		public JWTClaimSetConverter claimConverter() {
			return new JWTClaimSetConverter();
		}

		@Bean
		@Primary
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
	}
	
}