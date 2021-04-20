# Interoperability
## Federation Gateway Options
As for the `dpppt-backend-sdk-ws` different configuration classes provide the option to parameterize the runtime behavior of the interoperability service. 

Most of the properties found in the `WSBaseConfig` are similar to the ones found in the `WSBaseConfig` of the `dpppt-backend-sdk-ws`. Hence we only go into details for interoperability specific options. In contrast to the `dpppt-backend-sdk-ws` the interoperability service also uses configuration beans (c.f. `HubConfigs` and `EfgsGatewayConfig`).

### WSBaseConfig
- `interops.origin.country`: This property holds the [ISO-3166-1-Alpha-2](https://en.wikipedia.org/wiki/List_of_ISO_3166_country_codes) identifier for the country in which the service runs. This property will be added to keys which were uploaded via the `dpppt-backend-sdk-ws` and hence did not come from the federation gateway. *DEFAULT: null* - since this property needs to be specified by the country starting the service, it should fail if it was forgotten (not specified).

- `interops.efgs.dsosfilter.enabled`: Flag indicating if the `days since onset of symptoms filters` (DsosFilter) should be enabled or not. See the section about `Days Since Onset of Symptoms` for further information on the encoding and purpose of `dsos`. If enabled the filter will be injected into the filter pipeline and keys not matching the following criteria are filtered out. *DEFAULT: true*

- `interops.efgs.dsosfilter.symptomaticOnsetKnown.dropDaysBeforeOnset`: Drop keys which are older than `onset - n` and the onset of symptoms is known. Negative values reference to days *before* onset. *DEFAULT: -2*

- `interops.efgs.dsosfilter.symptomaticOnsetRange.dropDaysBeforeRangeStart`: Drop keys which are older than `onset - range - n`, where range specifies the range of days where the symptoms could have started. Negative values reference to days *before* onset. *DEFAULT: -2*

- `interops.efgs.dsosfilter.symptomaticOnsetKnown.dropDaysBeforeSubmission`: Drop keys keys which are older than `submission - n` where submission indicates the submission date of the keys, and the onset date is not known at all. Negative values reference to days *before* onset. *DEFAULT: -2*

- `interops.efgs.dsosfilter.asympomatic.dropDaysBeforeSubmission`: Drop keys which are older than `submission - n`, where submission indicates the submission date of the keys, and the person uploading the keys was asymptomatic. Negative values reference to days *before* onset. *DEFAULT: -2*

- `interops.efgs.dsosfilter.unknownSymptomStatus.dropDaysBeforeSubmission`: Drop keys which are older than `submission - n`, where submission indicates the submission date of the keys, and it is not known if the person uploading the keys was symptomatic or asymptomatic. Negative values reference to days *before* onset. *DEFAULT: -2*
### Configuration Beans

## Federation Gateway Synchronization
TODO: Describe architecture of sync service
## Days Since Onset of Symptoms

Since the EN framework does not provide a way to encode meta information about the infection of someone who uploaded the key. This though can be crucial (e.g.symptomatic vs asymptomatic, or fixed onset vs ranged onset) in certain situations to estimate the risk factor on the client.

In order to solve this flaw of the EN framework, the EFGS introduced an encoding of such meta information into the `days_since_onset_of_symptoms` field of the EN struct. The following is an overview of how the encoding works, and explained in context of the `EfgsDsosFilter` class used in the Swiss instance.

The encoding uses mutually exclusive ranges over the whole domain of possible values ( the `days_since_onset_of_symptoms` field is a signed 32-bit integer) to encode different ways of explaining the circumstances of the onset of symptoms and the date. 
//TODO: Fix reference
[EFGS](LINK)
### Symptomatic with known onset
`Range: [-14,+14]`

`0: Onset coincides with today's key date`

User knows exactly the Date of onset of Symptoms (e.g. cough started 5 days ago before submission)
### Symptomatic with onset range of `n` days
`Range: [n*100-14,n*100+14]`

`n*100: Onset coincides with yesterday's key date`

User knows just a time range. (e.g. cough started within the last week)

### Symptomatic with unknown onset (days since submission)
`Range: [2000-14,2000+14] = [1986, 2014]`

`2000: The onset coincides with this keys' date`

User knows that he had symptoms, but didn't know exactly when they started. 

### Asymptomatic (days since submission)
`Range: [3000-14,3000+14] = [2986, 3014]`

`3000: The onset coincides with this keys' date`

User knows that he had definitely nothing (e.g. never had cough, fever etc.)

### Unknown symptom status (days since submission)
`Range: [4000-14,4000+14] = [3986, 4014]`

`4000: The onset coincides with this keys' date`

User does not know which Symptoms are Covid-symptoms or he did something wrong in the app during the submission.
## References
- [European Federation Gateway Service](https://github.com/eu-federation-gateway-service/efgs-federation-gateway)
- [Swiss Federation Gateway Service](https://github.com/admin-ch/chgs-federation-gateway)