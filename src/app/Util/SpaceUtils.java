package app.Util;

import app.Errors.FatalException;
import app.Models.*;
import app.notification.NotificationManager;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.TransactionFactory;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.space.JavaSpace;
import net.jini.space.JavaSpace05;

import java.rmi.RemoteException;

/**
 * Static tools for working with JavaSpace. Originally written by Garry Allan.
 * Later modified to reuse javaspace connection and transaction manager
 */
public class SpaceUtils {

    public final static long LONGLEASE = 1000 * 60 * 60 * 24;
    public final static long TIMEOUT = 1000 * 5;
    private final static String HOST = "localhost";
    //reuse java space and transaction manager to save time and not reinitialize
    private static JavaSpace05 javaSpace;
    private static TransactionManager transactionManager;

    public static JavaSpace05 getSpace() throws FatalException {
        if (javaSpace == null) {
            javaSpace = getSpaceNew();
        }
        return javaSpace;
    }

    private static JavaSpace05 getSpaceNew() throws FatalException {
        JavaSpace js;
        try {
            LookupLocator l = new LookupLocator("jini://" + HOST);

            ServiceRegistrar sr = l.getRegistrar();

            Class c = Class.forName("net.jini.space.JavaSpace");
            Class[] classTemplate = {c};

            js = (JavaSpace) sr.lookup(new ServiceTemplate(null, classTemplate, null));

        } catch (Exception e) {
            throw new FatalException("No JavaSpace found.", e);
        }
        return (JavaSpace05) js;
    }


    public static TransactionManager getManager() throws FatalException {
        if (transactionManager == null) {
            transactionManager = getManagerNew();
        }
        return transactionManager;
    }

    private static TransactionManager getManagerNew() throws FatalException {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        TransactionManager tm;
        try {
            LookupLocator l = new LookupLocator("jini://" + HOST);

            ServiceRegistrar sr = l.getRegistrar();

            Class c = Class.forName("net.jini.core.transaction.server.TransactionManager");
            Class[] classTemplate = {c};

            tm = (TransactionManager) sr.lookup(new ServiceTemplate(null, classTemplate, null));

        } catch (Exception e) {
            throw new FatalException("No transaction manager found.", e);
        }
        return tm;
    }

    /**
     * Removes whole object tree for any ad endpoint.
     *
     * @param ad         - ad that should be deleted with all related objects.
     * @param retryCount - number of times operation should be retried.
     * @return
     */
    public static boolean cleanAdRemove(U1467085AdBase ad, Integer retryCount) {
        try {
            TransactionManager tm = SpaceUtils.getManager();
            Transaction.Created transaction = TransactionFactory.create(tm, SpaceUtils.TIMEOUT);
            try {
                JavaSpace05 js = SpaceUtils.getSpace();
                U1467085AdBase adTemplate;
                if (ad instanceof U1467085AdBuy)
                    adTemplate = new U1467085AdBuy();
                else
                    adTemplate = new U1467085AdBid();
                U1467085AdBase adLock = (U1467085AdBase) js.takeIfExists(adTemplate, transaction.transaction, SpaceUtils.TIMEOUT);
                if (adLock == null) {
                    transaction.transaction.abort();
                    return false;
                }

                U1467085Comment commentTemplate = new U1467085Comment();
                commentTemplate.adId = adLock.id;
                Entry e;
                do {
                    e = js.takeIfExists(commentTemplate, transaction.transaction, SpaceUtils.TIMEOUT);
                } while (e != null);
                cleanAdRemoveByType(ad, transaction, js, adLock);
                transaction.transaction.commit();
                return true;
            } catch (Exception e) {
                transaction.transaction.abort();
                if (retryCount > 0)
                    NotificationManager.getInstance().rescheduleTask(() -> cleanAdRemove(ad, retryCount - 1));
                e.printStackTrace();
            }
        } catch (Exception e) {
            if (retryCount > 0)
                NotificationManager.getInstance().rescheduleTask(() -> cleanAdRemove(ad, retryCount - 1));
            e.printStackTrace();
        }
        return false;
    }

    private static void cleanAdRemoveByType(U1467085AdBase ad, Transaction.Created transaction, JavaSpace05 js, U1467085AdBase adLock) throws UnusableEntryException, TransactionException, InterruptedException, RemoteException {
        if (ad instanceof U1467085AdBuy) {
            U1467085BuyOrder buyOrderTemplate = new U1467085BuyOrder();
            buyOrderTemplate.adId = adLock.id;
            Entry e;
            do {
                e = js.takeIfExists(buyOrderTemplate, transaction.transaction, SpaceUtils.TIMEOUT);
            } while (e != null);
        } else {
            U1467085Bid bidOrderTemplate = new U1467085Bid();
            bidOrderTemplate.adId = adLock.id;
            Entry e;
            do {
                e = js.takeIfExists(bidOrderTemplate, transaction.transaction, SpaceUtils.TIMEOUT);
            } while (e != null);

            U1467085BidConfirmation bidNotificationTemplate = new U1467085BidConfirmation();
            bidNotificationTemplate.adId = adLock.id;
            do {
                e = js.takeIfExists(bidNotificationTemplate, transaction.transaction, SpaceUtils.TIMEOUT);
            } while (e != null);
        }
    }
}