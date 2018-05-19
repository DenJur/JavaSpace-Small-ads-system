package app.Models;

import java.time.ZonedDateTime;

/**
 * Implements fixed price ad
 */
public class U1467085AdBuy extends U1467085AdBase {
    public U1467085AdBuy() {
    }

    public U1467085AdBuy(String owner, String title, String description, Integer price, ZonedDateTime endDate) {
        super(owner, title, description, price, endDate);
    }

}
