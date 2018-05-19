package app.Controllers;

import app.Errors.CommonException;
import app.Errors.ErrorTypes;
import app.Errors.FatalException;
import app.Models.U1467085AdBase;
import app.Models.U1467085AdBid;
import app.Models.U1467085AdBuy;
import app.Models.U1467085BuyOrder;
import app.Util.AdTypes;
import app.Util.AddressBook;
import app.Util.SpaceUtils;
import app.Util.UserSession;
import app.components.AdCell;
import app.components.DateTimePicker;
import app.components.PriceSpinner;
import app.notification.NotificationManager;
import app.notification.NotificationUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import net.jini.core.entry.Entry;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.space.AvailabilityEvent;
import net.jini.space.JavaSpace05;
import net.jini.space.MatchSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;

import java.rmi.NoSuchObjectException;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class MainController extends ControllerBase {

    public ListView<U1467085AdBase> adsListView;
    public PriceSpinner priceSpinner;
    public DateTimePicker closeDatePicker;
    public ChoiceBox<AdTypes> typeChoiceBox;
    public TextArea descriptionTextArea;
    public TextField titleTextField;
    public Button clearButton;
    public Button postButton;
    public TextField searchTextField;
    public ListView<U1467085AdBase> myAdListView;
    public ListView<String> notificationListView;
    public ChoiceBox<AdTypes> searchAdTypeChoiceBox;
    public Label priceLabel;
    public CheckBox adNotificationBox;
    public CheckBox itemBoughtNotificationBox;
    public CheckBox auctionFinishedNotificationBox;
    public CheckBox commentNotificationBox;
    public CheckBox bidConfirmedNotificationBox;
    public Button notificationApplyButton;
    //list of browsable ads
    private ObservableList<U1467085AdBase> browsingList;
    //list of user owned ads
    private ObservableList<U1467085AdBase> myList;
    //list of notifications
    private ObservableList<String> notificationList;
    //filter for ads based on keywords(title,owner,description)
    private Predicate<U1467085AdBase> keywordFilter = item -> searchTextField.getText().isEmpty() ||
            StringUtils.containsIgnoreCase(item.title, searchTextField.getText()) ||
            StringUtils.containsIgnoreCase(item.description, searchTextField.getText()) ||
            StringUtils.containsIgnoreCase(item.ownerUsername, searchTextField.getText());
    //filter ads based on ad type
    private Predicate<U1467085AdBase> typeFilter = item -> searchAdTypeChoiceBox.getValue() == AdTypes.ANY ||
            (searchAdTypeChoiceBox.getValue() == AdTypes.BUY && item instanceof U1467085AdBuy) ||
            (searchAdTypeChoiceBox.getValue() == AdTypes.BID && item instanceof U1467085AdBid);
    //filter ads that are out of date or have been bought already
    private Predicate<U1467085AdBase> unavailableFilter = item -> {
        if (Instant.now().isAfter(Instant.ofEpochMilli(item.endDate)))
            return false;
        try {
            JavaSpace05 js = SpaceUtils.getSpace();
            if (item instanceof U1467085AdBuy) {
                U1467085BuyOrder buyOrderTemplate = new U1467085BuyOrder();
                buyOrderTemplate.adId = item.id;
                return js.readIfExists(buyOrderTemplate, null, SpaceUtils.TIMEOUT) == null;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    };
    private EventRegistration newAdEventRegistration;
    //listen for new ads being added to the space
    private RemoteEventListener newAdListener = new RemoteEventListener() {
        @Override
        public synchronized void notify(RemoteEvent theEvent) {
            AvailabilityEvent event = (AvailabilityEvent) theEvent;
            try {
                U1467085AdBase newAdd = (U1467085AdBase) event.getEntry();
                if (newAdd.verifySignature(AddressBook.getUserKey(newAdd.ownerUsername))) {
                    if (newAdd.ownerUsername.equals(UserSession.getInstance().username) &&
                            myList.stream().noneMatch(item -> item.id.equals(newAdd.id)))
                        Platform.runLater(() -> myList.add(newAdd));
                    else if (!newAdd.ownerUsername.equals(UserSession.getInstance().username) &&
                            keywordFilter.evaluate(newAdd) && typeFilter.evaluate(newAdd) &&
                            browsingList.stream().noneMatch(item -> item.id.equals(newAdd.id)))
                        Platform.runLater(() -> browsingList.add(newAdd));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @FXML
    public void initialize() {
        browsingList = FXCollections.observableArrayList();
        myList = FXCollections.observableArrayList();
        adsListView.setItems(browsingList);
        adsListView.setCellFactory(param -> new AdCell());
        myAdListView.setItems(myList);
        myAdListView.setCellFactory(param -> new AdCell());
        notificationList = FXCollections.observableArrayList();
        NotificationManager.getInstance().setObservableNotificationList(notificationList);
        try {
            UnicastRemoteObject.exportObject(newAdListener, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SpinnerValueFactory<Double> factory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.01, 1000, 0.01, 0.01);
        factory.valueProperty().bindBidirectional(priceSpinner.getPriceFormatter().valueProperty());
        priceSpinner.setValueFactory(factory);
        typeChoiceBox.getItems().addAll(AdTypes.BUY, AdTypes.BID);
        searchAdTypeChoiceBox.getItems().addAll(AdTypes.ANY, AdTypes.BUY, AdTypes.BID);
        searchAdTypeChoiceBox.setValue(AdTypes.ANY);
        postButton.setOnMouseClicked(event -> handlerPost());
        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> refreshLists());
        searchAdTypeChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshLists());
        typeChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == AdTypes.BUY) {
                priceLabel.setText("Price: ");
            } else {
                priceLabel.setText("Starting Bid:");
            }
        });
        cleanPostingForm();
        clearButton.setOnMouseClicked(event -> cleanPostingForm());
        notificationApplyButton.setOnMouseClicked(event -> applyNotificationSettings());
    }

    /**
     * Handles new ad posting to JavaSpace
     */
    private void handlerPost() {
        if (!checkPostFields()) return;
        try {
            JavaSpace05 sp = SpaceUtils.getSpace();
            LocalDateTime localDate = closeDatePicker.getDateTimeValue();
            ZonedDateTime date = ZonedDateTime.of(localDate, ZoneId.systemDefault());
            U1467085AdBase newAd;
            //create different ad object based on selected ad type
            if (typeChoiceBox.getValue().equals(AdTypes.BUY))
                newAd = new U1467085AdBuy(UserSession.getInstance().username, titleTextField.getText(),
                        descriptionTextArea.getText(), (int) (priceSpinner.getValue() * 100), date);
            else
                newAd = new U1467085AdBid(UserSession.getInstance().username, titleTextField.getText(),
                        descriptionTextArea.getText(), (int) (priceSpinner.getValue() * 100), date);
            newAd.signObject(UserSession.getInstance().privKey);
            sp.write(newAd, null, date.toInstant().toEpochMilli() - Instant.now().toEpochMilli() + SpaceUtils.LONGLEASE);
            cleanPostingForm();
            //notify manage that new ad should be looked for
            NotificationManager.getInstance().newAdPostedByMe(newAd);
            NotificationUtil.showSuccess("Ad posted successfully");
        } catch (FatalException | NoSuchAlgorithmException | NoSuchProviderException e) {
            handler.addError(e, ErrorTypes.FATAL);
            handler.showErrorDialog(ErrorTypes.FATAL);
        } catch (Exception e) {
            handler.addError(e, ErrorTypes.COMMON);
            handler.showErrorDialog(ErrorTypes.COMMON);
            NotificationUtil.showError("Failed to post the ad.");
        }
    }

    /**
     * refresh all lists of ads
     */
    private void refreshLists() {
        List<U1467085AdBase> newAdList = getAllValidAds();
        refreshMyAds(newAdList);
        filterAds(newAdList);
        refreshBrowsingAds(newAdList);
    }

    /**
     * Clear all fields and selections on new ad posting screen
     */
    private void cleanPostingForm() {
        titleTextField.setText("");
        descriptionTextArea.setText("");
        closeDatePicker.setDateTimeValue(LocalDateTime.now());
        typeChoiceBox.setValue(AdTypes.BUY);
        priceSpinner.getValueFactory().setValue(0.01);
    }

    private void applyNotificationSettings() {
        NotificationManager manager = NotificationManager.getInstance();
        manager.showNewAdNotification = adNotificationBox.isSelected();
        manager.showBidNotification = auctionFinishedNotificationBox.isSelected();
        manager.showBidConfirmedNotification = bidConfirmedNotificationBox.isSelected();
        manager.showBoughtNotification = itemBoughtNotificationBox.isSelected();
        manager.showNewCommentNotification = commentNotificationBox.isSelected();
    }

    /**
     * Checks that are fields for new ad posting are valid
     *
     * @return - if are field values are valid
     */
    private boolean checkPostFields() {
        if (titleTextField.getText().isEmpty() || descriptionTextArea.getText().isEmpty()) {
            NotificationUtil.showError("Ad requires title and a description.");
            return false;
        }
        if (!closeDatePicker.getDateTimeValue().isAfter(LocalDateTime.now().plusMinutes(5))) {
            NotificationUtil.showError("Ad end date should be at least 5 minutes from now");
            return false;
        }
        if (priceSpinner.getValue() < 0.01 || priceSpinner.getValue() > 1000.0) {
            NotificationUtil.showError("Item price should be between 0.01$ and 1000$");
            return false;
        }
        return true;
    }

    /**
     * Get list of all ads that are still valid for buying/bidding
     *
     * @return - list of all ads that are not out of date or already bought
     */
    private List<U1467085AdBase> getAllValidAds() {
        List<U1467085AdBase> adBaseList = new ArrayList<>();
        try {
            JavaSpace05 js = SpaceUtils.getSpace();
            List<Entry> allTemplates = new ArrayList<>();
            allTemplates.add(new U1467085AdBuy());
            allTemplates.add(new U1467085AdBid());
            MatchSet allAds = js.contents(allTemplates, null, SpaceUtils.TIMEOUT, Integer.MAX_VALUE);
            for (U1467085AdBase i = (U1467085AdBase) allAds.next(); i != null; i = (U1467085AdBase) allAds.next()) {
                try {
                    if (unavailableFilter.evaluate(i) &&
                            i.verifySignature(AddressBook.getUserKey(i.ownerUsername))) {
                        adBaseList.add(i);
                    }
                } catch (Exception e) {
                    handler.addError(new CommonException("Failed to validate ad: " + i.id, e), ErrorTypes.COMMON);
                    handler.showErrorDialog(ErrorTypes.COMMON);
                }
            }
        } catch (FatalException e) {
            handler.addError(e, ErrorTypes.FATAL);
            handler.showErrorDialog(ErrorTypes.FATAL);
        } catch (Exception e) {
            handler.addError(e, ErrorTypes.COMMON);
            handler.showErrorDialog(ErrorTypes.COMMON);
        }
        return adBaseList;
    }

    /**
     * Filter out and display all ads owned by current user.
     *
     * @param newAdList - list of ads
     */
    private void refreshMyAds(List<U1467085AdBase> newAdList) {
        myList.clear();
        UserSession session = UserSession.getInstance();
        for (U1467085AdBase i : newAdList) {
            if (i.ownerUsername.equals(session.username)) {
                myList.add(i);
            }
        }
        newAdList.removeAll(myList);
    }

    /**
     * Apply all filters to ad list
     *
     * @param adList - ad list
     */
    private void filterAds(List<U1467085AdBase> adList) {
        CollectionUtils.filter(adList, keywordFilter);
        CollectionUtils.filter(adList, typeFilter);
    }

    /**
     * Display all ads from the list
     *
     * @param newAdList - ad list
     */
    private void refreshBrowsingAds(List<U1467085AdBase> newAdList) {
        browsingList.clear();
        browsingList.addAll(newAdList);
    }

    @Override
    public void onClose() {
        terminateListeners();
        NotificationManager.getInstance().onClose();
        try {
            UnicastRemoteObject.unexportObject(newAdListener, true);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void display() {
        super.display();
        refreshLists();
        notificationListView.setItems(notificationList);
        try {
            JavaSpace05 js = SpaceUtils.getSpace();
            U1467085AdBid templateBid = new U1467085AdBid();
            U1467085AdBuy templateBuy = new U1467085AdBuy();
            List<U1467085AdBase> templateList = new ArrayList<>();
            templateList.add(templateBid);
            templateList.add(templateBuy);
            newAdEventRegistration = js.registerForAvailabilityEvent(templateList, null, false, newAdListener, SpaceUtils.LONGLEASE, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void hide() {
        super.hide();
        terminateListeners();
        notificationListView.setItems(null);
    }

    /**
     * Cancel new ad listener filter registration.
     */
    private void terminateListeners() {
        try {
            if (newAdEventRegistration != null) {
                newAdEventRegistration.getLease().cancel();
                newAdEventRegistration = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
