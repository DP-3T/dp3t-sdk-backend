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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class CryptoProvider {

  // private final PrivateKey privateKey;
  // private final X509Certificate publicKey;
  private final KeyStore keyStore;
  private final String alias;
  private final PrivateKey privateKey;
  private final X509Certificate certificate;

  /** Creates a CryptoProvider */
  public CryptoProvider(String signClientCertificate, String signClientCertificatePassword)
      throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException,
          UnrecoverableKeyException {
    try (var byteInputStream =
        new ByteArrayInputStream(Base64.getDecoder().decode(signClientCertificate))) {
      this.keyStore = KeyStore.getInstance("PKCS12");
      this.keyStore.load(byteInputStream, signClientCertificatePassword.toCharArray());
      this.alias = keyStore.aliases().nextElement();
      this.privateKey =
          (PrivateKey) keyStore.getKey(this.alias, signClientCertificatePassword.toCharArray());
      this.certificate = (X509Certificate) keyStore.getCertificate(alias);
    }
  }

  /** Creates a CryptoProvider */
  public CryptoProvider(
      String signClientCertificate, String signClientCertificatePassword, String alias)
      throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException,
          UnrecoverableKeyException {
    try (var byteInputStream =
        new ByteArrayInputStream(Base64.getDecoder().decode(signClientCertificate))) {
      this.keyStore = KeyStore.getInstance("PKCS12");
      this.keyStore.load(byteInputStream, signClientCertificatePassword.toCharArray());
      this.alias = alias;
      privateKey =
          (PrivateKey) keyStore.getKey(this.alias, signClientCertificatePassword.toCharArray());
      this.certificate = (X509Certificate) keyStore.getCertificate(alias);
    }
  }

  public X509Certificate getCertificate() {
    return this.certificate;
  }

  /**
   * Returns the {@link PrivateKey} configured in the application properties.
   *
   * @return private key
   */
  public PrivateKey getPrivateKey() {
    return this.privateKey;
  }
}
