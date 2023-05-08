/*
 * --------------------------------------------------------------------
 * RecordAction:	VpsGenerateFormattedOutput
 * Object:			webpage_id__c
 * Author:			Amol Chinchalkar @ Veeva
 * Date:			2023-05-04
 *---------------------------------------------------------------------
 * Description:
 *---------------------------------------------------------------------
 * Copyright (c) 2023 Veeva Systems Inc.  All Rights Reserved.
 *      This code is based on pre-existing content developed and
 *      owned by Veeva Systems Inc. and may only be used in connection
 *      with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.custom.actions;

import com.veeva.vault.custom.classes.VpsUtilHelper;
import com.veeva.vault.sdk.api.action.RecordAction;
import com.veeva.vault.sdk.api.action.RecordActionContext;
import com.veeva.vault.sdk.api.action.RecordActionInfo;
import com.veeva.vault.sdk.api.action.Usage;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.queue.Message;
import com.veeva.vault.sdk.api.queue.PutMessageResponse;
import com.veeva.vault.sdk.api.queue.QueueService;

import java.util.List;

@RecordActionInfo(object = "webpage_id__c",
        label = "SDK: Generate Formatted Output",
        usages = Usage.LIFECYCLE_ENTRY_ACTION)
public class VpsGenerateFormattedOutput implements RecordAction {

    private static final String CONFIG_FO_ACTION_COMMUNICATION = "approved_state__c.download_wepbage_id_pdf_useraction__c";
    private static final String OBJFIELD_ID = "id";
    private static final String OBJFIELD_STATE = "state__v";
    private static final String QUEUE_NAME = "formatted_output_generation_queue__c";
    private static final String OBJECT_NAME = "webpage_id__c";


    public boolean isExecutable(RecordActionContext context) {
        return true;
    }

    /**
     * *
     *
     * @param context
     */
    public void execute(RecordActionContext context) {
        List<String> recordIdsToSave = VaultCollections.newList();
        for (Record record : context.getRecords()) {
            String recordId = record.getValue(OBJFIELD_ID, ValueType.STRING);
            String stateName = record.getValue(OBJFIELD_STATE, ValueType.STRING);
            recordIdsToSave.add(recordId);
        }

        if (!recordIdsToSave.isEmpty()) {
            queueMessage(CONFIG_FO_ACTION_COMMUNICATION, recordIdsToSave);
        }


    }

    /**
     * @param actionToRun
     * @param recordIds
     */
    private void queueMessage(String actionToRun, List<String> recordIds) {
        QueueService queueService = ServiceLocator.locate(QueueService.class);

        Message message = queueService.newMessage(QUEUE_NAME)
                .setAttribute("object", OBJECT_NAME)
                .setAttribute("actionToRun", actionToRun)
                .setMessageItems(recordIds);

        //The PutMessageResponse can be used to review if queuing was successful or not
        PutMessageResponse response = queueService.putMessage(message);
        if (response.getError() != null) {
            VpsUtilHelper utilHelper = new VpsUtilHelper();
            utilHelper.LogData("ERROR Queuing Failed: " + response.getError().getMessage());
        }
    }

}