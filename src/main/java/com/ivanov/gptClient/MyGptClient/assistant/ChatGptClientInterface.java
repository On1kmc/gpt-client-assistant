package com.ivanov.gptClient.MyGptClient.assistant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivanov.gptClient.MyGptClient.entities.GPTResponse;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.nio.charset.StandardCharsets;

public interface ChatGptClientInterface {

    String ENDPOINT = "https://api.openai.com/v1/responses";
    ObjectMapper objectMapper = new ObjectMapper();

    GPTResponse sendMessages(Long userId, String messageText, String imageUrl, boolean needWeb) throws JsonProcessingException;

    default GPTResponse sendRequest(StringEntity stringEntity, String API_TOKEN) {
        HttpPost request = new HttpPost(ENDPOINT);
        request.addHeader("Authorization", "Bearer " + API_TOKEN);
        request.addHeader("Content-Type", "application/json");
        request.setEntity(stringEntity);

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .disableCookieManagement()
                .build()) {
            String str = httpclient.execute(request, response -> EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));

            JsonNode obj = objectMapper.readTree(str);
            JsonNode jsonNode = obj.get("output");
            StringBuilder stringBuilder = new StringBuilder();
            for (JsonNode jsonNode1 : jsonNode) {
                if (jsonNode1.get("type").asText().equals("message")) {
                    stringBuilder.append(jsonNode1.get("content").get(0).get("text").textValue()).append("\n");
                }
            }
            GPTResponse myResponse = new GPTResponse();
            myResponse.setAnswer(stringBuilder.toString());
            JsonNode usageNode = obj.get("usage");
            myResponse.setInputTokens(usageNode.get("input_tokens").numberValue().longValue());
            myResponse.setOutputTokens(usageNode.get("output_tokens").numberValue().longValue());

            return myResponse;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send request to GPT API", e);
        }
    }
}
