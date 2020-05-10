package components.request;

import java.util.List;

public class ForwardRequest implements Request{
    private String messageContent;
    private int senderId;
    private List<Integer> conversationIds;
    private final RequestType requestType = RequestType.FORWARD;

    public ForwardRequest() {
        super();
    }

    public ForwardRequest(String messageContent, int senderId, List<Integer> conversationIds) {
        this.messageContent = messageContent;
        this.senderId = senderId;
        this.conversationIds = conversationIds;
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public int getSenderId() {
        return senderId;
    }

    public List<Integer> getConversationIds() {
        return conversationIds;
    }
}
