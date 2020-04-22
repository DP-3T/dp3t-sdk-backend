package org.dpppt.backend.sdk.ws.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dpppt.backend.sdk.data.DPPPTDataService;
import org.dpppt.backend.sdk.data.EtagGeneratorInterface;
import org.dpppt.backend.sdk.model.ExposedOverview;
import org.dpppt.backend.sdk.model.Exposee;
import org.dpppt.backend.sdk.model.ExposeeRequest;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import javax.validation.Valid;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Controller
@RequestMapping("/v1")
public class DPPPTController {

	private final DPPPTDataService dataService;
	private final EtagGeneratorInterface etagGenerator;
	private final String appSource;
	private final int exposedListCacheContol;
	private final ValidateRequest validateRequest;
	@Autowired
	private ObjectMapper jacksonObjectMapper;

	private static final DateTimeFormatter DAY_DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd")
			.withZone(DateTimeZone.UTC);

	private static final Logger logger = LoggerFactory.getLogger(DPPPTController.class);

	public DPPPTController(DPPPTDataService dataService, EtagGeneratorInterface etagGenerator, String appSource,
			int exposedListCacheControl, ValidateRequest validateRequest) {
		this.dataService = dataService;
		this.appSource = appSource;
		this.etagGenerator = etagGenerator;
		this.exposedListCacheContol = exposedListCacheControl;
		this.validateRequest = validateRequest;
	}

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@GetMapping(value = "")
	public @ResponseBody String hello() {
		return "Hello from DP3T WS";
	}

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@PostMapping(value = "/exposed")
	public @ResponseBody ResponseEntity<String> addExposee(@Valid @RequestBody ExposeeRequest exposeeRequest,
			@RequestHeader(value = "User-Agent", required = true) String userAgent, @AuthenticationPrincipal Object principal) {
		if (!isValidBase64(exposeeRequest.getKey())) {
			return new ResponseEntity<>("No valid base64 key", HttpStatus.BAD_REQUEST);
		}
		//TODO: should we give that information?
		if (!this.validateRequest.isValid(principal)) {
			return new ResponseEntity<>("Invalid authentication", HttpStatus.BAD_REQUEST);
		}
		if (!isValidDate(exposeeRequest.getOnset())) {
			return new ResponseEntity<>("Invalid onset date", HttpStatus.BAD_REQUEST);
		}
		Exposee exposee = new Exposee();
		exposee.setKey(exposeeRequest.getKey());
		String onsetDate = this.validateRequest.getOnset(principal, exposeeRequest);
	
		exposee.setOnset(onsetDate);
		dataService.upsertExposee(exposee, appSource);
		return ResponseEntity.ok().build();
	}

	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@GetMapping(value = "/hashtest/{dayDateStr}")
	public @ResponseBody ResponseEntity<ExposedOverview> getExposed(@PathVariable String dayDateStr ) throws NoSuchAlgorithmException, JsonProcessingException {
		DateTime dayDate = DAY_DATE_FORMATTER.parseDateTime(dayDateStr);
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		List<Exposee> exposeeList = dataService.getSortedExposedForDay(dayDate);

		ExposedOverview overview = new ExposedOverview(exposeeList);
		byte[] hash = digest.digest(jacksonObjectMapper.writeValueAsString(
			overview
		).getBytes());
		
		return ResponseEntity.ok().header("JSON-Sha256-Hash", bytesToHex(hash)).body(overview);
	}

	private static String bytesToHex(byte[] hash) {
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < hash.length; i++) {
		String hex = Integer.toHexString(0xff & hash[i]);
		if(hex.length() == 1) hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}
	@CrossOrigin(origins = { "https://editor.swagger.io" })
	@GetMapping(value = "/exposed/{dayDateStr}")
	public @ResponseBody ResponseEntity<ExposedOverview> getExposed(@PathVariable String dayDateStr,
			WebRequest request) {
		DateTime dayDate = DAY_DATE_FORMATTER.parseDateTime(dayDateStr);
		int max = dataService.getMaxExposedIdForDay(dayDate);
		String etag = etagGenerator.getEtag(max);
		if (request.checkNotModified(etag)) {
			return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
		} else {
			List<Exposee> exposeeList = dataService.getSortedExposedForDay(dayDate);
			ExposedOverview overview = new ExposedOverview(exposeeList);
			return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(exposedListCacheContol)))
					.body(overview);
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

	private boolean isValidDate(String value) {
		try {
			DAY_DATE_FORMATTER.parseDateTime(value);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
