package com.ivanov.gptClient.MyGptClient;

public enum GPTModel {

    GPT_5_4("gpt-5.4"),
    GPT_5_4_MINI("gpt-5.4-mini");

    String title;

    GPTModel(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
