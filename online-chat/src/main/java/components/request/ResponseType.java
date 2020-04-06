package components.request;

public enum ResponseType {
    OK_NO_DATA(0),
    OK_WITH_DATA(1),
    ERROR(2);

    private final int responseID;

    ResponseType(int responseID) {
        this.responseID = responseID;
    }

    public int getResponseID() {
        return responseID;
    }
}
