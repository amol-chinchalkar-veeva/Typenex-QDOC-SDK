/*
 * --------------------------------------------------------------------
 * RecordTrigger:	VpsGenerateFomattedOutput
 * Object:			webpadeid__c
 * Author:			Amol Chinchalkar @ Veeva
 * Date:			2023-03-10
 *---------------------------------------------------------------------
 * Description:
 *---------------------------------------------------------------------
 * Copyright (c) 2023 Veeva Systems Inc.  All Rights Reserved.
 *		This code is based on pre-existing content developed and
 *		owned by Veeva Systems Inc. and may only be used in connection
 *		with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.custom.triggers;

import com.veeva.vault.custom.classes.VpsUtilHelper;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.*;
import com.veeva.vault.sdk.api.queue.Message;
import com.veeva.vault.sdk.api.queue.PutMessageResponse;
import com.veeva.vault.sdk.api.queue.QueueService;

import java.util.List;

@RecordTriggerInfo(object = "webpage_id__c",
        events = {RecordEvent.AFTER_UPDATE})
public class VpsGenerateFomattedOutput implements RecordTrigger {
    private static final String CONFIG_FO_ACTION_COMMUNICATION = "approved_state__c.download_wepbage_id_pdf_useraction__c";
    private static final String OBJFIELD_ID = "id";
    private static final String OBJFIELD_STATE = "state__v";
    private static final String QUEUE_NAME = "formatted_output_generation_queue__c";
    private static final String OBJECT_NAME = "webpage_id__c";
    private static final String APPROVED_STATE = "approved_state__c";

    public void execute(RecordTriggerContext context) {
        List<String> recordIdsToSave = VaultCollections.newList();
        for (RecordChange inputRecordChange : context.getRecordChanges()) {
            String recordId = inputRecordChange.getNew().getValue(OBJFIELD_ID, ValueType.STRING);
            String stateName = inputRecordChange.getNew().getValue(OBJFIELD_STATE, ValueType.STRING);
            recordIdsToSave.add(recordId);
            //process only in approved state
            if (context.getRecordEvent().toString().equals((RecordEvent.AFTER_UPDATE).toString()) && stateName.equals(APPROVED_STATE)) {
                if (!recordIdsToSave.isEmpty()) {
                    queueMessage(CONFIG_FO_ACTION_COMMUNICATION, recordIdsToSave);
                }
            }
        }
    }

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
