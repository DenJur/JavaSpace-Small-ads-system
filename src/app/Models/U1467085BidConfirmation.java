package app.Models;

import java.util.UUID;

/**
 * Implements confirmation object for an accepted bid
 */
public class U1467085BidConfirmation extends SignedEntry {
    public UUID bidId;
    public UUID adId;

    public U1467085BidConfirmation() {
    }

    public U1467085BidConfirmation(UUID ad, UUID bid) {
        this.bidId = bid;
        this.adId = ad;
    }
}
