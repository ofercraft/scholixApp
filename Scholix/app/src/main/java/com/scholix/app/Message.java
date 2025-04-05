package com.scholix.app;

public class Message {
    public String sender;
    public String title;
    public String iconUrl;

    public Message(String sender, String title, String iconUrl) {
        this.sender = sender;
        this.title = title;
        this.iconUrl = iconUrl;
    }
}
