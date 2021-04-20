# Interoperability
## Federation Gateway Options
TODO: Describe startup options and what they do

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