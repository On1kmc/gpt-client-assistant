# 🤖 GPT Client Assistant

[![](https://jitpack.io/v/On1kmc/gpt-client-assistant.svg)](https://jitpack.io/#On1kmc/gpt-client-assistant)
![Java](https://img.shields.io/badge/Java-24-orange)
![Maven](https://img.shields.io/badge/Maven-3.6+-blue)
![License](https://img.shields.io/badge/License-MIT-green)

Высокопроизводительная Java библиотека для интеграции с OpenAI GPT API с поддержкой многопоточности, истории сообщений и управления потоками разговоров.

## ✨ Возможности

- ✅ **Поддержка текстовых и графических сообщений**
- ✅ **Управление историей сообщений** (conversation threads)
- ✅ **Многопоточность** с синхронизацией через `ReentrantLock`
- ✅ **Автоматическая очистка** неиспользуемых потоков
- ✅ **Web search интеграция** (возможность использовать информацию из интернета)
- ✅ **System message поддержка** (контроль поведения ассистента)
- ✅ **Builder pattern** для удобной конфигурации
- ✅ **SLF4J логирование** (выбери свой логгер)

## 📦 Установка

### Maven

Добавьте JitPack репозиторий:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Добавьте зависимость:

```xml
<dependency>
    <groupId>com.github.On1kmc</groupId>
    <artifactId>gpt-client-assistant</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Логирование (опционально)

Выберите один из логгеров для SLF4J:

```xml
<!-- Logback (рекомендуется) -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.11</version>
</dependency>

<!-- Или Log4j2 -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-slf4j2-impl</artifactId>
    <version>2.20.0</version>
</dependency>
```

## 🚀 Быстрый старт

### Базовый пример

```java
import com.ivanov.gptClient.MyGptClient.GPTModel;
import com.ivanov.gptClient.MyGptClient.assistant.ChatGPTClient;
import com.ivanov.gptClient.MyGptClient.entities.GPTResponse;

// Создание клиента
ChatGPTClient client = new ChatGPTClient.Builder()
    .apiToken("sk-your-api-key-here")
    .model(GPTModel.GPT_5_4)
    .build();

// Отправка текстового сообщения
Long userId = 1L;
GPTResponse response = client.sendTextMessage(userId, "Привет, как дела?");

System.out.println("Ответ: " + response.getAnswer());
System.out.println("Входные токены: " + response.getInputTokens());
System.out.println("Выходные токены: " + response.getOutputTokens());
```

### С системным сообщением (System Message)

```java
ChatGPTClient client = new ChatGPTClient.Builder()
    .apiToken("sk-your-api-key-here")
    .model(GPTModel.GPT_5_4)
    .enableSystemMessage("Ты полезный ассистент. Ответь кратко и по делу.")
    .build();

GPTResponse response = client.sendTextMessage(1L, "Что такое Java?");
```

### С историей сообщений

```java
ChatGPTClient client = new ChatGPTClient.Builder()
    .apiToken("sk-your-api-key-here")
    .model(GPTModel.GPT_5_4)
    .enableSystemMessage("Ты помощник программиста.")
    .enableHistory()  // Сохранять историю
    .build();

Long userId = 1L;

// Первое сообщение
GPTResponse response1 = client.sendTextMessage(userId, "Как создать ArrayList в Java?");
System.out.println(response1.getAnswer());

// Второе сообщение - клиент помнит контекст!
GPTResponse response2 = client.sendTextMessage(userId, "А как добавить элемент?");
System.out.println(response2.getAnswer());  // Ответит с учётом предыдущего контекста
```

### С автоматической очисткой потоков

```java
import java.util.concurrent.TimeUnit;

ChatGPTClient client = new ChatGPTClient.Builder()
    .apiToken("sk-your-api-key-here")
    .model(GPTModel.GPT_5_4)
    .enableCleaner(TimeUnit.HOURS, 1)  // Удалять потоки неиспользуемые более 1 часа
    .build();

// Клиент будет автоматически удалять старые потоки каждую минуту
```

### С web search

```java
ChatGPTClient client = new ChatGPTClient.Builder()
    .apiToken("sk-your-api-key-here")
    .model(GPTModel.GPT_5_4)
    .build();

// Включить поиск в интернете
GPTResponse response = client.sendTextMessageWithWeb(1L, "Какая сейчас погода в Москве?");
```

### Отправка изображений

```java
// Отправить изображение
GPTResponse response = client.sendImageMessage(1L, 
    "https://example.com/image.jpg");

System.out.println("Описание: " + response.getAnswer());

// С web search
GPTResponse responseWithWeb = client.sendImageMessageWithWeb(1L,
    "https://example.com/image.jpg");
```

## 📚 API Документация

### ChatGPTClient

#### Builder Configuration

| Метод | Описание | Параметры | Обязателен |
|-------|---------|-----------|-----------|
| `apiToken(String)` | API ключ OpenAI | строка | ✅ Да |
| `model(GPTModel)` | Выбор GPT модели | `GPT_5_4`, `GPT_5_4_MINI` | ✅ Да |
| `enableSystemMessage(String)` | Установить системное сообщение | инструкция для ассистента | ❌ Нет |
| `enableHistory()` | Включить сохранение истории | - | ❌ Нет |
| `enableCleaner(TimeUnit, long)` | Автоматическая очистка потоков | единица времени, длительность | ❌ Нет |
| `build()` | Создать клиента | - | ✅ Да |

#### Основные методы

```java
// Текстовые сообщения
GPTResponse sendTextMessage(Long userId, String messageText) 
    throws JsonProcessingException;

GPTResponse sendTextMessageWithWeb(Long userId, String messageText) 
    throws JsonProcessingException;

// Графические сообщения
GPTResponse sendImageMessage(Long userId, String imageLink) 
    throws JsonProcessingException;

GPTResponse sendImageMessageWithWeb(Long userId, String imageLink) 
    throws JsonProcessingException;

```

### GPTModel

```java
public enum GPTModel {
    GPT_5_4("gpt-5.4"),           // Основная модель
    GPT_5_4_MINI("gpt-5.4-mini")  // Лёгкая версия
}
```

## 🔐 Безопасность

### Получение API ключа

1. Перейдите на [OpenAI Platform](https://platform.openai.com/)
2. Войдите в свой аккаунт
3. Перейдите в [API Keys](https://platform.openai.com/api-keys)
4. Создайте новый ключ

### Хранение API ключа

**❌ НИКОГДА** не hardcode'ируйте ключ в коде:

```java
// ❌ НЕПРАВИЛЬНО
String apiKey = "sk-your-api-key-here";
```

**✅ Используйте переменные окружения:**

```java
String apiKey = System.getenv("OPENAI_API_KEY");

ChatGPTClient client = new ChatGPTClient.Builder()
    .apiToken(apiKey)
    .model(GPTModel.GPT_5_4)
    .build();
```

**✅ Или конфигурационные файлы:**

```properties
# application.properties
openai.api.key=${OPENAI_API_KEY}
```

## 💡 Примеры использования

### Пример 1: Простой чатбот

```java
public class SimpleChatbot {
    public static void main(String[] args) throws Exception {
        ChatGPTClient client = new ChatGPTClient.Builder()
            .apiToken(System.getenv("OPENAI_API_KEY"))
            .model(GPTModel.GPT_5_4)
            .enableSystemMessage("Ты дружелюбный и полезный бот.")
            .enableHistory()
            .build();
        
        Scanner scanner = new Scanner(System.in);
        Long userId = 1L;
        
        System.out.println("Введите вопрос (для выхода напишите 'exit'):");
        while (true) {
            String question = scanner.nextLine();
            if ("exit".equalsIgnoreCase(question)) break;
            
            GPTResponse response = client.sendTextMessage(userId, question);
            System.out.println("Бот: " + response.getAnswer());
        }
    }
}
```

### Пример 2: Многопользовательский чат

```java
public class MultiUserChat {
    private final ChatGPTClient client;
    private final Map<Long, String> userContexts = new ConcurrentHashMap<>();
    
    public MultiUserChat() {
        this.client = new ChatGPTClient.Builder()
            .apiToken(System.getenv("OPENAI_API_KEY"))
            .model(GPTModel.GPT_5_4)
            .enableHistory()
            .enableCleaner(TimeUnit.HOURS, 2)
            .build();
    }
    
    public String processUserMessage(Long userId, String message) 
            throws JsonProcessingException {
        GPTResponse response = client.sendTextMessage(userId, message);
        return response.getAnswer();
    }
    
    public void removeUserSession(Long userId) {
        client.removeThread(userId);
        userContexts.remove(userId);
    }
}
```

### Пример 3: Анализ изображений

```java
public class ImageAnalyzer {
    private final ChatGPTClient client;
    
    public ImageAnalyzer() {
        this.client = new ChatGPTClient.Builder()
            .apiToken(System.getenv("OPENAI_API_KEY"))
            .model(GPTModel.GPT_5_4)
            .enableSystemMessage("Ты эксперт по анализу изображений. Дай детальное описание.")
            .build();
    }
    
    public String analyzeImage(Long userId, String imageUrl) 
            throws JsonProcessingException {
        GPTResponse response = client.sendImageMessage(userId, imageUrl);
        return response.getAnswer();
    }
    
    public String analyzeImageWithContext(Long userId, String imageUrl) 
            throws JsonProcessingException {
        GPTResponse response = client.sendImageMessageWithWeb(userId, imageUrl);
        return response.getAnswer();
    }
}
```

## ⚙️ Конфигурация логирования

### Logback (logback.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.ivanov.gptClient" level="DEBUG"/>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### Log4j2 (log4j2.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
    </Appenders>
    
    <Loggers>
        <Logger name="com.ivanov.gptClient" level="DEBUG"/>
        <Root level="INFO">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

### Spring Boot (application.properties)

```properties
logging.level.root=INFO
logging.level.com.ivanov.gptClient=DEBUG
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

## 🐛 Обработка ошибок

```java
try {
    GPTResponse response = client.sendTextMessage(1L, "Привет");
} catch (JsonProcessingException e) {
    // Ошибка при обработке JSON
    System.err.println("JSON Processing Error: " + e.getMessage());
} catch (RuntimeException e) {
    // Ошибк�� при запросе к API
    System.err.println("API Error: " + e.getMessage());
}
```

## 📊 Мониторинг использования токенов

```java
long totalInputTokens = 0;
long totalOutputTokens = 0;

for (int i = 0; i < 10; i++) {
    GPTResponse response = client.sendTextMessage(
        1L, 
        "Расскажи анекдот"
    );
    
    totalInputTokens += response.getInputTokens();
    totalOutputTokens += response.getOutputTokens();
    
    System.out.printf(
        "Запрос %d: Input=%d, Output=%d%n",
        i + 1,
        response.getInputTokens(),
        response.getOutputTokens()
    );
}

System.out.printf(
    "Итого: Input=%d, Output=%d, Total=%d%n",
    totalInputTokens,
    totalOutputTokens,
    totalInputTokens + totalOutputTokens
);
```


## 📧 Контакты

- GitHub Issues: [Report a bug](https://github.com/On1kmc/gpt-client-assistant/issues)
- Автор: [@On1kmc](https://github.com/On1kmc)

## ⭐ Благодарности

Спасибо за использование библиотеки! Если она вам нравится, не забудьте поставить ⭐ на GitHub.

---

