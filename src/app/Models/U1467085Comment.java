package app.Models;

import java.time.Instant;
import java.util.UUID;

/**
 * Implements messages to be posted to different ads
 */
public class U1467085Comment extends SignedEntry {
    public UUID adId;
    public String userName;
    public String comment;
    public Long date;

    public U1467085Comment() {
    }

    public U1467085Comment(UUID ad, String user, String comment) {
        this.adId = ad;
        this.userName = user;
        this.comment = comment;
        this.date = Instant.now().toEpochMilli();
    }
}
