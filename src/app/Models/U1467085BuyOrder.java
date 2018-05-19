package app.Models;

import java.util.UUID;

/**
 * Implements object used to signify fixed priced item being bought by a user
 */
public class U1467085BuyOrder extends SignedEntry {

    public UUID adId;
    public String buyerUserName;

    public U1467085BuyOrder() {
    }

    public U1467085BuyOrder(UUID ad, String user) {
        this.adId = ad;
        this.buyerUserName = user;
    }
}
