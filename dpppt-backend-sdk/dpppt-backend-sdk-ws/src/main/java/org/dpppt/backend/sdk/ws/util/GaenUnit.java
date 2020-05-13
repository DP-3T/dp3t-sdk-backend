/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.backend.sdk.ws.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;

public class GaenUnit implements TemporalUnit {
    public static GaenUnit TenMinutes = new GaenUnit();

    @Override
    public Duration getDuration() {
       
        return Duration.ofMinutes(10);
    }

    @Override
    public boolean isDurationEstimated() {
        return false;
    }

    @Override
    public boolean isDateBased() {
        return false;
    }

    @Override
    public boolean isTimeBased() {
        return true;
    }

    @Override
    public <R extends Temporal> R addTo(R temporal, long amount) {
        return (R)temporal.plus(this.getDuration().multipliedBy(amount));
    }

    @Override
    public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
        var between = ChronoUnit.MINUTES.between(temporal1Inclusive,temporal2Exclusive) / 10;
        return between;
    }
    
}