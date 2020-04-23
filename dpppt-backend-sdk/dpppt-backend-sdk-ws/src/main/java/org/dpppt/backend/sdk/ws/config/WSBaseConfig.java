/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.config;

import java.security.KeyPair;

import java.util.List;

import javax.sql.DataSource;

import org.dpppt.backend.sdk.data.DPPPTDataService;
import org.dpppt.backend.sdk.data.EtagGenerator;
import org.dpppt.backend.sdk.data.EtagGeneratorInterface;
import org.dpppt.backend.sdk.data.JDBCDPPPTDataServiceImpl;
import org.dpppt.backend.sdk.ws.controller.DPPPTController;
import org.dpppt.backend.sdk.ws.filter.ResponseWrapperFilter;
import org.dpppt.backend.sdk.ws.security.NoValidateRequest;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;


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

	@Value("${ws.app.source}")
	String appSource;

	@Autowired(required = false)
	ValidateRequest requestValidator;

	final SignatureAlgorithm algorithm = SignatureAlgorithm.ES256;

	@Bean
	public DPPPTController dppptSDKController() {
		ValidateRequest theValidator = requestValidator;
		if (theValidator == null) {
			theValidator = new NoValidateRequest();
		}
		return new DPPPTController(dppptSDKDataService(), etagGenerator(), appSource, exposedListCacheControl,
				theValidator, batchLength);
	}

	@Bean
	public DPPPTDataService dppptSDKDataService() {
		return new JDBCDPPPTDataServiceImpl(getDbType(), dataSource());
	}

	@Bean
	public MappingJackson2HttpMessageConverter converter() {
		ObjectMapper mapper = new ObjectMapper()
				.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
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

}
