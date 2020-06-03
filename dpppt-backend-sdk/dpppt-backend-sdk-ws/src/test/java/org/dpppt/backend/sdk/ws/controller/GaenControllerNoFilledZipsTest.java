package org.dpppt.backend.sdk.ws.controller;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "ws.app.jwt.publickey=classpath://generated_pub.pem",
        "logging.level.org.springframework.security=DEBUG", "ws.exposedlist.batchlength=7200000",
        "ws.gaen.fillemptyzips=false" })
public class GaenControllerNoFilledZipsTest extends BaseControllerTest {
    @Test
	@Transactional
	public void testEmptyResponseWhenNoZipFill() throws Exception {
		MockHttpServletResponse response = mockMvc
				.perform(get("/v1/gaen/exposed/"
						+ LocalDate.now(ZoneOffset.UTC).minusDays(8).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
								.header("User-Agent", "MockMVC"))
				.andExpect(status().isNoContent()).andReturn().getResponse();
		
		var etag = response.getHeader("ETag");
        var firstPublishUntil = response.getHeader("X-PUBLISHED-UNTIL");
        var signature = response.getHeader("Signature");
        assertNull(signature); //Todo: Android fix
        assertNotNull(firstPublishUntil);
        assertNull(etag);
	}
}