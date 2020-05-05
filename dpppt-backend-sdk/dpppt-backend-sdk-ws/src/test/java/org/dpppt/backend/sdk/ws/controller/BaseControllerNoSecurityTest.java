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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "dev" })
@TestPropertySource(properties = { "ws.app.source=org.dpppt.demo", })
public abstract class BaseControllerNoSecurityTest {
	protected MockMvc mockMvc;

	protected final String jwtToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI1LVJxcVRUWW9tZnBWejA2VlJFT2ZyYmNxYTVXdTJkX1g4MnZfWlRNLVZVIn0.eyJqdGkiOiI4ZmE5MWRlMi03YmYwLTRhNmYtOWIzZC1hNzdiZDM3ZDdiMTMiLCJleHAiOjE1ODczMTYzMTgsIm5iZiI6MCwiaWF0IjoxNTg3MzE2MDE4LCJpc3MiOiJodHRwczovL2lkZW50aXR5LXIuYml0LmFkbWluLmNoL3JlYWxtcy9iYWctcHRzIiwic3ViIjoiMWVmYTliZWYtOWU5ZC00MjNjLTkxMjctZmQwYjAxNWQxOTY2IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoicHRhLWFwcC1iYWNrZW5kIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiZGVjYzQ3Y2YtMmUyZi00NjZlLThiNTAtZmQ4MTAzZmQ4ZDNhIiwiYWNyIjoiMSIsInNjb3BlIjoiZXhwb3NlZCIsIm9uc2V0IjoiMjAyMC0wNS0yNSJ9.J5beGE6GjgRWEZfwzB9_G6X1uTZcZdm7Mkng8od5Fr3UPT4BbkKgPbpGRscouiAPBjOlZDCs3rcT_qiioX5wAZ0UjqLTe370K53vb1I_f4nQKfTMBYfzvdpS5i5V64LoKbXHpF7PLsGSiox6dA8g5Ssqf5uoTHz1_NY-6GvVq-LmFozV6_1zzYkBVZCLVh0gsqcG9EH2peuhEt9akv_Jmc1Ls0lZQeU1szeRmsk44mg8_FbG33yB3F0azhs0pfEuuYCzGbAqdFCU2RDnRCOXXr7o8Z_klrKE6NArWgbHbk8CE0a-3UwEdi6zw0xm1VNwbnMtjxVcyxECw7V2bSNu9A";

	protected final String wrongToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI1LVJxcVRUWW9tZnBWejA2VlJFT2ZyYmNxYTVXdTJkX1g4MnZfWlRNLVZVIn0.eyJqdGkiOiI4ZmE5MWRlMi03YmYwLTRhNmYtOWIzZC1hNzdiZDM3ZDdiMTMiLCJleHAiOjE1ODczMTYzMTgsIm5iZiI6MCwiaWF0IjoxNTg3MzE2MDE4LCJpc3MiOiJodHRwczovL2lkZW50aXR5LXIuYml0LmFkbWluLmNoL3JlYWxtcy9iYWctcHRzIiwic3ViIjoiMWVmYTliZWYtOWU5ZC00MjNjLkxMjctZmQwYjAxNWQxOTY2IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoicHRhLWFwcC1iYWNrZW5kIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiZGVjYzQ3Y2YtMmUyZi00NjZlLThiNTAtZmQ4MTAzZmQ4ZDNhIiwiYWNyIjoiMSIsInNjb3BlIjoiZXhwb3NlZCIsIm9uc2V0IjoiMjAyMC0wNS0yNSJ9.J5beGE6GjgRWEZfwzB9_G6X1uTZcZdm7Mkng8od5Fr3UPT4BbkKgPbpGRscouiAPBjOlZDCs3rcT_qiioX5wAZ0UjqLTe370K53vb1I_f4nQKfTMBYfzvdpS5i5V64LoKbXHpF7PLsGSiox6dA8g5Ssqf5uoTHz1_NY-6GvVq-LmFozV6_1zzYkBVZCLVh0gsqcG9EH2peuhEt9akv_Jmc1Ls0lZQeU1szeRmsk44mg8_FbG33yB3F0azhs0pfEuuYCzGbAqdFCU2RDnRCOXXr7o8Z_klrKE6NArWgbHbk8CE0a-3UwEdi6zw0xm1VNwbnMtjxVcyxECw7V2bSNu9A";

	@Autowired
	private WebApplicationContext webApplicationContext;
	protected ObjectMapper objectMapper;

	@Before
	public void setup() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		this.objectMapper = new ObjectMapper(new JsonFactory());
		this.objectMapper.registerModule(new JavaTimeModule());
		// this makes sure, that the objectmapper does not fail, when a filter
		// is not provided.
		this.objectMapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
	}

	protected String json(Object o) throws IOException {
		return objectMapper.writeValueAsString(o);
	}
}