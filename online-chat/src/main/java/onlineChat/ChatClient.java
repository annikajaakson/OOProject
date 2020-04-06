package onlineChat;

import com.fasterxml.jackson.databind.ObjectMapper;
import components.chat.Conversation;
import components.chat.User;
import components.database.Database;
import components.request.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.stream.Collectors;

/**
 * JavaFX App
 */
public class ChatClient extends Application {
    private String ipAddress = "localhost";
    private int port = 1337;

    private User currentUser;

    private final int WINDOW_WIDTH = 800;
    private final int WINDOW_HEIGHT = 600;
    private Scene logInScene, registerScene, chatScene;

    public boolean sendRequest(Request request) {
        try (Socket socket = new Socket(ipAddress, port);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            System.out.println("connected to chat server");

            var mapper = new ObjectMapper();

            out.writeUTF(mapper.writeValueAsString(request));

            var response = mapper.readValue(in.readUTF(), Response.class);
            if (response.getResponseType() == ResponseType.OK_NO_DATA) {
                System.out.println("Request completed successfully");
                System.out.println(response.getErrorMsg());
                return true;
            } else if (response.getResponseType() == ResponseType.OK_WITH_DATA) {
                System.out.println("Request completed successfully");
                System.out.println(response.getErrorMsg());

                // TODO: Figure out a better way to send user data from server
                if (request.getRequestType() == RequestType.GETDATA) {
                    // Read current user data from socket
                    Database currentUserData = mapper.readValue(in.readUTF(), Database.class);
                    // Initialize current user
                    currentUser = currentUserData.getUsers().get(0);
                    currentUser.setConversations(currentUserData.getConversations());
                }

                return true;
            } else if (response.getResponseType() == ResponseType.ERROR) {
                System.out.println("There was an error while completing the request: ");
                System.out.println(response.getErrorMsg());
                return false;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

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
            if (!usernameField.getText().equals("") && !pwdField.getText().equals("")) {
                boolean reqStatus = sendRequest(new LoginRequest(usernameField.getText(), pwdField.getText()));

                // If login was not successful, return
                if (!reqStatus) return;

                // Fetch user data and save it in currentUser
                reqStatus = sendRequest(new GetDataRequest(usernameField.getText()));

                // If data fetching was not successful, return
                if (!reqStatus) return;

                // If all server communication worked, create the chat scene and navigate to it
                chatScene = createChatScene(primaryStage);
                primaryStage.setScene(chatScene);
                usernameField.setText("");
                pwdField.setText("");
            }
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
            if (!usernameField.getText().equals("")
                    && !pwdField.getText().equals("")
                    && pwdField.getText().equals(repPwdField.getText())
                    && !emailField.getText().equals("")) {
                boolean reqStatus = sendRequest(new RegisterRequest(usernameField.getText(), pwdField.getText(), emailField.getText()));

                // If register request was successful
                if (reqStatus) {
                    primaryStage.setScene(this.logInScene);
                    usernameField.setText("");
                    pwdField.setText("");
                    repPwdField.setText("");
                    emailField.setText("");
                }
            }
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
        ObservableList<Conversation> conversationList = FXCollections.observableArrayList();
        conversationList.addAll(currentUser.getConversations());
        ListView<Conversation> conversationListView = new ListView<>(conversationList);
        conversationListView.setMinWidth(150);
        conversationListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Conversation item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || item.getParticipants() == null) {
                    setText(null);
                } else {
                    setText(item.getParticipants().stream()
                            .filter(user -> user.getId() != currentUser.getId())
                            .map(User::getUsername)
                            .collect(Collectors.joining(", ")));
                }
            }
        });

        // Conversation area
        var messagesArea = new TextArea();
        messagesArea.setEditable(false);

        // Show messages in convo when convo is selected from menu (conversation list view)
        conversationListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Conversation selectedConvo = conversationListView.getSelectionModel().getSelectedItem();
                messagesArea.setText(selectedConvo.getMessages().stream()
                        .map(message -> message.getSender().getUsername() + ": " + message.getContent())
                        .collect(Collectors.joining("\n")));
            }
        });

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
        gridPane.add(conversationListView, 0, 1);
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
        registerScene = createRegisterScene(primaryStage);

        primaryStage.setScene(logInScene);
        primaryStage.setTitle("Fake messenger");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}