/*
 * --------------------------------------------------------------------
 * UDC:         VpsBaseHelper
 * Author:      markarnold @ Veeva
 * Date:        2019-07-25
 *---------------------------------------------------------------------
 * Description:
 *---------------------------------------------------------------------
 * Copyright (c) 2019 Veeva Systems Inc.  All Rights Reserved.
 *      This code is based on pre-existing content developed and
 *      owned by Veeva Systems Inc. and may only be used in connection
 *      with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.custom.classes;

import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.query.QueryService;

import java.util.List;

@UserDefinedClassInfo()
public class VpsBaseHelper {

	private List<String> errorList = VaultCollections.newList();
	private LogService logService = ServiceLocator.locate(LogService.class);
	private static final String USER_QUERY = "select id from user__sys where id = '%s' limit 0";

	public VpsBaseHelper() {
		super();
	}

	public List<String> getErrorList() {
		return errorList;
	}

	/**
	 * Internal method to get a log service. If a service has already been created
	 * it returns the existing service.
	 * @return LogService
	 */
	protected LogService getLogService() {
		//initialize the service on the first call
		if (logService == null) {
			logService = ServiceLocator.locate(LogService.class);
		}
		return logService;
	}

	/**
	 * Method to simulate a delay. Queries the current user.
	 */
	protected void sleep() {
		try {
			QueryService queryService = ServiceLocator.locate(QueryService.class);
			queryService.query(String.format(USER_QUERY, RequestContext.get().getCurrentUserId()));
		}
		catch (VaultRuntimeException exception) {
			getLogService().error("VpsRecordHelper.sleep - {}", exception.getMessage());
		}
	}
}
