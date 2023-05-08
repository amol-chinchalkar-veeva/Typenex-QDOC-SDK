/*
 * --------------------------------------------------------------------
 * UDC:         VpsAPIClient
 * Author:      markarnold @ Veeva
 * Date:        2019-07-25
 * --------------------------------------------------------------------
 * Description: Helper for running vql queries via api
 * --------------------------------------------------------------------
 * Copyright (c) 2019 Veeva Systems Inc.  All Rights Reserved.
 * This code is based on pre-existing content developed and
 * owned by Veeva Systems Inc. and may only be used in connection
 * with the deliverable with which it was provided to Customer.
 * --------------------------------------------------------------------
 */
package com.veeva.vault.custom.classes.api;

import com.veeva.vault.custom.classes.VpsBaseHelper;
import com.veeva.vault.custom.classes.VpsSettingHelper;
import com.veeva.vault.custom.classes.VpsSettingRecord;
import com.veeva.vault.custom.classes.VpsUtilHelper;
import com.veeva.vault.sdk.api.core.RollbackException;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.http.HttpMethod;
import com.veeva.vault.sdk.api.http.HttpRequest;
import com.veeva.vault.sdk.api.http.HttpResponseBodyValueType;
import com.veeva.vault.sdk.api.http.HttpService;
import com.veeva.vault.sdk.api.json.JsonArray;
import com.veeva.vault.sdk.api.json.JsonObject;
import com.veeva.vault.sdk.api.json.JsonValueType;

import java.util.List;
import java.util.Map;
import java.util.Set;

@UserDefinedClassInfo
public class VpsAPIClient extends VpsBaseHelper {

	private static final String APIFIELD_ACTIONS = "lifecycle_actions__v";
	private static final String APIFIELD_ASSIGNED_GROUPS = "assignedGroups";
	private static final String APIFIELD_ASSIGNED_USERS = "assignedUsers";
	private static final String APIFIELD_DOCUMENT_ROLES = "documentRoles";
	private static final String APIFIELD_FROM_TEMPLATE = "fromTemplate";
	private static final String APIFIELD_LABEL = "label__v";
	private static final String APIFIELD_NAME = "name__v";
	private static final String APIFIELD_ERROR_MESSAGE = "message";
	private static final String APIFIELD_ERROR_TYPE = "type";
	private static final String APIFIELD_QUERY = "q";
	private static final String PROCESS_ERROR = "ERROR";
	private static final String PROCESS_RACECONDITION = "RACE_CONDITION";
	private static final String PROCESS_SUCCESS = "SUCCESS";
	private static final String RESPONSESTATUS_SUCCESS = "SUCCESS";
	private static final int RETRY_ATTEMPTS_COUNT = 5;
	private static final String SDK_EXTERNAL_ID = "VpsAPIClient";
	private static final String SETTING_APIVERSION = "api_version";
	private static final String URL_BINDER_CREATETEMPLATE = "/api/%s/objects/binders";
	private static final String URL_DOCUMENT_CREATETEMPLATE = "/api/%s/objects/documents";
	private static final String URL_DOCUMENT_LIFEYCLEACTIONS = "/api/%s/objects/documents/%s/versions/%s/%s/lifecycle_actions/";
	private static final String URL_INITIATE_OBJECT_ACTION = "/api/%s/vobjects/%s/%s/actions/%s";
	private static final String URL_QUERY = "/api/%s/query";
	private static final String URL_ROLES = "/api/%s/objects/documents/%s/roles/%s";
	private static final String URL_WORKFLOW_DETAILS = "/api/%s/objects/objectworkflows";
	private static final String URL_WORKFLOW_CANCEL = "/api/%s/objects/objectworkflows/%s/actions/cancel";
	private static final String URL_DOCUMENT_TEMPLATES = "/api/%s/objects/documents/templates";
	private static final String URL_OBJECT_CREATE = "/api/%s/vobjects/%s";
	private static final String URL_GET_DOCUMENT_FILE = "/api/%s/objects/documents/%s/file";
	private static final String URL_GET_ATTACHMENT_DATA = "/api/%s/vobjects/%s/%s/attachments";

	HttpService httpService = ServiceLocator.locate(HttpService.class);
	String apiVersion = "v22.3";
	String apiConnection;
	VpsSettingRecord sdkSettings;

	/**
	 * Class to assist in making using Vault Query Language
	 */
	public VpsAPIClient(String apiConnection) {
		super();

		this.apiConnection = apiConnection;

		VpsSettingHelper settingHelper = new VpsSettingHelper(SDK_EXTERNAL_ID,true);
		sdkSettings = settingHelper.items().get(SDK_EXTERNAL_ID);
		if (sdkSettings != null) {
			apiVersion = sdkSettings.getValue(SETTING_APIVERSION, apiVersion);
		}
	}

	public Boolean createBinderFromTemplate(String templateName, Map<String, String> documentMetadata) {
		return createDocumentFromTemplate(templateName,documentMetadata,true);
	}

	public Boolean createDocumentFromTemplate(String templateName, Map<String, String> documentMetadata) {
		return createDocumentFromTemplate(templateName,documentMetadata,false);
	}

	private Boolean createDocumentFromTemplate(String templateName,
											   Map<String, String> documentMetadata,
											   Boolean isBinder) {

		List<Boolean> successList = VaultCollections.newList();

		String createTemplateUrl;
		if (isBinder) {
			createTemplateUrl = String.format(URL_BINDER_CREATETEMPLATE, apiVersion);
		}
		else {
			createTemplateUrl = String.format(URL_DOCUMENT_CREATETEMPLATE, apiVersion);
		}
		getLogService().info("createDocumentFromTemplate {}", createTemplateUrl);

		HttpRequest request = httpService.newHttpRequest(apiConnection)
				.setMethod(HttpMethod.POST)
				.setBodyParam(APIFIELD_FROM_TEMPLATE, templateName)
				.appendPath(createTemplateUrl);

		for (String fieldName : documentMetadata.keySet()) {
			String fieldValue = documentMetadata.get(fieldName);
			request.setBodyParam(fieldName, fieldValue);
		}

		httpService.send(request, HttpResponseBodyValueType.STRING)
				.onError(response -> {
					String errorMessage = "HTTP Status Code: " + response.getHttpResponse().getHttpStatusCode();
					getLogService().error("createDocumentFromTemplate {}", errorMessage);
					getErrorList().add(errorMessage);
				})
				.onSuccess(response -> {
					VpsAPIResponse apiResponse = new VpsAPIResponse(response.getResponseBody());
					if (apiResponse.getResponseStatus().equals(RESPONSESTATUS_SUCCESS)) {
						successList.add(true);
					}
					else {
						JsonArray errors = apiResponse.getErrors();
						if (errors != null) {
							for (int i = 0; i < errors.getSize(); i++) {
								JsonObject error = errors.getValue(i, JsonValueType.OBJECT);
								String errorType = error.getValue(APIFIELD_ERROR_TYPE, JsonValueType.STRING);
								String errorMessage = error.getValue(APIFIELD_ERROR_MESSAGE, JsonValueType.STRING);
								getLogService().error("createDocumentFromTemplate {}", errorType + " - " + errorMessage);
								getErrorList().add(errorType + " - " + errorMessage);
							}
						}
					}

				})
				.execute();

		return successList.size() > 0;
	}

	public String createDocumentFromTemplateGetId(String templateName, Map<String, String> documentMetadata) {
		return createDocumentFromTemplateGetId(templateName,documentMetadata,false);
	}

	private String createDocumentFromTemplateGetId(String templateName,
											   Map<String, String> documentMetadata,
											   Boolean isBinder) {

		List<String> docList = VaultCollections.newList();

		String createTemplateUrl;
		if (isBinder) {
			createTemplateUrl = String.format(URL_BINDER_CREATETEMPLATE, apiVersion);
		}
		else {
			createTemplateUrl = String.format(URL_DOCUMENT_CREATETEMPLATE, apiVersion);
		}
		getLogService().info("createDocumentFromTemplate {}", createTemplateUrl);

		HttpRequest request = httpService.newHttpRequest(apiConnection)
				.setMethod(HttpMethod.POST)
				.setBodyParam(APIFIELD_FROM_TEMPLATE, templateName)
				.appendPath(createTemplateUrl);

		for (String fieldName : documentMetadata.keySet()) {
			String fieldValue = documentMetadata.get(fieldName);
			request.setBodyParam(fieldName, fieldValue);
		}

		httpService.send(request, HttpResponseBodyValueType.STRING)
				.onError(response -> {
					String errorMessage = "HTTP Status Code: " + response.getHttpResponse().getHttpStatusCode();
					getLogService().error("createDocumentFromTemplate {}", errorMessage);
					getErrorList().add(errorMessage);
				})
				.onSuccess(response -> {
					VpsAPIResponse apiResponse = new VpsAPIResponse(response.getResponseBody());
					if (apiResponse.getResponseStatus().equals(RESPONSESTATUS_SUCCESS)) {

						docList.add(apiResponse.getJson().getValue("id", JsonValueType.NUMBER).toString());
					}
					else {
						JsonArray errors = apiResponse.getErrors();
						if (errors != null) {
							for (int i = 0; i < errors.getSize(); i++) {
								JsonObject error = errors.getValue(i, JsonValueType.OBJECT);
								String errorType = error.getValue(APIFIELD_ERROR_TYPE, JsonValueType.STRING);
								String errorMessage = error.getValue(APIFIELD_ERROR_MESSAGE, JsonValueType.STRING);
								getLogService().error("createDocumentFromTemplate {}", errorType + " - " + errorMessage);
								getErrorList().add(errorType + " - " + errorMessage);
							}
						}
					}

				})
				.execute();

		if (docList.size() > 0) {
			return docList.get(0);
		}
		else {
			return null;
		}
	}

	public Map<String,String> getDocumentLifecycleActions(String docId,
														  String majorVersion,
														  String minorVersion) {

		Map<String,String> lifecycleActionMap = VaultCollections.newMap();

		String lifeycleActionUrl = String.format(URL_DOCUMENT_LIFEYCLEACTIONS,apiVersion,docId,majorVersion,minorVersion);
		getLogService().info("getDocumentLifecycleActions {}", lifeycleActionUrl);

		HttpRequest request = httpService.newHttpRequest(apiConnection)
				.appendPath(lifeycleActionUrl);
		httpService.send(request, HttpResponseBodyValueType.STRING)
				.onError(response -> {
					String errorMessage = "HTTP Status Code: " + response.getHttpResponse().getHttpStatusCode();
					getLogService().error("getDocumentLifecycleActions {}", errorMessage);
					getErrorList().add(errorMessage);
				})
				.onSuccess(response -> {
					VpsAPIResponse apiResponse = new VpsAPIResponse(response.getResponseBody());
					if (apiResponse.getResponseStatus().equals(RESPONSESTATUS_SUCCESS)) {
						JsonArray actionArray = apiResponse.getArray(APIFIELD_ACTIONS);
						for (int i = 0; i< actionArray.getSize(); i++) {
							JsonObject lifecycleAction = actionArray.getValue(i, JsonValueType.OBJECT);

							lifecycleActionMap.put(
									lifecycleAction.getValue(APIFIELD_LABEL, JsonValueType.STRING),
									lifecycleAction.getValue(APIFIELD_NAME, JsonValueType.STRING));
						}
					}
					else {
						JsonArray errors = apiResponse.getErrors();
						if (errors != null) {
							for (int i = 0; i < errors.getSize(); i++) {
								JsonObject error = errors.getValue(i, JsonValueType.OBJECT);
								String errorType = error.getValue(APIFIELD_ERROR_TYPE, JsonValueType.STRING);
								String errorMessage = error.getValue(APIFIELD_ERROR_MESSAGE, JsonValueType.STRING);
								getLogService().error("getDocumentLifecycleActions {}", errorType + " - " + errorMessage);
								getErrorList().add(errorType + " - " + errorMessage);
							}
						}
					}
				})
				.execute();

		return lifecycleActionMap;
	}

	public List<String> getDocumentUsersAndGroupsFromRole(String docId, String roleApiName) {
		List<String> usersAndGroups = VaultCollections.newList();
		String roleUrl = String.format(URL_ROLES, apiVersion, docId, roleApiName);
		getLogService().info("getDocumentUsersAndGroupsFromRole {}", roleUrl);

		HttpRequest request = httpService.newHttpRequest(apiConnection)
				.appendPath(roleUrl);

		httpService.send(request, HttpResponseBodyValueType.STRING)
				.onError(response -> {
					String errorMessage = "HTTP Status Code: " + response.getHttpResponse().getHttpStatusCode();
					getLogService().error("getDocumentLifecycleActions {}", errorMessage);
					getErrorList().add(errorMessage);
				})
				.onSuccess(response -> {

					VpsAPIResponse apiResponse = new VpsAPIResponse(response.getResponseBody());
					if (apiResponse.getResponseStatus().equals(RESPONSESTATUS_SUCCESS)) {
						JsonArray roleArray = apiResponse.getArray(APIFIELD_DOCUMENT_ROLES);

						for (int i = 0; i < roleArray.getSize(); i++) {
							JsonObject role = roleArray.getValue(i, JsonValueType.OBJECT);

							JsonArray groupArray = role.getValue(APIFIELD_ASSIGNED_GROUPS, JsonValueType.ARRAY);
							for (int g = 0; g < groupArray.getSize(); g++) {
								usersAndGroups.add("group:" + groupArray.getValue(g, JsonValueType.NUMBER));
							}
							JsonArray userArray = role.getValue(APIFIELD_ASSIGNED_USERS, JsonValueType.ARRAY);
							for (int u = 0; u< userArray.getSize(); u++) {
								usersAndGroups.add("user:" + userArray.getValue(u, JsonValueType.NUMBER));
							}
						}
					}
					else {
						JsonArray errors = apiResponse.getErrors();
						if (errors != null) {
							for (int i = 0; i < errors.getSize(); i++) {
								JsonObject error = errors.getValue(i, JsonValueType.OBJECT);
								String errorType = error.getValue(APIFIELD_ERROR_TYPE, JsonValueType.STRING);
								String errorMessage = error.getValue(APIFIELD_ERROR_MESSAGE, JsonValueType.STRING);
								getLogService().error("getDocumentLifecycleActions {}", errorType + " - " + errorMessage);
								getErrorList().add(errorType + " - " + errorMessage);
							}
						}
					}
				})
				.execute();

		return usersAndGroups;
	}

	public Boolean initiateObjectAction(String objectName,
										String userActionName,
										List<String> idList) {
		return initiateObjectAction(objectName,userActionName,idList,true);
	}

	public Boolean initiateObjectAction(String objectName,
										String userActionName,
										List<String> idList,
										Boolean rollbackOnError) {
		for (String objectId : idList) {
			if (!initiateObjectAction(objectName, userActionName, objectId, rollbackOnError)) {
				return false;
			}
		}
		return true;
	}

	public Boolean initiateObjectAction(String objectName,
										String userActionName,
										String objectId) {
		return initiateObjectAction(objectName, userActionName, objectId,true);
	}

	public Boolean initiateObjectAction(String objectName,
										String userActionName,
										String objectId,
										Boolean rollbackOnError) {
		String initiateObjectActionUrl = String.format(
				URL_INITIATE_OBJECT_ACTION,
				apiVersion,
				objectName,
				objectId,
				userActionName);
		getLogService().info("initiateObjectAction {}", initiateObjectActionUrl);

		HttpRequest request = httpService.newHttpRequest(apiConnection)
				.setMethod(HttpMethod.POST)
				.setBody("")
				.appendPath(initiateObjectActionUrl);

		//this list is used to track the results of the batch process (success vs. error)
		//errors that are non-race conditions are marked as error
		//note: using a list because lambda expressions require final variables
		Set<String> results = VaultCollections.newSet();

		//this list is used to track the number of batch attempts
		//race condition errors are retried up to RETRY_ATTEMPTS_COUNT
		//note: using a list because lambda expressions require final variables
		List<String> batchAttempts = VaultCollections.newList();

		while ((batchAttempts.size() < RETRY_ATTEMPTS_COUNT)
				&& (!results.contains(PROCESS_ERROR))
				&& (!results.contains(PROCESS_SUCCESS))) {
			httpService.send(request, HttpResponseBodyValueType.STRING)
					.onError(response -> {
						batchAttempts.add(PROCESS_ERROR);
						results.add(PROCESS_ERROR);

						String errorMessage = "HTTP Status Code: " + response.getHttpResponse().getHttpStatusCode();
						getLogService().error("initiateObjectAction {}", errorMessage);
						getErrorList().add(errorMessage);
					})
					.onSuccess(response -> {
						VpsAPIResponse apiResponse = new VpsAPIResponse(response.getResponseBody());
						if (apiResponse.getResponseStatus().equals(RESPONSESTATUS_SUCCESS)) {
							batchAttempts.add(PROCESS_SUCCESS);
							results.add(PROCESS_SUCCESS);
						}
						//This is HTTP 200, but an application level error
						else {
							JsonArray errors = apiResponse.getErrors();
							if (errors != null) {
								for (int i = 0; i < errors.getSize(); i++) {
									JsonObject error = errors.getValue(i, JsonValueType.OBJECT);
									String errorType = error.getValue(APIFIELD_ERROR_TYPE, JsonValueType.STRING);
									String errorMessage = error.getValue(APIFIELD_ERROR_MESSAGE, JsonValueType.STRING);
									getLogService().error("initiateObjectAction {}", errorType + " - " + errorMessage);
									getErrorList().add(errorType + " - " + errorMessage);

									//handles race conditions for record level locking
									if (errorType.equals(PROCESS_RACECONDITION)) {
										results.add(PROCESS_RACECONDITION);
										sleep();

										//if we reached the max number of retries and rollebackOnError=true
										//throw rollback exception
										if ((batchAttempts.size() == RETRY_ATTEMPTS_COUNT) && (rollbackOnError)) {
											throw new RollbackException("OPERATION_NOT_ALLOWED", errorMessage + PROCESS_RACECONDITION);
										}
									} else {
										results.add(PROCESS_ERROR);

										if (rollbackOnError) {
											throw new RollbackException("OPERATION_NOT_ALLOWED", errorMessage);
										}
									}
								}
							}
						}

					})
					.execute();
		}

		return results.contains(PROCESS_SUCCESS);
	}

	/**
	 * Runs the current query. Query is logged to LogService
	 *
	 * @return QueryResponse with results from the VQL query
	 */
	public VpsVQLResponse runVQL(VpsVQLRequest vpsVQLRequest) {
		vpsVQLRequest.logVQL();

		List<VpsVQLResponse> resultList = VaultCollections.newList();

		String queryUrl = String.format(URL_QUERY, apiVersion);
		getLogService().info("runVQL {}", queryUrl);

		//now call GET on the documents available user actions and build a map
		HttpRequest request = httpService.newHttpRequest(apiConnection)
				.setMethod(HttpMethod.POST)
				.setBodyParam(APIFIELD_QUERY, vpsVQLRequest.getVQL())
				.appendPath(queryUrl);

		httpService.send(request, HttpResponseBodyValueType.STRING)
				.onError(response -> {
					String errorMessage = "HTTP Status Code: " + response.getHttpResponse().getHttpStatusCode();
					getLogService().error("runVQL {}", errorMessage);
					getErrorList().add(errorMessage);
				})
				.onSuccess(response -> {
					VpsVQLResponse vpsVQLResponse = new VpsVQLResponse(response.getResponseBody());
					resultList.add(vpsVQLResponse);

					JsonArray errors = vpsVQLResponse.getErrors();
					if (errors != null) {
						for (int i = 0; i < errors.getSize(); i++) {
							JsonObject error = errors.getValue(i, JsonValueType.OBJECT);
							String errorType = error.getValue(APIFIELD_ERROR_TYPE, JsonValueType.STRING);
							String errorMessage = error.getValue(APIFIELD_ERROR_MESSAGE, JsonValueType.STRING);
							getLogService().error("VpsVQLResponse {}", errorType + " - " + errorMessage);
							getErrorList().add(errorType + " - " + errorMessage);
						}
					}

				})
				.execute();

		if (resultList.size() > 0) {
			return resultList.get(0);
		}
		else {
			return null;
		}
	}

	public Boolean startDocumentWorkflow(String docId,
										 String majorVersion,
										 String minorVersion,
										 String lifecycleActionName,
										 String roleName,
										 Set<String> users,
										 Set<String> groups) {

		List<Boolean> successList = VaultCollections.newList();

		String startWorkflowUrl = String.format(
				URL_DOCUMENT_LIFEYCLEACTIONS,
				apiVersion,
				docId,
				majorVersion,
				minorVersion) + lifecycleActionName;
		getLogService().info("startDocumentWorkflow {}", startWorkflowUrl);

		Set<String> usersAndGroups = VaultCollections.newSet();
		if (users != null) {
			for (String userId : users) {
				usersAndGroups.add("user:" + userId);
			}
		}
		if (groups != null) {
			for (String groupId : groups) {
				usersAndGroups.add("group:" + groupId);
			}
		}

		HttpRequest request = httpService.newHttpRequest(apiConnection)
				.setMethod(HttpMethod.PUT)
				.setBodyParam(roleName, VpsUtilHelper.setToString(usersAndGroups,",",false))
				.appendPath(startWorkflowUrl);

		httpService.send(request, HttpResponseBodyValueType.STRING)
				.onError(response -> {
					String errorMessage = "HTTP Status Code: " + response.getHttpResponse().getHttpStatusCode();
					getLogService().error("startDocumentWorkflow {}", errorMessage);
					getErrorList().add(errorMessage);
				})
				.onSuccess(response -> {
					VpsAPIResponse apiResponse = new VpsAPIResponse(response.getResponseBody());
					if (apiResponse.getResponseStatus().equals(RESPONSESTATUS_SUCCESS)) {
						successList.add(true);
					}
					else {
						JsonArray errors = apiResponse.getErrors();
						if (errors != null) {
							for (int i = 0; i < errors.getSize(); i++) {
								JsonObject error = errors.getValue(i, JsonValueType.OBJECT);
								String errorType = error.getValue(APIFIELD_ERROR_TYPE, JsonValueType.STRING);
								String errorMessage = error.getValue(APIFIELD_ERROR_MESSAGE, JsonValueType.STRING);
								getLogService().error("startDocumentWorkflow {}", errorType + " - " + errorMessage);
								getErrorList().add(errorType + " - " + errorMessage);
							}
						}
					}

				})
				.execute();

		return successList.size() > 0;
	}

	public List<String> getWorkflowId(String objectName, String recordId)
	{
		List<String> workflowId = VaultCollections.newList();

		String getWorkflowUrl = String.format(
				URL_WORKFLOW_DETAILS,
				apiVersion,
				objectName,
				recordId);
		getLogService().info("getWorkflowUrl {}", getWorkflowUrl);

		HttpRequest request = httpService.newHttpRequest(apiConnection)
				.setMethod(HttpMethod.GET)
				.appendPath(getWorkflowUrl)
				.setQuerystringParam("object__v", objectName)
				.setQuerystringParam("record_id__v", recordId);

		httpService.send(request, HttpResponseBodyValueType.STRING)
				.onError(response -> {
					String errorMessage = "HTTP Status Code: " + response.getHttpResponse().getHttpStatusCode();
					getLogService().error("getWorkflowId {}", errorMessage);
					getErrorList().add(errorMessage);
				})
				.onSuccess(response -> {
					VpsAPIResponse apiResponse = new VpsAPIResponse(response.getResponseBody());
					if (apiResponse.getResponseStatus().equals(RESPONSESTATUS_SUCCESS)) {
						//TODO Need to get Id from results
						JsonArray results = apiResponse.getArray("data");
						for (int i = 0; i< results.getSize(); i++) {
							JsonObject dataItem = results.getValue(i, JsonValueType.OBJECT);
							if (dataItem.contains("id"))
								workflowId.add(dataItem.getValue("id", JsonValueType.NUMBER).toString());
						}

					}
					else {
						JsonArray errors = apiResponse.getErrors();
						if (errors != null) {
							for (int i = 0; i < errors.getSize(); i++) {
								JsonObject error = errors.getValue(i, JsonValueType.OBJECT);
								String errorType = error.getValue(APIFIELD_ERROR_TYPE, JsonValueType.STRING);
								String errorMessage = error.getValue(APIFIELD_ERROR_MESSAGE, JsonValueType.STRING);
								getLogService().error("getWorkflowId {}", errorType + " - " + errorMessage);
								getErrorList().add(errorType + " - " + errorMessage);
							}
						}
					}

				})
				.execute();



		return workflowId;
	}

	public Boolean cancelWorkflow(String workflowId)
	{
		List<Boolean> success = VaultCollections.newList();
		success.add(false);

		String cancelWorkflowURL = String.format(
				URL_WORKFLOW_CANCEL,
				apiVersion,
				workflowId);

		getLogService().info("cancelWorkflowURL {}", cancelWorkflowURL);

		HttpRequest request = httpService.newHttpRequest(apiConnection)
				.setMethod(HttpMethod.POST)
				.appendPath(cancelWorkflowURL);

		httpService.send(request, HttpResponseBodyValueType.STRING)
				.onError(response -> {
					String errorMessage = "HTTP Status Code: " + response.getHttpResponse().getHttpStatusCode();
					getLogService().error("cancelWorkflow {}", errorMessage);
					getErrorList().add(errorMessage);
				})
				.onSuccess(response -> {
					VpsAPIResponse apiResponse = new VpsAPIResponse(response.getResponseBody());
					if (apiResponse.getResponseStatus().equals(RESPONSESTATUS_SUCCESS)) {
						success.add(true);
					}
					else {
						JsonArray errors = apiResponse.getErrors();
						if (errors != null) {
							for (int i = 0; i < errors.getSize(); i++) {
								JsonObject error = errors.getValue(i, JsonValueType.OBJECT);
								String errorType = error.getValue(APIFIELD_ERROR_TYPE, JsonValueType.STRING);
								String errorMessage = error.getValue(APIFIELD_ERROR_MESSAGE, JsonValueType.STRING);
								getLogService().error("startDocumentWorkflow {}", errorType + " - " + errorMessage);
								getErrorList().add(errorType + " - " + errorMessage);
							}
						}
					}

				})
				.execute();


		return success.get(0);
	}

	public VpsDocumentTemplateResponse getDocumentTemplates()
	{
		List<VpsDocumentTemplateResponse> resultList = VaultCollections.newList();

		String documentTemplateURL = String.format(
				URL_DOCUMENT_TEMPLATES,
				apiVersion);

		HttpRequest request = httpService.newHttpRequest(apiConnection)
				.setMethod(HttpMethod.GET)
				.appendPath(documentTemplateURL);

		httpService.send(request, HttpResponseBodyValueType.STRING)
				.onError(response -> {
					String errorMessage = "HTTP Status Code: " + response.getHttpResponse().getHttpStatusCode();
					getLogService().error("getDocumentTemplates {}", errorMessage);
					getErrorList().add(errorMessage);
				})
				.onSuccess(response -> {
					VpsAPIResponse apiResponse = new VpsAPIResponse(response.getResponseBody());
					if (apiResponse.getResponseStatus().equals(RESPONSESTATUS_SUCCESS)) {

						VpsDocumentTemplateResponse documentTemplateResponse = new VpsDocumentTemplateResponse(response.getResponseBody());
						resultList.add(documentTemplateResponse);

					}
					else {
						JsonArray errors = apiResponse.getErrors();
						if (errors != null) {
							for (int i = 0; i < errors.getSize(); i++) {
								JsonObject error = errors.getValue(i, JsonValueType.OBJECT);
								String errorType = error.getValue(APIFIELD_ERROR_TYPE, JsonValueType.STRING);
								String errorMessage = error.getValue(APIFIELD_ERROR_MESSAGE, JsonValueType.STRING);
								getLogService().error("getDocumentTemplates {}", errorType + " - " + errorMessage);
								getErrorList().add(errorType + " - " + errorMessage);
							}
						}
					}

				})
				.execute();

		if (resultList.size() > 0) {
			return resultList.get(0);
		}
		else {
			return null;
		}

	}

	public Boolean createObjects(String objectName, String json)
	{
		List<Boolean> success = VaultCollections.newList();

		String objectCreateURL = String.format(
				URL_OBJECT_CREATE,
				apiVersion,
				objectName);

		HttpRequest request = httpService.newHttpRequest(apiConnection)
				.setMethod(HttpMethod.POST)
				.setHeader("Content-Type", "application/json")
				.setBody(json)
				.appendPath(objectCreateURL);

		httpService.send(request, HttpResponseBodyValueType.STRING)
				.onError(response -> {
					String errorMessage = "HTTP Status Code: " + response.getHttpResponse().getHttpStatusCode();
					getLogService().error("createObjects {}", errorMessage);
					getErrorList().add(errorMessage);
				})
				.onSuccess(response -> {
					VpsAPIResponse apiResponse = new VpsAPIResponse(response.getResponseBody());
					if (apiResponse.getResponseStatus().equals(RESPONSESTATUS_SUCCESS)) {
						success.add(true);
					}
					else {
						JsonArray errors = apiResponse.getErrors();
						if (errors != null) {
							for (int i = 0; i < errors.getSize(); i++) {
								JsonObject error = errors.getValue(i, JsonValueType.OBJECT);
								String errorType = error.getValue(APIFIELD_ERROR_TYPE, JsonValueType.STRING);
								String errorMessage = error.getValue(APIFIELD_ERROR_MESSAGE, JsonValueType.STRING);
								getLogService().error("createObjects {}", errorType + " - " + errorMessage);
								getErrorList().add(errorType + " - " + errorMessage);
							}
						}
					}

				})
				.execute();


		return success.get(0);

	}

	public byte[] getDocumentFile(String docId)
	{
		String getDocUrl = String.format(
				URL_GET_DOCUMENT_FILE,
				apiVersion,
				docId);

		List<byte[]> fileBytes = VaultCollections.newList();

		HttpRequest request = httpService.newHttpRequest(apiConnection)
				.setMethod(HttpMethod.GET)
				.appendPath(getDocUrl);

		httpService.send(request, HttpResponseBodyValueType.STRING)
				.onError(response -> {
					String errorMessage = "HTTP Status Code: " + response.getHttpResponse().getHttpStatusCode();
					getLogService().error("createObjects {}", errorMessage);
					getErrorList().add(errorMessage);
				})
				.onSuccess(response -> {

					if (response.getHttpStatusCode() == 200) {
						byte[] file = response.getResponseBody().getBytes();
						fileBytes.add(file);
					}
					else {
						getLogService().error("getDocumentFile {}", response.getHttpStatusCode());
						getErrorList().add("getDocumentFile error - " + response.getHttpStatusCode());
					}
				})
				.execute();

		return fileBytes.get(0);
	}

	public Map<String, Map<String, String>> getAttachmentData(String objectName, String objectId)
	{
		String getAttachmentDataUrl = String.format(URL_GET_ATTACHMENT_DATA,
				apiVersion,
				objectName,
				objectId);

		Map<String, Map<String, String>> attachmentsData = VaultCollections.newMap();

		HttpRequest request = httpService.newHttpRequest(apiConnection)
				.setMethod(HttpMethod.GET)
				.appendPath(getAttachmentDataUrl);

		httpService.send(request, HttpResponseBodyValueType.STRING)
				.onError(response -> {
					String errorMessage = "HTTP Status Code: " + response.getHttpResponse().getHttpStatusCode();
					getLogService().error("getAttachmentData {}", errorMessage);
					getErrorList().add(errorMessage);
				})
				.onSuccess(response -> {

					if (response.getHttpStatusCode() == 200) {
						VpsAPIResponse apiResponse = new VpsAPIResponse(response.getResponseBody());
						if (apiResponse.getResponseStatus().equals(RESPONSESTATUS_SUCCESS)) {
							JsonArray results = apiResponse.getArray("data");
							for (int i = 0; i< results.getSize(); i++) {
								Map<String, String> attachmentData = VaultCollections.newMap();
								JsonObject dataItem = results.getValue(i, JsonValueType.OBJECT);
								attachmentData.put("filename__v", dataItem.getValue("filename__v", JsonValueType.STRING));
								attachmentData.put("format__v", dataItem.getValue("format__v", JsonValueType.STRING));
								attachmentData.put("created_date__v", dataItem.getValue("created_date__v", JsonValueType.STRING));
								attachmentData.put("id", dataItem.getValue("id", JsonValueType.NUMBER).toString());
								attachmentData.put("size__v", dataItem.getValue("size__v", JsonValueType.NUMBER).toString());
								attachmentData.put("version__v", dataItem.getValue("version__v", JsonValueType.NUMBER).toString());
								attachmentData.put("created_by__v", dataItem.getValue("created_by__v", JsonValueType.NUMBER).toString());
								attachmentsData.put(attachmentData.get("id").toString(), attachmentData);
							}

						}
					}
					else {
						getLogService().error("getAttachmentData {}", response.getHttpStatusCode());
						getErrorList().add("getAttachmentData error - " + response.getHttpStatusCode());
					}
				})
				.execute();

		return attachmentsData;
	}

}