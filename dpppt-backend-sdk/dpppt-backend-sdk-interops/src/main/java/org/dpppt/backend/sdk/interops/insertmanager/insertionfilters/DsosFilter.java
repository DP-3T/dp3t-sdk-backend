package org.dpppt.backend.sdk.interops.insertmanager.insertionfilters;

import java.util.List;
import java.util.stream.Collectors;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.utils.UTCInstant;

/**
 * Filter keys that have a day since onset of symptoms (dsos) not relevant for our epidemiological
 * parameters
 */
public class DsosFilter implements InteropsKeyInsertionFilter {

  @Override
  public List<GaenKeyForInterops> filter(UTCInstant now, List<GaenKeyForInterops> content) {
    return content.stream().filter(key -> !hasIrrelevantDsos(key)).collect(Collectors.toList());
  }

  private boolean hasIrrelevantDsos(GaenKeyForInterops key) {
    Integer dsos = key.getDaysSinceOnsetOfSymptoms();
    if (dsos == null) { // keys with dsos null values are dropped
      return true;
    }
    int normalizedDsos = dsos;
    if (dsos < 20) { // onset known. dsos in [-14, +14]
      // dsos is already normalized
    } else if (dsos < 1986) { // onset range `n` days (n < 19). dsos in [n*100-14, n*100+14].
      final int nMax = 19;
      final int n = dsos + nMax / 100;
      int endOfRange = n * 100;
      int startOfRange = endOfRange - n;
      normalizedDsos = dsos - startOfRange;
    } else if (dsos < 2986) { // unknown onset. dsos in [1986, 2014]
      normalizedDsos = dsos - 2000;
    } else if (dsos < 3986) { // asymptomatic. dsos in [2986, 3014]
      normalizedDsos = dsos - 3000;
    } else { // unknown symptom status. dsos in [3986, 4014]
      normalizedDsos = dsos - 4000;
    }
    return normalizedDsos < -2;
  }

  @Override
  public String getName() {
    return "DSOS";
  }
}
