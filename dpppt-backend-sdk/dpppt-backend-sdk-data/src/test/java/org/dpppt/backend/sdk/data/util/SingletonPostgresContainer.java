/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.data.util;

import org.testcontainers.containers.PostgreSQLContainer;

// JVM handles shut down
public class SingletonPostgresContainer {

  private static final String IMAGE_VERSION = "postgres:12";
  private static final String DB_URL = "DB_URL";
  private static final String DB_PORT = "DB_PORT";
  private static final String DB_USERNAME = "DB_USERNAME";
  private static final String DB_PASSWORD = "DB_PASSWORD";
  private static SingletonPostgresContainer INSTANCE;

  private PostgreSQLContainer<?> container;

  private String jdbcUrl;
  private String databaseName;
  private String username;
  private String password;

  private SingletonPostgresContainer() {
    this.databaseName = "test-db";
    container =
        new PostgreSQLContainer<>(IMAGE_VERSION)
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName(this.databaseName);
  }

  public static SingletonPostgresContainer getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new SingletonPostgresContainer();
    }
    return INSTANCE;
  }

  public void start() {
    String baseUrl = "jdbc:postgresql://%s:%s/test-db";
    String dbUrl = System.getenv(DB_URL);
    String dbPort = System.getenv(DB_PORT);
    this.jdbcUrl = String.format(baseUrl, dbUrl, dbPort);
    this.username = System.getenv(DB_USERNAME);
    this.password = System.getenv(DB_PASSWORD);

    // avoid start container if is already up
    if (System.getenv("SKIP_POSTGRES_CONTAINER") == null) {
      container.start();
      this.jdbcUrl = container.getJdbcUrl();
      this.username = container.getUsername();
      this.password = container.getPassword();
    }

    System.setProperty(DB_URL, this.jdbcUrl);
    System.setProperty(DB_USERNAME, this.username);
    System.setProperty(DB_PASSWORD, this.password);
  }

  public String getDriverClassName() {
    return container.getDriverClassName();
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}
