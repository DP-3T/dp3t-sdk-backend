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
	
		SignatureResponseWrapper wrapper = new SignatureResponseWrapper(httpResponse, pair, retentionDays,
				protectedHeaders, setDebugHeaders);
		chain.doFilter(request, wrapper);
		try{
			request.getAsyncContext().addListener(new AsyncListener() {
				@Override
				public void onComplete(AsyncEvent event) throws IOException {
					wrapper.outputData(httpResponse.getOutputStream());
				}

				/// We ignore this function, since the signature is anyways only valid if the request succeeds.
				@Override
				public void onTimeout(AsyncEvent event) throws IOException {
					
				}
				/// We ignore this function, since the signature is anyways only valid if the request succeeds.
				@Override
				public void onError(AsyncEvent event) throws IOException {
					
				}
				/// We ignore this function. We don't need any preparation.
				@Override
				public void onStartAsync(AsyncEvent event) throws IOException {
					
				}
			});
		}
		catch(Exception ex) {
			wrapper.outputData(httpResponse.getOutputStream());
		}
	}
}