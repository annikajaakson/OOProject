package components.request;

import java.util.List;

public class ConversationRequest implements Request {
    private List<String> participantNames;
    private final RequestType requestType = RequestType.CONVERSATION;

    public ConversationRequest() {
        super();
    }

    public ConversationRequest(List<String> participantNames) {
        this.participantNames = participantNames;
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }

    public List<String> getParticipantNames() {
        return participantNames;
    }
}
