/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.logging.integrate;

import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;
import com.mendmix.common.model.AuthUser;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.common.util.TokenGenerator;
import com.mendmix.logging.helper.LogMessageFormat;
import com.mendmix.logging.integrate.storage.HttpApiLogStorageProvider;
import com.mendmix.spring.InstanceFactory;



/**
 * 行为日志采集
 * 
 * <br>
 * Class Name   : ActionLogCollector
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年8月22日
 */
public class ActionLogCollector {
	

	private static Logger log = LoggerFactory.getLogger("global.request.logger");
	
	public static final String CURRENT_LOG_CONTEXT_NAME = "ctx_cur_log";
	
	private static final String TIMER_TASK = "timerTask";
	private static final String TIMER_TASK_ALIAS = "定时任务";
	private static final String ACTION_KEY_FORMAT = "%s_%s";
	private static boolean taskLogEnabled = ResourceUtils.getBoolean("application.task.log.enabled");

	private static ThreadPoolExecutor asyncSendExecutor;

	private static LogStorageProvider logStorageProvider;
	
	static {
		logStorageProvider = InstanceFactory.getInstance(LogStorageProvider.class);
		if(logStorageProvider == null && ResourceUtils.containsProperty("mendmix.actionlog.api.baseUrl")) {
			logStorageProvider = new HttpApiLogStorageProvider(ResourceUtils.getProperty("mendmix.actionlog.api.baseUrl"));
		}
		if(logStorageProvider != null) {
			int maxThreads = ResourceUtils.getInt("actionlog.push.threads",5);
			int maxQueueSize = ResourceUtils.getInt("actionlog.push.queue.size",2000);
			asyncSendExecutor  = new ThreadPoolExecutor(3, maxThreads,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(maxQueueSize),
                    new StandardThreadFactory("logPushExecutor"),
                    new DiscardPolicy());
		}
	}
	

	public static ActionLog onRequestStart(String httpMethod,String uri,String requestIp){
		ActionLog actionLog = new ActionLog();
		actionLog.setActionKey(String.format(ACTION_KEY_FORMAT,httpMethod, uri));
		actionLog.setRequestIp(requestIp);
		actionLog.setEnv(GlobalRuntimeContext.ENV);
		actionLog.setAppId(StringUtils.defaultIfBlank(GlobalRuntimeContext.SYSTEM_ID, GlobalRuntimeContext.APPID));
		actionLog.setModuleId(GlobalRuntimeContext.APPID);
		actionLog.setRequestAt(new Date());
		actionLog.setRequestId(CurrentRuntimeContext.getRequestId());
		AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
		if(currentUser != null){
			actionLog.setUserId(currentUser.getId());
			actionLog.setUserName(currentUser.getName());
		}
		actionLog.setClientType(CurrentRuntimeContext.getClientType());
		actionLog.setPlatformType(CurrentRuntimeContext.getPlatformType());
		actionLog.setTenantId(CurrentRuntimeContext.getTenantId());

		return actionLog;
	}
	
	public static void onResponseEnd(int httpStatus,Throwable throwable){
		ActionLog actionLog = ThreadLocalContext.get(CURRENT_LOG_CONTEXT_NAME);
		onResponseEnd(actionLog, httpStatus, throwable);
	}
    
    public static void onResponseEnd(ActionLog actionLog,int httpStatus,Throwable throwable){
    	if(actionLog == null) {
    		if(throwable != null) {
    			if (throwable instanceof MendmixBaseException) {
    				log.warn("MENDMIX-TRACE-LOGGGING-->> bizError"+LogMessageFormat.buildLogTail(null)+":{}",LogMessageFormat.buildExceptionMessages(throwable));
    			}else {
    				log.error("MENDMIX-TRACE-LOGGGING-->> systemError" + LogMessageFormat.buildLogTail(null),throwable);
    			}
    		}
    		return;
    	}

    	actionLog.setResponseAt(new Date());
    	if(actionLog.getResponseCode() <= 0) {
    		actionLog.setResponseCode(httpStatus);
    	}
    	if(throwable != null) {
    		if (throwable instanceof MendmixBaseException) {
				log.warn("MENDMIX-TRACE-LOGGGING-->> bizError"+LogMessageFormat.buildLogTail(actionLog.getActionKey())+":{}",LogMessageFormat.buildExceptionMessages(throwable));
			}else {
				log.error("MENDMIX-TRACE-LOGGGING-->> systemError" + LogMessageFormat.buildLogTail(actionLog.getActionKey()),throwable);
			}
		}else  if(log.isDebugEnabled()) {
			String requestLogMessage = RequestLogBuilder.responseLogMessage(actionLog.getResponseCode(), null, actionLog.getResponseData());
			log.debug(requestLogMessage);
		}
    	
    	try {	
    		if(httpStatus != 404) {
    			asyncPushLog(actionLog);
        	}
		} catch (Exception e) {
		}
    }
    
    
    public static void onSystemBackendTaskStart(String taskKey,String taskName){
    	if(!taskLogEnabled)return;
    	ActionLog actionLog = new ActionLog();
		actionLog.setAppId(GlobalRuntimeContext.APPID);
		actionLog.setEnv(GlobalRuntimeContext.ENV);
		actionLog.setRequestAt(new Date());
		actionLog.setRequestId(TokenGenerator.generate());
		actionLog.setActionName(taskName);
		actionLog.setActionKey(taskKey);
		actionLog.setUserId(TIMER_TASK);
		actionLog.setUserName(TIMER_TASK_ALIAS);
		actionLog.setTenantId(CurrentRuntimeContext.getTenantId());
		ThreadLocalContext.set(CURRENT_LOG_CONTEXT_NAME, actionLog);
		ThreadContext.put(LogConstants.LOG_CONTEXT_REQUEST_ID, actionLog.getRequestId());
	}
    
    public static void onSystemBackendTaskEnd(Throwable throwable){
    	ActionLog actionLog = ThreadLocalContext.get(CURRENT_LOG_CONTEXT_NAME);
    	if(actionLog == null)return;
    	try {	
    		actionLog.setResponseCode(throwable == null ? 200 : 500);
    		actionLog.setResponseAt(new Date());
    		if(throwable != null) {
    			actionLog.setExceptions(ExceptionUtils.getMessage(throwable));
    		}
    		//send to logserver 
    		asyncPushLog(actionLog);
		} catch (Exception e) {
		}
    }
    
    public static ActionLog currentActionLog() {
    	return ThreadLocalContext.get(CURRENT_LOG_CONTEXT_NAME);
    }
    

    private static void asyncPushLog(ActionLog actionLog){
        if(asyncSendExecutor == null)return;
		asyncSendExecutor.execute(new Runnable() {
			@Override
			public void run() {
				logStorageProvider.storage(actionLog);
			}
		});
    }

    public static void destroy(){
    	if(asyncSendExecutor != null) {    		
    		asyncSendExecutor.shutdown();
    	}
    }
    
}
