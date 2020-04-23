/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.filter;

import java.io.IOException;
import java.security.KeyPair;
import java.security.Security;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.dpppt.backend.sdk.ws.security.signature.SignatureResponseWrapper;

public class ResponseWrapperFilter implements Filter {

	private final KeyPair pair;
	private final int retentionDays;
    private final List<String> protectedHeaders;

	public ResponseWrapperFilter(KeyPair pair, int retentionDays, List<String> protectedHeaders) {
		Security.addProvider(new BouncyCastleProvider());
		Security.setProperty("crypto.policy", "unlimited");
		this.pair = pair;
        this.retentionDays = retentionDays;
        this.protectedHeaders = protectedHeaders;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		SignatureResponseWrapper wrapper = new SignatureResponseWrapper(httpResponse, pair, retentionDays, protectedHeaders);
		chain.doFilter(request, wrapper);
		wrapper.outputData(httpResponse.getOutputStream());
	}

}