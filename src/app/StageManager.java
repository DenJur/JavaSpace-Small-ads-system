package app;

import app.Controllers.AdController;
import app.Controllers.ControllerBase;
import app.Errors.ErrorHandler;
import app.Errors.ErrorTypes;
import app.Models.U1467085AdBase;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Static object used to manage what scene is currently displayed in the main window.
 * Reuses scenes after initialization.
 */
public class StageManager {

    private static StageManager instance;
    private Main application;
    private ErrorHandler errorHandler;
    private Stage rootStage;
    //Collection of all initialized controllers.
    private HashMap<String, ControllerBase> controllers = new HashMap<>();
    //Controller of currently active scene.
    private ControllerBase rootController;


    private StageManager() {
    }

    public static StageManager getInstance() {
        if (instance == null)
            instance = new StageManager();
        return instance;
    }

    /**
     * Setup initial scene of the application.
     *
     * @param application - context of the application.
     */
    protected void setPrimaryStage(Main application) {
        this.application = application;
        errorHandler = ErrorHandler.getInstance();
        initPrimaryStage();
        //Call onClose methods off all created controllers.
        rootStage.setOnCloseRequest(event -> {
            for (Map.Entry<String, ControllerBase> entry : controllers.entrySet()) {
                entry.getValue().onClose();
            }
        });
    }

    private void initPrimaryStage() {
        try {
            rootStage = application.getStage();
            showScene("Views/LoginView.fxml");
            rootStage.show();
        } catch (IOException e) {
            errorHandler.addError(e, ErrorTypes.COMMON);
            errorHandler.showErrorDialog(ErrorTypes.COMMON);
        }
    }

    private ControllerBase showScene(String path) throws IOException {
        if (rootController != null) rootController.hide();
        if (controllers.containsKey(path)) {
            rootController = controllers.get(path);
        } else {
            FXMLLoader rootLoader = new FXMLLoader();
            rootLoader.setLocation(Main.class.getResource(path));
            Parent rootLayout = rootLoader.load();
            rootController = rootLoader.getController();
            rootController.setStage(rootStage);
            Scene mainScene = new Scene(rootLayout);
            rootController.setMyScene(mainScene);
            controllers.put(path, rootController);
        }
        rootController.display();
        return rootController;
    }

    public Main getApplication() {
        return application;
    }

    public void showLogin() {
        try {
            showScene("Views/LoginView.fxml");
            rootStage.show();
        } catch (IOException e) {
            errorHandler.addError(e, ErrorTypes.COMMON);
            errorHandler.showErrorDialog(ErrorTypes.COMMON);
        }
    }

    public void showAd(U1467085AdBase ad) {
        try {
            AdController rootController = (AdController) showScene("Views/AdView.fxml");
            rootController.updateView(ad);
            rootStage.show();
        } catch (IOException e) {
            errorHandler.addError(e, ErrorTypes.COMMON);
            errorHandler.showErrorDialog(ErrorTypes.COMMON);
        }
    }

    public void showRegistration() {
        try {
            showScene("Views/RegisterView.fxml");
            rootStage.show();
        } catch (IOException e) {
            errorHandler.addError(e, ErrorTypes.COMMON);
            errorHandler.showErrorDialog(ErrorTypes.COMMON);
        }
    }

    public void showMain() {
        try {
            showScene("Views/MainView.fxml");
            rootStage.show();
        } catch (IOException e) {
            errorHandler.addError(e, ErrorTypes.COMMON);
            errorHandler.showErrorDialog(ErrorTypes.COMMON);
        }
    }
}