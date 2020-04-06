package components.request;

public class RegisterRequest implements Request {
    private String username;
    private String password;
    private String email;
    private final RequestType requestType = RequestType.REGISTER;

    public RegisterRequest() {
        super();
    }

    public RegisterRequest(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
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

    public String getEmail() {
        return email;
    }
}
