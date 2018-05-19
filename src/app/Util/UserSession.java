package app.Util;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Contains username and keypair for currently logged in user.
 */
public class UserSession {

    private static UserSession instance;
    public String username;
    public PrivateKey privKey;
    public PublicKey pubKey;

    private UserSession() {
    }

    public static UserSession getInstance() {
        if (instance == null)
            instance = new UserSession();
        return instance;
    }

    public void setup(String username, PrivateKey privateKey, PublicKey publicKey) {
        this.username = username;
        this.privKey = privateKey;
        this.pubKey = publicKey;
    }
}
