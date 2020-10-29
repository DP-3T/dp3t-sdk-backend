package org.dpppt.backend.sdk.ws.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.dpppt.backend.sdk.semver.Version;
import org.junit.Test;

public class SemverTests {

  @Test
  public void testToString() throws Exception {
    var v = new Version("ios-1.1.3-test+meta");
    assertEquals("1.1.3-test+meta", v.toString());
    v = new Version("1.1.3+meta");
    assertEquals("1.1.3+meta", v.toString());
    v = new Version("ios-1.1.3-meta");
    assertEquals("1.1.3-meta", v.toString());
    v = new Version("ios-1.1.3");
    assertEquals("1.1.3", v.toString());
    v = new Version("1.1.3");
    assertEquals("1.1.3", v.toString());
  }

  @Test
  public void testVersionFromString() throws Exception {
    var cases =
        List.of(
            new Version("ios-0.1.0"),
            new Version("android-0.1.1"),
            new Version("0.2.0"),
            new Version("1.0.0-prerelease"),
            new Version("1.0.0"),
            new Version("1.0.1+ios"));
    for (int i = 0; i < cases.size(); i++) {
      var currentVersion = cases.get(i);
      assertTrue(currentVersion.isSameVersionAs(currentVersion));
      for (int j = 0; j < i; j++) {
        var olderVersion = cases.get(j);
        assertTrue(currentVersion.isLargerVersionThan(olderVersion));
      }
    }
    var releaseVersion = new Version("1.0.0");
    var metaInfoVersion = new Version("1.0.0+ios");
    assertTrue(releaseVersion.isSameVersionAs(metaInfoVersion));
    assertNotEquals(metaInfoVersion, releaseVersion);
    var sameIosVersion = new Version("1.0.0+ios");
    assertEquals(sameIosVersion, metaInfoVersion);
  }

  @Test
  public void testPlatform() throws Exception {
    var iosNonStandard = new Version("ios-1.0.0");
    var iosStandard = new Version("1.0.0+ios");
    assertTrue(iosNonStandard.isIOS());
    assertTrue(iosStandard.isIOS());
    assertFalse(iosNonStandard.isAndroid());
    assertFalse(iosStandard.isAndroid());

    var androidNonStandard = new Version("android-1.0.0");
    var androidStandard = new Version("1.0.0+android");
    assertFalse(androidNonStandard.isIOS());
    assertFalse(androidStandard.isIOS());
    assertTrue(androidNonStandard.isAndroid());
    assertTrue(androidStandard.isAndroid());

    var random = new Version("1.0.0");
    assertFalse(random.isAndroid());
    assertFalse(random.isIOS());
  }

  @Test
  public void testVersionFromExplicit() throws Exception {
    var cases =
        List.of(
            new Version(0, 1, 0),
            new Version(0, 1, 1),
            new Version(0, 2, 0),
            new Version(1, 0, 0, "prerelease", ""),
            new Version(1, 0, 0),
            new Version(1, 0, 1, "", "ios"));
    for (int i = 0; i < cases.size(); i++) {
      var currentVersion = cases.get(i);
      assertTrue(currentVersion.isSameVersionAs(currentVersion));
      for (int j = 0; j < i; j++) {
        var olderVersion = cases.get(j);
        assertTrue(currentVersion.isLargerVersionThan(olderVersion));
      }
    }
    var releaseVersion = new Version(1, 0, 0);
    var metaInfoVersion = new Version(1, 0, 0, "", "ios");
    assertTrue(releaseVersion.isSameVersionAs(metaInfoVersion));
    assertNotEquals(metaInfoVersion, releaseVersion);
    var sameIosVersion = new Version(1, 0, 0, "", "ios");
    assertEquals(sameIosVersion, metaInfoVersion);
  }

  @Test
  public void testMissingMinorOrPatch() throws Exception {
    var apiLevel = "29";
    var iosVersion = "13.6";
    var apiLevelWithMeta = "29+test";
    var iosVersionWithMeta = "13.6+test";
    var apiLevelVersion = new Version(apiLevel);
    assertTrue(
        apiLevelVersion.getMajor().equals(29)
            && apiLevelVersion.getMinor().equals(0)
            && apiLevelVersion.getPatch().equals(0));

    var iosVersionVersion = new Version(iosVersion);
    assertTrue(
        iosVersionVersion.getMajor().equals(13)
            && iosVersionVersion.getMinor().equals(6)
            && iosVersionVersion.getPatch().equals(0));

    var apiLevelWithMetaVersion = new Version(apiLevelWithMeta);
    assertTrue(
        apiLevelWithMetaVersion.getMajor().equals(29)
            && apiLevelWithMetaVersion.getMinor().equals(0)
            && apiLevelWithMetaVersion.getPatch().equals(0)
            && apiLevelWithMetaVersion.getMetaInfo().equals("test"));

    var iosVersionVersionMeta = new Version(iosVersionWithMeta);

    assertTrue(
        iosVersionVersionMeta.getMajor().equals(13)
            && iosVersionVersionMeta.getMinor().equals(6)
            && iosVersionVersionMeta.getPatch().equals(0)
            && iosVersionVersionMeta.getMetaInfo().equals("test"));
  }
}
