package app.Models;

import net.jini.core.entry.Entry;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.security.*;

/**
 * Extension of Apache River Entry interface and allows signing object content.
 * Signing is performed by first calculating object hash and then signing that hash.
 */
public abstract class SignedEntry implements Entry, Serializable {
    public byte[] signature;

    /**
     * Creates a unique signature for an object using RSA private key.
     *
     * @param key - private RSA key used for object signing
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    public void signObject(PrivateKey key) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        //clean old signature first
        this.signature = null;
        Signature sign = Signature.getInstance("SHA256withRSA", "BC");
        sign.initSign(key);
        sign.update(hashMe());
        this.signature = sign.sign();
    }

    /**
     * Implements alternative hashing method to be used for object signing.
     * If some fields need to be ignored this method should be overridden accordingly.
     * It can return either hash of the object or whole object serialized to byte[].
     * Performance implications should be considered if the whole object is to be signed.
     *
     * @return byte array that captures object content that needs to be signed
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     */
    protected byte[] hashMe() throws NoSuchProviderException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256", "BC");
        digest.update(SerializationUtils.serialize(this));
        return digest.digest();
    }

    /**
     * Verifies that object content was not changed and that it is signed with correct key
     *
     * @param key - RSA public key used for signature verification
     * @return - true if signature matches
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    public boolean verifySignature(PublicKey key) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        if (signature == null) return false;
        byte[] tmpSig = this.signature;
        this.signature = null;
        Signature ver = Signature.getInstance("SHA256withRSA", "BC");
        ver.initVerify(key);
        ver.update(hashMe());
        return ver.verify(tmpSig);
    }
}
