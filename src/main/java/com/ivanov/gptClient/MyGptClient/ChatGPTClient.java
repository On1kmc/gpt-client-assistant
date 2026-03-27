package com.ivanov.gptClient.MyGptClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ChatGPTClient {

    private final String ENDPOINT = "https://api.openai.com/v1/responses";
    private final String API_TOKEN;
    private final GPTModel GPT_MODEL;
    private final ConcurrentMap<Long, GPTThread> threadsMap;
    private final GptMessage systemMessage;
    private volatile long expirationMillis;

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "threads-cleaner");
        t.setDaemon(true);
        return t;
    });

    public ChatGPTClient(String apiToken, GPTModel GPT_MODEL, String instruction) {
        threadsMap = new ConcurrentHashMap<>();
        API_TOKEN = apiToken;
        this.GPT_MODEL = GPT_MODEL;
        systemMessage = getSystemMessage(instruction);
        setTimeout(TimeUnit.HOURS, 1);
        startCleaner(1,  TimeUnit.MINUTES);
    }

    public ChatGPTClient(String apiToken, GPTModel GPT_MODEL, String instruction, TimeUnit timeUnit, int timeout) {
        threadsMap = new ConcurrentHashMap<>();
        API_TOKEN = apiToken;
        this.GPT_MODEL = GPT_MODEL;
        systemMessage = getSystemMessage(instruction);
        setTimeout(timeUnit, timeout);
        startCleaner(1,  TimeUnit.MINUTES);
    }



    public void setTimeout(TimeUnit unit, long value) {
        if (unit == null) throw new IllegalArgumentException("unit is null");
        if (value <= 0) throw new IllegalArgumentException("value must be > 0");
        this.expirationMillis = unit.toMillis(value);
    }


    // Запустить фоновую уборку (checkInterval — как часто проверять)
    public void startCleaner(long checkInterval, TimeUnit unit) {
        // предотвратить повторный запуск
        cleaner.scheduleAtFixedRate(this::cleanUp, 0, Math.max(1, checkInterval), unit);
    }

    // Остановить уборщик (вызывайте при shutdown)
    public void stopCleaner() {
        cleaner.shutdownNow();
    }

    // Основная логика очистки
    private void cleanUp() {
        try {
            long now = System.currentTimeMillis();
            for (Map.Entry<Long, GPTThread> entry : threadsMap.entrySet()) {
                GPTThread thread = entry.getValue();
                if (thread == null) continue;
                long last = thread.getLastAccessTimeMillis();
                if (now - last >= expirationMillis) {
                    threadsMap.remove(entry.getKey(), thread);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // Пример получения/создания потока: не забываем обновлять метку активности
    public GPTThread getOrCreateThread(Long userId) {
        return threadsMap.compute(userId, (k, existing) -> {
            if (existing == null) {
                List<GptMessage> list = new ArrayList<>();
                list.add(systemMessage);
                GPTThread nt = new GPTThread(list);
                nt.touch();
                return nt;
            } else {
                existing.touch();
                return existing;
            }
        });
    }

    // Принудительно удалить
    public void removeThread(Long userId) {
        threadsMap.remove(userId);
    }



    public String sendTextMessage(Long userId, String messageText, String name) throws JsonProcessingException {
        return sendMessages(userId, messageText, null, name);
    }

    public String sendImageMessage(Long userId, String imageLink, String name) throws JsonProcessingException {
        return sendMessages(userId, null, imageLink, name);
    }

    public String sendMessages(Long userId, String messageText, String name) throws JsonProcessingException {
        return sendMessages(userId, messageText, null, name);
    }

    public String sendMessages(Long userId, String messageText, String imageUrl, String name) throws JsonProcessingException {
        GPTThread thread = getOrCreateThread(userId);
        List<GptMessage> messages = thread.getMessages();

        GptMessage gptMessage = new GptMessage();
        List<ContentPart> contentParts = new ArrayList<>();


        if (messageText != null) {
            TextContentPart textContentPart = new TextContentPart();
            textContentPart.setType("input_text");
            textContentPart.setText(messageText);
            contentParts.add(textContentPart);
        }

        if (imageUrl != null) {
            ImageContentPart imageContentPart = new ImageContentPart();
            imageContentPart.setImage_url(imageUrl);
            imageContentPart.setType("input_image");
            contentParts.add(imageContentPart);
        }

        gptMessage.setContent(contentParts);
        gptMessage.setRole("user");
        messages.add(gptMessage);

        ObjectMapper objectMapper = new ObjectMapper();

        GptRequestEntity requestEntity = new GptRequestEntity();
        requestEntity.setModel(GPT_MODEL.getTitle());
        requestEntity.setInput(messages);

        String stringObject = objectMapper.writeValueAsString(requestEntity);
        StringEntity stringEntity = new StringEntity(stringObject, StandardCharsets.UTF_8);

        String answer = sendRequest(stringEntity);

        GptMessage answerMsg = new GptMessage();
        List<ContentPart> content = new ArrayList<>();
        TextContentPart textContent = new TextContentPart();
        textContent.setText(answer);
        textContent.setType("output_text");
        content.add(textContent);
        answerMsg.setContent(content);
        answerMsg.setRole("assistant");
        messages.add(answerMsg);

        return answer;
    }


    private String sendRequest(StringEntity stringEntity) {
        HttpPost request = new HttpPost(ENDPOINT);
        request.addHeader("Authorization", "Bearer " + API_TOKEN);
        request.addHeader("Content-Type", "application/json");
        request.setEntity(stringEntity);

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .disableCookieManagement()
                .build()) {
            try (CloseableHttpResponse response = httpclient.execute(request)) {
                HttpEntity responseEntity = response.getEntity();
                try (InputStream ins = responseEntity.getContent()) {
                    byte[] bytes1 = ins.readAllBytes();
                    String str = new String(bytes1, StandardCharsets.UTF_8);
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode obj = mapper.readTree(str);
                    JsonNode jsonNode = obj.get("output");
                    StringBuilder stringBuilder = new StringBuilder();
                    for (JsonNode jsonNode1 : jsonNode) {
                        if (jsonNode1.get("type").asText().equals("message")) {
                            stringBuilder.append(jsonNode1.get("content").get(0).get("text").textValue()).append("\n");
                        }
                    }
                    return stringBuilder.toString();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }


    public GptMessage getSystemMessage(String instruction) {
        GptMessage systemGRPMessage = new GptMessage();
        TextContentPart systemMessage = new TextContentPart();
        systemMessage.setText(instruction);
        systemMessage.setType("input_text");
        systemGRPMessage.setContent(List.of(systemMessage));
        systemGRPMessage.setRole("system");
        return systemGRPMessage;
    }
}
