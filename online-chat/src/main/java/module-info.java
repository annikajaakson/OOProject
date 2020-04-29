module onlineChat {
    requires javafx.controls;
    requires com.fasterxml.jackson.databind;
    requires mail;
    exports onlineChat;
    exports components.database;
    exports components.request;
    opens components.request;
    exports components.chat;
}