package com.ivanov.gptClient.MyGptClient;

import java.util.List;

public class GPTThread {

    private volatile long lastAccessTimeMillis = System.currentTimeMillis();

    public void touch() {
        lastAccessTimeMillis = System.currentTimeMillis();
    }

    public long getLastAccessTimeMillis() {
        return lastAccessTimeMillis;
    }

    public GPTThread(List<GptMessage> list) {
        messages = list;
    }


    private List<GptMessage> messages;

    public List<GptMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<GptMessage> messages) {
        this.messages = messages;
    }
}
