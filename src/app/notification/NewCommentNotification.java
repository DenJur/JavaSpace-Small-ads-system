package app.notification;

import app.Models.U1467085AdBase;
import app.Models.U1467085AdBid;
import app.Models.U1467085AdBuy;
import app.Models.U1467085Comment;
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

public class NewCommentNotification extends NotificationBase {

    private ArrayList<U1467085Comment> commentTemplates;

    public NewCommentNotification(NotificationManager manager, Object syncLock) {
        super(manager, syncLock);
        Listener = new newCommentListener();
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
            commentTemplates = new ArrayList<>();
            for (U1467085AdBase i = (U1467085AdBase) existingAds.next(); i != null; i = (U1467085AdBase) existingAds.next()) {
                try {
                    if (i.verifySignature(AddressBook.getUserKey(i.ownerUsername))) {
                        U1467085Comment commentTemplate = new U1467085Comment();
                        commentTemplate.adId = i.id;
                        commentTemplates.add(commentTemplate);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (commentTemplates.size() > 0)
                EventRegistration = js.registerForAvailabilityEvent(commentTemplates, null, false, Listener, SpaceUtils.LONGLEASE, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void newAdPostedByMe(U1467085AdBase ad) {
        try {
            JavaSpace05 js = SpaceUtils.getSpace();
            if (EventRegistration != null) {
                EventRegistration.getLease().cancel();
            }
            U1467085Comment newTemplate = new U1467085Comment();
            newTemplate.adId = ad.id;
            commentTemplates.add(newTemplate);
            EventRegistration = js.registerForAvailabilityEvent(commentTemplates, null, false, Listener, SpaceUtils.LONGLEASE, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class newCommentListener implements RemoteEventListener {

        @Override
        public void notify(RemoteEvent remoteEvent) {
            synchronized (syncLock) {
                AvailabilityEvent event = (AvailabilityEvent) remoteEvent;
                try {
                    U1467085Comment newComment = (U1467085Comment) event.getEntry();
                    if (!newComment.userName.equals(UserSession.getInstance().username) &&
                            newComment.verifySignature(AddressBook.getUserKey(newComment.userName))) {
                        JavaSpace05 js = SpaceUtils.getSpace();
                        U1467085AdBuy adTemplateBuy = new U1467085AdBuy();
                        adTemplateBuy.id = newComment.adId;
                        U1467085AdBid adTemplateBid = new U1467085AdBid();
                        adTemplateBid.id = newComment.adId;
                        U1467085AdBase ad = (U1467085AdBase) js.readIfExists(adTemplateBuy, null, SpaceUtils.TIMEOUT);
                        if (ad == null) ad = (U1467085AdBase) js.readIfExists(adTemplateBid, null, SpaceUtils.TIMEOUT);
                        if (ad == null || !ad.verifySignature(UserSession.getInstance().pubKey)) return;
                        String message = "New comment posted by " + newComment.userName + " on item " + ad.title;
                        Platform.runLater(() -> observableNotificationList.add(message));
                        if (manager.showNewCommentNotification)
                            Platform.runLater(() -> NotificationUtil.showNotification(message));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
