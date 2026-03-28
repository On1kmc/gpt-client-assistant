package com.ivanov.gptClient.MyGptClient.entities;

import com.ivanov.gptClient.MyGptClient.messages.GptMessage;

import java.util.List;


public class GptRequestEntity {

    public GptRequestEntity() {
    }
    public GptRequestEntity(boolean needWeb) {
        if (needWeb) {
            this.tools = List.of(new GPTTool());
        }
    }

    private String model;

    private List<GptMessage> input;

    private List<GPTTool> tools;


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

    public List<GPTTool> getTools() {
        return tools;
    }

    public void setTools(List<GPTTool> tools) {
        this.tools = tools;
    }
}
