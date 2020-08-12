# Insert-Manager

## Idea
The Insert-Manager was introduced to reduce logic in controllers. The idea is to provide a second abstraction layer next to the `DataServices` to provide for possible generic validation and normalization. The Insert-Manager holds a list of `InsertionFilter`, which provide some code, to either filter for invalid data or alter incoming data. Each filter can decide to either skip respective keys, or throw a `InsertException`. Throwing an exception aborts the current insert request, and throws to the controller. Inside the controller the exception can be mapped to a respective error message and http status code.

The current default only handles `KeyIsNotBase64Exception` and ignores all other exceptions (since there are none).

During construction, instances of `GAENDataService` and `ValidationUtils` are needed. Further, any filter can be added to the list with `addFilter(InsertionFilter filter)`. Ideally, this happens inside the [`WSBaseConfig`](../config/WSBaseConfig), where default filters are added right after constructing the `InsertManager`. To allow for conditional `InsertionFilters` refer to the following snippet:

```java
@ConditionalOnProperty(
value="ws.app.gaen.ioslegacy", 
havingValue = "true", 
matchIfMissing = true)
@Bean public IOSLegacyProblemRPLT144 iosLegacyProblemRPLT144(InsertManager manager){
    var iosFilter = new IOSLegacyProblemRPLT144();
    manager.addFilter(iosFilter);
    return iosFilter;
}
```

This looks for a property, either supplied via a `application.properties` file, or via `java` arguments (e.g. `java -D w.app.gaen.ioslegacy`) and constructs and inserts the respective filter bean into the filter chain. For further `SpringBoot` `Conditional` annotations have a look at ["Spring Boot Conditionals"](https://reflectoring.io/spring-boot-conditionals/)

Encapsulating the logic into smaller pieces of code, should allow for easier and better reviews of the respective filters. Further, for each filter an extensive documentation can be provided, without cluttering the code with too many comments. 

## InsertionFilter Interface
The `InsertionFilter` interface has the following signature:

```java
public List<GaenKey> filter(UTCInstant now, List<GaenKey> content, OSType osType, Version osVersion, Version appVersion, Object principal) throws InsertException;
```

It gets a `now` object representing _the time the request started_ from the controller , a list of keys, some OS and app related information taken from the `UserAgent` (c.f. `InsertManager@exctractOS` and following) and a possible principal object, representing a authenticated state (e.g. a `JWT`). The function is marked to throw a `InsertException` to stop the inserting process.

## InsertException

An `InsertException` can be thrown inside an implementation of the `InsertionFilter` interface to mark an insert as "unrecoverable" and abort it. Such "unrecoverable" states might be patterns in the uploaded model, which allows for packet sniffing and/or information gathering.

## Default Filters

Looking at the `WSBaseConfig`, we can see that during construction of the `InsertManager` bean, a set of default filters are added:

```java
@Bean
public InsertManager insertManager() {
    var manager = new InsertManager(gaenDataService(), gaenValidationUtils());
    manager.addFilter(new NoBase64Filter(gaenValidationUtils()));
    manager.addFilter(new KeysNotMatchingJWTFilter(gaenRequestValidator, gaenValidationUtils()));
    manager.addFilter(new RollingStartNumberAfterDayAfterTomorrow());
    manager.addFilter(new RollingStartNumberBeforeRetentionDay(gaenValidationUtils()));
    manager.addFilter(new FakeKeysFilter());
    manager.addFilter(new NegativeRollingPeriodFilter());
    return manager;
}
```

- `NoBase64Filter`
    > This filter validates that the key actually is a correctly encoded base64 string. Since we are using 16 bytes of key data, those can be represented with exactly 24 characters. The validation of the length is already done during model validation and is assumed to be correct when reaching the filter. This filter _throws_ a `KeyIsNotBase64Exception` if any of the keys is wrongly encoded. Every key submitted _MUST_ have correct base64 encoding
- `KeysNotMatchingJWTFilter`: 
    > 
- `RollingStartNumberAfterDayAfterTomorrow`: 
    > Representing the maximum allowed time skew. Any key which is further in the future as the day after tomorrow is considered to be _maliciously_ uploaded and is hence filtered out.
- `RollingStartNumberBeforeRetentionDay`: 
    > Any key which was valid earlier than `RetentionPeriod` is considered to be outdated and not saved in the database. The key would be removed during the next database clean anyways.
- `FakeKeysFilter`
    > Any key which has the `fake` flag is not inserted.
- `NegativeRollingPeriodFilter`: 
    > The `RollingPeriod` represents the 10 minutes interval of the key's validity. Negative numbers are not possible, hence any key having a negative rolling period is considered to be _maliciously_ uploaded.