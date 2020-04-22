package org.dpppt.backend.sdk.ws.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;


import org.dpppt.backend.sdk.ws.security.signature.SignatureResponseWrapper;
import org.dpppt.backend.sdk.ws.util.ByteArrayHelper;

public class ResponseWrapperFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        httpResponse.setHeader("test", "value");
        SignatureResponseWrapper wrapper = new SignatureResponseWrapper(httpResponse);
        chain.doFilter(request, wrapper);
        wrapper.outputData(httpResponse.getOutputStream());
    }

}