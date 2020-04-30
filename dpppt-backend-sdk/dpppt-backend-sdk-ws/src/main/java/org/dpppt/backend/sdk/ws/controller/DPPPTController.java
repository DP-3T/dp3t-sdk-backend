/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.controller;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.codec.binary.Hex;
import org.dpppt.backend.sdk.data.DPPPTDataService;
import org.dpppt.backend.sdk.data.EtagGeneratorInterface;
import org.dpppt.backend.sdk.model.BucketList;
import org.dpppt.backend.sdk.model.ExposedOverview;
import org.dpppt.backend.sdk.model.Exposee;
import org.dpppt.backend.sdk.model.ExposeeRequest;
import org.dpppt.backend.sdk.model.proto.Exposed;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.InvalidDateException;
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
	private final int retentionDays;

	private final long batchLength;

	private final long requestTime;
	@Autowired
	private ObjectMapper jacksonObjectMapper;


	public DPPPTController(DPPPTDataService dataService, EtagGeneratorInterface etagGenerator, String appSource,
			int exposedListCacheControl, ValidateRequest validateRequest, long batchLength, int retentionDays, long requestTime) {
		this.dataService = dataService;
		this.appSource = appSource;
		this.etagGenerator = etagGenerator;
		this.exposedListCacheContol = exposedListCacheControl;
		this.validateRequest = validateRequest;
		this.batchLength = batchLength;
		this.retentionDays = retentionDays;
		this.requestTime = requestTime;
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
			@AuthenticationPrincipal Object principal) throws InvalidDateException {
		long now = System.currentTimeMillis();
		if (!this.validateRequest.isValid(principal)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		if (!isValidBase64(exposeeRequest.getKey())) {
			return new ResponseEntity<>("No valid base64 key", HttpStatus.BAD_REQUEST);
		}
		// TODO: should we give that information?
		Exposee exposee = new Exposee();
		exposee.setKey(exposeeRequest.getKey());
		long keyDate = this.validateRequest.getKeyDate(principal, exposeeRequest);

		exposee.setKeyDate(keyDate);
		if(!this.validateRequest.isFakeRequest(principal, exposeeRequest)) {
			dataService.upsertExposee(exposee, appSource);
		} 
		
		long after = System.currentTimeMillis();
		long duration = after - now;
		try{
			Thread.sleep(Math.min(this.requestTime - duration,0));
		}
		catch (Exception ex) {
			
		}
		return ResponseEntity.ok().build();
	}

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@GetMapping(value = "/hashtest/{dayDateStr}")
	public @ResponseBody ResponseEntity<ExposedOverview> getExposed(@PathVariable String dayDateStr)
			throws NoSuchAlgorithmException, JsonProcessingException {
		OffsetDateTime dayDate = LocalDate.parse(dayDateStr).atStartOfDay().atOffset(ZoneOffset.UTC);
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		List<Exposee> exposeeList = dataService.getSortedExposedForDay(dayDate);

		ExposedOverview overview = new ExposedOverview(exposeeList);
		byte[] hash = digest.digest(jacksonObjectMapper.writeValueAsString(overview).getBytes());

		return ResponseEntity.ok().header("JSON-Sha256-Hash", Hex.encodeHexString(hash)).body(overview);
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
		if (batchReleaseTime > OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli()) {
			return ResponseEntity.notFound().build();
		}
		if (batchReleaseTime < OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).minusDays(retentionDays).toInstant().toEpochMilli()){
			return ResponseEntity.notFound().build();
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

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@GetMapping(value = "/buckets/{dayDateStr}", produces = "application/json")
	public @ResponseBody ResponseEntity<BucketList> getListOfBuckets(@PathVariable String dayDateStr) {
		OffsetDateTime day = LocalDate.parse(dayDateStr).atStartOfDay().atOffset(ZoneOffset.UTC);
		OffsetDateTime currentBucket = day;
		OffsetDateTime now = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC);
		List<Long> bucketList = new ArrayList<>();
		while(currentBucket.toInstant().toEpochMilli() < Math.min(day.plusDays(1).toInstant().toEpochMilli(), now.toInstant().toEpochMilli())) {
			bucketList.add(currentBucket.toInstant().toEpochMilli());
			currentBucket = currentBucket.plusSeconds(batchLength/1000);
		}
		BucketList list = new BucketList();
		list.setBuckets(bucketList);
		return ResponseEntity.ok(list);
	}


	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseEntity<Object> invalidArguments() {
		return ResponseEntity.badRequest().build();
	}

	@ExceptionHandler(InvalidDateException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseEntity<Object> invalidDate() {
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
