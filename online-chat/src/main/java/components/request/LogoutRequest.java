package components.request;

public class LogoutRequest implements Request {
    private String username;
    private final RequestType requestType = RequestType.LOGOUT;

    public LogoutRequest() {
        super();
    }

    public LogoutRequest(String username) {
        this.username = username;
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }

    public String getUsername() {
        return username;
    }

}
