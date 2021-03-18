/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.interops.config;

import org.dpppt.backend.sdk.interops.config.configbeans.ActuatorSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 9)
@Profile(value = "actuator-security")
@EnableWebSecurity
public class ActuatorSecurity extends WebSecurityConfigurerAdapter {

  private static final String PROMETHEUS_ROLE = "PROMETHEUS";

  @Value("${ws.monitor.prometheus.user}")
  private String user;

  @Autowired Environment environment;
  // region Actuator Passwords
  // ----------------------------------------------------------------------------------------------------------------------------------
  @Bean
  @Profile("cloud-dev")
  ActuatorSecurityConfig passwordCloudDev() {
    return new ActuatorSecurityConfig(
        user, environment.getProperty("vcap.services.ha_prometheus_dev.credentials.password"));
  }

  @Bean
  @Profile("cloud-test")
  ActuatorSecurityConfig passwordCloudTest() {
    return new ActuatorSecurityConfig(
        user, environment.getProperty("vcap.services.ha_prometheus_test.credentials.password"));
  }

  @Bean
  @Profile("cloud-abn")
  ActuatorSecurityConfig passwordCloudAbn() {
    return new ActuatorSecurityConfig(
        user, environment.getProperty("vcap.services.ha_prometheus_abn.credentials.password"));
  }

  @Bean
  @Profile("cloud-prod")
  ActuatorSecurityConfig passwordProdAbn() {
    return new ActuatorSecurityConfig(
        user, environment.getProperty("vcap.services.ha_prometheus_prod.credentials.password"));
  }

  @Bean
  @ConditionalOnMissingBean
  ActuatorSecurityConfig passwordDefault() {
    return new ActuatorSecurityConfig(
        user, environment.getProperty("ws.monitor.prometheus.password"));
  }
  // ----------------------------------------------------------------------------------------------------------------------------------
  // endregion

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.requestMatcher(
            org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
                .toAnyEndpoint())
        .authorizeRequests()
        .requestMatchers(
            org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.to(
                HealthEndpoint.class))
        .permitAll()
        .requestMatchers(
            org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.to(
                InfoEndpoint.class))
        .permitAll()
        .requestMatchers(
            org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.to(
                LoggersEndpoint.class))
        .hasRole(PROMETHEUS_ROLE)
        .requestMatchers(
            org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.to(
                PrometheusScrapeEndpoint.class))
        .hasRole(PROMETHEUS_ROLE)
        .anyRequest()
        .denyAll()
        .and()
        .httpBasic();

    http.csrf().ignoringAntMatchers("/actuator/loggers/**");
  }

  @Autowired
  protected void configureGlobal(
      AuthenticationManagerBuilder auth, ActuatorSecurityConfig securityConfig) throws Exception {
    auth.inMemoryAuthentication()
        .withUser(securityConfig.getUsername())
        .password(passwordEncoder().encode(securityConfig.getPassword()))
        .roles(PROMETHEUS_ROLE);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
