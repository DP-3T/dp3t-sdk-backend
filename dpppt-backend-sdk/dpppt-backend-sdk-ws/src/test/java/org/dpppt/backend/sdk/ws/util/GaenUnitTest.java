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

import static org.junit.Assert.assertEquals;

import java.time.Duration;

import org.junit.Test;

public class GaenUnitTest {
    @Test
    public void testTenMinutesAre10Minutes() throws Exception {
        var tenMinutes = Duration.of(1,GaenUnit.TenMinutes);
        var t10Minutes = Duration.ofMinutes(10);
        assertEquals(tenMinutes, t10Minutes);
    }
} 