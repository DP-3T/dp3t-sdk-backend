package org.dpppt.backend.sdk.ws.controller;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URL;
import java.util.Base64;

import org.dpppt.backend.sdk.model.ExposeeAuthData;
import org.dpppt.backend.sdk.model.ExposeeRequest;

public class DPPPTControllerNoSecurityTest extends BaseControllerNoSecurityTest {
    @Test
    public void testJWT() throws Exception {
        ExposeeRequest exposeeRequest = new ExposeeRequest();
        exposeeRequest.setAuthData(new ExposeeAuthData());
        exposeeRequest.setOnset("2020-04-10");
        exposeeRequest.setKey(Base64.getEncoder().encodeToString("test".getBytes("UTF-8")));
        MockHttpServletResponse response = mockMvc.perform(post("/v1/exposed")
                                                            .contentType(MediaType.APPLICATION_JSON)
                                                            .header("Authorization", "Bearer " + jwtToken)
                                                            .header("User-Agent", "MockMVC")
                                                            .content(json(exposeeRequest)))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse();
        response = mockMvc.perform(post("/v1/exposed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "MockMVC")
                        .content(json(exposeeRequest)))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse();
    }
}