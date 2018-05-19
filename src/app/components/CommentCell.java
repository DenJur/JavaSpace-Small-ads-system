package app.components;

import app.Models.U1467085Comment;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * View component for comment list view.
 */
public class CommentCell extends ListCell<U1467085Comment> {

    private GridPane grid = new GridPane();
    private Label author = new Label();
    private Label comment = new Label();
    private Label date = new Label();

    /**
     * Initialize view elements of the cell.
     */
    public CommentCell() {
        super();
        grid.setHgap(10);
        grid.setVgap(4);
        grid.setPadding(new Insets(0, 10, 5, 10));
        grid.setPrefWidth(1);
        author.setFont(Font.font("System", FontWeight.BOLD, 14));
        author.setMinWidth(Double.NEGATIVE_INFINITY);
        GridPane.setHalignment(date, HPos.RIGHT);
        GridPane.setFillWidth(author, true);
        GridPane.setHalignment(date, HPos.RIGHT);
        GridPane.setHgrow(comment, Priority.ALWAYS);
        GridPane.setValignment(author, VPos.TOP);
        date.setFont(Font.font("System", FontPosture.ITALIC, 8));
        comment.setWrapText(true);
        grid.add(author, 0, 0);
        grid.add(comment, 1, 0);
        grid.add(date, 1, 1);
    }

    /**
     * Updates the view with information from comment object.
     *
     * @param item  - comment record that provides information.
     * @param empty - indicates that the cell is empty.
     */
    @Override
    protected void updateItem(U1467085Comment item, boolean empty) {
        super.updateItem(item, empty);
        setText(null);
        setGraphic(null);

        if (item != null && !empty) {
            ZonedDateTime zonedDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(item.date), ZoneId.systemDefault());
            author.setText(item.userName + ":");
            comment.setText(item.comment);
            date.setText("Date Posted " + zonedDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")));
            setGraphic(grid);
        }
    }
}