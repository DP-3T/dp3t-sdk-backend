package org.dpppt.backend.sdk.ws.config;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@Profile(value = "jwt")
public class WSJWTConfig extends WebSecurityConfigurerAdapter {
	
	@Value("${ws.app.jwt.publickey}")
	String publicKeyUrl;

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
	public JwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties)
			throws InvalidKeySpecException, NoSuchAlgorithmException {
		X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(loadPublicKey()));
		KeyFactory kf = KeyFactory.getInstance("RSA");
		RSAPublicKey pubKey = (RSAPublicKey) kf.generatePublic(keySpecX509);
		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(pubKey).build();
		jwtDecoder.setClaimSetConverter(null); // TODO read claims out of jwt, onset?
		jwtDecoder.setJwtValidator(null); // TODO validate: time, not yet used via uuid -> check db, if empty, insert and return true, else false.hm
		return jwtDecoder;
	}

	private String loadPublicKey() {
		// TODO: read public key from differnt sources based on prefix:
		// file:/
		// classpath:/
		// http(s):/
		return null;
	}
}
