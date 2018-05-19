package app.Controllers;

import app.Errors.ErrorTypes;
import app.Errors.FatalException;
import app.Models.U1467085UserRecord;
import app.Util.SpaceUtils;
import app.keyStore.Encoder;
import app.notification.NotificationUtil;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.transaction.*;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.space.JavaSpace05;
import tray.animations.AnimationType;
import tray.notification.NotificationType;
import tray.notification.TrayNotification;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

/**
 * Controls user registration screen.
 */
public class RegisterController extends ControllerBase implements EventHandler<MouseEvent> {

    public Button registerButton;
    public PasswordField password2;
    public PasswordField password;
    public TextField username;
    public Button gotoLogin;

    @FXML
    public void initialize() {
        gotoLogin.setOnMouseClicked(event -> manager.showLogin());
        registerButton.setOnMouseClicked(this);
    }

    /**
     * Handles register button click.
     *
     * @param event - event object from mouse button click
     */
    @Override
    public void handle(MouseEvent event) {
        String user, pass, pass2;
        user = username.getText();
        pass = password.getText();
        pass2 = password2.getText();
        //verify user provided data
        if (user.isEmpty() | pass.isEmpty() | pass2.isEmpty()) {
            TrayNotification tray = new TrayNotification("Error", "Not all required fields are filled.", NotificationType.ERROR);
            tray.setAnimationType(AnimationType.POPUP);
            tray.showAndDismiss(Duration.seconds(1));
            return;
        }
        if (!pass.equals(pass2)) {
            TrayNotification tray = new TrayNotification("Error", "Passwords do not match.", NotificationType.ERROR);
            tray.setAnimationType(AnimationType.POPUP);
            tray.showAndDismiss(Duration.seconds(1));
            return;
        }
        //go to login screen if successful
        if (handleRegistration(user, pass)) {
            manager.showLogin();
        }
    }

    /**
     * Handles RSA key pair generation, user record addition to java space and subsequent private key encryption
     * and saving using user password.
     *
     * @param user - username for new user account
     * @param pass - password for private key encryption
     * @return Boolean dictating if registration was successful
     */
    private boolean handleRegistration(String user, String pass) {
        try {
            TransactionManager tm = SpaceUtils.getManager();
            Transaction.Created transaction = TransactionFactory.create(tm, SpaceUtils.TIMEOUT);
            try {
                JavaSpace05 js = SpaceUtils.getSpace();
                //check if user with such username already exists
                U1467085UserRecord userRecordTemplate = new U1467085UserRecord();
                userRecordTemplate.userName = user;
                U1467085UserRecord existing = (U1467085UserRecord) js.readIfExists(userRecordTemplate, null, SpaceUtils.TIMEOUT);
                if (existing != null) {
                    NotificationUtil.showError("User is already registered.");
                    return false;
                }
                //generate RSA keypair
                //in actual solution this would be provided by an admin
                //and would have certificate authentication on access to java space
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
                SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
                generator.initialize(2048, random);
                KeyPair pair = generator.generateKeyPair();
                PublicKey publicKey = pair.getPublic();
                PrivateKey privateKey = pair.getPrivate();

                //write our record to java space address book
                U1467085UserRecord newUser = new U1467085UserRecord(user, Encoder.encodePubkeyToBase64(publicKey));
                js.write(newUser, transaction.transaction, Lease.FOREVER);

                //save encrypted private key to file
                //would not be necessary in actual application as user would have to provide path
                //to the private key file on login/registration
                byte[] keyCrypt = Encoder.encryptPrivateKey(pass, privateKey);
                FileOutputStream fos = new FileOutputStream("key_" + user + ".p8");
                fos.write(keyCrypt);
                fos.close();
                transaction.transaction.commit();
                return true;

            } catch (RemoteException | InterruptedException | UnusableEntryException | TransactionException e) {
                //Errors with java space
                handler.addError(e, ErrorTypes.COMMON);
                handler.showErrorDialog(ErrorTypes.COMMON);
                transaction.transaction.abort();
            } catch (NoSuchAlgorithmException | FatalException | NoSuchProviderException | InvalidKeySpecException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | InvalidParameterSpecException | IOException | IllegalBlockSizeException e) {
                //other errors
                handler.addError(e, ErrorTypes.FATAL);
                handler.showErrorDialog(ErrorTypes.FATAL);
                transaction.transaction.abort();
            }
        } catch (FatalException | RemoteException | LeaseDeniedException | UnknownTransactionException | CannotAbortException e) {
            //error committing a transaction
            handler.addError(e, ErrorTypes.COMMON);
            handler.showErrorDialog(ErrorTypes.COMMON);
        }
        return false;
    }
}
