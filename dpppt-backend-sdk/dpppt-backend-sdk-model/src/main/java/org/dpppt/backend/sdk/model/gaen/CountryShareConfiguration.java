package org.dpppt.backend.sdk.model.gaen;

import ch.ubique.openapi.docannotations.Documentation;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

public class CountryShareConfiguration {

  @Valid
  @Size(max = 2, min = 2)
  @Documentation(description = "A iso-3166-1 alpha-2 country code", example = "\"CH\"")
  private String countryCode;

  @Valid
  @Min(value = 0)
  @Max(value = 1)
  @Documentation(description = "Share key with country: 0 -> no share, 1 -> share", example = "1")
  private int shareKeyWithCountry = 0;

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public int getShareKeyWithCountry() {
    return shareKeyWithCountry;
  }

  public void setShareKeyWithCountry(int shareKeyWithCountry) {
    this.shareKeyWithCountry = shareKeyWithCountry;
  }
}
