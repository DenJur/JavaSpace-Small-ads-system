package app.Models;

import net.jini.core.entry.Entry;

/**
 * Used to store users public key in Javaspace so that others can verify object signatures.
 */
public class U1467085UserRecord implements Entry {
    public String userName;
    public String pubKey;

    public U1467085UserRecord() {
    }

    public U1467085UserRecord(String user, String key) {
        userName = user;
        pubKey = key;
    }
}
