package components.request;

public class MessageRequest implements Request {
    private String messageContent;
    private int senderId;
    private int conversationId;
    private final RequestType requestType = RequestType.MESSAGE;

    public MessageRequest() {
        super();
    }

    public MessageRequest(String messageContent, int senderId, int conversationId) {
        this.messageContent = messageContent;
        this.senderId = senderId;
        this.conversationId = conversationId;
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

    public int getConversationId() {
        return conversationId;
    }
}
