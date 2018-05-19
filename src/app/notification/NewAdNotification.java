package app.notification;

import app.Models.U1467085AdBase;
import app.Models.U1467085AdBid;
import app.Models.U1467085AdBuy;
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
import java.util.List;
import java.util.UUID;

/**
 * Notification that listens for new ads being posted to JavaSpace
 */
public class NewAdNotification extends NotificationBase {
    //List of ads that we already know about
    //Is needed otherwise notification will fire when ad lock is released
    private List<UUID> existingAds = new ArrayList<>();

    public NewAdNotification(NotificationManager manager, Object syncLock) {
        super(manager, syncLock);
        this.Listener = new newAdListener();
        try {
            UnicastRemoteObject.exportObject(Listener, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setObservableList(ObservableList<String> list) {
        super.setObservableList(list);
        registerNewAdListener();
    }

    /**
     * Collect list of all ads that already exist
     * Start listening for new ads being added to the JavaSpace
     */
    private void registerNewAdListener() {
        try {
            if (EventRegistration != null) EventRegistration.getLease().cancel();
            JavaSpace05 js = SpaceUtils.getSpace();
            U1467085AdBid templateBid = new U1467085AdBid();
            U1467085AdBuy templateBuy = new U1467085AdBuy();
            List<U1467085AdBase> templateList = new ArrayList<>();
            templateList.add(templateBid);
            templateList.add(templateBuy);
            MatchSet existingAds = js.contents(templateList, null, SpaceUtils.TIMEOUT, Integer.MAX_VALUE);
            for (U1467085AdBase i = (U1467085AdBase) existingAds.next(); i != null; i = (U1467085AdBase) existingAds.next()) {
                try {
                    if (i.verifySignature(AddressBook.getUserKey(i.ownerUsername))) {
                        this.existingAds.add(i.id);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            EventRegistration = js.registerForAvailabilityEvent(templateList, null, false, Listener, SpaceUtils.LONGLEASE, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * RemoteEventListener that listens for ad object being written to JavaSpace
     */
    private class newAdListener implements RemoteEventListener {

        @Override
        public void notify(RemoteEvent remoteEvent) {
            synchronized (syncLock) {
                AvailabilityEvent event = (AvailabilityEvent) remoteEvent;
                try {
                    U1467085AdBase adPosted = (U1467085AdBase) event.getEntry();
                    //check if we already knew about the ad and it is just a lock being released by another user
                    if (existingAds.stream().noneMatch(item -> item.equals(adPosted.id)) &&
                            !adPosted.ownerUsername.equals(UserSession.getInstance().username) &&
                            adPosted.verifySignature(AddressBook.getUserKey(adPosted.ownerUsername))) {
                        existingAds.add(adPosted.id);
                        String message = "New ad \"" + adPosted.title + "\" posted by " + adPosted.ownerUsername;
                        Platform.runLater(() -> observableNotificationList.add(message));
                        if (manager.showNewAdNotification)
                            Platform.runLater(() -> NotificationUtil.showNotification(message));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
