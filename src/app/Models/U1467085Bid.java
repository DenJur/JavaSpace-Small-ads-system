package app.Models;

import java.util.UUID;

/**
 * Implements bids that are placed on auction type ad
 */
public class U1467085Bid extends SignedEntry {

    public UUID id;
    public UUID adId;
    public String bidderUserName;
    public Integer bid;

    public U1467085Bid() {
    }

    public U1467085Bid(UUID ad, String user, Integer bid) {
        this.adId = ad;
        this.bidderUserName = user;
        this.bid = bid;
        this.id = UUID.randomUUID();
    }
}
