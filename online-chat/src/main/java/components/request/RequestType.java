package components.request;

public enum RequestType {
    GETDATA(0),
    LOGIN(1),
    REGISTER(2);

    private final int requestID;

    RequestType(int requestID) {
        this.requestID = requestID;
    }

    public int getRequestID() {
        return requestID;
    }
}
