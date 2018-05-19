package test;

import app.Errors.FatalException;
import app.Models.*;
import app.Util.SpaceUtils;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.transaction.TransactionException;
import net.jini.space.JavaSpace05;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertNull;

class SpaceUtilsTest {
    private final String owner = "testOwner";
    private final String owner2 = "testOwnerTwo";
    private final String title = "testTitle";
    private final String description = "testDescription";
    private final String comment = "testComment";
    private final String comment2 = "testCommentTwo";
    private final Integer price = 100;
    private final ZonedDateTime endDate = ZonedDateTime.now();

    @Test
    void cleanAdRemoveBuy() throws FatalException, RemoteException, TransactionException, InterruptedException, UnusableEntryException {
        JavaSpace05 js = SpaceUtils.getSpace();
        U1467085AdBuy testBuy = new U1467085AdBuy(owner, title, description, price, endDate);
        U1467085Comment testComment = new U1467085Comment(testBuy.id, owner, comment);
        U1467085Comment testComment2 = new U1467085Comment(testBuy.id, owner2, comment2);
        U1467085BuyOrder testBuyOrder = new U1467085BuyOrder(testBuy.id, owner2);

        js.write(testBuy, null, 10 * 1000);
        js.write(testComment, null, 10 * 1000);
        js.write(testComment2, null, 10 * 1000);
        js.write(testBuyOrder, null, 10 * 1000);

        SpaceUtils.cleanAdRemove(testBuy, 0);

        assertNull(js.readIfExists(testBuy, null, 10 * 1000));
        assertNull(js.readIfExists(testComment, null, 10 * 1000));
        assertNull(js.readIfExists(testComment2, null, 10 * 1000));
        assertNull(js.readIfExists(testBuyOrder, null, 10 * 1000));
    }

    @Test
    void cleanAdRemoveBid() throws FatalException, RemoteException, TransactionException, InterruptedException, UnusableEntryException {
        JavaSpace05 js = SpaceUtils.getSpace();
        U1467085AdBid testBid = new U1467085AdBid(owner, title, description, price, endDate);
        U1467085Comment testComment = new U1467085Comment(testBid.id, owner, comment);
        U1467085Comment testComment2 = new U1467085Comment(testBid.id, owner2, comment2);
        U1467085Bid testBid1 = new U1467085Bid(testBid.id, owner2, price + 1);
        U1467085Bid testBid2 = new U1467085Bid(testBid.id, owner, price + 2);
        U1467085BidConfirmation testBidConfirmation = new U1467085BidConfirmation(testBid.id, testBid2.id);

        js.write(testBid, null, 10 * 1000);
        js.write(testComment, null, 10 * 1000);
        js.write(testComment2, null, 10 * 1000);
        js.write(testBid1, null, 10 * 1000);
        js.write(testBid2, null, 10 * 1000);
        js.write(testBidConfirmation, null, 10 * 1000);


        SpaceUtils.cleanAdRemove(testBid, 0);

        assertNull(js.readIfExists(testBid, null, 10 * 1000));
        assertNull(js.readIfExists(testComment, null, 10 * 1000));
        assertNull(js.readIfExists(testComment2, null, 10 * 1000));
        assertNull(js.readIfExists(testBid1, null, 10 * 1000));
        assertNull(js.readIfExists(testBid2, null, 10 * 1000));
        assertNull(js.readIfExists(testBidConfirmation, null, 10 * 1000));
    }
}