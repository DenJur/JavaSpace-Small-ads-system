package app.notification;

import app.Models.U1467085AdBase;
import app.Models.U1467085Bid;
import javafx.collections.ObservableList;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Static access point for all notification listeners.
 */
public class NotificationManager {
    private static NotificationManager instance;
    private final Object syncLock = new Object();
    public boolean showNewAdNotification = true;
    public boolean showBoughtNotification = true;
    public boolean showNewCommentNotification = true;
    public boolean showBidNotification = true;
    public boolean showBidConfirmedNotification = true;
    private NewAdNotification newAdNotificator;
    private ItemBoughtNotification itemBoughtNotificator;
    private NewCommentNotification commentNotificator;
    private BiddingFinishedNotification biddingFinishedNotificator;
    private BidConfirmedNotification bidConfirmedNotification;

    private ScheduledExecutorService execService = Executors.newScheduledThreadPool(5);

    /**
     * Set-up and start all listeners.
     */
    private NotificationManager() {
        newAdNotificator = new NewAdNotification(this, syncLock);
        itemBoughtNotificator = new ItemBoughtNotification(this, syncLock);
        commentNotificator = new NewCommentNotification(this, syncLock);
        biddingFinishedNotificator = new BiddingFinishedNotification(this, syncLock, execService);
        bidConfirmedNotification = new BidConfirmedNotification(this, syncLock);
    }

    public static NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    /**
     * Notify all the notification listeners that require it about new ad being added by the user.
     *
     * @param ad - ad that was added by the user
     */
    public void newAdPostedByMe(U1467085AdBase ad) {
        itemBoughtNotificator.newAdPostedByMe(ad);
        commentNotificator.newAdPostedByMe(ad);
        biddingFinishedNotificator.newAdPostedByMe(ad);
    }

    /**
     * Execute all onClose methods for listeners
     */
    public void onClose() {
        newAdNotificator.onClose();
        itemBoughtNotificator.onClose();
        commentNotificator.onClose();
        biddingFinishedNotificator.onClose();
        bidConfirmedNotification.onClose();
        execService.shutdownNow();
    }

    /**
     * Reschedules task to be run after 10 seconds.
     *
     * @param task - task to rerun.
     */
    public void rescheduleTask(Runnable task) {
        execService.schedule(task, 10 * 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Updates the list that notificators report to.
     *
     * @param observableNotificationList - list for notification messages
     */
    public void setObservableNotificationList(ObservableList<String> observableNotificationList) {
        newAdNotificator.setObservableList(observableNotificationList);
        itemBoughtNotificator.setObservableList(observableNotificationList);
        commentNotificator.setObservableList(observableNotificationList);
        biddingFinishedNotificator.setObservableList(observableNotificationList);
        bidConfirmedNotification.setObservableList(observableNotificationList);
    }

    /**
     * Notify all the notification listeners that require it about new bid being added by the user.
     *
     * @param newBid - new bid posted by the user
     */
    public void newBidPostedByMe(U1467085Bid newBid) {
        bidConfirmedNotification.newBidPostedByMe(newBid);
    }
}
