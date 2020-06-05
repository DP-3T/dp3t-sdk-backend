/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.model;

import java.util.ArrayList;
import java.util.List;

public class BucketList {
    List<Long> buckets;
    public List<Long> getBuckets() {
        return buckets;
    }
    public void setBuckets(List<Long> buckets) {
        this.buckets = buckets;
    }
    public void addBucket(Long bucket) {
        if(this.buckets == null) {
            this.buckets = new ArrayList<Long>();
        }
        this.buckets.add(bucket);
    }
}