/*
 * Copyright 2016-2020 www.mendmix.com.
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
package com.mendmix.gateway.filter;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;

import com.mendmix.gateway.GatewayConfigs;
import com.mendmix.gateway.GatewayConstants;
import com.mendmix.gateway.filter.post.ResponseLogHandler;
import com.mendmix.gateway.filter.post.ResponseRewriteHandler;
import com.mendmix.gateway.filter.post.RewriteBodyServerHttpResponse;
import com.mendmix.gateway.helper.RequestContextHelper;
import com.mendmix.gateway.model.BizSystemModule;

import reactor.core.publisher.Mono;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年4月7日
 */
public abstract class AbstracResponseFilter implements GlobalFilter, Ordered, InitializingBean  {

	static Logger logger = LoggerFactory.getLogger("com.mendmix.gateway");
	
	//private GatewayFilter delegate;

	//@Autowired
	//private ModifyResponseBodyGatewayFilterFactory modifyResponseBodyGatewayFilterFactory;
	
	public AbstracResponseFilter(PostFilterHandler...filterHandlers) {
		
		List<PostFilterHandler> handlers = new ArrayList<>();
		if(GatewayConfigs.actionLogEnabled) {
			handlers.add(new ResponseLogHandler());
		}
		//
		if(GatewayConfigs.respRewriteEnabled) {
			handlers.add(new ResponseRewriteHandler());
		}

		boolean has = filterHandlers != null && filterHandlers.length > 0 && filterHandlers[0] != null;
		if(has) {
			for (PostFilterHandler filterHandler : filterHandlers) {
				handlers.add(filterHandler);
			}
		}
		
		RewriteBodyServerHttpResponse.setHandlers(handlers);
	}
	
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    	//
    	if(exchange.getAttribute(GatewayConstants.CONTEXT_IGNORE_FILTER) != null) {
    		return chain.filter(exchange);
    	}
    	
    	if(RequestContextHelper.isWebSocketRequest(exchange.getRequest())) {
    		return chain.filter(exchange);
    	}
    	
    	//
    	BizSystemModule module = RequestContextHelper.getCurrentModule(exchange);
    	RewriteBodyServerHttpResponse newResponse = new RewriteBodyServerHttpResponse(exchange,module);
    	return chain.filter(exchange.mutate().response(newResponse).build()).then(Mono.fromRunnable(() -> {
			Long start = exchange.getAttribute(GatewayConstants.CONTEXT_REQUEST_START_TIME);
			if (logger.isDebugEnabled() && start != null) {
				logger.debug("MENDMIX-TRACE-LOGGGING-->> request_time_trace -> uri:{},useTime:{} ms" ,exchange.getRequest().getPath().value(),(System.currentTimeMillis() - start));
			}
		}));
    }

    @Override
    public int getOrder() {
    	return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1;
    }
    
    @Override
	public void afterPropertiesSet() throws Exception {
//    	delegate = modifyResponseBodyGatewayFilterFactory.apply(
//    			new ModifyResponseBodyGatewayFilterFactory.Config()
//				  .setRewriteFunction(new BodyRewriteFunction()) //
//				  .setInClass(byte[].class)   //
//				  .setOutClass(byte[].class)  //
//				);
		
	}
	
}