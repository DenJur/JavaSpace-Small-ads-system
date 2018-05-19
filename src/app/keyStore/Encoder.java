package app.keyStore;

import org.bouncycastle.asn1.bc.BCObjectIdentifiers;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

public class Encoder {

    /**
     * @param password Password for key encryption
     * @param key      Private key that should be encrypted
     * @return encrypted private key bytes
     */
    public static byte[] encryptPrivateKey(String password, PrivateKey key) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, InvalidAlgorithmParameterException, NoSuchPaddingException, BadPaddingException, InvalidParameterSpecException, NoSuchProviderException, IllegalBlockSizeException {
        int count = 100000;
        return encryptPrivateKey(password, key, count);
    }

    /**
     * @param password Password for key encryption
     * @param key      Private key that should be encrypted
     * @param count    Number of iterations for encryption(more better)
     * @return encrypted private key bytes
     */
    public static byte[] encryptPrivateKey(String password, PrivateKey key, int count) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidParameterSpecException, IOException {
        byte[] encodedPrivKey = key.getEncoded();

        String encAlg = BCObjectIdentifiers.bc_pbe_sha256_pkcs12_aes256_cbc.getId();
        // Generate random salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        // Create PBE parameter set
        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance(encAlg, "BC");
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

        Cipher pbeCipher = Cipher.getInstance(encAlg, "BC");

        // Initialize PBE Cipher with key and parameters
        pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);

        // Encrypt the encoded Private Key with the PBE key
        byte[] ciphertext = pbeCipher.doFinal(encodedPrivKey);

        // Now construct  PKCS #8 EncryptedPrivateKeyInfo object
        AlgorithmParameters algparms = AlgorithmParameters.getInstance(encAlg, "BC");
        algparms.init(pbeParamSpec);
        EncryptedPrivateKeyInfo encinfo = new EncryptedPrivateKeyInfo(algparms, ciphertext);

        // Pencoded PKCS#8 encrypted key
        return encinfo.getEncoded();
    }

    public static String encodePubkeyToBase64(PublicKey key) {
        return new String(Base64.encode(key.getEncoded()));
    }

}
