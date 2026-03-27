package com.ivanov.gptClient.MyGptClient;

import java.util.List;


public class GptRequestEntity {

    private String model;

    private List<GptMessage> input;


    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<GptMessage> getInput() {
        return input;
    }

    public void setInput(List<GptMessage> input) {
        this.input = input;
    }
}
