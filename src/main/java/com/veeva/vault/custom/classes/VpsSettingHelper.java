/*
 * --------------------------------------------------------------------
 * UDC:         vps_setting_helper__c
 * Author:      markarnold @ Veeva
 * Date:        2019-07-25
 *--------------------------------------------------------------------
 * Description: Map of Setting Records
 *--------------------------------------------------------------------
 * Copyright (c) 2019 Veeva Systems Inc.  All Rights Reserved.
 *      This code is based on pre-existing content developed and
 *      owned by Veeva Systems Inc. and may only be used in connection
 *      with the deliverable with which it was provided to Customer.
 *--------------------------------------------------------------------
 */
package com.veeva.vault.custom.classes;

import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.query.QueryResponse;

import java.util.Map;

@UserDefinedClassInfo()
public class VpsSettingHelper extends VpsBaseHelper {

	public static final String OBJECT_VPS_SETTING = "vps_setting__c";
	public static final String OBJFIELD_EXTERNAL_ID = "external_id__c";
	public static final String OBJFIELD_ID = "id";
	public static final String OBJFIELD_ITEM_DELIMITER = "item_delimiter__c";
	public static final String OBJFIELD_KEY_VALUE_DELIMITER = "key_value_delimiter__c";
	public static final String OBJFIELD_SETTING_DELIMITER = "setting_delimiter__c";
	public static final String OBJFIELD_STATUS = "status__v";
	public static final String OBJFIELD_VALUE = "value__c";
	public static final String STATUS_ACTIVE = "active__v";

	Map<String, VpsSettingRecord> settingRecordMap;

	/**
	 * Helper class to load and retrieve settings records
	 */
	public VpsSettingHelper() {
		super();
		getLogService().info("VpsSettingHelper - Initialize");
		settingRecordMap = VaultCollections.newMap();
		loadData("",false);
	}

	/**
	 * Helper class to load and retrieve settings records
	 *
	 * @param externalIdFilter external id of the setting record(s) to find
	 * @param useWildCard when true, any records that start with externalIdFilter will be loaded
	 */
	public VpsSettingHelper(String externalIdFilter, Boolean useWildCard) {
		super();
		getLogService().info("VpsSettingHelper - Initialize");
		settingRecordMap = VaultCollections.newMap();
		loadData(externalIdFilter,useWildCard);
	}

	/**
	 * setting records that have been loaded
	 *
	 * @return map of setting records
	 */
	public Map<String, VpsSettingRecord> items() {return  settingRecordMap;}

	/**
	 * Loads settings records into a map by querying Vault
	 *
	 * @param externalIdFilter external id of the setting record(s) to find
	 * @param useWildCard when true, any records that start with externalIdFilter will be loaded
	 */
	private void loadData(String externalIdFilter, Boolean useWildCard) {
		getLogService().info("VpsSettingHelper.loadData for {}; useWildCard = {}", externalIdFilter, useWildCard);

		try {
			settingRecordMap.clear();

			//query Vault for all settings
			VpsVQLHelper vqlHelper = new VpsVQLHelper();
			vqlHelper.appendVQL("select " + OBJFIELD_ID);
			vqlHelper.appendVQL("," + OBJFIELD_EXTERNAL_ID);
			vqlHelper.appendVQL("," + OBJFIELD_KEY_VALUE_DELIMITER);
			vqlHelper.appendVQL("," + OBJFIELD_ITEM_DELIMITER);
			vqlHelper.appendVQL("," + OBJFIELD_SETTING_DELIMITER);
			vqlHelper.appendVQL("," + OBJFIELD_VALUE);
			vqlHelper.appendVQL(" from " + OBJECT_VPS_SETTING);
			vqlHelper.appendVQL(" where " + OBJFIELD_STATUS +  " = '" + STATUS_ACTIVE + "' ");

			if ((externalIdFilter != null) && (externalIdFilter.length() > 0)) {
				vqlHelper.appendVQL(" and " + OBJFIELD_EXTERNAL_ID + " like '");
				vqlHelper.appendVQL(externalIdFilter);
				if (useWildCard) {
					vqlHelper.appendVQL("%");
				}
				vqlHelper.appendVQL("'");
			}

			QueryResponse queryResponse = vqlHelper.runVQL();
			queryResponse.streamResults().forEach(queryResult -> {
				String externalId = queryResult.getValue(OBJFIELD_EXTERNAL_ID, ValueType.STRING);
				String value = queryResult.getValue(OBJFIELD_VALUE, ValueType.STRING);

				VpsSettingRecord settingRecord = new VpsSettingRecord();
				settingRecord.setExternalId(externalId);
				settingRecord.setItemDelimiter(queryResult.getValue(OBJFIELD_ITEM_DELIMITER, ValueType.STRING));
				settingRecord.setKeyDelimiter(queryResult.getValue(OBJFIELD_KEY_VALUE_DELIMITER, ValueType.STRING));
				settingRecord.setSettingDelimiter(queryResult.getValue(OBJFIELD_SETTING_DELIMITER, ValueType.STRING));

				if (value != null) {
					String[] pairs = StringUtils.split(value,settingRecord.getSettingDelimiter());
					for (int i=0;i<pairs.length;i++) {
						String pair = pairs[i];
						String[] keyValue = StringUtils.split(pair,settingRecord.getKeyDelimiter());

						String settingKey = keyValue[0];
						if (keyValue.length > 1) {
							String settingValue = keyValue[1];
							settingRecord.setValue(settingKey, settingValue);
						}
					}
				}
				settingRecordMap.put(externalId, settingRecord);
			});
		}
		catch (VaultRuntimeException exception) {
			getLogService().error("VpsSettingHelper.loadData - {}", exception.getMessage());
			throw exception;
		}
	}
}