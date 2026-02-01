# Link Shortener Service

Сервис предоставляет REST API для генерации коротких ссылок и переходов по ним.

### Функционал

- Генерация короткой ссылки по полной (идемпотентно)

- Получение полной ссылки по короткой
    
- Redirect endpoint для перехода по короткой ссылке в браузере
    
- TTL для коротких ссылок
    
- Ограничение количества запросов (rate limiting) по IP
    
- Глобальное ограничение одновременной генераций ссылок
    

# Сборка и запуск

## Запуск приложения

### IDE

1. Открыть проект в IntelliJ IDEA или Eclipse
2. Запустить main метод в классе `ShortenedLinksApplication.java`

### Gradle

`./gradlew bootRun`

### Сборка jar

```bash
./gradlew clean build
java -jar build/libs/ShortenedLinks.jar
```
---

### Доступ к приложению

Приложение будет запущено на `http://localhost:8080`

Необходимо установить креды доступа для базы данных в `src/main/resources/application.yml`:

```yaml
DB_URL=jdbc:postgresql://localhost:5432/link_shortener  
DB_USERNAME=db_username  
DB_PASSWORD=1234
```
---

# Swagger / OpenAPI

После запуска документация доступна:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
    
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
    

---

# REST API

Базовый URL:

`/api/v1/short-links`

## 1) Создать короткую ссылку по полной

### Endpoint

`POST /api/v1/short-links/shorten`

### Описание

- Создаёт короткую ссылку
    
- Операция **идемпотентная**: для одинакового `fullLink` возвращается одна и та же `shortLink` (если она не истекла, иначе генерируется новая)
    

### Request body

```json
{
  "fullLink": "https://example.com/page?a=1&b=2"
}
```


### Response

**201 CREATED** — если новая ссылка создана  
**200 OK** — если ссылка уже существовала

```json
{
  "shortLink": "aZx91Qp",
  "expiredAt": "2026-02-01T12:00:00Z",
  "shortUrl": "http://localhost:8080/api/v1/short-links/redirect/aZx91Qp"
}
```

`shortLink` - короткая ссылка, полученная напрямую из полной

`shortUrl` - готовый короткий url, который можно использовать для перехода

---

## 2) Получить полную ссылку по короткой

### Endpoint

`GET /api/v1/short-links/{short-link}`

### Описание

Возвращает полную ссылку по короткой shortLink.

### Response

**200 OK**

```json
{
  "fullLink": "https://example.com/page?a=1&b=2",
  "shortLink": "aZx91Qp",
  "createdAt": "2026-02-01T11:50:00Z",
  "expiredAt": "2026-02-01T12:00:00Z"
}
```

---

## 3) Redirect по короткой ссылке

### Endpoint

`GET /api/v1/short-links/redirect/{short-link}`

### Описание

Осуществляет редирект в браузере на полную ссылку.

### Response

- **302 FOUND** (т.к. ссылка временная)
    
- Заголовок `Location: <fullLink>`
    

---

### TTL (время жизни коротких ссылок)

Каждая короткая ссылка создаётся со временем жизни:

- `expiredAt = createdAt + ttl-minutes`
    

TTL настраивается в `application.yml`:

```yaml
link:
  ttl-minutes: 10
```

Если ссылка истекла:

 на получение fullLink / redirect возвращается ошибка:

  ```json
  {
	"errorCode": "LINK_IS_EXPIRED",
	"message": "The link has expired.",
	"status": 410,
	"path": "/api/v1/short-links/redirect/67hRIQa",
	"timestamp": "2026-02-01T17:03:16.748123300Z"
  }
  ```
    

---

# Формат ошибок API

Ошибки возвращаются в JSON:

```json
{
  "errorCode": "LINK_NOT_FOUND",
  "message": "Link not found.",
  "status": 404,
  "timestamp": "2026-02-01T12:00:00",
  "path": "/api/v1/short-links/478t3U7"
}
```

Ошибки логируются и обрабатываются глобальным обработчиком.

---

# Redis через WSL

Redis используется для реализации rate limiting.

## Установка Redis в WSL
   
В Ubuntu/Debian WSL:
    

```
sudo apt update
sudo apt install redis-server 
sudo systemctl enable redis-server
redis-server --daemonize yes
```

Проверка:

`redis-cli ping`

Ожидаемый ответ:

`PONG`

Остановка Redis:

`redis-cli shutdown`

## Конфигурация (application.yml)


```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```


---

# Ограничение на одного пользователя

Одновременно (в одну единицу времени) нельзя сгенерировать более 100 ссылок для любого из пользователей.

В качестве идентификатора пользователя используется IP адрес клиента.


## Лимитирование

- максимум `max-requests` за промежуток времени `window`
    
- промежуток считается от первого запроса пользователя (реализация между fixed window и sliding window, чтобы сделать подсчет интервалов более точным, чем в fixed window, но минимизировать хранимый объем данных в redis, требуемый в sliding window). Как только window истек, у пользователя сбрасывается счетчик и обновляется интервал времени
    

Конфигурация:

```yaml
link:
  rate-limit:
    max-requests: 100
    window: 1m
```

Если лимит превышен - возвращается ошибка:

- `429 TOO MANY REQUESTS`    

- errorCode: `TOO_MANY_REQUESTS`

---

# Глобальное ограничение генераций

В любой момент времени сервис должен выполнять не более 100 генераций коротких ссылок одновременно суммарно по всем инстансам.

Поскольку ограничение выставляется одно на все инстансы, то нужен механизм с единым состоянием, доступным всем инстансам, в данном случае redis использован как распределённый семафор через permits: каждый запрос занимает место, которое освобождается после генерации. API/Controller на любом из инстансов принимает запрос пользователя, передает его в сервисный слой LinkService, который при генерации ссылки обращается к GlobalRateLimiter, отвечающий за выделение "места" на генерацию. Redis получается общим хранилищем permit-ов, поэтому ограничение на кол-во одновременных генераций применяется ко всему кластеру, а не к отдельному серверу. 

Если сервис упал — permit освобождается автоматически по TTL (такая реализация взята, чтобы гарантировать отсутствие застрявших в redis записей, например, при его падении).

В данном случае реализация подсчета генераций была сделана не такой, как в ограничении генераций для одного пользователя, так как требуется ограничивать кол-во генераций в каждый конкретный момент, поэтому смотрим кол-во не в заданный промежуток времени, а кол-во еще не завершенных генераций на данный момент.


Если превышение лимита:

- `503 SERVICE UNAVAILABLE`
    
- errorCode: `SERVICE_IS_BUSY`
    

Если Redis недоступен:

- сервис продолжает работать без него, rate limiting отключается    
