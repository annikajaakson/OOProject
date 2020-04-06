package components.request;

public class Response {
    private ResponseType responseType;
    private String errorMsg;

    public Response() {
        super();
    }

    public Response(ResponseType responseType, String errorMsg) {
        this.responseType = responseType;
        this.errorMsg = errorMsg;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public String getErrorMsg() {
        return errorMsg;
    }
}
