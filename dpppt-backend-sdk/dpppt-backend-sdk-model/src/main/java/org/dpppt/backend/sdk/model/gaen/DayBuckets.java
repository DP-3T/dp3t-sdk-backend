package org.dpppt.backend.sdk.model.gaen;

import ch.ubique.openapi.docannotations.Documentation;

import java.util.List;

@Documentation(description = "DayBuckets holds a list of all available release buckets of a given day.\n" +
        "The release buckets are stored as relative URLs to the base URL (currently /v1/gaen)\n" +
        "and can be used to get the exposed keys starting from this release bucket.")
public class DayBuckets {

    @Documentation(description = "The day of all buckets, as midnight in milliseconds since the Unix epoch (1970-01-01)",
        example = "1593043200000")
	private Long dayTimestamp;
    @Documentation(description = "The day as given by the request in /v1/gaen/buckets/{dayDateStr}",
        example = "2020-06-27")
    private String day;
    @Documentation(description = "Relative URLs for the available release buckets",
        example = "['/exposed/1593043200000', '/exposed/1593046800000'")
    private List<String> relativeUrls;

    public String getDay() {
        return this.day;
    }

    public List<String> getRelativeUrls() {
        return this.relativeUrls;
    }
    
    public Long getDayTimestamp() {
		return dayTimestamp;
	}

    public DayBuckets setDay(String day) {
        this.day = day;
        return this;
    }

    public DayBuckets setRelativeUrls(List<String> relativeUrls) {
        this.relativeUrls = relativeUrls;
        return this;
    }
    
    public DayBuckets setDayTimestamp(Long dayTimestamp) {
    	this.dayTimestamp = dayTimestamp;
    	return this;
    }
    
}