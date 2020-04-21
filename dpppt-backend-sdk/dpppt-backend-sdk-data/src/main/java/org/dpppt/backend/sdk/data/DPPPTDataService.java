/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.data;

import java.util.List;

import org.dpppt.backend.sdk.model.Exposee;
import org.joda.time.DateTime;

public interface DPPPTDataService {

	/**
	 * Upserts the given exposee
	 * 
	 * @param exposee
	 * @param appSource
	 */
	void upsertExposee(Exposee exposee, String appSource);

	/**
	 * returns all exposees for the given day [day: 00:00, day+1: 00:00)
	 * 
	 * @param day
	 * @return
	 */
	List<Exposee> getSortedExposedForDay(DateTime day);

	/**
	 * Returns the maximum id of the stored exposed entries for the given day date
	 * 
	 * @param day
	 * 
	 * @return
	 */
	Integer getMaxExposedIdForDay(DateTime day);

	/**
	 * Checks and inserts a publish uuid.
	 * 
	 * @param uuid
	 * @return return true if the uuid has been inserted. if the uuid is not valid,
	 *         returns false.
	 */
	boolean checkAndInsertPublishUUID(String uuid);

}
