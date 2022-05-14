package com.jeesuite.logging.integrate;

import java.util.Arrays;
import java.util.Collection;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.model.Page;
import com.jeesuite.common.util.BeanUtils;
import com.jeesuite.common.util.JsonUtils;

public class RequestLogBuilder {

	public static String requestLogMessage(String uri,String method,Object parameters,Object body) {
		StringBuilder builder = new StringBuilder();
		
    	builder.append("\n-----------request start-----------\n");
    	builder.append("uri      :").append(uri).append("\n");
    	builder.append("method   :").append(method).append("\n");
    	if(parameters != null) {
    		builder.append("parameters  :").append(parameters).append("\n");
    	}
    	
    	String tenantId = CurrentRuntimeContext.getTenantId(false);
    	if(tenantId != null)builder.append("tenantId  :").append(tenantId).append("\n");
    	AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
    	if(currentUser != null)builder.append("currentUser  :").append(currentUser.getName()).append("\n");
    	
    	if(body != null) {
    		String bodyString;
    		if(body instanceof byte[]) {
    			byte[] bodyBytes = (byte[])body;
    			if(bodyBytes.length > 1024)bodyBytes = Arrays.copyOf(bodyBytes, 1024);
    			bodyString = new String(bodyBytes);
    		}else if(body instanceof String) {
    			bodyString = body.toString();
    		}else {
    			bodyString = JsonUtils.toJson(body);
    		}
    		builder.append("body  :").append(bodyString).append("\n");
    	}
    	builder.append("-----------request end-----------\n");
    	
    	return builder.toString();
	}
	
	
	@SuppressWarnings("rawtypes")
	public static String responseLogMessage(int statusCode,Object headers,Object body) {
		StringBuilder builder = new StringBuilder();
    	builder.append("\n-----------response start-----------\n");
    	builder.append("statusCode      :").append(statusCode).append("\n");
    	if(body != null) {
    		String bodyString;
    		if(body instanceof byte[]) {
    			byte[] bodyBytes = (byte[])body;
    			if(bodyBytes.length > 1024)bodyBytes = Arrays.copyOf(bodyBytes, 1024);
    			bodyString = new String(bodyBytes);
    		}else if(BeanUtils.isSimpleDataType(body)) {
    			bodyString = body.toString();
    		}else if(body instanceof Collection) {
    			Collection bodyList = (Collection)body;
    			bodyString = "itemNums:" + bodyList.size();
    		}else if(body instanceof Page) {
    			Page apge = (Page)body;
    			bodyString = String.format("{\"pageNo\":%s,\"pageSize\":%s,\"total\":%s}", apge.getPageNo(),apge.getPageSize(),apge.getTotal());
    		}else {
    			bodyString = JsonUtils.toJson(body);
    		}
    		builder.append("body  :").append(bodyString).append("\n");
    	}
    	
    	builder.append("-----------response end-----------\n");
    	return builder.toString();
	}
	
}
