package test;

import app.Models.U1467085Bid;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class U1467085BidTest {
    static PublicKey publicKey;
    static PrivateKey privateKey;
    private final String owner = "testOwner";
    private final Integer bid = 100;
    private final UUID adId = UUID.randomUUID();

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
    public void testBidSetUp() {
        U1467085Bid testBid = new U1467085Bid(adId, owner, bid);
        assertEquals(owner, testBid.bidderUserName);
        assertEquals(bid, testBid.bid);
        assertEquals(adId, testBid.adId);
        assertNotNull(testBid.id);
    }

    @Test
    public void testSignatureUsername() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085Bid testBid = new U1467085Bid(adId, owner, bid);
        testBid.signObject(privateKey);
        testBid.bidderUserName = owner + "new";
        assertFalse(testBid.verifySignature(publicKey));
    }

    @Test
    public void testSignatureAdId() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085Bid testBid = new U1467085Bid(adId, owner, bid);
        testBid.signObject(privateKey);
        testBid.adId = UUID.randomUUID();
        assertFalse(testBid.verifySignature(publicKey));
    }

    @Test
    public void testSignatureBid() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085Bid testBid = new U1467085Bid(adId, owner, bid);
        testBid.signObject(privateKey);
        testBid.bid = bid + 1;
        assertFalse(testBid.verifySignature(publicKey));
    }

    @Test
    public void testSignature() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085Bid testBid = new U1467085Bid(adId, owner, bid);
        testBid.signObject(privateKey);
        assertTrue(testBid.verifySignature(publicKey));
    }

}