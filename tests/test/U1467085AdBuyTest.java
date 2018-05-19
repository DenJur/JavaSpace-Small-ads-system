package test;

import app.Models.U1467085AdBuy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class U1467085AdBuyTest {
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
    public void testBuyAdSetUp() {
        U1467085AdBuy testBuy = new U1467085AdBuy(owner, title, description, price, endDate);
        assertEquals(owner, testBuy.ownerUsername);
        assertEquals(title, testBuy.title);
        assertEquals(description, testBuy.description);
        assertEquals(price, testBuy.price);
        assertEquals(endDate, ZonedDateTime.ofInstant(Instant.ofEpochMilli(testBuy.endDate), ZoneId.systemDefault()));
        assertNotNull(testBuy.id);
    }

    @Test
    public void testSignatureTitle() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085AdBuy testBuy = new U1467085AdBuy(owner, title, description, price, endDate);
        testBuy.signObject(privateKey);
        testBuy.title = title + "new";
        assertFalse(testBuy.verifySignature(publicKey));
    }

    @Test
    public void testSignatureDescription() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085AdBuy testBuy = new U1467085AdBuy(owner, title, description, price, endDate);
        testBuy.signObject(privateKey);
        testBuy.description = description + "new";
        assertFalse(testBuy.verifySignature(publicKey));
    }

    @Test
    public void testSignaturePrice() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085AdBuy testBuy = new U1467085AdBuy(owner, title, description, price, endDate);
        testBuy.signObject(privateKey);
        testBuy.price = price + 1;
        assertFalse(testBuy.verifySignature(publicKey));
    }

    @Test
    public void testSignatureOwner() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085AdBuy testBuy = new U1467085AdBuy(owner, title, description, price, endDate);
        testBuy.signObject(privateKey);
        testBuy.ownerUsername = owner + "new";
        assertFalse(testBuy.verifySignature(publicKey));
    }

    @Test
    public void testSignatureEndDate() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085AdBuy testBuy = new U1467085AdBuy(owner, title, description, price, endDate);
        testBuy.signObject(privateKey);
        testBuy.endDate = 0L;
        assertFalse(testBuy.verifySignature(publicKey));
    }

    @Test
    public void testSignatureId() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085AdBuy testBuy = new U1467085AdBuy(owner, title, description, price, endDate);
        testBuy.signObject(privateKey);
        testBuy.id = UUID.randomUUID();
        assertFalse(testBuy.verifySignature(publicKey));
    }

    @Test
    public void testSignature() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085AdBuy testBuy = new U1467085AdBuy(owner, title, description, price, endDate);
        testBuy.signObject(privateKey);
        assertTrue(testBuy.verifySignature(publicKey));
    }

}