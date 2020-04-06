package onlineChat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import components.request.RequestType;

// Dummy class for reading a generic request (with only request type as a field) from JSON string
// This is necessary so that we can detect request type for further processing
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientRequest {
    private RequestType requestType;

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }
}
