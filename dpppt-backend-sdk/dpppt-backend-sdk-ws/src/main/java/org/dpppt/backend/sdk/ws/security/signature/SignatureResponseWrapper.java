package org.dpppt.backend.sdk.ws.security.signature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.util.Base64;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.dpppt.backend.sdk.ws.util.ByteArrayHelper;
import org.joda.time.DateTime;
import org.springframework.util.Base64Utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class SignatureResponseWrapper extends HttpServletResponseWrapper {

    //after 21 days the list and hence the signature is invalid
    public static final int RETENTION_PERIOD = 21;

    private MessageDigest digest;
    private ByteArrayOutputStream output;
    private HashStream stream;
    private PrintWriter writer;
    private KeyPair pair;
    public SignatureResponseWrapper(HttpServletResponse response, KeyPair pair) {
        super(response);
        this.pair = pair;
        try {
            this.output = new ByteArrayOutputStream(response.getBufferSize());
            this.digest = MessageDigest.getInstance("SHA-256");
            this.stream = new HashStream(this.digest, this.output);
        } catch (Exception ex) {

        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if(stream == null) {
            stream = new HashStream(this.digest, this.output);
        }
        return stream;
    }

    public byte[] getHash() throws IOException {
        return this.stream.getHash();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (output != null) {
            throw new IllegalStateException(
                    "getOutputStream() has already been called on this response.");
        }

        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(this.output,
                    getCharacterEncoding()));
        }

        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        this.setSignature();
        super.flushBuffer();
        if (writer != null) {
            writer.flush();
        } else if (output != null) {
            output.flush();
        }
    }

    public void outputData(OutputStream httpOutput) throws IOException{
        this.setSignature();
        httpOutput.write(this.output.toByteArray());
    }


    private void setSignature() throws IOException {
        byte[] theHash = this.getHash();
        
        Claims claims = Jwts.claims();
        claims.put("content-hash", Base64.getEncoder().encodeToString(theHash));
        claims.put("hash-alg", "sha-256");
        claims.setIssuer("d3pt");
        claims.setIssuedAt(DateTime.now().toDate());
        claims.setExpiration(DateTime.now().plusDays(RETENTION_PERIOD).toDate());
        String signature = Jwts.builder()
                .setClaims(claims)
                .signWith(pair.getPrivate())
            .compact();

        this.setHeader("Digest", "SHA-256:" + ByteArrayHelper.bytesToHex(theHash));
        this.setHeader("Public-Key", getPublicKeyAsPEM());
        this.setHeader("Signature", signature);
       
    }

    private String getPublicKeyAsPEM() throws IOException{
        StringWriter writer = new StringWriter();
        PemWriter pemWriter = new PemWriter(writer);
        pemWriter.writeObject(new PemObject("PUBLIC KEY", pair.getPublic().getEncoded()));
        pemWriter.flush();
        pemWriter.close();
        return Base64Utils.encodeToUrlSafeString(writer.toString().trim().getBytes());
    }
    
    private class HashStream extends ServletOutputStream {

        private MessageDigest digest;
        private ByteArrayOutputStream  output;

        public HashStream(MessageDigest digest, ByteArrayOutputStream outputStream) {
            this.digest = digest;
            this.output  = outputStream;
        }
        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener listener) {

        }

        @Override
        public void write(int b) throws IOException {
            this.digest.update((byte)b);
            this.output.write(b);
        }

        @Override
        public void close() throws IOException{
            this.output.close();
        }
        public byte[] getHash() throws IOException{
            return this.digest.digest();
        }   
    }

}