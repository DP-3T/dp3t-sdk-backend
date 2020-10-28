# Semver

## Introduction
This implementation follows the official specification found for [Semver 2.0 ](https://semver.org/). 
The regular expression used for matching is taken from the official specification ans was slightly adjusted.
The following changes were made, to allow using extended SemVer:

- it is allowed to have a prefix specifying the OS used. The following regex is used
    > (?:(?<platform>ios|android)-)?

- Only the major version is required. If minor or patch version are not given, a value of `0` i assumed. This was added as a `?` in the original regex for the minor and patch version.

## IsAndroid/IsIos

To allow for simple OS testing the following two implementations are added:

```java
public boolean isAndroid() {
        return platform.contains("android") || metaInfo.contains("android");
}
public boolean isIOS() {
    return platform.contains("ios") || metaInfo.contains("ios");
}
```
Whereas in SemVer it would be normal to specify further information in the `metaInfo` field, the dp3t clients use the prefix `ios` or `android`. This implementation though should be compatible with a more SemVer approach.
