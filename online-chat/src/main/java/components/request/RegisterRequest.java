package components.request;

public class RegisterRequest extends Request {
    private final String username;
    private final String password;
    private final String email;

    public RegisterRequest(String username, String password, String email) {
        super.setRequestType(RequestType.REGISTER);
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }
}
