package app.notification;

import javafx.util.Duration;
import tray.animations.AnimationType;
import tray.notification.NotificationType;
import tray.notification.TrayNotification;

/**
 * Couple methods used for tray notification display.
 */
public class NotificationUtil {

    public static void showError(String message) {
        TrayNotification tray = new TrayNotification("Error", message, NotificationType.ERROR);
        tray.setAnimationType(AnimationType.POPUP);
        tray.showAndDismiss(Duration.seconds(1));
    }

    public static void showSuccess(String message) {
        TrayNotification tray = new TrayNotification("Success", message, NotificationType.SUCCESS);
        tray.setAnimationType(AnimationType.POPUP);
        tray.showAndDismiss(Duration.seconds(1));
    }

    public static void showNotification(String message) {
        TrayNotification tray = new TrayNotification("Notification", message, NotificationType.INFORMATION);
        tray.setAnimationType(AnimationType.POPUP);
        tray.showAndDismiss(Duration.seconds(1));
    }
}
