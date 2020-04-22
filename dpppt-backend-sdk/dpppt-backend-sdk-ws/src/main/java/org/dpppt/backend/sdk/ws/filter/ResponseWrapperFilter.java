package org.dpppt.backend.sdk.ws.filter;

import java.io.IOException;
import java.security.KeyPair;
import java.security.Security;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.dpppt.backend.sdk.ws.security.signature.SignatureResponseWrapper;

public class ResponseWrapperFilter implements Filter {
    private KeyPair pair;

    public ResponseWrapperFilter(KeyPair pair) {
        Security.addProvider(new BouncyCastleProvider());
        Security.setProperty("crypto.policy", "unlimited");
        this.pair = pair;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        SignatureResponseWrapper wrapper = new SignatureResponseWrapper(httpResponse, pair);
        chain.doFilter(request, wrapper);
        wrapper.outputData(httpResponse.getOutputStream());
    }

}