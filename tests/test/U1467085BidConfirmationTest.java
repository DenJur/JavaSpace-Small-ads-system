package test;

import app.Models.U1467085BidConfirmation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class U1467085BidConfirmationTest {

    static PublicKey publicKey;
    static PrivateKey privateKey;
    private final UUID adId = UUID.randomUUID();
    private final UUID bidId = UUID.randomUUID();

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
    public void testBidConfirmationSetUp() {
        U1467085BidConfirmation testConfirmation = new U1467085BidConfirmation(adId, bidId);
        assertEquals(bidId, testConfirmation.bidId);
        assertEquals(adId, testConfirmation.adId);
    }

    @Test
    public void testSignatureAdId() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085BidConfirmation testConfirmation = new U1467085BidConfirmation(adId, bidId);
        testConfirmation.signObject(privateKey);
        testConfirmation.adId = UUID.randomUUID();
        assertFalse(testConfirmation.verifySignature(publicKey));
    }

    @Test
    public void testSignatureBidId() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085BidConfirmation testConfirmation = new U1467085BidConfirmation(adId, bidId);
        testConfirmation.signObject(privateKey);
        testConfirmation.bidId = UUID.randomUUID();
        assertFalse(testConfirmation.verifySignature(publicKey));
    }

    @Test
    public void testSignature() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085BidConfirmation testConfirmation = new U1467085BidConfirmation(adId, bidId);
        testConfirmation.signObject(privateKey);
        assertTrue(testConfirmation.verifySignature(publicKey));
    }

}