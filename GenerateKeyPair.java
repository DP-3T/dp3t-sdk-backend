/**
 * This file only serves as an example on how to get keys in the right encoding. This outputs the
 * keys in the PKCS8 format for the private key and the X509 format for the publickey
 *
 * <p>DO NOT USE THEM IN PRODUCTION UNLESS THE KEYSPECS ARE OK FOR YOU
 */
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Base64;

public class GenerateKeyPair {
  public static void main(String[] args) throws Exception {
    Security.setProperty("crypto.policy", "unlimited");
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    KeyPair pair = generator.genKeyPair();
    PrivateKey privateKey = pair.getPrivate();
    PublicKey publicKey = pair.getPublic();
    FileOutputStream outputStream = new FileOutputStream("generated_pub.pem");
    outputStream.write(Base64.getEncoder().encode(publicKey.getEncoded()));
    outputStream.close();

    outputStream = new FileOutputStream("generated_private.pem");
    outputStream.write(Base64.getEncoder().encode(privateKey.getEncoded()));
    outputStream.close();
  }
}
