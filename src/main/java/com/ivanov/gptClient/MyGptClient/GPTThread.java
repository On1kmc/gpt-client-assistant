package com.ivanov.gptClient.MyGptClient;

import java.util.List;

public class GPTThread {

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
