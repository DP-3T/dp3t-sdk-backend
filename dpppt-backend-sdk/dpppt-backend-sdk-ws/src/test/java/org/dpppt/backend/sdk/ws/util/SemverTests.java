package org.dpppt.backend.sdk.ws.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.dpppt.backend.sdk.semver.Version;
import org.junit.Test;

public class SemverTests {
    @Test
    public void testVersionFromString() throws Exception {
        var cases = List.of(new Version("ios-0.1.0"),new Version("android-0.1.1"),new Version("0.2.0"),new Version("1.0.0-prerelease"),new Version("1.0.0"),new Version("1.0.1+ios"));
        for(int i =0; i < cases.size(); i++) {
            var currentVersion = cases.get(i);
            assertTrue(currentVersion.isSameVersionAs(currentVersion));
            for(int j = 0; j < i; j++ ){
                var olderVersion = cases.get(j);
                assertTrue(currentVersion.isLargerVersionThan(olderVersion));
            }
        }
        var releaseVersion = new Version("1.0.0");
        var metaInfoVersion = new Version("1.0.0+ios");
        assertTrue(releaseVersion.isSameVersionAs(metaInfoVersion));
        assertFalse(releaseVersion.equals(metaInfoVersion));
        var sameIosVersion = new Version("1.0.0+ios");
        assertTrue( metaInfoVersion.equals(sameIosVersion));
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
        assertFalse( random.isAndroid());
        assertFalse( random.isIOS());
    }
    @Test
    public void testVersionFromExplicit() throws Exception {
        var cases = List.of(new Version(0,1,0),new Version(0,1,1),new Version(0,2,0),new Version(1,0,0,"prerelease", ""),new Version(1,0,0),new Version(1,0,1,"", "ios"));
        for(int i =0; i < cases.size(); i++) {
            var currentVersion = cases.get(i);
            assertTrue(currentVersion.isSameVersionAs(currentVersion));
            for(int j = 0; j < i; j++ ){
                var olderVersion = cases.get(j);
                assertTrue(currentVersion.isLargerVersionThan(olderVersion));
            }
        }
        var releaseVersion = new Version(1,0,0);
        var metaInfoVersion = new Version(1,0,0,"", "ios");
        assertTrue(releaseVersion.isSameVersionAs(metaInfoVersion));
        assertFalse(releaseVersion.equals(metaInfoVersion));
        var sameIosVersion = new Version(1,0,0,"", "ios");
        assertTrue( metaInfoVersion.equals(sameIosVersion));
    }
    @Test
    public void testMissingMinorOrPatch() throws Exception {
        var apiLevel = "29";
        var iosVersion = "13.6";
        var apiLevelWithMeta = "29+test";
        var iosVersionWithMeta = "13.6+test";
        var apiLevelVersion = new Version(apiLevel);
        assertTrue(apiLevelVersion.getMajor() == 29 
                && apiLevelVersion.getMinor() == 0
                && apiLevelVersion.getPatch() == 0);

        var iosVersionVersion = new Version(iosVersion);
        assertTrue(iosVersionVersion.getMajor() == 13 
        && iosVersionVersion.getMinor() == 6
        && iosVersionVersion.getPatch() == 0);

        var apiLevelWithMetaVersion = new Version("29+test");
        assertTrue(apiLevelWithMetaVersion.getMajor() == 29 
        && apiLevelWithMetaVersion.getMinor() == 0
        && apiLevelWithMetaVersion.getPatch() == 0
        && apiLevelWithMetaVersion.getMetaInfo() == "test");

        var iosVersionVersionMeta = new Version(iosVersionWithMeta);
        
        assertTrue(iosVersionVersionMeta.getMajor() == 13 
        && iosVersionVersionMeta.getMinor() == 6
        && iosVersionVersionMeta.getPatch() == 0
        && iosVersionVersionMeta.getMetaInfo() == "test");

    }
}