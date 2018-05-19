package app.components;

import javafx.collections.ObservableList;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * Modified spinner that allows only number with 2 decimal places and a $ character at the end. Filter out all invalid
 * characters.
 */
public class PriceSpinner extends Spinner<Double> {

    private TextFormatter<Double> priceFormatter;

    public PriceSpinner() {
        super();
        setPriceFormatter();
    }

    /**
     * Sets up text formatter to filter out invalid characters.
     */
    private void setPriceFormatter() {

        NumberFormat format = DecimalFormat.getInstance();
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(2);
        priceFormatter = new TextFormatter<>(new StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                return String.format("%.2f$", object);
            }

            @Override
            public Double fromString(String string) {
                return Double.valueOf(string);
            }
        }, 1000d,
                c -> {
                    if (c.isContentChange()) {
                        ParsePosition parsePosition = new ParsePosition(0);
                        format.parse(c.getControlNewText(), parsePosition);
                        if (parsePosition.getIndex() == 0 ||
                                parsePosition.getIndex() < c.getControlNewText().length()) {
                            return null;
                        }
                    }
                    return c;
                });
        this.getEditor().setTextFormatter(priceFormatter);
    }

    public PriceSpinner(int min, int max, int initialValue) {
        super(min, max, initialValue);
        setPriceFormatter();
    }

    public PriceSpinner(int min, int max, int initialValue, int amountToStepBy) {
        super(min, max, initialValue, amountToStepBy);
        setPriceFormatter();
    }

    public PriceSpinner(double min, double max, double initialValue) {
        super(min, max, initialValue);
        setPriceFormatter();
    }

    public PriceSpinner(double min, double max, double initialValue, double amountToStepBy) {
        super(min, max, initialValue, amountToStepBy);
        setPriceFormatter();
    }

    public PriceSpinner(ObservableList<Double> items) {
        super(items);
        setPriceFormatter();
    }

    public PriceSpinner(SpinnerValueFactory<Double> valueFactory) {
        super(valueFactory);
        setPriceFormatter();
    }

    public TextFormatter<Double> getPriceFormatter() {
        return priceFormatter;
    }

}
