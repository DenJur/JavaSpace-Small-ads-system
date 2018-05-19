package app.notification;

import app.Models.U1467085AdBase;
import app.Models.U1467085AdBid;
import app.Models.U1467085Bid;
import app.Models.U1467085BidConfirmation;
import app.Util.AddressBook;
import app.Util.SpaceUtils;
import app.Util.UserSession;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import net.jini.space.JavaSpace05;
import net.jini.space.MatchSet;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Notification listener that waits for auction type ads being finished
 */
public class BiddingFinishedNotification extends NotificationBase {
    //use ScheduledExecutorService to schedule when bidding is finished for specific ads
    private ScheduledExecutorService execService;

    public BiddingFinishedNotification(NotificationManager manager, Object syncLock, ScheduledExecutorService execService) {
        super(manager, syncLock);
        this.execService = execService;
    }

    @Override
    public void setObservableList(ObservableList<String> list) {
        super.setObservableList(list);
        notifyMissedBids();
        scheduleBiddingResolve();
    }

    /**
     * Find all auction type ads that have finished while user was away and write bid confirmations for those
     */
    private void notifyMissedBids() {
        try {
            JavaSpace05 js = SpaceUtils.getSpace();
            U1467085AdBid templateBidAd = new U1467085AdBid();
            templateBidAd.ownerUsername = UserSession.getInstance().username;
            MatchSet allMyAds = js.contents(Collections.singleton(templateBidAd), null, SpaceUtils.TIMEOUT, Integer.MAX_VALUE);
            boolean foundAny = false;
            for (U1467085AdBid i = (U1467085AdBid) allMyAds.next(); i != null; i = (U1467085AdBid) allMyAds.next()) {
                try {
                    if (i.verifySignature(UserSession.getInstance().pubKey)) {
                        U1467085BidConfirmation alreadyConfirmed = new U1467085BidConfirmation();
                        alreadyConfirmed.adId = i.id;
                        if (Instant.now().isAfter(Instant.ofEpochMilli(i.endDate)) &&
                                js.readIfExists(alreadyConfirmed, null, SpaceUtils.TIMEOUT) == null) {
                            if (processBids(i) != null)
                                foundAny = true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (foundAny) {
                NotificationUtil.showNotification("Some auctions finished while you were away.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Finds all users auction ads and schedules their resolvers to execute at ad closure time
     */
    private void scheduleBiddingResolve() {
        try {
            JavaSpace05 js = SpaceUtils.getSpace();
            U1467085AdBid templateBidAd = new U1467085AdBid();
            templateBidAd.ownerUsername = UserSession.getInstance().username;
            MatchSet allMyAds = js.contents(Collections.singleton(templateBidAd), null, SpaceUtils.TIMEOUT, Integer.MAX_VALUE);
            for (U1467085AdBid i = (U1467085AdBid) allMyAds.next(); i != null; i = (U1467085AdBid) allMyAds.next()) {
                try {
                    if (i.verifySignature(UserSession.getInstance().pubKey)) {
                        U1467085BidConfirmation alreadyConfirmed = new U1467085BidConfirmation();
                        alreadyConfirmed.adId = i.id;
                        if (Instant.ofEpochMilli(i.endDate).isAfter(Instant.now()) &&
                                js.readIfExists(alreadyConfirmed, null, SpaceUtils.TIMEOUT) == null) {
                            execService.schedule(new BidResolver(i.id),
                                    i.endDate - Instant.now().toEpochMilli(), TimeUnit.MILLISECONDS);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tries to find highest valid bid for an auction type ad.
     * Then writes confirmation for that bid to JavaSpace.
     *
     * @param ad - ad to find the highest valid bid for
     * @return highest bid or null if there are no valid bids
     */
    private U1467085Bid processBids(U1467085AdBid ad) {
        U1467085Bid finalBid = null;
        try {
            JavaSpace05 js = SpaceUtils.getSpace();
            U1467085Bid templateBid = new U1467085Bid();
            templateBid.adId = ad.id;
            MatchSet allMyAds = js.contents(Collections.singleton(templateBid), null, SpaceUtils.TIMEOUT, Integer.MAX_VALUE);
            List<U1467085Bid> bidList = new ArrayList<>();
            for (U1467085Bid i = (U1467085Bid) allMyAds.next(); i != null; i = (U1467085Bid) allMyAds.next()) {
                try {
                    if (i.verifySignature(AddressBook.getUserKey(i.bidderUserName))) {
                        bidList.add(i);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (bidList.size() > 0) {
                bidList.sort((U1467085Bid o1, U1467085Bid o2) -> o2.bid - o1.bid);
                finalBid = bidList.get(0);
                U1467085BidConfirmation confirmation = new U1467085BidConfirmation(ad.id, finalBid.id);
                confirmation.signObject(UserSession.getInstance().privKey);
                long adEndLease = ad.endDate - Instant.now().toEpochMilli() + SpaceUtils.LONGLEASE;
                js.write(confirmation, null, adEndLease);
                String notificationMessage = "Item " + ad.title + " was won by a bidder " + finalBid.bidderUserName +
                        " at " + String.format("%.2f$", (float) finalBid.bid / 100);
                Platform.runLater(() -> observableNotificationList.add(notificationMessage));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return finalBid;
    }

    @Override
    public void onClose() {
    }

    /**
     * Ads auction type ad to the schedule of ads that should be checked when time on them runs out.
     *
     * @param ad - ad to be added to the schedule
     */
    public void newAdPostedByMe(U1467085AdBase ad) {
        if (ad instanceof U1467085AdBid) {
            execService.schedule(new BidResolver(ad.id),
                    ad.endDate - Instant.now().toEpochMilli(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Runnable that is scheduled for each auction run.
     */
    private class BidResolver implements Runnable {
        private UUID adId;

        public BidResolver(UUID ad) {
            adId = ad;
        }

        @Override
        public void run() {
            try {
                JavaSpace05 js = SpaceUtils.getSpace();
                U1467085AdBid templateBidAd = new U1467085AdBid();
                templateBidAd.id = adId;
                U1467085AdBid ad = (U1467085AdBid) js.readIfExists(templateBidAd, null, SpaceUtils.TIMEOUT);

                if (ad != null && ad.verifySignature(UserSession.getInstance().pubKey)) {
                    U1467085BidConfirmation alreadyConfirmed = new U1467085BidConfirmation();
                    alreadyConfirmed.adId = ad.id;
                    if (Instant.now().isAfter(Instant.ofEpochMilli(ad.endDate)) &&
                            js.readIfExists(alreadyConfirmed, null, SpaceUtils.TIMEOUT) == null) {
                        U1467085Bid resultBid = processBids(ad);
                        if (resultBid != null && manager.showBidNotification) {
                            String notificationMessage = "Item " + ad.title + " was won by a bidder " + resultBid.bidderUserName;
                            Platform.runLater(() -> NotificationUtil.showNotification(notificationMessage));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
