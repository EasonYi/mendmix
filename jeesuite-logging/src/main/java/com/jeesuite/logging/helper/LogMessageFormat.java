package com.jeesuite.logging.helper;

import java.io.Serializable;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.util.ResourceUtils;

public class LogMessageFormat {

	private static int showLines = ResourceUtils.getInt("application.logdetails.show-lines", 5);
	/**
	 * 生成日志消息
	 * @param actionKey 操作关键词
	 * @param bizKey 业务关键信息如：订单号
	 * @return
	 */
	public static String buildLogHeader(String actionKey,Serializable bizKey) {
		StringBuilder builder = new StringBuilder();
		builder.append(actionKey);
		builder.append("<bizKey:").append(bizKey).append(">");
		String requestId = CurrentRuntimeContext.getRequestId();
		if(requestId != null)builder.append("<requestId:").append(requestId).append(">");
		String tenantId = CurrentRuntimeContext.getTenantId(false);
    	if(tenantId != null)builder.append("<tenantId:").append(tenantId).append(">");
    	AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
    	if(currentUser != null)builder.append("<currentUser:").append(currentUser.getName()).append(">");
    	
		return builder.toString();
	}
	
	
	public static String buildLogTail(String actionKey) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		if(actionKey != null) {
			builder.append("<actionKey:").append(actionKey).append(">");
		}
		String systemId = CurrentRuntimeContext.getSystemId();
		if(systemId != null)builder.append("<systemId:").append(systemId).append(">");
		String tenantId = CurrentRuntimeContext.getTenantId(false);
    	if(tenantId != null)builder.append("<tenantId:").append(tenantId).append(">");
    	AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
    	if(currentUser != null)builder.append("<currentUser:").append(currentUser.getName()).append(">");
    	builder.append("]");
    	
		return builder.toString();
	}
	
	
	public static String buildExceptionMessages(Throwable throwable) {
		return buildExceptionMessages(throwable, showLines);
	}
	
	public static String buildExceptionMessages(Throwable throwable,int showLines) {
		if(showLines <= 0)return ExceptionUtils.getStackTrace(throwable);
		if (throwable instanceof JeesuiteBaseException == false) {
			return ExceptionUtils.getStackTrace(throwable);
		}
    	String[] traces = ExceptionUtils.getRootCauseStackTrace(throwable);
    	try {
    		StringBuilder sb = new StringBuilder();
    		showLines = showLines >= traces.length ? traces.length : showLines;
    		for (int i = 0; i < showLines; i++) {
    			sb.append(traces[i]).append("\n");
			}
    		return sb.toString();
		} catch (Exception e) {
			return ExceptionUtils.getStackTrace(e);
		}
    	
    }
}
