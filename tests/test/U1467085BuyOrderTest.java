package test;

import app.Models.U1467085BuyOrder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class U1467085BuyOrderTest {

    static PublicKey publicKey;
    static PrivateKey privateKey;
    private final UUID adId = UUID.randomUUID();
    private final String owner = "testOwner";

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
    public void testBuyOrderSetUp() {
        U1467085BuyOrder testBuyOder = new U1467085BuyOrder(adId, owner);
        assertEquals(owner, testBuyOder.buyerUserName);
        assertEquals(adId, testBuyOder.adId);
    }

    @Test
    public void testSignatureAdId() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085BuyOrder testBuyOder = new U1467085BuyOrder(adId, owner);
        testBuyOder.signObject(privateKey);
        testBuyOder.adId = UUID.randomUUID();
        assertFalse(testBuyOder.verifySignature(publicKey));
    }

    @Test
    public void testSignatureBuyerUsername() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085BuyOrder testBuyOder = new U1467085BuyOrder(adId, owner);
        testBuyOder.signObject(privateKey);
        testBuyOder.buyerUserName = owner + "new";
        assertFalse(testBuyOder.verifySignature(publicKey));
    }

    @Test
    public void testSignature() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085BuyOrder testBuyOder = new U1467085BuyOrder(adId, owner);
        testBuyOder.signObject(privateKey);
        assertTrue(testBuyOder.verifySignature(publicKey));
    }

}