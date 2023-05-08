/*
 * --------------------------------------------------------------------
 * UDC:			VpsDocumentTemplateResponse
 * Author:		paulkwitkin @ Veeva
 * Date:		2019-12-20
 *---------------------------------------------------------------------
 * Description:	Object to hold data for a document template API call response
 *---------------------------------------------------------------------
 * Copyright (c) 2019 Veeva Systems Inc.  All Rights Reserved.
 *		This code is based on pre-existing content developed and
 * 		owned by Veeva Systems Inc. and may only be used in connection
 *		with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.custom.classes.api;

import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.data.*;
import com.veeva.vault.sdk.api.json.JsonArray;
import com.veeva.vault.sdk.api.json.JsonObject;

@UserDefinedClassInfo()
public class VpsDocumentTemplateResponse extends VpsAPIResponse {

	private static final String APIFIELD_DATA = "data";

	public VpsDocumentTemplateResponse(JsonObject jsonResponse) {
		super(jsonResponse);
	}
	public VpsDocumentTemplateResponse(String jsonResponse) {
		super(jsonResponse);
	}

	public JsonArray getData() {
		return getArray(APIFIELD_DATA);
	}

}
