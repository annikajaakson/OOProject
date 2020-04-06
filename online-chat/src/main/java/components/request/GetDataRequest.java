package components.request;

public class GetDataRequest implements Request {
    private String username;
    private final RequestType requestType = RequestType.GETDATA;

    public GetDataRequest() {
        super();
    }

    public GetDataRequest(String username) {
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