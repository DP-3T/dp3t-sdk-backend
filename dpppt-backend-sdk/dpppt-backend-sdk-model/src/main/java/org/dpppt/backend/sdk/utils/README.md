# UTC Instant

## Why a new class?

During the development of this project, various different time formats came into play. Mostly the 10 minute intervals used by GAEN and the default milliseconds used by the dp3t API. But time handling is hard. So `OffsetDateTimes` at UTC offsets were used, and sometimes `Dates` were needed, and some classes wanted different Objects and and and....

We ended up with a mess of 300 character lines, converting from one object into the other. For this reason we added this class, which gathers all usages and conversions we needed during development. It also should ensure that different people write the same code, by providing _one thing who times 'em all_.

This also allowed us to the name the functions in a concise and natural way. The first example reads more natural than the second one.

```java
if(UTCInstant.of(keyDate, GaenUnit.TenMinutes).isBeforeDateOf(UTCInstant.now().atStartOfDay().plusDays(2)) {

}
```

```java
if(Instant.ofEpochMilli(Duration.of(10, GaenUnit.TenMinutes).toMillis()).isBefore(LocalDate.now().atStartOfDay().plusDays(2).toInstant(ZoneOffset.UTC))) {
    
}
```