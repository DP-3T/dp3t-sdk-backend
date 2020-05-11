package org.dpppt.backend.sdk.ws.security.signature;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;

import com.google.protobuf.ByteString;

import org.dpppt.backend.sdk.model.gaen.proto.TemporaryExposureKeyFormat;
import org.dpppt.backend.sdk.model.gaen.proto.TemporaryExposureKeyFormat.SignatureInfo;

public class ProtoSignature {
    private final String algorithm;
    private final KeyPair keyPair;
    private final String appBundleId;
    private final String apkPackage;
    private final String keyVersion;
    private final String keyVerificationId;
    
    public ProtoSignature(String algorithm, KeyPair keyPair, String appBundleId, String apkPackage, String keyVersion, String keyVerificationId) {
        this.keyPair = keyPair;
        this.algorithm = algorithm.trim();
        this.appBundleId = appBundleId;
        this.apkPackage = apkPackage;
        this.keyVerificationId = keyVerificationId;
        this.keyVersion = keyVersion;
    }

    private byte[] sign(byte[] data) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {
        Signature signature = Signature.getInstance(algorithm);
        signature.initSign(keyPair.getPrivate());
        signature.update(data);
        return signature.sign();
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public TemporaryExposureKeyFormat.TEKSignatureList getSignatureObject(byte[] keyExport, SignatureInfo tekSignature)
            throws InvalidKeyException, SignatureException, NoSuchAlgorithmException {
        byte[] exportSignature = sign(keyExport);
        var signatureList = TemporaryExposureKeyFormat.TEKSignatureList.newBuilder();
        var theSignature = TemporaryExposureKeyFormat.TEKSignature.newBuilder();
        theSignature.setSignatureInfo(tekSignature)
                    .setSignature(ByteString.copyFrom(exportSignature))
                    .setBatchNum(1)
                    .setBatchSize(1);
        signatureList.addSignatures(theSignature);
        return signatureList.build();
    }

    public SignatureInfo getSignatureInfo() {
        var tekSignature = TemporaryExposureKeyFormat.SignatureInfo.newBuilder();
        tekSignature.setAppBundleId(appBundleId)
                    .setAndroidPackage(apkPackage)
                    .setVerificationKeyVersion(keyVersion)
                    .setVerificationKeyId(keyVerificationId)
                    .setSignatureAlgorithm(algorithm);
        return tekSignature.build();
    }
    
}