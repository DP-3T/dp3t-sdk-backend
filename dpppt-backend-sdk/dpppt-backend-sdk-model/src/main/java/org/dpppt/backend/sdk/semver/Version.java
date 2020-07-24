package org.dpppt.backend.sdk.semver;

import java.util.Objects;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {
    private Integer major;
    private Integer minor;
    private Integer patch;
    private String preReleaseString = "";
    private String metaInfo = "";
    private String platform = "";

    private final Pattern semVerPattern = Pattern.compile("^(?:(?<platform>ios|android)-)?(?<major>0|[1-9]\\d*)\\.(?<minor>0|[1-9]\\d*)\\.(?<patch>0|[1-9]\\d*)(?:-(?<prerelease>(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+(?<buildmetadata>[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    public Version() {
    }

    public Version(String versionString) {
        if(versionString == null) {
            this.setInvalidValue();
            return;
        }

        var matches = semVerPattern.matcher(versionString.trim());
        if(matches.find()) {
            this.major = Integer.parseInt(matches.group("major"));
            this.minor = Integer.parseInt(matches.group("minor"));
            this.patch = Integer.parseInt(matches.group("patch"));
            if(matches.group("platform") != null) {
                this.platform = matches.group("platform");
            }
            if(matches.group("prerelease") != null) {
                this.preReleaseString = matches.group("prerelease");
            }
            if(matches.group("buildmetadata") != null) {
                this.metaInfo = matches.group("buildmetadata");
            }
        } else {
           this.setInvalidValue();
        }
    }

    public Version(Integer major, Integer minor, Integer patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preReleaseString = "";
        this.metaInfo = "";
    }
    public Version(Integer major, Integer minor) {
        this.major = major;
        this.minor = minor;
        this.patch = 0;
        this.preReleaseString = "";
        this.metaInfo = "";
    }
    public Version(Integer major) {
        this.major = major;
        this.minor = 0;
        this.patch = 0;
        this.preReleaseString = "";
        this.metaInfo = "";
    }

    public Version(Integer major, Integer minor, Integer patch, String preReleaseString, String metaInfo) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preReleaseString = preReleaseString;
        this.metaInfo = metaInfo;
    }

    private void setInvalidValue() {
        this.major = -1;
        this.minor = -1;
        this.patch = -1;
        this.preReleaseString = "";
        this.metaInfo = "";
    }
    public boolean isValid() {
        return major.compareTo(Integer.valueOf(0)) >= 0
            && minor.compareTo(Integer.valueOf(0)) >= 0
            && patch.compareTo(Integer.valueOf(0)) >= 0;
    }

    public Integer getMajor() {
        return this.major;
    }

    public void setMajor(Integer major) {
        this.major = major;
    }

    public Integer getMinor() {
        return this.minor;
    }

    public void setMinor(Integer minor) {
        this.minor = minor;
    }

    public Integer getPatch() {
        return this.patch;
    }

    public void setPatch(Integer patch) {
        this.patch = patch;
    }

    public String getPreReleaseString() {
        return this.preReleaseString;
    }

    public void setPreReleaseString(String preReleaseString) {
        this.preReleaseString = preReleaseString;
    }

    public String getMetaInfo() {
        return this.metaInfo;
    }

    public void setMetaInfo(String metaInfo) {
        this.metaInfo = metaInfo;
    }
    public String getPlatform() {
        return this.platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Version major(Integer major) {
        this.major = major;
        return this;
    }

    public Version minor(Integer minor) {
        this.minor = minor;
        return this;
    }

    public Version patch(Integer patch) {
        this.patch = patch;
        return this;
    }

    public Version preReleaseString(String preReleaseString) {
        this.preReleaseString = preReleaseString;
        return this;
    }

    public Version metaInfo(String metaInfo) {
        this.metaInfo = metaInfo;
        return this;
    }

    public boolean isPrerelease() {
        return !preReleaseString.isEmpty();
    }

    public boolean isAndroid() {
        return platform.contains("android") || metaInfo.contains("android");
    }
    public boolean isIOS() {
        return platform.contains("ios") || metaInfo.contains("ios");
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Version)) {
            return false;
        }
        Version version = (Version) o;
        return Objects.equals(major, version.major) && Objects.equals(minor, version.minor) && Objects.equals(patch, version.patch) && Objects.equals(preReleaseString, version.preReleaseString) && Objects.equals(metaInfo, version.metaInfo) && Objects.equals(platform, version.platform);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, preReleaseString, metaInfo);
    }

    @Override
    public String toString() {
        return "{" +
            " major='" + getMajor() + "'" +
            ", minor='" + getMinor() + "'" +
            ", patch='" + getPatch() + "'" +
            ", preReleaseString='" + getPreReleaseString() + "'" +
            ", metaInfo='" + getMetaInfo() + "'" +
            "}";
    }

    @Override
    public int compareTo(Version o) {
        if(this.major.compareTo(o.major) != 0) {
            return this.major.compareTo(o.major);
        }
        if(this.minor.compareTo(o.minor) != 0) {
            return this.minor.compareTo(o.minor);
        }
        if(this.patch.compareTo(o.patch) != 0) {
            return this.patch.compareTo(o.patch);
        }
        if(this.isPrerelease() && o.isPrerelease()) {
            if(this.preReleaseString.compareTo(o.preReleaseString) != 0) {
                return this.preReleaseString.compareTo(o.preReleaseString);
            }
        } else if(this.isPrerelease() && !o.isPrerelease()) {
            return -1;
        } else if(!this.isPrerelease() && o.isPrerelease()) {
            return 1;
        }
        return 0;
    }

    public boolean isSmallerVersionThan(Version other) {
        return this.compareTo(other) < 0;
    }
    public boolean isLargerVersionThan(Version other) {
        return this.compareTo(other) > 0;
    }
    public boolean isSameVersionAs(Version other) {
        return this.compareTo(other) == 0;
    }

}