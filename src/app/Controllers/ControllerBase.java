package app.Controllers;

import app.Errors.ErrorHandler;
import app.StageManager;
import javafx.scene.Scene;
import javafx.stage.Stage;

public abstract class ControllerBase {

    protected Stage rootStage;
    protected Scene myScene;
    protected StageManager manager = StageManager.getInstance();
    protected ErrorHandler handler = ErrorHandler.getInstance();

    public ControllerBase() {
    }

    public void setStage(Stage stage) {
        rootStage = stage;
    }

    /**
     * Perform all operations necessary for proper application closure.
     */
    public void onClose() {
    }

    public void setMyScene(Scene scene) {
        myScene = scene;
    }

    /**
     * Allow controller to self display its scene to a root stage.
     */
    public void display() {
        rootStage.setMinWidth(800);
        rootStage.setMinHeight(600);
        rootStage.setScene(myScene);
    }

    /**
     * Actions that need to be performed when scene is not in view.
     */
    public void hide() {
    }
}
