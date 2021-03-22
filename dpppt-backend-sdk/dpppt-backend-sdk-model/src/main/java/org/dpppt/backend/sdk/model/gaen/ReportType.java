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

  public static ReportType fromEfgsProtoReportType(EfgsProto.ReportType protoReportType) {
    switch (protoReportType) {
      case UNKNOWN:
        return UNKNOWN;
      case CONFIRMED_TEST:
        return CONFIRMED_TEST;
      case CONFIRMED_CLINICAL_DIAGNOSIS:
        return CONFIRMED_CLINICAL_DIAGNOSIS;
      case SELF_REPORT:
        return SELF_REPORT;
      case RECURSIVE:
        return RECURSIVE;
      case REVOKED:
        return REVOKED;
      default:
        throw new RuntimeException("no report type mapping for: " + protoReportType);
    }
  }
}
