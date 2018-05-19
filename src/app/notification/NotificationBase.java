package app.notification;

import javafx.collections.ObservableList;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;

import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public abstract class NotificationBase {

    protected final Object syncLock;
    protected EventRegistration EventRegistration;
    protected RemoteEventListener Listener;
    protected List<String> observableNotificationList;
    protected NotificationManager manager;

    public NotificationBase(NotificationManager manager, Object syncLock) {
        this.manager = manager;
        this.syncLock = syncLock;
    }

    public void setObservableList(ObservableList<String> list) {
        observableNotificationList = list;
    }

    public void onClose() {
        try {
            UnicastRemoteObject.unexportObject(Listener, true);
            if (EventRegistration != null) EventRegistration.getLease().cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
