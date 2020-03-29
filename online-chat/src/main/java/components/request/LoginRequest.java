package components.request;

public class LoginRequest extends Request {
    private final String username;
    private final String password;

    public LoginRequest(String username, String password) {
        super.setRequestType(RequestType.LOGIN);
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
