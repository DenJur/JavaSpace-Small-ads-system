package app.Controllers;

import app.Errors.CommonException;
import app.Errors.ErrorTypes;
import app.Errors.FatalException;
import app.Models.*;
import app.StageManager;
import app.Util.AddressBook;
import app.Util.SpaceUtils;
import app.Util.UserSession;
import app.components.CommentCell;
import app.components.PriceSpinner;
import app.notification.NotificationManager;
import app.notification.NotificationUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.TransactionFactory;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.space.AvailabilityEvent;
import net.jini.space.JavaSpace05;
import net.jini.space.MatchSet;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

public class AdController extends ControllerBase {

    public Label labelTitle;
    public Label labelExpiration;
    public Label labelDescription;
    public ListView<U1467085Comment> listComments;
    public TextArea textAreaComment;
    public Button buttonPostComment;
    public Button buttonBack;
    public Label labelPrice;
    public PriceSpinner spinnerBid;
    public Button buttonBuy;
    public Button buttonDelete;
    public Label labelOwner;
    //List of comments for UI update.
    private ObservableList<U1467085Comment> comments;
    //Ad object that this view represents.
    private U1467085AdBase ad;
    //Listener registration for comment updates.
    private EventRegistration newCommentEventRegistration;
    //Listener for new comments
    private RemoteEventListener newCommentListener = new RemoteEventListener() {
        @Override
        public synchronized void notify(RemoteEvent theEvent) {
            AvailabilityEvent event = (AvailabilityEvent) theEvent;
            try {
                U1467085Comment newComment = (U1467085Comment) event.getEntry();
                //Add o comment list if signature is verified
                if (newComment.verifySignature(AddressBook.getUserKey(newComment.userName))) {
                    Platform.runLater(() -> comments.add(newComment));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    @FXML
    public void initialize() {
        comments = FXCollections.observableArrayList();
        //Export listener  for new comments. Not a critical problem if it fails.
        try {
            UnicastRemoteObject.exportObject(newCommentListener, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        listComments.setItems(comments);
        listComments.setCellFactory(param -> new CommentCell());
        buttonBack.setOnMouseClicked(event -> StageManager.getInstance().showMain());
        buttonPostComment.setOnMouseClicked(event -> postNewComment());
        buttonPostComment.setDisable(true);
        textAreaComment.textProperty().addListener((observable, oldValue, newValue) -> buttonPostComment.setDisable(newValue.isEmpty()));
        buttonDelete.setDisable(true);
        spinnerBid.setEditable(true);
        buttonDelete.setOnMouseClicked(mouseEvent -> deleteMe());
    }

    /**
     * Action called upon pressing of "Post comment" button.
     */
    private void postNewComment() {
        try {
            TransactionManager tm = SpaceUtils.getManager();
            Transaction.Created transaction = TransactionFactory.create(tm, SpaceUtils.TIMEOUT);
            try {
                JavaSpace05 js = SpaceUtils.getSpace();
                //Get a lock on the ad so that it can not be deleted during new comment addition.
                U1467085AdBase adLockTemplate;
                if (ad instanceof U1467085AdBuy)
                    adLockTemplate = new U1467085AdBuy();
                else
                    adLockTemplate = new U1467085AdBid();
                adLockTemplate.id = ad.id;
                U1467085AdBase adLock = (U1467085AdBase) js.takeIfExists(adLockTemplate, transaction.transaction, SpaceUtils.TIMEOUT);
                if (adLock == null) throw new CommonException("404. Ad not found.");

                //Create new comment object and sign it.
                long adEndLease = adLock.endDate - Instant.now().toEpochMilli() + SpaceUtils.LONGLEASE;
                U1467085Comment comment = new U1467085Comment(ad.id, UserSession.getInstance().username, textAreaComment.getText());
                comment.signObject(UserSession.getInstance().privKey);

                //Post the comment and release lock
                js.write(comment, transaction.transaction, adEndLease);
                js.write(adLock, transaction.transaction, adEndLease);
                transaction.transaction.commit();
                textAreaComment.setText("");
            } catch (Exception e) {
                NotificationUtil.showError("Failed to post a comment. Make sure that ad was not deleted");
                transaction.transaction.abort();
            }
        } catch (Exception e) {
            handler.addError(new CommonException("Failed to get transaction manager", e), ErrorTypes.COMMON);
            handler.showErrorDialog(ErrorTypes.COMMON);
        }
    }

    /**
     * Delete current ad.
     */
    private void deleteMe() {
        if (SpaceUtils.cleanAdRemove(ad, 0)) {
            NotificationUtil.showSuccess("Ad deleted.");
            //return to main view.
            manager.showMain();
        } else {
            NotificationUtil.showError("Failed to delete ad. Make sure that ad exists.");
        }
    }

    @Override
    public void onClose() {
        terminateListeners();
        try {
            UnicastRemoteObject.unexportObject(newCommentListener, true);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cancel comment listener filter registration.
     */
    private void terminateListeners() {
        try {
            if (newCommentEventRegistration != null) {
                newCommentEventRegistration.getLease().cancel();
                newCommentEventRegistration = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void hide() {
        super.hide();
        terminateListeners();
    }

    /**
     * Updates UI information to a new target ad. Should be called whenever new ad needs to be viewed.
     *
     * @param ad - target ad that sould be displayed.
     */
    public void updateView(U1467085AdBase ad) {
        try {
            JavaSpace05 js = SpaceUtils.getSpace();
            //Verify that ad still exists. Might be either type of ad.
            //Assume that ad is auction type ad.
            U1467085AdBid adBidTemplate = new U1467085AdBid();
            adBidTemplate.id = ad.id;
            this.ad = (U1467085AdBase) js.readIfExists(adBidTemplate, null, SpaceUtils.TIMEOUT);
            //If no ad is found the ad is either buyout type or no longer exists.
            if (this.ad == null) {
                U1467085AdBuy adBuyTemplate = new U1467085AdBuy();
                adBuyTemplate.id = ad.id;
                this.ad = (U1467085AdBase) js.readIfExists(adBuyTemplate, null, SpaceUtils.TIMEOUT);
            }
            if (this.ad != null && this.ad.verifySignature(AddressBook.getUserKey(ad.ownerUsername))) {
                //Update common ad elements.
                labelTitle.setText(this.ad.title);
                labelDescription.setText(this.ad.description);
                ZonedDateTime expr = ZonedDateTime.ofInstant(Instant.ofEpochMilli(this.ad.endDate), ZoneId.systemDefault());
                labelExpiration.setText("Expiration time: " + expr.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")));
                labelOwner.setText("Owner: " + this.ad.ownerUsername);
                textAreaComment.setText("");
                updateCommentList();
                //If user is the owner allow them to delete this ad.
                if (ad.ownerUsername.equals(UserSession.getInstance().username)) {
                    buttonDelete.setDisable(false);
                    buttonBuy.setDisable(true);
                } else {
                    buttonDelete.setDisable(true);
                    buttonBuy.setDisable(false);
                }
                //Try to update comment listener to anew filter. If it fails it is not critical and we can continue
                // working without comments.
                try {
                    terminateListeners();
                    U1467085Comment template = new U1467085Comment();
                    template.adId = this.ad.id;
                    newCommentEventRegistration = js.registerForAvailabilityEvent(Collections.singleton(template), null, false, newCommentListener, SpaceUtils.LONGLEASE, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //Call updates of specific ad types.
                if (this.ad instanceof U1467085AdBuy) {
                    updateBuyView(this.ad);
                } else {
                    updateBidView(this.ad);
                }
            } else {
                throw new CommonException("Invalid ad.");
            }
        } catch (Exception e) {
            NotificationUtil.showError("Failed to get the ad.");
            manager.showMain();
        }
    }

    /**
     * Refresh comment list for the current ad.
     */
    private void updateCommentList() {
        try {
            JavaSpace05 js = SpaceUtils.getSpace();
            U1467085Comment commentTemplate = new U1467085Comment();
            commentTemplate.adId = ad.id;
            MatchSet commentsForAd = js.contents(Collections.singleton(commentTemplate), null, SpaceUtils.TIMEOUT, Integer.MAX_VALUE);
            comments.clear();
            for (U1467085Comment i = (U1467085Comment) commentsForAd.next(); i != null; i = (U1467085Comment) commentsForAd.next()) {
                try {
                    //Verify that none of the ads are edited.
                    if (i.verifySignature(AddressBook.getUserKey(i.userName)))
                        this.comments.add(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //Sort comments by date just in case.
            comments.sort((o1, o2) -> (int) (o1.date - o2.date));
        } catch (Exception e) {
            handler.addError(new CommonException("Failed to get comment list", e), ErrorTypes.COMMON);
            handler.showErrorDialog(ErrorTypes.COMMON);
        }
    }

    /**
     * Updates view elements specific to buyout ad types.
     *
     * @param ad - Should be U1467085AdBuy type but is not verified to be.
     */
    private void updateBuyView(U1467085AdBase ad) {
        spinnerBid.setVisible(false);
        labelPrice.setText(String.format("Price: %.2f $", (float) ad.price / 100));
        buttonBuy.setText("Buy");
        buttonBuy.setOnMouseClicked(mouseEvent -> buyMe());
    }

    /**
     * Updates view elements to auction ad types.
     *
     * @param ad - Should be U1467085AdBid but is not verified to be.
     */
    private void updateBidView(U1467085AdBase ad) {
        buttonBuy.setText("Bid");
        spinnerBid.setVisible(true);
        buttonBuy.setOnMouseClicked(mouseEvent -> bidOnMe());
        try {
            U1467085Bid highestBid = findHighestBid(ad);
            //Update information about curent highest bid and limit the spiner element.
            labelPrice.setText(String.format("Highest bid: %.2f $", (float) highestBid.bid / 100));
            SpinnerValueFactory.DoubleSpinnerValueFactory factory = new SpinnerValueFactory.DoubleSpinnerValueFactory((float) (highestBid.bid + 1) / 100,
                    1000, (float) (highestBid.bid + 1) / 100, 0.01);
            factory.valueProperty().bindBidirectional(spinnerBid.getPriceFormatter().valueProperty());
            spinnerBid.getPriceFormatter().setValue((double) (highestBid.bid + 1) / 100);
            spinnerBid.getPriceFormatter().valueProperty().setValue((double) (highestBid.bid + 1) / 100);
            factory.setValue((double) (highestBid.bid + 1) / 100);
            spinnerBid.setValueFactory(factory);
        } catch (Exception e) {
            buttonBuy.setDisable(true);
            NotificationUtil.showError("Error getting bid information");
        }
    }

    /**
     * Action that posts buy order to the JavaSpace for current ad. Ad type is verified before posting.
     */
    private void buyMe() {
        try {
            TransactionManager tm = SpaceUtils.getManager();
            Transaction.Created transaction = TransactionFactory.create(tm, SpaceUtils.TIMEOUT);
            try {
                JavaSpace05 js = SpaceUtils.getSpace();
                //Get a lock on the ad and check that ad is still open.
                U1467085AdBuy adLockTemplate = new U1467085AdBuy();
                adLockTemplate.id = ad.id;
                U1467085AdBase adLock = (U1467085AdBase) js.takeIfExists(adLockTemplate, transaction.transaction, SpaceUtils.TIMEOUT);
                if (adLock == null) throw new CommonException("404. Ad not found.");
                if (Instant.now().isAfter(Instant.ofEpochMilli(adLock.endDate)))
                    throw new CommonException("Ad has ended.");
                //Check that there are no buy order posted already.
                U1467085BuyOrder buyOrderTemplate = new U1467085BuyOrder();
                buyOrderTemplate.adId = adLock.id;
                if (js.readIfExists(buyOrderTemplate, transaction.transaction, SpaceUtils.TIMEOUT) != null) {
                    throw new CommonException("Item is already bought.");
                }
                //Post new buy order and release the lock.
                long adEndLease = adLock.endDate - Instant.now().toEpochMilli() + SpaceUtils.LONGLEASE;
                U1467085BuyOrder buyTheItem = new U1467085BuyOrder(adLock.id, UserSession.getInstance().username);
                buyTheItem.signObject(UserSession.getInstance().privKey);
                js.write(buyTheItem, transaction.transaction, adEndLease);
                js.write(adLock, transaction.transaction, adEndLease);
                transaction.transaction.commit();
                StageManager.getInstance().showMain();
            } catch (CommonException e) {
                transaction.transaction.abort();
                NotificationUtil.showError(e.getMessage());
            } catch (Exception e) {
                transaction.transaction.abort();
                NotificationUtil.showError("Failed to buy the item.");
            }
        } catch (Exception e) {
            NotificationUtil.showError("Failed to get transaction manager.");
        }
    }

    /**
     * Action that posts a bid to the JavaSpace for current ad. Ad type is verified before posting.
     */
    private void bidOnMe() {
        try {
            TransactionManager tm = SpaceUtils.getManager();
            Transaction.Created transaction = TransactionFactory.create(tm, SpaceUtils.TIMEOUT);
            try {
                JavaSpace05 js = SpaceUtils.getSpace();
                //Get a lock on the ad and check that the date is still valid
                U1467085AdBid adLockTemplate = new U1467085AdBid();
                adLockTemplate.id = ad.id;
                U1467085AdBase adLock = (U1467085AdBase) js.takeIfExists(adLockTemplate, transaction.transaction, SpaceUtils.TIMEOUT);
                if (adLock == null) throw new CommonException("404. Ad not found.");
                if (Instant.now().isAfter(Instant.ofEpochMilli(adLock.endDate)))
                    throw new CommonException("Ad has ended.");

                U1467085BidConfirmation bidConfirmationTemplate = new U1467085BidConfirmation();
                bidConfirmationTemplate.adId = adLock.id;
                if (js.readIfExists(bidConfirmationTemplate, transaction.transaction, SpaceUtils.TIMEOUT) != null) {
                    throw new CommonException("Item is already bought.");
                }

                //Check if bid is too low
                U1467085Bid highestBid = findHighestBid(ad);
                if (highestBid.bid >= (int) (spinnerBid.getValue() * 100))
                    throw new CommonException("Your bid is lower than the highest bid.");

                //Post the bid and release the lock
                long adEndLease = adLock.endDate - Instant.now().toEpochMilli() + SpaceUtils.LONGLEASE;
                U1467085Bid newBid = new U1467085Bid(adLock.id, UserSession.getInstance().username, (int) (spinnerBid.getValue() * 100));
                newBid.signObject(UserSession.getInstance().privKey);
                js.write(newBid, transaction.transaction, adEndLease);
                js.write(adLock, transaction.transaction, adEndLease);
                transaction.transaction.commit();
                NotificationManager.getInstance().newBidPostedByMe(newBid);
                StageManager.getInstance().showMain();
            } catch (CommonException e) {
                transaction.transaction.abort();
                NotificationUtil.showError(e.getMessage());
            } catch (Exception e) {
                transaction.transaction.abort();
                NotificationUtil.showError("Failed to buy the item.");
            }
        } catch (Exception e) {
            NotificationUtil.showError("Failed to get transaction manager.");
        }
    }

    /**
     * Finds and returns the highest bid for the given ad. If no bid is found creates a new bid object and sets the
     * bid amount equal to the ad price.
     *
     * @param ad - ad to find the bid for
     * @return
     * @throws FatalException
     * @throws RemoteException
     * @throws TransactionException
     * @throws UnusableEntryException
     */
    private U1467085Bid findHighestBid(U1467085AdBase ad) throws FatalException, RemoteException, TransactionException, UnusableEntryException {
        JavaSpace05 js = SpaceUtils.getSpace();
        U1467085Bid bidTemplate = new U1467085Bid();
        bidTemplate.adId = ad.id;
        MatchSet allBids = js.contents(Collections.singleton(bidTemplate), null, SpaceUtils.TIMEOUT, Integer.MAX_VALUE);
        U1467085Bid highestBid = new U1467085Bid();
        highestBid.bid = ad.price;
        //iterate through all the bids for the ad
        for (U1467085Bid i = (U1467085Bid) allBids.next(); i != null; i = (U1467085Bid) allBids.next()) {
            try {
                if (i.verifySignature(AddressBook.getUserKey(i.bidderUserName)) &&
                        i.bid >= highestBid.bid)
                    highestBid = i;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return highestBid;
    }
}
