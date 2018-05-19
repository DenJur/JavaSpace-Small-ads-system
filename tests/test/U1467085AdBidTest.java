package test;

import app.Models.U1467085AdBid;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class U1467085AdBidTest {
    static PublicKey publicKey;
    static PrivateKey privateKey;
    private final String owner = "testOwner";
    private final String title = "testTitle";
    private final String description = "testDescription";
    private final Integer price = 100;
    private final ZonedDateTime endDate = ZonedDateTime.now();

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
    public void testBidAdSetUp() {
        U1467085AdBid testBid = new U1467085AdBid(owner, title, description, price, endDate);
        assertEquals(owner, testBid.ownerUsername);
        assertEquals(title, testBid.title);
        assertEquals(description, testBid.description);
        assertEquals(price, testBid.price);
        assertEquals(endDate, ZonedDateTime.ofInstant(Instant.ofEpochMilli(testBid.endDate), ZoneId.systemDefault()));
        assertNotNull(testBid.id);
    }

    @Test
    public void testSignatureTitle() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085AdBid testBid = new U1467085AdBid(owner, title, description, price, endDate);
        testBid.signObject(privateKey);
        testBid.title = title + "new";
        assertFalse(testBid.verifySignature(publicKey));
    }

    @Test
    public void testSignatureDescription() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085AdBid testBid = new U1467085AdBid(owner, title, description, price, endDate);
        testBid.signObject(privateKey);
        testBid.description = description + "new";
        assertFalse(testBid.verifySignature(publicKey));
    }

    @Test
    public void testSignaturePrice() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085AdBid testBid = new U1467085AdBid(owner, title, description, price, endDate);
        testBid.signObject(privateKey);
        testBid.price = price + 1;
        assertFalse(testBid.verifySignature(publicKey));
    }

    @Test
    public void testSignatureOwner() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085AdBid testBid = new U1467085AdBid(owner, title, description, price, endDate);
        testBid.signObject(privateKey);
        testBid.ownerUsername = owner + "new";
        assertFalse(testBid.verifySignature(publicKey));
    }

    @Test
    public void testSignatureEndDate() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085AdBid testBid = new U1467085AdBid(owner, title, description, price, endDate);
        testBid.signObject(privateKey);
        testBid.endDate = 0L;
        assertFalse(testBid.verifySignature(publicKey));
    }

    @Test
    public void testSignatureId() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085AdBid testBid = new U1467085AdBid(owner, title, description, price, endDate);
        testBid.signObject(privateKey);
        testBid.id = UUID.randomUUID();
        assertFalse(testBid.verifySignature(publicKey));
    }

    @Test
    public void testSignature() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085AdBid testBid = new U1467085AdBid(owner, title, description, price, endDate);
        testBid.signObject(privateKey);
        assertTrue(testBid.verifySignature(publicKey));
    }

}