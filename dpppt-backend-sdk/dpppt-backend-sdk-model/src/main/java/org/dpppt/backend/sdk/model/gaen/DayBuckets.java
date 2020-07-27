package org.dpppt.backend.sdk.model.gaen;

import java.util.List;

public class DayBuckets {
	
	private Long dayTimestamp;
    private String day;
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

    public DayBuckets day(String day) {
        this.day = day;
        return this;
    }

    public DayBuckets relativeUrls(List<String> relativeUrls) {
        this.relativeUrls = relativeUrls;
        return this;
    }
    
    public DayBuckets dayTimestamp(Long dayTimestamp) {
    	this.dayTimestamp = dayTimestamp;
    	return this;
    }
    
}