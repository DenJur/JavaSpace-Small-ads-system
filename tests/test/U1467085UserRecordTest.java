package test;

import app.Models.U1467085UserRecord;
import app.keyStore.Decoder;
import app.keyStore.Encoder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class U1467085UserRecordTest {

    static PublicKey publicKey;
    private final String owner = "testOwner";

    @BeforeAll
    public static void setUp() throws NoSuchAlgorithmException, NoSuchProviderException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        generator.initialize(2048, random);
        KeyPair pair = generator.generateKeyPair();
        publicKey = pair.getPublic();
    }

    @Test
    public void testUserRecordSetUp() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        U1467085UserRecord testComment = new U1467085UserRecord(owner, Encoder.encodePubkeyToBase64(publicKey));
        assertEquals(owner, testComment.userName);
        assertEquals(publicKey, Decoder.decodePubkeyBase64(testComment.pubKey));
    }

}