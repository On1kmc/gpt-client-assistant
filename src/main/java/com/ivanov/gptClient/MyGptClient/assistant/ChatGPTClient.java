package com.ivanov.gptClient.MyGptClient.assistant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivanov.gptClient.MyGptClient.*;
import com.ivanov.gptClient.MyGptClient.entities.GPTResponse;
import com.ivanov.gptClient.MyGptClient.entities.GptRequestEntity;
import com.ivanov.gptClient.MyGptClient.messages.ContentPart;
import com.ivanov.gptClient.MyGptClient.messages.GptMessage;
import com.ivanov.gptClient.MyGptClient.messages.ImageContentPart;
import com.ivanov.gptClient.MyGptClient.messages.TextContentPart;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class ChatGPTClient {

    final String ENDPOINT = "https://api.openai.com/v1/responses";
    private final String API_TOKEN;
    private final GPTModel GPT_MODEL;
    private final ConcurrentMap<Long, GPTThread> threadsMap;
    private final boolean needSystemMessage;
    private GptMessage systemMessage;
    private final long expirationMillis;
    private final ConcurrentMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();
    private final boolean needHistory;

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "threads-cleaner");
        t.setDaemon(true);
        return t;
    });

    private ChatGPTClient(Builder builder) {
        API_TOKEN = builder.apiToken;
        GPT_MODEL = builder.model;

        this.needSystemMessage = builder.needSystemMessage;
        boolean needCleaner = builder.needCleaner;
        this.threadsMap = new ConcurrentHashMap<>();
        this.needHistory = builder.needHistory;

        this.expirationMillis = builder.expirationMillis;
        if (needCleaner) {
            startCleaner(1,  TimeUnit.MINUTES);
        }

        if (needSystemMessage) {
            this.systemMessage = getSystemMessage(builder.instruction);
        } else {
            this.systemMessage = null;
        }
    }




    public static class Builder {

        private String apiToken;
        private GPTModel model;
        private String instruction;
        private boolean needSystemMessage = false;
        private boolean needCleaner = false;
        private boolean needHistory = false;
        private long expirationMillis;

        // --- setters ---
        public Builder apiToken(String apiToken) {
            this.apiToken = apiToken;
            return this;
        }

        public Builder model(GPTModel model) {
            this.model = model;
            return this;
        }

        public Builder enableCleaner(TimeUnit unit, long duration) {
            this.needCleaner = true;
            this.expirationMillis = unit.toMillis(duration);
            this.needHistory = true;
            return this;
        }

        public Builder enableHistory() {
            this.needHistory = true;
            return this;
        }

        public Builder enableSystemMessage(String instruction) {
            this.needSystemMessage = true;
            this.instruction = instruction;
            return this;
        }

        // --- BUILD ---
        public ChatGPTClient build() {
            validate();
            return new ChatGPTClient(this);
        }

        // ===== VALIDATION =====
        private void validate() {
            if (apiToken == null || apiToken.isBlank()) {
                throw new IllegalStateException("API token is required");
            }

            if (model == null) {
                throw new IllegalStateException("GPT model is required");
            }

            if (needSystemMessage && (instruction == null || instruction.isBlank())) {
                throw new IllegalStateException("System message enabled but not provided");
            }

            if (needCleaner && expirationMillis == 0) {
                throw new IllegalStateException("Expiration must be > 0");
            }

            if (needCleaner && !needHistory) {
                throw new IllegalStateException("history is not needed, but cleaner is enabled");
            }
        }
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
                    userLocks.remove(entry.getKey());
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
                GPTThread nt;
                if (needSystemMessage) {
                    List<GptMessage> list = new ArrayList<>();
                    list.add(systemMessage);
                    nt = new GPTThread(list);
                } else {
                    nt = new GPTThread(new ArrayList<>());
                }
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
        userLocks.remove(userId);
    }



    public GPTResponse sendTextMessage(Long userId, String messageText) throws JsonProcessingException {
        return sendMessages(userId, messageText, null, false);
    }
    public GPTResponse sendTextMessageWithWeb(Long userId, String messageText) throws JsonProcessingException {
        return sendMessages(userId, messageText, null, true);
    }

    public GPTResponse sendImageMessage(Long userId, String imageLink) throws JsonProcessingException {
        return sendMessages(userId, null, imageLink, false);
    }
    public GPTResponse sendImageMessageWithWeb(Long userId, String imageLink) throws JsonProcessingException {
        return sendMessages(userId, null, imageLink, true);
    }

    public GPTResponse sendMessages(Long userId, String messageText, String imageUrl, boolean needWeb) throws JsonProcessingException {
        ReentrantLock lock = userLocks.computeIfAbsent(userId, id -> new ReentrantLock());

        lock.lock();
        try {
            List<GptMessage> messages;
            if (needHistory) {
                GPTThread thread = getOrCreateThread(userId);
                messages = thread.getMessages();
            } else {
                messages = new ArrayList<>();
                if (needSystemMessage) {
                    messages.add(systemMessage);
                }
            }


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

            GptRequestEntity requestEntity = new GptRequestEntity(needWeb);
            requestEntity.setModel(GPT_MODEL.getTitle());
            requestEntity.setInput(messages);

            String stringObject = objectMapper.writeValueAsString(requestEntity);
            StringEntity stringEntity = new StringEntity(stringObject, StandardCharsets.UTF_8);

            GPTResponse response = sendRequest(stringEntity);

            if (needHistory) {
                GptMessage answerMsg = new GptMessage();
                List<ContentPart> content = new ArrayList<>();
                TextContentPart textContent = new TextContentPart();
                textContent.setText(response.getAnswer());
                textContent.setType("output_text");
                content.add(textContent);
                answerMsg.setContent(content);
                answerMsg.setRole("assistant");
                messages.add(answerMsg);
            }
            return response;
        } finally {
            lock.unlock();
        }
    }


    private GPTResponse sendRequest(StringEntity stringEntity) {
        HttpPost request = new HttpPost(ENDPOINT);
        request.addHeader("Authorization", "Bearer " + API_TOKEN);
        request.addHeader("Content-Type", "application/json");
        request.setEntity(stringEntity);

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .disableCookieManagement()
                .build()) {
            String str = httpclient.execute(request, response -> EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));

            ObjectMapper mapper = new ObjectMapper();
            JsonNode obj = mapper.readTree(str);
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

    public void setSystemMessage(String instruction) {
        GptMessage systemGRPMessage = new GptMessage();
        TextContentPart systemMessage = new TextContentPart();
        systemMessage.setText(instruction);
        systemMessage.setType("input_text");
        systemGRPMessage.setContent(List.of(systemMessage));
        systemGRPMessage.setRole("system");
        this.systemMessage = systemGRPMessage;
    }
}
