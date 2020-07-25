/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.ws.controller;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.dpppt.backend.sdk.data.DPPPTDataService;
import org.dpppt.backend.sdk.model.BucketList;
import org.dpppt.backend.sdk.model.ExposedOverview;
import org.dpppt.backend.sdk.model.Exposee;
import org.dpppt.backend.sdk.model.ExposeeRequest;
import org.dpppt.backend.sdk.model.ExposeeRequestList;
import org.dpppt.backend.sdk.model.proto.Exposed;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.ClaimIsBeforeOnsetException;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.InvalidDateException;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.WrongScopeException;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;
import org.dpppt.backend.sdk.ws.util.ValidationUtils.BadBatchReleaseTimeException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

import ch.ubique.openapi.docannotations.Documentation;

import com.google.protobuf.ByteString;

@Controller
@RequestMapping("/v1")
public class DPPPTController {

	private final DPPPTDataService dataService;
	private final String appSource;
	private final int exposedListCacheControl;
	private final ValidateRequest validateRequest;
	private final ValidationUtils validationUtils;
	// time in milliseconds that exposed keys are hidden before being served, in order to prevent timing attacks
	private final long releaseBucketDuration;
	private final long requestTime;


	public DPPPTController(DPPPTDataService dataService, String appSource,
			int exposedListCacheControl, ValidateRequest validateRequest, ValidationUtils validationUtils, long releaseBucketDuration,
			long requestTime) {
		this.dataService = dataService;
		this.appSource = appSource;
		this.exposedListCacheControl = exposedListCacheControl/1000/60;
		this.validateRequest = validateRequest;
		this.validationUtils = validationUtils;
		this.releaseBucketDuration = releaseBucketDuration;
		this.requestTime = requestTime;
	}

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@GetMapping(value = "")
	@Documentation(description = "Hello return", responses = {"200=>server live"})
	public @ResponseBody ResponseEntity<String> hello() {
		return ResponseEntity.ok().header("X-HELLO", "dp3t").body("Hello from DP3T WS");
	}

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@PostMapping(value = "/exposed")
	@Documentation(
			description = "Send exposed key to server",
			responses = {
					"200=>The exposed keys have been stored in the database",
					"400=>Invalid base64 encoding in expose request",
					"403=>Authentication failed"
			})
	public @ResponseBody ResponseEntity<String> addExposee(@Valid @RequestBody
															   @Documentation(description = "The ExposeeRequest contains the SecretKey from the guessed infection date, the infection date itself, and some authentication data to verify the test result")
																	   ExposeeRequest exposeeRequest,
			@RequestHeader(value = "User-Agent", required = true)
            @Documentation(description = "App Identifier (PackageName/BundleIdentifier) + App-Version + OS (Android/iOS) + OS-Version", example = "ch.ubique.android.starsdk;1.0;iOS;13.3")
                    String userAgent,
			@AuthenticationPrincipal Object principal) throws InvalidDateException, WrongScopeException, ClaimIsBeforeOnsetException {
		var now = UTCInstant.now();

		if (!this.validateRequest.isValid(principal)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		if (!validationUtils.isValidBase64Key(exposeeRequest.getKey())) {
			return new ResponseEntity<>("No valid base64 key", HttpStatus.BAD_REQUEST);
		}
		// TODO: should we give that information?
		Exposee exposee = new Exposee();
		exposee.setKey(exposeeRequest.getKey());
		long keyDate = this.validateRequest.validateKeyDate(now, principal, exposeeRequest);

		exposee.setKeyDate(keyDate);
		if (!this.validateRequest.isFakeRequest(principal, exposeeRequest)) {
			dataService.upsertExposee(exposee, appSource);
		}

		long after = System.currentTimeMillis();
		long duration = after - now.getTimestamp();
		try {
			Thread.sleep(Math.max(this.requestTime - duration, 0));
		} catch (Exception ex) {

		}
		return ResponseEntity.ok().build();
	}

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@PostMapping(value = "/exposedlist")
	@Documentation(description = "Send a list of exposed keys to server",
			responses = {
	                "200=>The exposed keys have been stored in the database",
			        "400=>Invalid base64 encoding in exposee request",
                    "403=>Authentication failed"})
	public @ResponseBody ResponseEntity<String> addExposee(@Valid @RequestBody
                                                               @Documentation(description = "The ExposeeRequest contains the SecretKey from the guessed infection date, the infection date itself, and some authentication data to verify the test result")
                                                                       ExposeeRequestList exposeeRequests,
			@RequestHeader(value = "User-Agent", required = true)
            @Documentation(description = "App Identifier (PackageName/BundleIdentifier) + App-Version + OS (Android/iOS) + OS-Version", example = "ch.ubique.android.starsdk;1.0;iOS;13.3")
                    String userAgent,
			@AuthenticationPrincipal Object principal) throws InvalidDateException, WrongScopeException, ClaimIsBeforeOnsetException  {
		var now = UTCInstant.now();

		if (!this.validateRequest.isValid(principal)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		List<Exposee> exposees = new ArrayList<>();
		for (var exposedKey : exposeeRequests.getExposedKeys()) {
			if (!validationUtils.isValidBase64Key(exposedKey.getKey())) {
				return new ResponseEntity<>("No valid base64 key", HttpStatus.BAD_REQUEST);
			}

			Exposee exposee = new Exposee();
			exposee.setKey(exposedKey.getKey());
			long keyDate = this.validateRequest.validateKeyDate(now, principal, exposedKey);

			exposee.setKeyDate(keyDate);
			exposees.add(exposee);
		}

		if (!this.validateRequest.isFakeRequest(principal, exposeeRequests)) {
			dataService.upsertExposees(exposees, appSource);
		}

		long after = System.currentTimeMillis();
		long duration = after - now.getTimestamp();
		try {
			Thread.sleep(Math.max(this.requestTime - duration, 0));
		} catch (Exception ex) {

		}
		return ResponseEntity.ok().build();
	}

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@GetMapping(value = "/exposedjson/{batchReleaseTime}", produces = "application/json")
	@Documentation(description = "Query list of exposed keys from a specific batch release time",
			responses = {
	                "200=>Returns ExposedOverview in json format, which includes all exposed keys which were published on _batchReleaseTime_",
                    "404=>Couldn't find _batchReleaseTime_"
	})
	public @ResponseBody ResponseEntity<ExposedOverview> getExposedByDayDate(@PathVariable
                                                                                 @Documentation(description = "The batch release date of the exposed keys in milliseconds since Unix Epoch (1970-01-01), must be a multiple of 2 * 60 * 60 * 1000",
                                                                                         example = "1593043200000")
                                                                                         long batchReleaseTime,
			WebRequest request) throws BadBatchReleaseTimeException{
		var now = UTCInstant.now();
		if(!validationUtils.isValidBatchReleaseTime(UTCInstant.ofEpochMillis(batchReleaseTime), now)) {
			return ResponseEntity.notFound().build();
		}

		List<Exposee> exposeeList = dataService.getSortedExposedForBatchReleaseTime(batchReleaseTime, releaseBucketDuration);
		ExposedOverview overview = new ExposedOverview(exposeeList);
		overview.setBatchReleaseTime(batchReleaseTime);
		return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(exposedListCacheControl)))
				.header("X-BATCH-RELEASE-TIME", Long.toString(batchReleaseTime)).body(overview);
	}

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@GetMapping(value = "/exposed/{batchReleaseTime}", produces = "application/x-protobuf")
    @Documentation(description = "Query list of exposed keys from a specific batch release time",
            responses = {
                    "200=>Returns ExposedOverview in protobuf format, which includes all exposed keys which were published on _batchReleaseTime_",
                    "404=>Couldn't find _batchReleaseTime_"
            })
	public @ResponseBody ResponseEntity<Exposed.ProtoExposedList> getExposedByBatch(@PathVariable
                                                                                        @Documentation(description = "The batch release date of the exposed keys in milliseconds since Unix Epoch (1970-01-01), must be a multiple of 2 * 60 * 60 * 1000",
                                                                                                example = "1593043200000")
                                                                                                long batchReleaseTime,
			WebRequest request) throws BadBatchReleaseTimeException {
		var now = UTCInstant.now();
		if(!validationUtils.isValidBatchReleaseTime(UTCInstant.ofEpochMillis(batchReleaseTime), now)) {
			return ResponseEntity.notFound().build();
		}

		List<Exposee> exposeeList = dataService.getSortedExposedForBatchReleaseTime(batchReleaseTime, releaseBucketDuration);
		List<Exposed.ProtoExposee> exposees = new ArrayList<>();
		for (Exposee exposee : exposeeList) {
			Exposed.ProtoExposee protoExposee = Exposed.ProtoExposee.newBuilder()
					.setKey(ByteString.copyFrom(Base64.getDecoder().decode(exposee.getKey())))
					.setKeyDate(exposee.getKeyDate()).build();
			exposees.add(protoExposee);
		}
		Exposed.ProtoExposedList protoExposee = Exposed.ProtoExposedList.newBuilder().addAllExposed(exposees)
				.setBatchReleaseTime(batchReleaseTime).build();

		return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(exposedListCacheControl)))
				.header("X-BATCH-RELEASE-TIME", Long.toString(batchReleaseTime)).body(protoExposee);
	}

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@GetMapping(value = "/buckets/{dayDateStr}", produces = "application/json")
    @Documentation(description = "Query number of available buckets in a given day, starting from midnight UTC",
            responses = {
                    "200=>Returns BucketList in json format, indicating all available buckets since _dayDateStr_"
            })
	public @ResponseBody ResponseEntity<BucketList> getListOfBuckets(@PathVariable
                                                                         @Documentation(description = "The date starting when to return the available buckets, in ISO8601 date format", example = "2019-01-31")
                                                                                 String dayDateStr) {
		var day = UTCInstant.parseDate(dayDateStr);
		var currentBucket = day;
		var now = UTCInstant.now();
		List<Long> bucketList = new ArrayList<>();
		while (currentBucket.getTimestamp() < Math.min(day.plusDays(1).getTimestamp(),
				now.getTimestamp())) {
			bucketList.add(currentBucket.getTimestamp());
			currentBucket = currentBucket.plusSeconds(releaseBucketDuration / 1000);
		}
		BucketList list = new BucketList();
		list.setBuckets(bucketList);
		return ResponseEntity.ok(list);
	}

	@ExceptionHandler({IllegalArgumentException.class, InvalidDateException.class, JsonProcessingException.class,
			MethodArgumentNotValidException.class, BadBatchReleaseTimeException.class, DateTimeParseException.class})
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseEntity<Object> invalidArguments() {
		return ResponseEntity.badRequest().build();
	}

}
