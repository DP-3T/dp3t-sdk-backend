package org.dpppt.backend.sdk.ws.controller;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class DPPPTControllerTest extends BaseControllerTest {

    private final String jwtTokenn = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI1LVJxcVRUWW9tZnBWejA2VlJFT2ZyYmNxYTVXdTJkX1g4MnZfWlRNLVZVIn0.eyJqdGkiOiI4ZmE5MWRlMi03YmYwLTRhNmYtOWIzZC1hNzdiZDM3ZDdiMTMiLCJleHAiOjE1ODczMTYzMTgsIm5iZiI6MCwiaWF0IjoxNTg3MzE2MDE4LCJpc3MiOiJodHRwczovL2lkZW50aXR5LXIuYml0LmFkbWluLmNoL3JlYWxtcy9iYWctcHRzIiwic3ViIjoiMWVmYTliZWYtOWU5ZC00MjNjLTkxMjctZmQwYjAxNWQxOTY2IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoicHRhLWFwcC1iYWNrZW5kIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiZGVjYzQ3Y2YtMmUyZi00NjZlLThiNTAtZmQ4MTAzZmQ4ZDNhIiwiYWNyIjoiMSIsInNjb3BlIjoiZXhwb3NlZCIsIm9uc2V0IjoiMjAyMC0wNS0yNSJ9.J5beGE6GjgRWEZfwzB9_G6X1uTZcZdm7Mkng8od5Fr3UPT4BbkKgPbpGRscouiAPBjOlZDCs3rcT_qiioX5wAZ0UjqLTe370K53vb1I_f4nQKfTMBYfzvdpS5i5V64LoKbXHpF7PLsGSiox6dA8g5Ssqf5uoTHz1_NY-6GvVq-LmFozV6_1zzYkBVZCLVh0gsqcG9EH2peuhEt9akv_Jmc1Ls0lZQeU1szeRmsk44mg8_FbG33yB3F0azhs0pfEuuYCzGbAqdFCU2RDnRCOXXr7o8Z_klrKE6NArWgbHbk8CE0a-3UwEdi6zw0xm1VNwbnMtjxVcyxECw7V2bSNu9A";

    @Test
    public void testHello() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/v1"))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse();

        assertNotNull(response);
        assertEquals("Hello from DP3T WS", response.getContentAsString());
    }

    // @Test
    // public void testJWT() throws Exception {
    //     MockHttpServer
    // }

}