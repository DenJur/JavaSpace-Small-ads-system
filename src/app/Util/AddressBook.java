package app.Util;

import app.Errors.ErrorHandler;
import app.Errors.ErrorTypes;
import app.Models.U1467085UserRecord;
import app.keyStore.Decoder;
import net.jini.space.JavaSpace05;

import java.security.PublicKey;
import java.util.HashMap;

/**
 * Retrieves user records based on username and then stores them for runtime of application.
 */
public class AddressBook {
    private static HashMap<String, PublicKey> userList = new HashMap<>();

    public static PublicKey getUserKey(String username) {
        if (userList.containsKey(username)) {
            return userList.get(username);
        }
        try {
            JavaSpace05 js = SpaceUtils.getSpace();
            U1467085UserRecord userRecordTemplate = new U1467085UserRecord();
            userRecordTemplate.userName = username;
            U1467085UserRecord record = (U1467085UserRecord) js.readIfExists(userRecordTemplate, null, SpaceUtils.TIMEOUT);
            if (record != null) {
                PublicKey publicKeyDecoded = Decoder.decodePubkeyBase64(record.pubKey);
                userList.put(record.userName, publicKeyDecoded);
                return publicKeyDecoded;
            }
        } catch (Exception e) {
            ErrorHandler handler = ErrorHandler.getInstance();
            handler.addError(e, ErrorTypes.COMMON);
            handler.showErrorDialog(ErrorTypes.COMMON);
        }
        return null;
    }
}
