/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.ws.filter;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Security;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
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
	private final boolean setDebugHeaders;

	public PublicKey getPublicKey() {
		return pair.getPublic();
	}

	public ResponseWrapperFilter(KeyPair pair, int retentionDays, List<String> protectedHeaders,
			boolean setDebugHeaders) {
		Security.addProvider(new BouncyCastleProvider());
		Security.setProperty("crypto.policy", "unlimited");
		this.pair = pair;
		this.retentionDays = retentionDays;
		this.protectedHeaders = protectedHeaders;
		this.setDebugHeaders = setDebugHeaders;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		if(!request.isAsyncSupported()) {
			SignatureResponseWrapper wrapper = new SignatureResponseWrapper(httpResponse, pair, retentionDays,
			protectedHeaders, setDebugHeaders);
			chain.doFilter(request, wrapper);
			wrapper.outputData(httpResponse.getOutputStream());
		}
		else {
			SignatureResponseWrapper wrapper = new SignatureResponseWrapper(httpResponse, pair, retentionDays,
					protectedHeaders, setDebugHeaders);
			chain.doFilter(request, wrapper);
			var asyncContext = AsyncHelper.getAsyncContext(request, response);
			asyncContext.addListener(new AsyncListener() {
				@Override
				public void onComplete(AsyncEvent event) throws IOException {
					wrapper.outputData(httpResponse.getOutputStream());
				}
	
				@Override
				public void onTimeout(AsyncEvent event) throws IOException {
					
				}
	
				@Override
				public void onError(AsyncEvent event) throws IOException {
					
				}
	
				@Override
				public void onStartAsync(AsyncEvent event) throws IOException {
					
				}
			});
		}
	}
	public static class AsyncHelper {
		public static AsyncContext getAsyncContext(ServletRequest request, ServletResponse response) {
			AsyncContext asyncContext = null;
			if (request.isAsyncStarted()) {
				asyncContext = request.getAsyncContext();
			}
			else {
				asyncContext = request.startAsync(request, response);
				asyncContext.setTimeout(2000);
			}
			return asyncContext;
		}
	}
}