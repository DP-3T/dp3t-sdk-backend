/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.backend.sdk.ws.security.signature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.protobuf.ByteString;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.proto.TemporaryExposureKeyFormat;
import org.dpppt.backend.sdk.model.gaen.proto.TemporaryExposureKeyFormat.SignatureInfo;
import org.dpppt.backend.sdk.ws.util.GaenUnit;

public class ProtoSignature {
    public static final String EXPORT_MAGIC_STRING = "EK Export v1    ";
    public static final byte[] EXPORT_MAGIC = {0x45, 0x4B, 0x20, 0x45, 0x78, 0x70, 0x6F, 0x72, 0x74, 0x20, 0x76, 0x31, 0x20, 0x20, 0x20, 0x20}; //"EK Export v1    "

    private final String algorithm;
    private final KeyPair keyPair;
    private final String appBundleId;
    private final String apkPackage;
    private final String keyVersion;
    private final String keyVerificationId;
    private final String gaenRegion;
    private final Duration bucketLength;

    public Map<String, String> oidToJavaSignature = Map.of("1.2.840.10045.4.3.2", "SHA256withECDSA");

    
    public ProtoSignature(String algorithm, KeyPair keyPair, String appBundleId, String apkPackage, String keyVersion, String keyVerificationId, String gaenRegion, Duration bucketLength) {
        this.keyPair = keyPair;
        this.algorithm = algorithm.trim();
        this.appBundleId = appBundleId;
        this.apkPackage = apkPackage;
        this.keyVerificationId = keyVerificationId;
        this.keyVersion = keyVersion;
        this.gaenRegion = gaenRegion;
        this.bucketLength = bucketLength;
    }

    private byte[] sign(byte[] data) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {
        Signature signature = Signature.getInstance(oidToJavaSignature.get(algorithm));
        signature.initSign(keyPair.getPrivate());
        signature.update(data);
        return signature.sign();
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public TemporaryExposureKeyFormat.TEKSignatureList getSignatureObject(byte[] keyExport)
            throws InvalidKeyException, SignatureException, NoSuchAlgorithmException {
        byte[] exportSignature = sign(keyExport);
        var signatureList = TemporaryExposureKeyFormat.TEKSignatureList.newBuilder();
        var theSignature = TemporaryExposureKeyFormat.TEKSignature.newBuilder();
        theSignature.setSignatureInfo(tekSignature())
                    .setSignature(ByteString.copyFrom(exportSignature))
                    .setBatchNum(1)
                    .setBatchSize(1);
        signatureList.addSignatures(theSignature);
        return signatureList.build();
    }

    public SignatureInfo tekSignature() {
        var tekSignature = TemporaryExposureKeyFormat.SignatureInfo.newBuilder();
        tekSignature.setAppBundleId(appBundleId)
                    .setAndroidPackage(apkPackage)
                    .setVerificationKeyVersion(keyVersion)
                    .setVerificationKeyId(keyVerificationId)
                    .setSignatureAlgorithm(algorithm);
        return tekSignature.build();
    }

    public byte[] getPayload(Map<String, List<GaenKey>> groupedBuckets) throws IOException, InvalidKeyException, SignatureException,
    NoSuchAlgorithmException {
        ByteArrayOutputStream byteOutCollection = new ByteArrayOutputStream();
        ZipOutputStream zipCollection = new ZipOutputStream(byteOutCollection);
        
        for(var keyGroup : groupedBuckets.entrySet()) {
            var keys = keyGroup.getValue();
            var group = keyGroup.getKey();
            if(keys.isEmpty()) continue;

            var keyDate = Duration.of(keys.get(0).getRollingStartNumber(), GaenUnit.TenMinutes);
            var keyLocalDate = LocalDate.ofInstant(Instant.ofEpochMilli(keyDate.toMillis()), ZoneOffset.UTC);
            var protoFile = getProtoKey(keys, keyDate);
            var zipFileName = new StringBuilder();
            
            zipFileName.append("key_export_").append(group);
           
            zipCollection.putNextEntry(new ZipEntry(zipFileName.toString()));
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(byteOut);
           
            zip.putNextEntry(new ZipEntry("export.bin"));
            byte[] exportBin = protoFile.toByteArray();
            zip.write(EXPORT_MAGIC);
            zip.write(exportBin);
            zip.closeEntry();
        
            var signatureList = getSignatureObject(exportBin);

            byte[] exportSig = signatureList.toByteArray();
            zip.putNextEntry(new ZipEntry("export.sig"));
            zip.write(exportSig);
            zip.closeEntry();
            zip.flush();
            zip.close();
            byteOut.flush();
            byteOut.close();
            zipCollection.write(byteOut.toByteArray());
            
            zipCollection.closeEntry();
        }
        zipCollection.flush();
        zipCollection.close();
        byteOutCollection.close();
        return byteOutCollection.toByteArray();
    }
    public byte[] getPayload(Collection<List<GaenKey>> buckets) throws IOException, InvalidKeyException, SignatureException,
            NoSuchAlgorithmException {
        Map<String, List<GaenKey>> grouped = new HashMap<String, List<GaenKey>>();
        for(var keys : buckets) {
            if(keys.isEmpty()) continue;
            var keyDate = Duration.of(keys.get(0).getRollingStartNumber(), GaenUnit.TenMinutes);
            var keyLocalDate = LocalDate.ofInstant(Instant.ofEpochMilli(keyDate.toMillis()), ZoneOffset.UTC).toString();
            grouped.put(keyLocalDate, keys);
        }
       return getPayload(grouped);
    }

    private TemporaryExposureKeyFormat.TemporaryExposureKeyExport getProtoKey(List<GaenKey> exposedKeys, Duration batchReleaseTimeDuration) {
        var file = TemporaryExposureKeyFormat.TemporaryExposureKeyExport.newBuilder();
       
        var tekList = new ArrayList<TemporaryExposureKeyFormat.TemporaryExposureKey>();
        for (var key : exposedKeys) {
            var protoKey = TemporaryExposureKeyFormat.TemporaryExposureKey.newBuilder()
                    .setKeyData(ByteString.copyFrom(Base64.getDecoder().decode(key.getKeyData())))
                    .setRollingPeriod(key.getRollingPeriod()).setRollingStartIntervalNumber(key.getRollingStartNumber())
                    .setTransmissionRiskLevel(key.getTransmissionRiskLevel()).build();
            tekList.add(protoKey);
        }

        file.addAllKeys(tekList);

        file.setRegion(gaenRegion).setBatchNum(1).setBatchSize(1)
                .setStartTimestamp(batchReleaseTimeDuration.toSeconds())
                .setEndTimestamp(batchReleaseTimeDuration.toSeconds() + bucketLength.toSeconds());

        file.addSignatureInfos(tekSignature());

        return file.build();
    }
    
}