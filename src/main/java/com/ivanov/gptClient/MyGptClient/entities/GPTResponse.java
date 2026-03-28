package com.ivanov.gptClient.MyGptClient.entities;


public class GPTResponse {

    private String answer;
    private long inputTokens;
    private long outputTokens;


    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public long getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(long inputTokens) {
        this.inputTokens = inputTokens;
    }

    public long getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(long outputTokens) {
        this.outputTokens = outputTokens;
    }

    @Override
    public String toString() {
        return "GPTResponse{" +
                "answer='" + answer + '\'' +
                ", inputTokens=" + inputTokens +
                ", outputTokens=" + outputTokens +
                '}';
    }
}
