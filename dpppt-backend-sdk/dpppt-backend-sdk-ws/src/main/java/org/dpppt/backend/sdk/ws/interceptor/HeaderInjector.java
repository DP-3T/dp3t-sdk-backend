package org.dpppt.backend.sdk.ws.interceptor;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class HeaderInjector implements HandlerInterceptor {
    private final Map<String,String> headers;
    public HeaderInjector(Map<String,String> headers) {
        this.headers = headers;
    } 

    @Override
    public void postHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler, 
      ModelAndView modelAndView) throws Exception {
        if(headers != null){
            for(var header : headers.keySet()){
                response.setHeader(header, headers.get(header));
            }
		}
      }
}