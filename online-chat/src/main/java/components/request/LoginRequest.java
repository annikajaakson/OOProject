package components.request;

public class LoginRequest implements Request {
    private String username;
    private String password;
    private final RequestType requestType = RequestType.LOGIN;

    public LoginRequest() {
        super();
    }

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
