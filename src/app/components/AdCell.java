package app.components;

import app.Models.U1467085AdBase;
import app.Models.U1467085AdBuy;
import app.StageManager;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * View component for ad list view. Provides a button that allows to view the full ad screen.
 */
public class AdCell extends ListCell<U1467085AdBase> {

    private GridPane grid = new GridPane();
    private Label title = new Label("");
    private Label price = new Label("");
    private Label date = new Label("");
    private Button button = new Button("View");
    private U1467085AdBase ad;

    /**
     * Initialize view elements of the cell.
     */
    public AdCell() {
        super();
        grid.setHgap(10);
        grid.setVgap(4);
        grid.setPadding(new Insets(0, 10, 5, 10));
        grid.setPrefWidth(1);
        GridPane.setHgrow(title, Priority.ALWAYS);
        date.setMinWidth(Double.NEGATIVE_INFINITY);
        price.setMinWidth(Double.NEGATIVE_INFINITY);
        button.setMinWidth(Double.NEGATIVE_INFINITY);
        grid.add(title, 0, 0);
        grid.add(date, 1, 0);
        grid.add(price, 2, 0);
        grid.add(button, 3, 0);
        button.setOnAction(event -> StageManager.getInstance().showAd(ad));
    }

    /**
     * Updates the view with information from ad object.
     *
     * @param item  - ad object that provides information.
     * @param empty - indicates that the cell is empty.
     */
    @Override
    protected void updateItem(U1467085AdBase item, boolean empty) {
        super.updateItem(item, empty);
        setText(null);
        setGraphic(null);

        ad = item;
        if (item != null && !empty) {
            if (item instanceof U1467085AdBuy) {
                DecimalFormat df = new DecimalFormat("#.##");
                price.setText(df.format((double) item.price / 100) + "$");
            } else {
                price.setText("Auction");
            }
            ZonedDateTime zonedDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(item.endDate), ZoneId.systemDefault());
            date.setText("Date Closing " + zonedDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")));
            title.setText(item.title);
            setGraphic(grid);
        }
    }
}
