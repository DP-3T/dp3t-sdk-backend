/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.interops.syncer.efgs.signing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

public class CryptoProvider {

  private final PrivateKey privateKey;
  private final X509Certificate publicKey;

  /** Creates a CryptoProvider, using {@link BouncyCastleProvider}. */
  public CryptoProvider(String signClientCert, String signClientCertPassword)
      throws CertificateException {
    privateKey = loadPrivateKey(signClientCertPassword);
    publicKey = loadPublicKey(signClientCert);
    Security.addProvider(new BouncyCastleProvider());
  }

  private static PrivateKey getPrivateKeyFromStream(InputStream privateKeyStream)
      throws IOException {
    PEMParser pemParser = new PEMParser(new InputStreamReader(privateKeyStream));
    JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
    var parsedObject = pemParser.readObject();
    if (parsedObject instanceof PEMKeyPair) {
      return converter.getPrivateKey(((PEMKeyPair) parsedObject).getPrivateKeyInfo());
    } else {
      PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(parsedObject);
      return converter.getPrivateKey(privateKeyInfo);
    }
  }

  private X509Certificate loadPublicKey(String signClientCert) throws CertificateException {
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    try (InputStream publicKeyStream = new ByteArrayInputStream(signClientCert.getBytes())) {
      return (X509Certificate) cf.generateCertificate(publicKeyStream);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load public key", e);
    }
  }

  public X509Certificate getCertificate() {
    return this.publicKey;
  }

  /**
   * Returns the {@link PrivateKey} configured in the application properties.
   *
   * @return private key
   */
  public PrivateKey getPrivateKey() {
    return privateKey;
  }

  private PrivateKey loadPrivateKey(String signClientCertPassword) {
    try (InputStream privateKeyStream =
        new ByteArrayInputStream(signClientCertPassword.getBytes())) {
      return getPrivateKeyFromStream(privateKeyStream);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load private key", e);
    }
  }
}
