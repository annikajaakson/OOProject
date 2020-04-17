package components.request;

public enum ResponseType {
    OK_NO_DATA(0),
    OK_WITH_DATA(1),
    NEW_MESSAGE(2),
    ERROR(3);

    private final int responseID;

    ResponseType(int responseID) {
        this.responseID = responseID;
    }

    public int getResponseID() {
        return responseID;
    }
}
