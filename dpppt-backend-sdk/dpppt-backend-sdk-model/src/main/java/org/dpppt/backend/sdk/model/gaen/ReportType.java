package org.dpppt.backend.sdk.model.gaen;

import org.dpppt.backend.sdk.model.interops.proto.EfgsProto;

public enum ReportType {
  UNKNOWN,
  CONFIRMED_TEST,
  CONFIRMED_CLINICAL_DIAGNOSIS,
  SELF_REPORT,
  RECURSIVE,
  REVOKED;

  public EfgsProto.ReportType toEfgsProtoReportType() {
    switch (this) {
      case UNKNOWN:
        return EfgsProto.ReportType.UNKNOWN;
      case CONFIRMED_TEST:
        return EfgsProto.ReportType.CONFIRMED_TEST;
      case CONFIRMED_CLINICAL_DIAGNOSIS:
        return EfgsProto.ReportType.CONFIRMED_CLINICAL_DIAGNOSIS;
      case SELF_REPORT:
        return EfgsProto.ReportType.SELF_REPORT;
      case RECURSIVE:
        return EfgsProto.ReportType.RECURSIVE;
      case REVOKED:
        return EfgsProto.ReportType.REVOKED;
      default:
        throw new RuntimeException("no efgs proto report type mapping for: " + this);
    }
  }
}
