package org.dpppt.backend.sdk.model.gaen;

import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class File {
    @NotNull
    @NotEmpty
    List<GaenKey> gaenKeys;
}