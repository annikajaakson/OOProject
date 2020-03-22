package oop2020;

import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX App
 */
public class ChatClient extends Application {
    private List<String> conversations = new ArrayList<>();

    private final int WINDOW_WIDTH = 800;
    private final int WINDOW_HEIGHT = 600;
    private Scene logInScene, registerScene, chatScene;

    public Scene createLogInScene (Stage primaryStage) {
        // All elements are stored in a FlowPane
        var container = new FlowPane();
        // Gaps between container elements
        container.setVgap(5);
        // Position log in fields at the center of the window
        container.setAlignment(Pos.CENTER);
        // FlowPane is oriented vertically
        container.setOrientation(Orientation.VERTICAL);

        // Log-in screen contents
        var logInLabel = new Label("Log in");
        var usernameLabel = new Label("Username:");
        var usernameField = new TextField();
        usernameField.setMaxWidth(200);
        var pwdLabel = new Label("Password:");
        var pwdField = new PasswordField();
        pwdField.setMaxWidth(200);
        var logInButton = new Button("Log in");
        var registerLink = new Hyperlink("Not a member yet? Register here");

        // Go to chat area when log in button is pressed
        logInButton.setOnAction(event -> {
            primaryStage.setScene(this.chatScene);
            usernameField.setText("");
            pwdField.setText("");
        });
        // Go to register window when register link clicked
        registerLink.setOnAction(event -> primaryStage.setScene(this.registerScene));

        container.getChildren().addAll(
                logInLabel,
                usernameLabel,
                usernameField,
                pwdLabel,
                pwdField,
                logInButton,
                registerLink
        );

        return new Scene(container, WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    public Scene createRegisterScene (Stage primaryStage) {
        // All elements are stored in a FlowPane
        var container = new FlowPane();
        // Gaps between container elements
        container.setVgap(5);
        // Position register fields at the center of the window
        container.setAlignment(Pos.CENTER);
        // FlowPane is oriented vertically
        container.setOrientation(Orientation.VERTICAL);

        // Register screen contents
        var regLabel = new Label("Register");
        var usernameLabel = new Label("Username:");
        var usernameField = new TextField();
        usernameField.setMaxWidth(200);
        var pwdLabel = new Label("Password:");
        var pwdField = new PasswordField();
        pwdField.setMaxWidth(200);
        var repPwdLabel = new Label("Repeat password:");
        var repPwdField = new PasswordField();
        repPwdField.setMaxWidth(200);
        var emailLabel = new Label("Email:");
        var emailField = new TextField();
        emailField.setMinWidth(200);
        var regButton = new Button("Register");

        // Go to log in screen on register button press
        regButton.setOnAction(event -> {
            primaryStage.setScene(this.logInScene);
            usernameField.setText("");
            pwdField.setText("");
            repPwdField.setText("");
            emailField.setText("");
        });

        container.getChildren().addAll(
                regLabel,
                usernameLabel,
                usernameField,
                pwdLabel,
                pwdField,
                repPwdLabel,
                repPwdField,
                emailLabel,
                emailField,
                regButton
        );

        return new Scene(container, WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    public Scene createChatScene(Stage primaryStage) {
        // Create dummy conersations
        for (int i = 0; i < 40; i++) {
            conversations.add("Conversation " + (i + 1));
        }

        // All elements are stored in a gridpane
        var gridPane = new GridPane();
        // Gaps between grid elements
        gridPane.setVgap(10);
        gridPane.setHgap(10);
        // Position grid at the center of the window
        gridPane.setAlignment(Pos.CENTER);

        // New conversation button
        var newConvoButton = new Button("New Conversation");
        newConvoButton.setOnAction(event -> showNewConvoPopup());
        // Button for seeing, adding and searching for contacts
        var contactsButton = new Button("Contacts");

        // Hbox for holding buttons created above
        var buttonBox = new HBox();
        buttonBox.getChildren().addAll(newConvoButton, contactsButton);
        buttonBox.setSpacing(5);

        // Log out button
        var logOutButton = new Button("Log out");
        logOutButton.setOnAction(event -> primaryStage.setScene(this.logInScene));

        // List of available contacts
        ListView<String> contactsList = new ListView<>();
        contactsList.getItems().addAll(conversations);
        contactsList.setMinWidth(150);

        // Conversation area
        var messagesArea = new TextArea();
        messagesArea.setEditable(false);

        // New message input
        var inputField = new TextField();
        inputField.setMinWidth(300);
        // Send message button
        var sendButton = new Button("Send");

        // HBox for typing message and sed button
        var inputBox = new HBox();
        inputBox.setSpacing(5);
        inputBox.getChildren().addAll(inputField, sendButton);

        gridPane.add(buttonBox, 0, 0);
        gridPane.add(logOutButton, 1, 0);
        gridPane.setHalignment(logOutButton, HPos.RIGHT);
        gridPane.add(contactsList, 0, 1);
        gridPane.add(messagesArea, 1, 1);
        gridPane.add(inputBox, 1, 2);

        return new Scene(gridPane, WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    public void showNewConvoPopup() {
        // Create new window
        var newConvoWindow = new Stage();
        newConvoWindow.initModality(Modality.APPLICATION_MODAL);
        newConvoWindow.setTitle("Create new conversation");

        // All elements are stored in a gridpane
        var gridPane = new GridPane();
        // Gaps between grid elements
        gridPane.setVgap(10);
        gridPane.setHgap(10);
        // Position grid at the center of the window
        gridPane.setAlignment(Pos.CENTER);

        // New conversation window contents
        var infoLabel = new Label("Type names to add people to the conversation");
        var nameField = new TextField();
        var addButton = new Button("Add to conversation");
        // People already added to the conversation
        ListView<String> convoPeople = new ListView<>();
        convoPeople.setPrefHeight(150);
        var createButton = new Button("Create new conversation");

        // Add people to listview if add button is pressed
        addButton.setOnAction(event -> {
            convoPeople.getItems().add(nameField.getText());
            nameField.setText("");
        });

        // Close window when conversation created
        createButton.setOnAction(event -> newConvoWindow.close());

        gridPane.add(infoLabel, 0, 0);
        gridPane.add(nameField, 0, 1);
        gridPane.add(addButton, 1, 1);
        gridPane.add(convoPeople, 0, 2);
        gridPane.add(createButton, 0, 3);

        // Show window
        newConvoWindow.setScene(new Scene(gridPane, 400, 300));
        newConvoWindow.show();
    }

    @Override
    public void start(Stage primaryStage) {
        logInScene = createLogInScene(primaryStage);
        chatScene = createChatScene(primaryStage);
        registerScene = createRegisterScene(primaryStage);

        primaryStage.setScene(logInScene);
        primaryStage.setTitle("Fake messenger");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}