package org.dpppt.backend.sdk.ws.security.signature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.MessageDigest;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.dpppt.backend.sdk.ws.util.ByteArrayHelper;

public class SignatureResponseWrapper extends HttpServletResponseWrapper {

    private MessageDigest digest;
    private ByteArrayOutputStream output;
    private HashStream stream;
    private PrintWriter writer;
    public SignatureResponseWrapper(HttpServletResponse response) {
        super(response);
        try {
            this.output = new ByteArrayOutputStream(response.getBufferSize());
            this.digest = MessageDigest.getInstance("SHA-256");
            this.stream = new HashStream(this.digest, this.output);
            this.setHeader("test2", "value2");
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
        this.setHeader("Digest", "SHA-256:" + ByteArrayHelper.bytesToHex(this.getHash()));
        super.flushBuffer();
        if (writer != null) {
            writer.flush();
        } else if (output != null) {
            output.flush();
        }
    }

    public void outputData(OutputStream httpOutput) throws IOException{
        this.setHeader("Digest", "SHA-256:" + ByteArrayHelper.bytesToHex(this.getHash()));
        httpOutput.write(this.output.toByteArray());
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