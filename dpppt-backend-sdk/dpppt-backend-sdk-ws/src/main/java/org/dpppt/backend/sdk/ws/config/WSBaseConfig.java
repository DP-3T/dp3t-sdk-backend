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

import java.security.KeyPair;
import java.time.Duration;
import java.util.List;

import javax.sql.DataSource;

import org.dpppt.backend.sdk.data.DPPPTDataService;
import org.dpppt.backend.sdk.data.JDBCDPPPTDataServiceImpl;
import org.dpppt.backend.sdk.data.JDBCRedeemDataServiceImpl;
import org.dpppt.backend.sdk.data.RedeemDataService;
import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.data.gaen.JDBCGAENDataServiceImpl;
import org.dpppt.backend.sdk.ws.controller.DPPPTController;
import org.dpppt.backend.sdk.ws.controller.GaenController;
import org.dpppt.backend.sdk.ws.filter.ResponseWrapperFilter;
import org.dpppt.backend.sdk.ws.security.KeyVault;
import org.dpppt.backend.sdk.ws.security.NoValidateRequest;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.ws.security.signature.ProtoSignature;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

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

	@Value("${ws.headers.debug: false}")
	boolean setDebugHeaders;

	@Value("${ws.retentiondays: 21}")
	int retentionDays;

	@Value("${ws.exposedlist.batchlength: 7200000}")
	long batchLength;

	@Value("${ws.exposedlist.requestTime: 1500}")
	long requestTime;

	@Value("${ws.app.source}")
	String appSource;

	@Value("${ws.app.gaen.region:ch}")
	String gaenRegion;

	@Value("${ws.app.gaen.key_size: 16}")
	int gaenKeySizeBytes;
	@Value("${ws.app.key_size: 32}")
	int keySizeBytes;

	@Value("${ws.app.ios.bundleId:org.dppt.ios.demo}")
	String bundleId;
	@Value("${ws.app.android.packageName:org.dpppt.android.demo}")
	String packageName;
	@Value("${ws.app.gaen.keyVersion:v1}")
	String keyVersion;
	@Value("${ws.app.gaen.keyIdentifier:228}")
	String keyIdentifier;
	@Value("${ws.app.gaen.algorithm:1.2.840.10045.4.3.2}")
	String gaenAlgorithm;

	@Autowired(required = false)
	ValidateRequest requestValidator;

	@Autowired(required = false)
	ValidateRequest gaenRequestValidator;

	@Autowired
	@Lazy
	KeyVault keyVault;

	final SignatureAlgorithm algorithm = SignatureAlgorithm.ES256;
	
	public String getBundleId() {
		return this.bundleId;
	}

	public String getPackageName() {
		return this.packageName;
	}

	public String getKeyVersion() {
		return this.keyVersion;
	}
	
	public String getKeyIdentifier() {
		return this.keyIdentifier;
	}

	@Bean
	public ProtoSignature gaenSigner() {
		try {
			return new ProtoSignature(gaenAlgorithm, keyVault.get("gaen"), getBundleId(), getPackageName(), getKeyVersion(),
					getKeyIdentifier(), gaenRegion, Duration.ofMillis(batchLength));
		} catch (Exception ex) {
			throw new RuntimeException("Cannot initialize signer for protobuf");
		}
	}

	@Bean
	public DPPPTController dppptSDKController() {
		ValidateRequest theValidator = requestValidator;
		if (theValidator == null) {
			theValidator = new NoValidateRequest();
		}
		return new DPPPTController(dppptSDKDataService(), appSource, exposedListCacheControl,
				theValidator, dpptValidationUtils(), batchLength, requestTime);
	}

	@Bean
	public ValidationUtils dpptValidationUtils() {
		return new ValidationUtils(keySizeBytes, Duration.ofDays(retentionDays), batchLength);
	}

	@Bean
	public ValidationUtils gaenValidationUtils() {
		return new ValidationUtils(gaenKeySizeBytes, Duration.ofDays(retentionDays), batchLength);
	}

	@Bean
	public GaenController gaenController() {
		ValidateRequest theValidator = gaenRequestValidator;
		if (theValidator == null) {
			theValidator = backupValidator();
		}
		return new GaenController(gaenDataService(), theValidator, gaenSigner(), gaenValidationUtils(),
				Duration.ofMillis(batchLength), Duration.ofMillis(requestTime),
				Duration.ofMinutes(exposedListCacheControl), keyVault.get("nextDayJWT").getPrivate());
	}

	@Bean
	ValidateRequest backupValidator() {
		return new NoValidateRequest();
	}

	@Bean
	public DPPPTDataService dppptSDKDataService() {
		return new JDBCDPPPTDataServiceImpl(getDbType(), dataSource());
	}

	@Bean
	public GAENDataService gaenDataService() {
		return new JDBCGAENDataServiceImpl(getDbType(), dataSource());
	}

	@Bean
	public RedeemDataService redeemDataService() {
		return new JDBCRedeemDataServiceImpl(dataSource());
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
	public ResponseWrapperFilter hashFilter() {
		return new ResponseWrapperFilter(keyVault.get("hashFilter"), retentionDays, protectedHeaders, setDebugHeaders);
	}

	public KeyPair getKeyPair(SignatureAlgorithm algorithm) {
		logger.warn("USING FALLBACK KEYPAIR. WONT'T PERSIST APP RESTART AND PROBABLY DOES NOT HAVE ENOUGH ENTROPY.");

		return Keys.keyPairFor(algorithm);
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addFixedRateTask(new IntervalTask(() -> {
			logger.info("Start DB cleanup");
			dppptSDKDataService().cleanDB(Duration.ofDays(retentionDays));
			gaenDataService().cleanDB(Duration.ofDays(retentionDays));
			redeemDataService().cleanDB(Duration.ofDays(1));
			logger.info("DB cleanup up");
		}, 60 * 60 * 1000L));
	}
}
