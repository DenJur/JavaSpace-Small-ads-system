package app.notification;

import app.Models.U1467085AdBase;
import app.Models.U1467085AdBuy;
import app.Models.U1467085BuyOrder;
import app.Util.AddressBook;
import app.Util.SpaceUtils;
import app.Util.UserSession;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.space.AvailabilityEvent;
import net.jini.space.JavaSpace05;
import net.jini.space.MatchSet;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Notification that listens for user owned ads at fixed price being bought
 */
public class ItemBoughtNotification extends NotificationBase {
    //list of all buyorders that could be placed on ads owned by user
    private List<U1467085BuyOrder> allPossibleBuys;

    public ItemBoughtNotification(NotificationManager manager, Object syncLock) {
        super(manager, syncLock);
        this.Listener = new ItemBoughtListener();
        try {
            UnicastRemoteObject.exportObject(Listener, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setObservableList(ObservableList<String> list) {
        super.setObservableList(list);
        notifyMissedPurchases();
        registerBoughtListener();
    }

    /**
     * Find all items that were bought while user was offline
     */
    private void notifyMissedPurchases() {
        try {
            JavaSpace05 js = SpaceUtils.getSpace();
            U1467085AdBuy templateBuy = new U1467085AdBuy();
            templateBuy.ownerUsername = UserSession.getInstance().username;
            MatchSet allMyAds = js.contents(Collections.singleton(templateBuy), null, SpaceUtils.TIMEOUT, Integer.MAX_VALUE);
            boolean foundAny = false;
            for (U1467085AdBuy i = (U1467085AdBuy) allMyAds.next(); i != null; i = (U1467085AdBuy) allMyAds.next()) {
                try {
                    if (i.verifySignature(UserSession.getInstance().pubKey)) {
                        U1467085BuyOrder orderTemplate = new U1467085BuyOrder();
                        orderTemplate.adId = i.id;
                        U1467085BuyOrder order = (U1467085BuyOrder) js.readIfExists(orderTemplate, null, SpaceUtils.TIMEOUT);
                        if (order != null) {
                            String notificationMessage = "Item " + i.title + " was bought by " + order.buyerUserName;
                            Platform.runLater(() -> observableNotificationList.add(notificationMessage));
                            foundAny = true;
                            SpaceUtils.cleanAdRemove(i, 3);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (foundAny) {
                NotificationUtil.showNotification("Some items were bought while you were away.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Construct list of templates for ads that user owns that could be bought.
     */
    private void registerBoughtListener() {
        try {
            JavaSpace05 js = SpaceUtils.getSpace();
            U1467085AdBuy templateBuy = new U1467085AdBuy();
            templateBuy.ownerUsername = UserSession.getInstance().username;
            MatchSet allMyAds = js.contents(Collections.singleton(templateBuy), null, SpaceUtils.TIMEOUT, Integer.MAX_VALUE);
            allPossibleBuys = new ArrayList<>();
            for (U1467085AdBuy i = (U1467085AdBuy) allMyAds.next(); i != null; i = (U1467085AdBuy) allMyAds.next()) {
                try {
                    if (i.verifySignature(UserSession.getInstance().pubKey)) {
                        U1467085BuyOrder orderTemplate = new U1467085BuyOrder();
                        orderTemplate.adId = i.id;
                        allPossibleBuys.add(orderTemplate);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (allPossibleBuys.size() > 0)
                EventRegistration = js.registerForAvailabilityEvent(allPossibleBuys, null, false, Listener, SpaceUtils.LONGLEASE, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Ad new template to listen for
     *
     * @param ad - ad that could be bought
     */
    public void newAdPostedByMe(U1467085AdBase ad) {
        if (ad instanceof U1467085AdBuy) {
            try {
                JavaSpace05 js = SpaceUtils.getSpace();
                if (EventRegistration != null) {
                    EventRegistration.getLease().cancel();
                }
                U1467085BuyOrder newTemplate = new U1467085BuyOrder();
                newTemplate.adId = ad.id;
                allPossibleBuys.add(newTemplate);
                EventRegistration = js.registerForAvailabilityEvent(allPossibleBuys, null, false, Listener, SpaceUtils.LONGLEASE, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * RemoteEventListener that listens for buy orders being placed to JavaSpace
     */
    private class ItemBoughtListener implements RemoteEventListener {

        @Override
        public void notify(RemoteEvent remoteEvent) {
            synchronized (syncLock) {
                AvailabilityEvent event = (AvailabilityEvent) remoteEvent;
                try {
                    U1467085BuyOrder buyOrder = (U1467085BuyOrder) event.getEntry();
                    if (buyOrder.verifySignature(AddressBook.getUserKey(buyOrder.buyerUserName))) {
                        JavaSpace05 js = SpaceUtils.getSpace();
                        U1467085AdBuy myAdTemplate = new U1467085AdBuy();
                        myAdTemplate.id = buyOrder.adId;
                        U1467085AdBuy myAd = (U1467085AdBuy) js.readIfExists(myAdTemplate, null, SpaceUtils.TIMEOUT);
                        if (myAd == null || !myAd.ownerUsername.equals(UserSession.getInstance().username))
                            return;
                        String message = "Your item " + myAd.title + " has been bought by " + buyOrder.buyerUserName;
                        Platform.runLater(() -> observableNotificationList.add(message));
                        if (manager.showBoughtNotification)
                            Platform.runLater(() -> NotificationUtil.showNotification(message));
                        SpaceUtils.cleanAdRemove(myAd, 3);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
