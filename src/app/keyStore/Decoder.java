package app.keyStore;

import org.bouncycastle.util.encoders.Base64;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Decoder {

    public static PrivateKey decryptKey(String password, byte[] encryptedKey) throws
            IOException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException {
        // this is a encoded PKCS#8 encrypted private key
        EncryptedPrivateKeyInfo ePKInfo = new EncryptedPrivateKeyInfo(encryptedKey);

        // first we have to read algorithm name and parameters (salt, iterations) used
        // to encrypt the file
        Cipher cipher = Cipher.getInstance(ePKInfo.getAlgName());
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());

        SecretKeyFactory skFac = SecretKeyFactory.getInstance(ePKInfo.getAlgName());
        Key pbeKey = skFac.generateSecret(pbeKeySpec);

        // Extract the iteration count and the salt
        AlgorithmParameters algParams = ePKInfo.getAlgParameters();
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, algParams);

        // Decrypt the encryped private key into a PKCS8EncodedKeySpec
        KeySpec pkcs8KeySpec = ePKInfo.getKeySpec(cipher);

        // Now retrieve the RSA Private key by using an RSA key factory.
        KeyFactory rsaKeyFac = KeyFactory.getInstance("RSA");
        return rsaKeyFac.generatePrivate(pkcs8KeySpec);
    }

    public static PublicKey decodePubkeyBase64(String key) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] encoded = Base64.decode(key);
        X509EncodedKeySpec spec =
                new X509EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
        return kf.generatePublic(spec);
    }
}
