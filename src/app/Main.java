package app;

import javafx.application.Application;
import javafx.stage.Stage;

import java.security.Security;


public class Main extends Application {

    private Stage mainStage;

    public static void main(String[] args) {
        //initialize bouncycastle and setup security policy if possible
        Security.setProperty("crypto.policy", "unlimited");
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        mainStage = primaryStage;
        mainStage.setTitle("Ads");
        StageManager.getInstance().setPrimaryStage(this);
    }

    public Stage getStage() {
        return mainStage;
    }
}
