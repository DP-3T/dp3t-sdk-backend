/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.controller;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.validation.Valid;

import org.dpppt.backend.sdk.data.DPPPTDataService;
import org.dpppt.backend.sdk.data.EtagGeneratorInterface;
import org.dpppt.backend.sdk.model.ExposedOverview;
import org.dpppt.backend.sdk.model.Exposee;
import org.dpppt.backend.sdk.model.ExposeeRequest;
import org.dpppt.backend.sdk.model.proto.Exposed;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;

@Controller
@RequestMapping("/v1")
public class DPPPTController {

	private final DPPPTDataService dataService;
	private final EtagGeneratorInterface etagGenerator;
	private final String appSource;
	private final int exposedListCacheContol;
	private final ValidateRequest validateRequest;

	private final long batchLength;

	@Autowired
	private ObjectMapper jacksonObjectMapper;

	private static final DateTimeFormatter DAY_DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd")
			.withZone(DateTimeZone.UTC);

	private static final Logger logger = LoggerFactory.getLogger(DPPPTController.class);

	public DPPPTController(DPPPTDataService dataService, EtagGeneratorInterface etagGenerator, String appSource,
			int exposedListCacheControl, ValidateRequest validateRequest, long batchLength) {
		this.dataService = dataService;
		this.appSource = appSource;
		this.etagGenerator = etagGenerator;
		this.exposedListCacheContol = exposedListCacheControl;
		this.validateRequest = validateRequest;
		this.batchLength = batchLength;
	}

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@GetMapping(value = "")
	public @ResponseBody ResponseEntity<String> hello() {
		return ResponseEntity.ok().header("X-HELLO", "dp3t").body("Hello from DP3T WS");
	}

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@PostMapping(value = "/exposed")
	public @ResponseBody ResponseEntity<String> addExposee(@Valid @RequestBody ExposeeRequest exposeeRequest,
			@RequestHeader(value = "User-Agent", required = true) String userAgent,
			@AuthenticationPrincipal Object principal) {
		if (!isValidBase64(exposeeRequest.getKey())) {
			return new ResponseEntity<>("No valid base64 key", HttpStatus.BAD_REQUEST);
		}
		// TODO: should we give that information?
		if (!this.validateRequest.isValid(principal)) {
			return new ResponseEntity<>("Invalid authentication", HttpStatus.BAD_REQUEST);
		}
		Exposee exposee = new Exposee();
		exposee.setKey(exposeeRequest.getKey());
		long keyDate = this.validateRequest.getKeyDate(principal, exposeeRequest);

		exposee.setKeyDate(keyDate);
		dataService.upsertExposee(exposee, appSource);
		return ResponseEntity.ok().build();
	}

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@GetMapping(value = "/hashtest/{dayDateStr}")
	public @ResponseBody ResponseEntity<ExposedOverview> getExposed(@PathVariable String dayDateStr)
			throws NoSuchAlgorithmException, JsonProcessingException {
		DateTime dayDate = DAY_DATE_FORMATTER.parseDateTime(dayDateStr);
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		List<Exposee> exposeeList = dataService.getSortedExposedForDay(dayDate);

		ExposedOverview overview = new ExposedOverview(exposeeList);
		byte[] hash = digest.digest(jacksonObjectMapper.writeValueAsString(overview).getBytes());

		return ResponseEntity.ok().header("JSON-Sha256-Hash", bytesToHex(hash)).body(overview);
	}

	private static String bytesToHex(byte[] hash) {
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if (hex.length() == 1)
				hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@GetMapping(value = "/exposedjson/{batchReleaseTime}", produces = "application/json")
	public @ResponseBody ResponseEntity<ExposedOverview> getExposedByDayDate(@PathVariable Long batchReleaseTime,
			WebRequest request) {
		if (batchReleaseTime % batchLength != 0) {
			return ResponseEntity.badRequest().build();
		}
		if (batchReleaseTime > System.currentTimeMillis()) {
			return ResponseEntity.badRequest().build();
		}

		int max = dataService.getMaxExposedIdForBatchReleaseTime(batchReleaseTime, batchLength);
		String etag = etagGenerator.getEtag(max, "json");
		if (request.checkNotModified(etag)) {
			return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
		} else {
			List<Exposee> exposeeList = dataService.getSortedExposedForBatchReleaseTime(batchReleaseTime, batchLength);
			ExposedOverview overview = new ExposedOverview(exposeeList);
			overview.setBatchReleaseTime(batchReleaseTime);
			return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(exposedListCacheContol)))
					.header("X-BATCH-RELEASE-TIME", batchReleaseTime.toString()).body(overview);
		}
	}

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@GetMapping(value = "/exposed/{batchReleaseTime}", produces = "application/x-protobuf")
	public @ResponseBody ResponseEntity<Exposed.ProtoExposedList> getExposedByBatch(@PathVariable Long batchReleaseTime,
			WebRequest request) {
		if (batchReleaseTime % batchLength != 0) {
			return ResponseEntity.badRequest().build();
		}
		if (batchReleaseTime > System.currentTimeMillis()) {
			return ResponseEntity.badRequest().build();
		}
		int max = dataService.getMaxExposedIdForBatchReleaseTime(batchReleaseTime, batchLength);
		String etag = etagGenerator.getEtag(max, "proto");
		if (request.checkNotModified(etag)) {
			return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
		} else {
			List<Exposee> exposeeList = dataService.getSortedExposedForBatchReleaseTime(batchReleaseTime, batchLength);
			List<Exposed.ProtoExposee> exposees = new ArrayList<>();
			for (Exposee exposee : exposeeList) {
				Exposed.ProtoExposee protoExposee = Exposed.ProtoExposee.newBuilder()
						.setKey(ByteString.copyFrom(Base64.getDecoder().decode(exposee.getKey())))
						.setKeyDate(exposee.getKeyDate()).build();
				exposees.add(protoExposee);
			}
			Exposed.ProtoExposedList protoExposee = Exposed.ProtoExposedList.newBuilder().addAllExposed(exposees)
					.setBatchReleaseTime(batchReleaseTime).build();

			return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(exposedListCacheContol)))
					.header("X-BATCH-RELEASE-TIME", batchReleaseTime.toString()).body(protoExposee);
		}
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseEntity<Object> invalidArguments() {
		return ResponseEntity.badRequest().build();
	}

	private boolean isValidBase64(String value) {
		try {
			Base64.getDecoder().decode(value);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
