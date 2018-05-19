package app.notification;

import app.Models.U1467085AdBid;
import app.Models.U1467085Bid;
import app.Models.U1467085BidConfirmation;
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
 * Notification listener that listens for user bids being confirmed
 */
public class BidConfirmedNotification extends NotificationBase {

    //list of templates for all ads that user has bid on and that can be confirmed
    private List<U1467085BidConfirmation> allPossibleConfirmations;

    public BidConfirmedNotification(NotificationManager manager, Object syncLock) {
        super(manager, syncLock);
        this.Listener = new BidConfirmedListener();
        try {
            UnicastRemoteObject.exportObject(Listener, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setObservableList(ObservableList<String> list) {
        super.setObservableList(list);
        notifyMissedConfirmations();
        registerBidConfirmationListener();
    }

    /**
     * Find all confirmations that happened while user was offline
     */
    private void notifyMissedConfirmations() {
        try {
            JavaSpace05 js = SpaceUtils.getSpace();
            U1467085Bid templateBid = new U1467085Bid();
            templateBid.bidderUserName = UserSession.getInstance().username;
            //find all bids from the user
            MatchSet allMyAds = js.contents(Collections.singleton(templateBid), null, SpaceUtils.TIMEOUT, Integer.MAX_VALUE);
            boolean foundAny = false;
            for (U1467085Bid i = (U1467085Bid) allMyAds.next(); i != null; i = (U1467085Bid) allMyAds.next()) {
                try {
                    //check if confirmation was written for that bid
                    U1467085BidConfirmation confirmationTemplate = new U1467085BidConfirmation();
                    confirmationTemplate.bidId = i.id;
                    U1467085BidConfirmation confirmation = (U1467085BidConfirmation) js.readIfExists(confirmationTemplate, null, SpaceUtils.TIMEOUT);
                    U1467085AdBid adTemplate = new U1467085AdBid();
                    adTemplate.id = i.adId;
                    U1467085AdBid ad = (U1467085AdBid) js.readIfExists(adTemplate, null, SpaceUtils.TIMEOUT);
                    if (confirmation != null && ad != null && ad.verifySignature(AddressBook.getUserKey(ad.ownerUsername)) &&
                            confirmation.verifySignature(AddressBook.getUserKey(ad.ownerUsername)) &&
                            i.verifySignature(UserSession.getInstance().pubKey)) {
                        String message = "Your bid of " + String.format("%.2f$", (float) i.bid / 100) + " on item: " + ad.title + " has won.";
                        Platform.runLater(() -> observableNotificationList.add(message));
                        foundAny = true;
                        SpaceUtils.cleanAdRemove(ad, 3);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //Avoid duplicate popup notifications but showing a single notification if any confirmations
            //were found
            if (foundAny) {
                NotificationUtil.showNotification("Some bids won while you were away.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Construct list of all ads that user has bid on and register listener to listen for confirmation on those.
     */
    private void registerBidConfirmationListener() {
        try {
            JavaSpace05 js = SpaceUtils.getSpace();
            U1467085Bid templateBid = new U1467085Bid();
            templateBid.bidderUserName = UserSession.getInstance().username;
            MatchSet allMyBids = js.contents(Collections.singleton(templateBid), null, SpaceUtils.TIMEOUT, Integer.MAX_VALUE);
            allPossibleConfirmations = new ArrayList<>();
            for (U1467085Bid i = (U1467085Bid) allMyBids.next(); i != null; i = (U1467085Bid) allMyBids.next()) {
                try {
                    U1467085Bid finalI = i;
                    if (i.verifySignature(UserSession.getInstance().pubKey) &&
                            allPossibleConfirmations.stream().noneMatch(o -> o.adId == finalI.adId)) {
                        U1467085BidConfirmation orderTemplate = new U1467085BidConfirmation();
                        orderTemplate.adId = i.adId;
                        allPossibleConfirmations.add(orderTemplate);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (allPossibleConfirmations.size() > 0)
                EventRegistration = js.registerForAvailabilityEvent(allPossibleConfirmations, null, false, Listener, SpaceUtils.LONGLEASE, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds template for the ad if it wasn't previously added and restarts the listener
     *
     * @param bid - bid that was posted
     */
    public void newBidPostedByMe(U1467085Bid bid) {
        try {
            JavaSpace05 js = SpaceUtils.getSpace();
            if (EventRegistration != null) {
                EventRegistration.getLease().cancel();
            }
            if (allPossibleConfirmations.stream().noneMatch((o) -> o.adId == bid.adId)) {
                U1467085BidConfirmation newTemplate = new U1467085BidConfirmation();
                newTemplate.adId = bid.adId;
                allPossibleConfirmations.add(newTemplate);
                EventRegistration = js.registerForAvailabilityEvent(allPossibleConfirmations, null, false, Listener, SpaceUtils.LONGLEASE, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * RemoteEventListener implementation that listens for new bid confirmations added to the JavaSpace.
     */
    private class BidConfirmedListener implements RemoteEventListener {

        @Override
        public void notify(RemoteEvent remoteEvent) {
            synchronized (syncLock) {
                AvailabilityEvent event = (AvailabilityEvent) remoteEvent;
                try {
                    U1467085BidConfirmation bidConfirmation = (U1467085BidConfirmation) event.getEntry();
                    JavaSpace05 js = SpaceUtils.getSpace();
                    U1467085AdBid adTemplate = new U1467085AdBid();
                    adTemplate.id = bidConfirmation.adId;
                    U1467085AdBid ad = (U1467085AdBid) js.readIfExists(adTemplate, null, SpaceUtils.TIMEOUT);
                    U1467085Bid bidTemplate = new U1467085Bid();
                    bidTemplate.id = bidConfirmation.bidId;
                    U1467085Bid bid = (U1467085Bid) js.readIfExists(bidTemplate, null, SpaceUtils.TIMEOUT);
                    if (ad != null && bid != null && ad.verifySignature(AddressBook.getUserKey(ad.ownerUsername)) &&
                            bidConfirmation.verifySignature(AddressBook.getUserKey(ad.ownerUsername)) &&
                            bid.verifySignature(UserSession.getInstance().pubKey)) {
                        String message = "Your bid of " + String.format("%.2f$", (float) bid.bid / 100) + " on item: " + ad.title + " has won.";
                        Platform.runLater(() -> observableNotificationList.add(message));
                        if (manager.showBidConfirmedNotification)
                            Platform.runLater(() -> NotificationUtil.showNotification(message));
                        SpaceUtils.cleanAdRemove(ad, 3);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
