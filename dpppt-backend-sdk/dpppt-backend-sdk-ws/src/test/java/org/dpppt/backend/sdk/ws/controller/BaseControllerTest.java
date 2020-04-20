package org.dpppt.backend.sdk.ws.controller;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.dpppt.backend.sdk.ws.controller.config.DPPPTControllerConfig;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.IOException;

@WebAppConfiguration
@ContextConfiguration(loader = AnnotationConfigWebContextLoader.class, classes = {DPPPTControllerConfig.class})
@EnableWebMvc
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class BaseControllerTest {

	protected MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;
	protected ObjectMapper objectMapper;

	@Before
	public void setup() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		this.objectMapper = new ObjectMapper(new JsonFactory());
		this.objectMapper.registerModule(new JavaTimeModule());
		this.objectMapper.registerModule(new JodaModule());
		// this makes sure, that the objectmapper does not fail, when a filter
		// is not provided.
		this.objectMapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
	}

	protected String json(Object o) throws IOException {
		return objectMapper.writeValueAsString(o);
	}

}
