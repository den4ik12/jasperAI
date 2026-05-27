# JasperAI

Spring Boot приложение, интегрирующее DeepSeek AI через Spring AI.

## Стек

- Java 17
- Spring Boot 3.5.13
- Spring AI 1.1.3 (deepseek starter)
- Lombok
- Maven

## Команды

```bash
# Сборка
./mvnw clean package

# Запуск
./mvnw spring-boot:run

# Тесты
./mvnw test
```

## Конфигурация

`src/main/resources/application.properties` — ключ DeepSeek API, базовый URL, параметры модели (`deepseek-chat`, temperature=0.01).

> Не коммитить API-ключи в репозиторий.

## Структура

```
src/main/java/ru/volodin/jasperai/   — основной код
src/test/java/ru/volodin/jasperai/   — тесты
```
