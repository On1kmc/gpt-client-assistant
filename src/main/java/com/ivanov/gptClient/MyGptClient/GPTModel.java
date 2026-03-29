package com.ivanov.gptClient.MyGptClient;

public enum GPTModel {

    GPT_5_4("gpt-5.4"),
    GPT_5_4PRO("gpt-5.4-pro"),
    GPT_5_4_MINI("gpt-5.4-mini"),
    GPT_5_4_NANO("gpt-5.4-nano"),
    GPT_5("gpt-5"),
    GPT_5_MINI("gpt-5-mini"),
    GPT_5_NANO("gpt-5-nano"),
    GPT_4_1("gpt-4.1");

    final String title;

    GPTModel(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
