package app.Controllers;

import app.Errors.CommonException;
import app.Errors.ErrorTypes;
import app.Errors.FatalException;
import app.Models.U1467085UserRecord;
import app.Util.SpaceUtils;
import app.Util.UserSession;
import app.keyStore.Decoder;
import app.keyStore.Encoder;
import app.notification.NotificationUtil;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.transaction.TransactionException;
import net.jini.space.JavaSpace05;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

public class LoginController extends ControllerBase implements EventHandler<MouseEvent> {

    public TextField username;
    public TextField password;
    public Button loginButton;
    public Button gotoRegister;

    @FXML
    public void initialize() {
        gotoRegister.setOnMouseClicked(event -> manager.showRegistration());

        loginButton.setOnMouseClicked(this);
    }

    /**
     * Handles click event for login button
     *
     * @param event - Mouse click event
     */
    @Override
    public void handle(MouseEvent event) {
        String user, pass;
        //validate all input fields
        user = username.getText();
        pass = password.getText();
        if (user.isEmpty() | pass.isEmpty()) {
            NotificationUtil.showError("Not all required fields are filled.");
            return;
        }
        if (!Files.exists(Paths.get("key_" + user + ".p8"))) {
            NotificationUtil.showError("Can't find key file.");
            return;
        }
        //if successfully logged in transfer to main screen.
        if (handleLogin(user, pass)) {
            manager.showMain();
        }

    }

    /**
     * Check if username and password pai are valid
     *
     * @param user - username
     * @param pass - password
     * @return - true if user credentials are all valid
     */
    private boolean handleLogin(String user, String pass) {
        try {
            //read and decrypt users private key.
            byte[] key = Files.readAllBytes(Paths.get("key_" + username.getText() + ".p8"));
            PrivateKey privateKey = Decoder.decryptKey(pass, key);

            //check if user record exists in JavaSpace
            JavaSpace05 js = SpaceUtils.getSpace();
            U1467085UserRecord userRecordTemplate = new U1467085UserRecord();
            userRecordTemplate.userName = user;
            U1467085UserRecord userRecord = (U1467085UserRecord) js.readIfExists(userRecordTemplate, null, SpaceUtils.TIMEOUT);
            if (userRecord == null) {
                NotificationUtil.showError("Can't find user record.");
                return false;
            }
            //generate public key again
            KeyFactory rsaKeyFac = KeyFactory.getInstance("RSA", "BC");
            RSAPublicKeySpec rsaPubKeySpec = new RSAPublicKeySpec(((RSAKey) privateKey).getModulus(),
                    ((RSAPrivateCrtKey) privateKey).getPublicExponent());
            PublicKey rsaPubKey = rsaKeyFac.generatePublic(rsaPubKeySpec);
            //check that public key from user record and newly generated key match
            if (!Encoder.encodePubkeyToBase64(rsaPubKey).equals(userRecord.pubKey)) {
                handler.addError(new CommonException("Your user record has been altered!"), ErrorTypes.COMMON);
                handler.showErrorDialog(ErrorTypes.COMMON);
                return false;
            }
            //initialize user session object
            UserSession.getInstance().setup(userRecord.userName, privateKey, rsaPubKey);
            return true;
        } catch (NoSuchAlgorithmException | IOException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | InvalidKeySpecException | InterruptedException | UnusableEntryException | TransactionException | NoSuchProviderException e) {
            //handler.addError(e, ErrorTypes.COMMON);
            //handler.showErrorDialog(ErrorTypes.COMMON);
            handler.addError(new CommonException("Check that you have entered correct password.", e), ErrorTypes.COMMON);
            handler.showErrorDialog(ErrorTypes.COMMON);
        } catch (FatalException e) {
            handler.addError(e, ErrorTypes.FATAL);
            handler.showErrorDialog(ErrorTypes.FATAL);
        }
        return false;
    }
}
