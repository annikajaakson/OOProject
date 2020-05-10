package components.request;

public enum RequestType {
    GETDATA(0),
    LOGIN(1),
    LOGOUT(2),
    REGISTER(3),
    MESSAGE(4),
    CONVERSATION(5),
    FORWARD(6);

    private final int requestID;

    RequestType(int requestID) {
        this.requestID = requestID;
    }

    public int getRequestID() {
        return requestID;
    }
}
