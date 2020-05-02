/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.dpppt.backend.sdk.data.DPPPTDataService;
import org.dpppt.backend.sdk.data.EtagGenerator;
import org.dpppt.backend.sdk.data.EtagGeneratorInterface;
import org.dpppt.backend.sdk.data.JDBCDPPPTDataServiceImpl;
import org.dpppt.backend.sdk.ws.controller.AuthorityController;
import org.dpppt.backend.sdk.ws.controller.DPPPTController;
import org.dpppt.backend.sdk.ws.filter.ResponseWrapperFilter;
import org.dpppt.backend.sdk.ws.security.NoValidateRequest;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.ws.util.AuthorizationCodeHelper;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableScheduling
public abstract class WSBaseConfig implements SchedulingConfigurer, WebMvcConfigurer {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public abstract DataSource dataSource();

	public abstract Flyway flyway();

	public abstract String getDbType();

	@Value("${ws.exposedlist.cachecontrol: 5}")
	int exposedListCacheControl;

	@Value("${ws.headers.protected:}")
	List<String> protectedHeaders;

	@Value("${ws.retentiondays: 21}")
	int retentionDays;

	@Value("${ws.exposedlist.batchlength: 7200000}")
	long batchLength;

	@Value("${ws.exposedlist.requestTime: 1500}")
	long requestTime;

	@Value("${ws.app.source}")
	String appSource;

	@Autowired(required = false)
	ValidateRequest requestValidator;

	final SignatureAlgorithm algorithm = SignatureAlgorithm.ES256;

	@Bean
	public DPPPTController dppptSDKController(@Value("${ws.app.authority.infection.signing.rsa.public.key}") String publicKey) throws IOException {
		ValidateRequest theValidator = requestValidator;
		if (theValidator == null) {
			theValidator = new NoValidateRequest();
		}
		RSAKeyParameters p = (RSAKeyParameters) PublicKeyFactory.createKey(loadKey(publicKey));
		return new DPPPTController(dppptSDKDataService(), etagGenerator(), appSource, exposedListCacheControl,
				theValidator, batchLength, retentionDays, requestTime, p);
	}

	/**
	 * Override the method in order to use a persistent salt value.
	 */
	@Bean
	public byte[] signingAuthorizationCodeScryptSalt() {
		byte[] result = new byte[AuthorizationCodeHelper.SCRYPT_SALT_LENGTH];
		new SecureRandom().nextBytes(result);
		return result;
	}

	@Bean
	public AuthorityController authorityController(@Value("#{signingAuthorizationCodeScryptSalt}") byte[] signingAuthorizationCodeScryptSalt,
												   @Value("${ws.app.authority.infection.signing.rsa.private.key}") String infectionSigningRSAPrivateKey) throws IOException {
		RSAPrivateCrtKeyParameters p = (RSAPrivateCrtKeyParameters) PrivateKeyFactory.createKey(loadKey(infectionSigningRSAPrivateKey));
		return new AuthorityController(dppptSDKDataService(), signingAuthorizationCodeScryptSalt, p);
	}

	@Bean
	public DPPPTDataService dppptSDKDataService() {
		return new JDBCDPPPTDataServiceImpl(getDbType(), dataSource());
	}

	@Bean
	public MappingJackson2HttpMessageConverter converter() {
		ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
				.setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
				.registerModules(new ProtobufModule(), new Jdk8Module());
		return new MappingJackson2HttpMessageConverter(mapper);
	}

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.add(new ProtobufHttpMessageConverter());
		WebMvcConfigurer.super.extendMessageConverters(converters);
	}

	@Bean
	public EtagGeneratorInterface etagGenerator() {
		return new EtagGenerator();
	}

	@Bean
	public ResponseWrapperFilter hashFilter() {
		return new ResponseWrapperFilter(getKeyPair(algorithm), retentionDays, protectedHeaders);
	}

	public KeyPair getKeyPair(SignatureAlgorithm algorithm) {
		logger.warn("USING FALLBACK KEYPAIR. WONT'T PERSIST APP RESTART AND PROBABLY DOES NOT HAVE ENOUGH ENTROPY.");
		return Keys.keyPairFor(algorithm);
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addFixedRateTask(new IntervalTask(() -> {
			logger.info("Start DB cleanup");
			dppptSDKDataService().cleanDB(retentionDays);
			logger.info("DB cleanup up");
		}, 60 * 60 * 1000L));
	}

	protected byte[] loadKey(String key) throws IOException {
		if (key.startsWith("classpath:/")) {
			InputStream inputStream = new ClassPathResource(key.substring(11)).getInputStream();
			key = IOUtils.toString(inputStream);
		} else if (key.startsWith("file:/")) {
			InputStream inputStream = new FileInputStream(key);
			key = IOUtils.toString(inputStream);
		}
		return Base64.getDecoder().decode(key);
	}
}
