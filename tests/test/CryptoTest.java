package test;

import app.keyStore.Decoder;
import app.keyStore.Encoder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CryptoTest {

    static PublicKey publicKey;
    static PrivateKey privateKey;
    private final String password = "pass";

    @BeforeAll
    public static void setUp() throws NoSuchAlgorithmException, NoSuchProviderException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        generator.initialize(2048, random);
        KeyPair pair = generator.generateKeyPair();
        publicKey = pair.getPublic();
        privateKey = pair.getPrivate();

    }

    @Test
    void encryptDecryptPrivateKey() throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException, BadPaddingException, InvalidParameterSpecException, InvalidKeySpecException, IllegalBlockSizeException {
        byte[] encrypted = Encoder.encryptPrivateKey(password, privateKey);
        assertNotEquals(privateKey.getEncoded(), encrypted);
        PrivateKey decrypted = Decoder.decryptKey(password, encrypted);
        assertEquals(privateKey, decrypted);
    }

    @Test
    void encodeDecodePubkey() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        String encoded = Encoder.encodePubkeyToBase64(publicKey);
        PublicKey decoded = Decoder.decodePubkeyBase64(encoded);
        assertEquals(publicKey, decoded);
    }
}