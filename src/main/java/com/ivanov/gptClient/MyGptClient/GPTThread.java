package com.ivanov.gptClient.MyGptClient;

import java.util.List;

public class GPTThread {

    private volatile long lastAccessTimeMillis = System.currentTimeMillis();

    private volatile long tokens;
    private List<GptMessage> messages;

    public GPTThread(List<GptMessage> list) {
        messages = list;
    }



    public void touch() {
        lastAccessTimeMillis = System.currentTimeMillis();
    }

    public long getLastAccessTimeMillis() {
        return lastAccessTimeMillis;
    }

    public List<GptMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<GptMessage> messages) {
        this.messages = messages;
    }
}
