package test;

import app.Models.U1467085Comment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class U1467085CommentTest {

    static PublicKey publicKey;
    static PrivateKey privateKey;
    private final UUID adId = UUID.randomUUID();
    private final String owner = "testOwner";
    private final String comment = "testComment";

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
    public void testCommentSetUp() {
        U1467085Comment testComment = new U1467085Comment(adId, owner, comment);
        assertEquals(owner, testComment.userName);
        assertEquals(adId, testComment.adId);
        assertEquals(comment, testComment.comment);
    }

    @Test
    public void testSignatureAdId() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085Comment testComment = new U1467085Comment(adId, owner, comment);
        testComment.signObject(privateKey);
        testComment.adId = UUID.randomUUID();
        assertFalse(testComment.verifySignature(publicKey));
    }

    @Test
    public void testSignatureUserName() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085Comment testComment = new U1467085Comment(adId, owner, comment);
        testComment.signObject(privateKey);
        testComment.userName = owner + "new";
        assertFalse(testComment.verifySignature(publicKey));
    }

    @Test
    public void testSignatureComment() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085Comment testComment = new U1467085Comment(adId, owner, comment);
        testComment.signObject(privateKey);
        testComment.comment = comment + "new";
        assertFalse(testComment.verifySignature(publicKey));
    }

    @Test
    public void testSignatureTime() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085Comment testComment = new U1467085Comment(adId, owner, comment);
        testComment.signObject(privateKey);
        testComment.date = 0L;
        assertFalse(testComment.verifySignature(publicKey));
    }

    @Test
    public void testSignature() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        U1467085Comment testComment = new U1467085Comment(adId, owner, comment);
        testComment.signObject(privateKey);
        assertTrue(testComment.verifySignature(publicKey));
    }

}