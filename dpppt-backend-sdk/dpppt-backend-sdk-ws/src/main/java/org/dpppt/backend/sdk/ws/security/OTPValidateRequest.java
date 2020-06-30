/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.ws.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.openjson.JSONObject;
import org.dpppt.backend.sdk.model.ExposeeRequest;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;

public class OTPValidateRequest implements ValidateRequest {

	private boolean validateAndConsumeOTP(String otp) {
		HashMap<String, String> values = new HashMap<>() {{
			put("code", otp);
		}};

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			String requestBody = objectMapper.writeValueAsString(values);

			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("http://iti-386.iti.gr:57000/api/code-generator/consume"))
					.setHeader("token", "24beac0f63eaf1183893f19ba2f7d3b008fb889e")
					.setHeader("content-type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(requestBody))
					.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			String jsonString = response.body();
			JSONObject obj = new JSONObject(jsonString);

			return obj.getBoolean("status");
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean isValid(Object authObject) {
		if (authObject instanceof ExposeeRequest) {
			String otp = ((ExposeeRequest) authObject).getAuthData().getValue();

			try {
				Base64.Decoder decoder = Base64.getDecoder();
				String decodedOtp = new String(decoder.decode(otp));
				return validateAndConsumeOTP(decodedOtp);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public long getKeyDate(Object authObject, Object others) throws InvalidDateException {
		if (others instanceof ExposeeRequest) {
			ExposeeRequest request = ((ExposeeRequest) others);
			if (request.getKeyDate() < OffsetDateTime.now().minusDays(21).toInstant().toEpochMilli()) {
				throw new InvalidDateException();
			} else if (request.getKeyDate() > OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant()
					.toEpochMilli()) {
				throw new InvalidDateException();
			}
			return request.getKeyDate();
		}
		if (others instanceof GaenKey) {
			GaenKey key = ((GaenKey) others);
			var requestDate = Duration.of(key.getRollingStartNumber(), GaenUnit.TenMinutes);
			if (requestDate.toMillis() < OffsetDateTime.now().minusDays(21).toInstant().toEpochMilli()) {
				throw new InvalidDateException();
			} else if (requestDate.toMillis() > OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant()
					.toEpochMilli()) {
				throw new InvalidDateException();
			}
			return requestDate.toMillis();
		}
		throw new IllegalArgumentException();
	}

	@Override
	public boolean isFakeRequest(Object authObject, Object others) {
		if (others instanceof ExposeeRequest) {
			ExposeeRequest request = ((ExposeeRequest) others);
			return request.isFake() == 1;
		}
		if (others instanceof GaenKey) {
			GaenKey request = ((GaenKey) others);
			return request.getFake() == 1;
		}
		throw new IllegalArgumentException();
	}

}