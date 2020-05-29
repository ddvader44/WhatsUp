package com.example.whatsup.Chat;

import java.util.ArrayList;

public class MessageObject {
    String messageId, senderId , message ;
    ArrayList<String> mediaUrllist;
    public MessageObject(String messageId, String senderId, String message,ArrayList<String> mediaUrllist)
    {
        this.messageId = messageId;
        this.senderId = senderId;
        this.message = message;
        this.mediaUrllist = mediaUrllist;
    }
    public String getMessage() { return message; }
    public String getMessageId() { return messageId;}
    public String getSenderId() { return senderId; }
    public ArrayList<String> getMediaUrllist() { return mediaUrllist; }
}
