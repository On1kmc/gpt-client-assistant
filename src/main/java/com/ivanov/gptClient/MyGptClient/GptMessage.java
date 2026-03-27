package com.ivanov.gptClient.MyGptClient;


import java.util.List;


public class GptMessage {

    private String role;

    private List<ContentPart> content;


    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<ContentPart> getContent() {
        return content;
    }

    public void setContent(List<ContentPart> content) {
        this.content = content;
    }
}
