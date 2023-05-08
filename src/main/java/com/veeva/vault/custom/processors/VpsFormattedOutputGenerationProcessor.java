/*
 * --------------------------------------------------------------------
 * Processor:	VpsFormattedOutputGenerationProcessor
 * Author:		paulkwitkin @ Veeva
 * Date:		2019-12-05
 *---------------------------------------------------------------------
 * Description:	Queue Message Processor for running the Formatted Output User Action
 *---------------------------------------------------------------------
 * Copyright (c) 2019 Veeva Systems Inc.  All Rights Reserved.
 *		This code is based on pre-existing content developed and
 *		owned by Veeva Systems Inc. and may only be used in connection
 *		with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.custom.processors;

import com.veeva.vault.custom.classes.api.VpsAPIClient;
import com.veeva.vault.sdk.api.core.LogService;
import com.veeva.vault.sdk.api.core.RuntimeService;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.queue.*;

@MessageProcessorInfo()
public class VpsFormattedOutputGenerationProcessor implements MessageProcessor {
    private static final String HTTP_CALLOUT_CONNECTION_NAME = "local_http_callout_connection";
    private static final String USER_ACTION_PREFIX = "Objectlifecyclestateuseraction";

    public void execute(MessageContext context) {
        LogService logService = ServiceLocator.locate(LogService.class);
        boolean dedug = logService.isDebugEnabled();
        if (dedug) logService.debug("Process FO Generation message start");

        RuntimeService runtimeService = ServiceLocator.locate(RuntimeService.class);
        runtimeService.sleep(500);

        VpsAPIClient vpsAPIClient = new VpsAPIClient(HTTP_CALLOUT_CONNECTION_NAME);
        Message message = context.getMessage();
        String objectName = message.getAttribute("object", MessageAttributeValueType.STRING);
        String actionToRun = USER_ACTION_PREFIX + "." + objectName + "." + message.getAttribute("actionToRun", MessageAttributeValueType.STRING);
        if (dedug) {
            logService.debug("actionToRun: " + actionToRun);
            logService.debug("message item size: " + message.getMessageItems().size());
            logService.debug("message object: " + objectName);
        }
        if (!message.getMessageItems().isEmpty()) {
			if (dedug) logService.debug("Call initiateObjectAction");
            boolean result = vpsAPIClient.initiateObjectAction(objectName, actionToRun, message.getMessageItems());
			if (dedug) logService.debug("After Call initiateObjectAction with result: " + result);
        }
    }
}
