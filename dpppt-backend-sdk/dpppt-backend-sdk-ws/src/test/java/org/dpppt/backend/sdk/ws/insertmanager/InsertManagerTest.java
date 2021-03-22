package org.dpppt.backend.sdk.ws.insertmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.insertionmodifier.OldAndroid0RPModifier;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class InsertManagerTest {
  @Test
  public void testOSEnumWorks() {
    assertEquals("Android", OSType.ANDROID.toString());
    assertEquals("iOS", OSType.IOS.toString());
  }

  @Test
  public void emptyListShouldNotFail() {
    Object theException = null;
    try {
      InsertManager manager =
          new InsertManager(
              new MockDataSource(),
              new ValidationUtils(16, Duration.ofDays(14), Duration.ofHours(2).toMillis()));
      manager.insertIntoDatabase(new ArrayList<>(), null, null, null, false);
    } catch (Exception ex) {
      theException = ex;
    }
    assertNull(theException);
  }

  @Test
  public void nullListShouldNotFail() throws Exception {
    Object theException = null;
    try {
      InsertManager manager =
          new InsertManager(
              new MockDataSource(),
              new ValidationUtils(16, Duration.ofDays(14), Duration.ofHours(2).toMillis()));
      manager.insertIntoDatabase(null, null, null, null, false);
    } catch (Exception ex) {
      theException = ex;
    }
    assertNull(theException);
  }

  @Test
  public void wrongHeaderShouldNotFail() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(InsertManager.class);
    var appender = new TestAppender();
    appender.setContext(logger.getLoggerContext());
    appender.start();
    logger.addAppender(appender);
    InsertManager manager =
        new InsertManager(
            new MockDataSource(),
            new ValidationUtils(16, Duration.ofDays(14), Duration.ofHours(2).toMillis()));
    var key = new GaenKey("POSTMAN+POSTMAN+", (int) UTCInstant.now().get10MinutesSince1970(), 144);
    try {
      manager.insertIntoDatabase(List.of(key), "test", null, UTCInstant.now(), false);
    } catch (RuntimeException ex) {
      if (!ex.getMessage().equals("UPSERT_EXPOSEES")) {
        throw ex;
      }
    }
    appender.stop();
    for (var event : appender.getLog()) {
      assertEquals(Level.ERROR, event.getLevel());
      assertEquals("We received an invalid header, setting default.", event.getMessage());
    }
  }

  @Test
  public void iosRP0ShouldLog() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(OldAndroid0RPModifier.class);
    var appender = new TestAppender();
    appender.setContext(logger.getLoggerContext());
    appender.start();
    logger.addAppender(appender);
    InsertManager manager =
        new InsertManager(
            new MockDataSource(),
            new ValidationUtils(16, Duration.ofDays(14), Duration.ofHours(2).toMillis()));
    manager.addModifier(new OldAndroid0RPModifier());
    var key = new GaenKey("POSTMAN+POSTMAN+", (int) UTCInstant.now().get10MinutesSince1970(), 0);
    try {
      manager.insertIntoDatabase(
          List.of(key), "org.dpppt.testrunner;1.0.0;1;iOS;29", null, UTCInstant.now(), false);
    } catch (RuntimeException ex) {
      if (!ex.getMessage().equals("UPSERT_EXPOSEES")) {
        throw ex;
      }
    }
    appender.stop();
    assertEquals(1, appender.getLog().size());
    for (var event : appender.getLog()) {
      assertEquals(Level.ERROR, event.getLevel());
      assertEquals("We got a rollingPeriod of 0 ({},{},{})", event.getMessage());
      // osType, osVersion, appVersion
      var osType = (OSType) event.getArgumentArray()[0];
      var osVersion = (Version) event.getArgumentArray()[1];
      var appVersion = (Version) event.getArgumentArray()[2];
      assertEquals(OSType.IOS, osType);
      assertEquals("29.0.0", osVersion.toString());
      assertEquals("1.0.0+1", appVersion.toString());
    }
  }

  class TestAppender extends AppenderBase<ILoggingEvent> {
    private final List<ILoggingEvent> log = new ArrayList<ILoggingEvent>();

    @Override
    protected void append(ILoggingEvent eventObject) {
      log.add(eventObject);
    }

    public List<ILoggingEvent> getLog() {
      return log;
    }
  }
}
