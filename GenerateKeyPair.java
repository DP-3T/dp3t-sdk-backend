import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.security.spec.ECGenParameterSpec;

import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Application{
    public static void main(String[] args) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		Security.setProperty("crypto.policy", "unlimited");
		KeyPairGenerator generator =  KeyPairGenerator.getInstance("ECDSA", "BC");
		ECGenParameterSpec spec = new ECGenParameterSpec("secp256r1");
		generator.initialize(spec);
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