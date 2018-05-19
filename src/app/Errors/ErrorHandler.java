package app.Errors;

import app.StageManager;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Allows to collect exceptions and show an error dialog for those exceptions.
 */
public class ErrorHandler {

    private static ErrorHandler instance;
    private Stage mainStage;
    private Map<ErrorTypes, Stack<Exception>> mapExceptions;
    private Alert alert;

    private ErrorHandler() {
        mapExceptions = new HashMap<>();
        mainStage = StageManager.getInstance().getApplication().getStage();
    }

    public static ErrorHandler getInstance() {
        if (instance == null)
            instance = new ErrorHandler();
        return instance;
    }

    /**
     * Add error to the stack for later display.
     *
     * @param ex   - Exception for the error.
     * @param type - Severity of error.
     */
    public void addError(Exception ex, ErrorTypes type) {
        if (mapExceptions.containsKey(type))
            mapExceptions.get(type).push(ex);
        else {
            Stack<Exception> stack = new Stack<>();
            stack.push(ex);
            mapExceptions.put(type, stack);
        }
    }

    /**
     * Checks if there are any errors of the type
     *
     * @param type - type of error to check
     * @return
     */
    public boolean hasErrors(ErrorTypes type) {
        boolean has = false;
        if (!mapExceptions.isEmpty() && mapExceptions.containsKey(type) && !mapExceptions.get(type).isEmpty())
            has = true;
        return has;
    }

    public void showErrorDialog(ErrorTypes type) {
        showErrorDialog(mainStage.getScene(), type);
    }

    /**
     * Display allert dialogs for set type of errors
     *
     * @param ownerScene
     * @param type
     */
    public void showErrorDialog(Scene ownerScene, ErrorTypes type) {
        alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(ownerScene.getWindow());
        alert.setTitle("Error");
        alert.setHeaderText("Error");
        switch (type) {
            case COMMON:
                if (mapExceptions.get(type).size() == 1 && mapExceptions.get(type).peek() instanceof CommonException) {
                    Exception e = mapExceptions.get(type).peek();
                    alert.getDialogPane().contentProperty().set(new Label(e.getMessage()));
                } else {
                    Label text = new Label("Application has encountered an error.");
                    alert.getDialogPane().contentProperty().set(text);
                }
                setExpandable(getErrors(type));
                break;
            case FATAL:
                alert.setHeaderText("Fatal error");
                if (mapExceptions.get(type).size() == 1 && mapExceptions.get(type).peek() instanceof FatalException) {
                    Exception e = mapExceptions.get(type).peek();
                    alert.getDialogPane().contentProperty().set(new Label(e.getMessage()));
                } else {
                    Label text = new Label("Application has encountered a fatal error.");
                    alert.getDialogPane().contentProperty().set(text);
                }
                setExpandable(getErrors(type));
                break;
        }
        alert.showAndWait();
        //kill the program if error was fatal
        if (type.equals(ErrorTypes.FATAL))
            System.exit(-1);
    }

    /**
     * Sets an expandable element of the dialog to display exception stack trace
     *
     * @param exceptions - stack of exceptions to display
     */
    private void setExpandable(Stack<Exception> exceptions) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        while (!exceptions.isEmpty())
            exceptions.pop().printStackTrace(pw);

        TextArea textArea = new TextArea(sw.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(new Label("The exception stacktrace was:"), 0, 0);
        expContent.add(textArea, 0, 1);
        alert.getDialogPane().setExpandableContent(expContent);
    }

    public Stack<Exception> getErrors(ErrorTypes type) {
        return mapExceptions.get(type);
    }
}
