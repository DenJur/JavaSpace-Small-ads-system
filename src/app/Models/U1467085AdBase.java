package app.Models;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Base class for implementing further ad types
 */
public abstract class U1467085AdBase extends SignedEntry {

    public UUID id;
    public String ownerUsername;
    public String title;
    public String description;
    public Integer price;
    public Long endDate;

    public U1467085AdBase() {
    }

    public U1467085AdBase(String owner, String title, String description, Integer price, ZonedDateTime endDate) {
        this.ownerUsername = owner;
        this.description = description;
        this.price = price;
        this.endDate = endDate.toInstant().toEpochMilli();
        this.title = title;
        this.id = UUID.randomUUID();
    }

}
