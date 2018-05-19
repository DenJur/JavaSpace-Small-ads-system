package app.Models;

import java.time.ZonedDateTime;

/**
 * Implements Auction ad type
 */
public class U1467085AdBid extends U1467085AdBase {
    public U1467085AdBid() {
    }

    public U1467085AdBid(String owner, String title, String description, Integer price, ZonedDateTime endDate) {
        super(owner, title, description, price, endDate);
    }
}
