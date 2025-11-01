# JumpHash Event Router — Realistic Demo (Spring Boot + Kafka)

Демо показує перехід від наївного роутингу (V1) до **Jump Consistent Hashing** (V2) у реалістичному сетапі: окремий Producer-сервіс, кілька Consumer-сервісів, Kafka як шина подій, та Dashboard для візуалізації розподілу і lag.

> **Git-політика**
>
> * **V1** у гілці `1-7-jump-hash`
    >   Комміт: `V1: Manual/Modulo router, minimal UI, basic tests`
> * **V2** у гілці `1-7-jump-hash-completed`
    >   Історія V1 + **рівно один** комміт: `V2: Switch to Jump Consistent Hashing;`
> * Цей README додається у «першому» комміті V1 і **надалі не змінюється**.
    >   Перехід на V2 робимо **без правок** до README, UI, API, тестів і `pom.xml`.

---

## Зміст

1. Архітектура
2. Технології
3. Швидкий старт (Kafka + сервіси)
4. Як спостерігати демо
5. Режими V1 та відмінності V2
6. V1 **Unsafe mode** (наочний анти-патерн): конфіг і код
7. Конфіги/код: що важливо (Producer/Consumer/Dashboard)
8. API Dashboard
9. Мінімальні тести
10. TODO для переходу на V2 (один комміт)
11. Troubleshooting
12. Sanity-drill (3 кроки для швидкої перевірки)
13. Makefile (необов’язково)
14. Додаток A: Код JumpHash / Partitioner / Зразок події

---

## 1) Архітектура

```
+----------------+        Kafka Topic: demo.events        +------------------+
|  producer-svc  |  --->  key -> partition (V1/V2)  --->  |  consumer-svc xN |
|  Spring Boot   |                                         |  Spring Boot     |
+----------------+                                         +------------------+
         |                                                             |
         |----------------------------- metrics -----------------------|
                                           v
                                  Kafka Topic: demo.stats
                                           |
                                    +--------------+
                                    |  dashboard   |  UI: http://localhost:8081/
                                    |  Spring Boot |  Kafka UI: http://localhost:8080/
                                    +--------------+
```

* **Producer** шле події з ключем (напр., `userId`) у `demo.events`.
* **Consumers** у спільній **consumer-group** обробляють партиції.
* **Dashboard** читає `demo.stats` і показує агреговані метрики.

---

## 2) Технології

* Java 17+, Maven
* Spring Boot 3 (`spring-boot-starter-web`, `spring-kafka`)
* Apache Kafka + **Kafka UI** (через Docker Compose)
* Простий HTML+JS у `dashboard` (`src/main/resources/static`)
* JUnit 5 (мінімальні тести)

---

## 3) Швидкий старт (Kafka + сервіси)

### 0) Попередні вимоги

* Docker & Docker Compose
* Java 17, Maven

### 1) Підняти Kafka-інфраструктуру

Файл: `infra/docker-compose.kafka.yml` (є в репозиторії).

> **Важливо: advertised listeners**
> Сервіси запускаються з **хоста**, Kafka — у контейнері. У Compose мають бути **два лістенери**: внутрішній (для контейнерів) і зовнішній (для хоста).
>
> **Confluent (`cp-kafka`):**
>
> ```yaml
> environment:
>   KAFKA_LISTENERS: PLAINTEXT://:29092,PLAINTEXT_HOST://:9092
>   KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
>   KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
>   KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
> ```
>
> **Bitnami (`bitnami/kafka`):**
>
> ```yaml
> environment:
>   KAFKA_CFG_LISTENERS: PLAINTEXT://:29092,EXTERNAL://:9092
>   KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,EXTERNAL://localhost:9092
>   KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,EXTERNAL:PLAINTEXT
>   KAFKA_CFG_INTER_BROKER_LISTENER_NAME: PLAINTEXT
> ```

Запуск:

```bash
cd infra
docker compose -f docker-compose.kafka.yml up -d
```

Створити топіки (**ідемпотентно**):

```bash
# основний топік подій (старт з 3 партицій)
docker compose -f docker-compose.kafka.yml exec kafka \
  kafka-topics --create --if-not-exists --topic demo.events \
  --bootstrap-server kafka:29092 --partitions 3 --replication-factor 1

# топік метрик
docker compose -f docker-compose.kafka.yml exec kafka \
  kafka-topics --create --if-not-exists --topic demo.stats \
  --bootstrap-server kafka:29092 --partitions 1 --replication-factor 1

# перевірити
docker compose -f docker-compose.kafka.yml exec kafka \
  kafka-topics --describe --topic demo.events --bootstrap-server kafka:29092
```

> **Примітка:** збільшення кількості партицій **не** перерозкладає вже записані повідомлення — лише **майбутні**.
> **Після зміни кількості партицій перезапустіть Producer**, щоб він побачив новий `partition count`.

### 2) Зібрати все

```bash
mvn -q -DskipTests=false clean verify
```

### 3) Запустити сервіси (окремі термінали)

```bash
# Dashboard (UI на 8081)
mvn -q -pl dashboard -am spring-boot:run

# Producer (8082; навантаження можна змінювати, див. нижче)
mvn -q -pl producer -am spring-boot:run

# Consumer x2 (порти 8091 та 8092 для зручності)
SERVER_PORT=8091 mvn -q -pl consumer -am spring-boot:run
SERVER_PORT=8092 mvn -q -pl consumer -am spring-boot:run
```

### 4) Відкрити інтерфейси

* **Dashboard UI:** [http://localhost:8081/](http://localhost:8081/)
* **Kafka UI:** [http://localhost:8080/](http://localhost:8080/)

---

## 4) Як спостерігати демо

1. На **Dashboard** дві таблиці:

   * **Consumer Instances** — `instanceId | processed | lag | droppedOnRebalance | lastSeen`.
   * **Producer Routing Metrics** — `instanceId | trackedKeys | keysMigrated | migration% | assignmentsObserved | partitionCount | partitionChanges | lastSeen`.

   Другу таблицю зручно відкритою тримати перед масштабуванням: вона миттєво покаже, скільки ключів «перескочило» після додавання партицій.
2. У **Kafka UI** → `Topics → demo.events → Partitions / Consumer groups` — розподіл і assignment.
3. **Масштабування**:

    * Додати консюмера:

      ```bash
      SERVER_PORT=8093 mvn -q -pl consumer -am spring-boot:run
      ```
    * Збільшити партиції: 3 → 4

      ```bash
      docker compose -f infra/docker-compose.kafka.yml exec kafka \
        kafka-topics --alter --topic demo.events \
        --bootstrap-server kafka:29092 --partitions 4
      ```

---

## 5) Режими V1 та відмінності V2

**V1 (дефолтний роутинг)**

* Producer використовує **дефолтний** `murmur2/modulo` partitioner.
* Додавання партицій/консюмерів → грубий ребаланс; **багато ключів «переїжджає»**.

**V1 (опціонально) — *Unsafe mode***

* Навмисний анти-патерн споживання: коміт офсетів **до** обробки + ін-меморі стан без відновлення.
* При ребалансі/краші частина подій **не буде оброблена**, хоча **lag може дорівнювати 0** (офсети вже закомічені).
* Потрібен лише для **наочної демонстрації** ризиків, не використовуйте так у продакшені.

**V2 (Jump Consistent Hash)**

* Кастомний `JumpHashPartitioner` делегує у спільну реалізацію Jump Hash.
* При переході **N → N+1** бакетів мігрує ≈ **1/(N+1)** ключів → частковий, керований ребаланс.
* UI, API, тести, `pom.xml` — **без змін**. Єдиний diff — увімкнути кастомний partitioner у producer.

---

## 6) V1 **Unsafe mode** (анти-патерн): конфіг і код

### Конфіг (consumer `application.yml`)

```yaml
app:
  unsafe-demo: true          # <- УВІМКНУТИ для «зламаного» V1
  processing-delay-ms: 200   # опційна затримка для унаочнення
```

У секції `spring.kafka.consumer`:

```yaml
spring:
  kafka:
    consumer:
      bootstrap-servers: localhost:9092
      group-id: demo.group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false   # керуємо комітами вручну (MANUAL_IMMEDIATE)
```

### Фабрика контейнера (batch + manual ack)

```java
// consumer/src/main/java/.../KafkaConfig.java
@Configuration
public class KafkaConfig {

  @Value("${app.unsafe-demo:false}")
  boolean unsafe;

  public static final AtomicLong DROPPED_ON_REBALANCE = new AtomicLong();

  @Bean
  ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      ConsumerFactory<String, String> cf
  ) {
    var f = new ConcurrentKafkaListenerContainerFactory<String, String>();
    f.setConsumerFactory(cf);
    f.setBatchListener(true);
    f.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    f.getContainerProperties().setSyncCommits(true);
    f.getContainerProperties().setConsumerRebalanceListener(new ConsumerAwareRebalanceListener() {
      @Override
      public void onPartitionsRevokedBeforeCommit(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        // Навмисна "втрата" ін-меморі стану для демо (рахуємо, скільки разів трапилось)
        DROPPED_ON_REBALANCE.addAndGet(partitions.size());
      }
    });
    return f;
  }
}
```

### Listener: «коміт ДО/ПІСЛЯ обробки»

```java
@KafkaListener(topics = "${app.events-topic}", containerFactory = "kafkaListenerContainerFactory")
public void onMessage(List<ConsumerRecord<String,String>> batch, Acknowledgment ack) {
  boolean unsafe = props.isUnsafeDemo();
  int delay = props.getProcessingDelayMs();

  if (unsafe) {
    // АНТИ-ПАТЕРН: комітимо весь батч ДО обробки (at-most-once)
    ack.acknowledge();
  }

  for (var rec : batch) {
    process(rec); // оновлення метрик, емісія у demo.stats тощо
    if (delay > 0) try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
  }

  if (!unsafe) {
    // ПРАВИЛЬНО: коміт ПІСЛЯ успішної обробки (at-least-once)
    ack.acknowledge();
  }
}
```

> Рекомендуємо додати в `demo.stats` поле `droppedOnRebalance` (агрегат), та показати маленький бейдж у Dashboard (опційно). Це не порушує «README не змінюємо на V2», бо і V1, і V2 користуються тим самим API.

**Як побачити втрату в Unsafe:**

1. Запустіть Producer (напр., `events-per-second=600`) і **один** Consumer `unsafe=true`.
2. Додайте **другий** Consumer `unsafe=true` → ребаланс.
3. Вбийте один Consumer (Ctrl+C).
4. Побачите: `processed` відстає від очікуваного; у `kafka-consumer-groups --describe` lag може бути 0 (бо комітили ДО обробки).

---

## 7) Конфіги/код: що важливо

### Producer (V1 → V2)

`producer/src/main/resources/application.yml`:

```yaml
server:
  port: 8082

spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      # V1: дефолтний partitioner (рядки нижче закоментовані)
      # V2: ЄДИНИЙ diff — розкоментувати властивість нижче
      # properties:
      #   partitioner.class: org.codeus.jumphash.routing.JumpHashPartitioner

app:
  topic: demo.events
  events-per-second: 300
  user-pool-size: 5000
  event-types:
    - CLICK
    - VIEW
    - PURCHASE
    - LOGIN
    - LOGOUT
  tick-interval-ms: 1000
  stats-topic: demo.stats
  stats-interval-ms: 1000
  instance-id: producer-${server.port:8082}
```

**Зміна навантаження:**

```bash
# через аргумент
mvn -q -pl producer -am spring-boot:run \
  -Dspring-boot.run.arguments="--app.events-per-second=800"

# або через env
APP_EVENTS_PER_SECOND=800 mvn -q -pl producer -am spring-boot:run
```

> **Ключ події має бути стабільним та непорожнім** (напр., `userId`). Порожні ключі сконцентруються в одному бакеті → «гаряча» партиція.

### Consumer (безпечний режим за замовчуванням)

`consumer/src/main/resources/application.yml`:

```yaml
server:
  port: ${SERVER_PORT:8091}

spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: demo.group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false  # manual ack у listener'і

app:
  events-topic: demo.events
  stats-topic: demo.stats
  instance-id: ${HOSTNAME:local}-${server.port}
  stats-interval-ms: 1000
  unsafe-demo: false
```

### Dashboard

`dashboard/src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: demo.dashboard
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

app:
  stats-topic: demo.stats
```

**Lag у Dashboard**:
`lag ≈ endOffset(topic, partition) − committedOffset(consumer, partition)` (агреговано по партиціях інстанса).

---

## 8) API Dashboard

* `GET /api/stats` → агреговані метрики, напр.:

```json
{
  "consumers": {
    "local-8091": {"processed": 104233, "lag": 0, "droppedOnRebalance": 0, "lastSeen": 1730023456789}
  },
  "producers": {
    "producer-8082": {
      "trackedKeys": 5000,
      "keysMigrated": 3240,
      "assignmentsObserved": 21000,
      "partitionCount": 6,
      "partitionChanges": 1,
      "lastSeen": 1730023456790
    }
  },
  "updatedAt": 1730023456795
}
```

---

## 9) Мінімальні тести

* `common/JumpHashTest`: детермінізм + діапазон індексу + precondition (`buckets > 0`).
* `producer/ProducerSmokeTest`: надіслати кілька подій, перевірити наявність у `demo.events` (тестовим консюмером із таймаутом).
* `dashboard/ApiSmokeTest`: `GET /api/stats` → `200` і валідний JSON (коли надходять метрики).

> **Рекомендація:** маленький property-based тест «частка міграції» для `N=3 → 4` на 100–200k випадкових ключів; очікуйте частку у районі ~`1/(N+1)` (для кроку 3→4 ≈ 0.25; у тесті дозволити, скажімо, `0.18..0.30`).

---

## 10) TODO для переходу на V2 (один комміт)

Гілка: `1-7-jump-hash-completed`, комміт:
`V2: Switch to Jump Consistent Hashing;`

1. Додати клас `producer/.../JumpHashPartitioner.java` (див. Додаток A).
2. У `producer` у `application.yml` додати:

   ```yaml
   spring:
     kafka:
       producer:
         properties:
           partitioner.class: org.codeus.jumphash.routing.JumpHashPartitioner
   ```
3. Переконатись, що **більше нічого не змінено** (UI, Dashboard API, тести, `pom.xml`).
4. Перевірити: додайте консюмера або збільшіть партиції → у Kafka UI видно **частковий ребаланс** (≈`1/(N+1)` для кроку N→N+1), на Dashboard новий інстанс швидко починає обробку.

---

## 11) Troubleshooting

* **«Connection to localhost:9092 refused»** → перевірити `ADVERTISED_LISTENERS` у Compose (див. розділ 3.1).
* **Події «падають» у одну партицію** → продюсер відправляє **порожній ключ**.
* **V2 «не дає ефекту»** → має бути `partitioner.class=...JumpHashPartitioner`; після зміни партицій **перезапустіть producer**.
* **Dashboard порожній** → чи шлють консюмери метрики в `demo.stats`? чи підписаний на цей топік Dashboard?
* **Lag росте** → зменшити `app.events-per-second`, додати консюмерів/партицій; за потреби підкрутити `max.poll.interval.ms` / `max.poll.records`.
* **Kafka UI порожній** → UI слухає `8080`, брокер має бути доступний на `localhost:9092`.

---

## 12) Sanity-drill (3 кроки для швидкої перевірки)

1. **2 консюмери, 3 партиції** → у Kafka UI побачите розклад 3→2 (один інстанс з 2 партиціями, другий з 1).
2. **Додайте 3-го консюмера** → assignment переграється; стежте за `processed` і `lag` у Dashboard.
3. **Збільште партиції 3→4** → порівняйте V1 vs V2: у V2 частка «міграції ключів» значно менша (≈`1/(N+1)` для кроку N→N+1).

> Для демонстрації **втрати** в V1: увімкніть `unsafe-demo=true`, повторіть крок 2, «вбийте» один консюмер — `processed` відстане, хоча lag може бути 0.

---

## 13) Makefile (необов’язково)

```makefile
up:
	cd infra && docker compose -f docker-compose.kafka.yml up -d

topics:
	cd infra && docker compose -f docker-compose.kafka.yml exec kafka \
	  kafka-topics --create --if-not-exists --topic demo.events \
	  --bootstrap-server kafka:29092 --partitions 3 --replication-factor 1
	cd infra && docker compose -f docker-compose.kafka.yml exec kafka \
	  kafka-topics --create --if-not-exists --topic demo.stats \
	  --bootstrap-server kafka:29092 --partitions 1 --replication-factor 1

down:
	cd infra && docker compose -f docker-compose.kafka.yml down

clean: down
	mvn -q clean
```

---

## 14) Додаток A: Код / Зразок події

**JumpHash (common):**

```java
package org.codeus.jumphash.common;

public final class JumpHash {
  private JumpHash() {}
  /** Returns bucket in [0, buckets). Precondition: buckets > 0. */
  public static int bucket(long key, int buckets) {
    if (buckets <= 0) throw new IllegalArgumentException("buckets must be > 0");
    long b = -1, j = 0;
    while (j < buckets) {
      b = j;
      key = key * 2862933555777941757L + 1;
      j = (long)((b + 1) * (1L << 31) / ((key >>> 33) + 1));
    }
    return (int) b;
  }
}
```

**Partitioner (producer):**

```java
package org.codeus.jumphash.routing;

import org.codeus.jumphash.common.JumpHash;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.utils.Utils;
import java.util.Map;

public class JumpHashPartitioner implements Partitioner {
  @Override
  public int partition(String topic, Object key, byte[] keyBytes, Object value,
                       byte[] valueBytes, Cluster cluster) {
    int n = cluster.partitionsForTopic(topic).size();
    if (n <= 0) return 0;
    long k = (keyBytes == null) ? 0L : (Utils.murmur2(keyBytes) & 0xffffffffL);
    return JumpHash.bucket(k, n);
  }
  @Override public void close() {}
  @Override public void configure(Map<String, ?> configs) {}
}
```

**Зразок події (стабільний ключ `userId`):**

```json
{"userId":"u-123456","eventType":"CLICK","ts":1730023456000}
```

**Властивості Jump Consistent Hash:**

* При **N → N+1** бакетів мігрує ≈ **1/(N+1)** ключів.
* Зберігає **order-by-key** (як і дефолтний partitioner): повідомлення з однаковим ключем потрапляють у ту саму партицію.

---

**TL;DR**

* **V1 (дефолтний partitioner):** масштабування → «болючий» ребаланс.
* **V1 (unsafe-demo=true):** коміт до обробки + ін-меморі стан → наочно «втрачаємо обробку» при ребалансі.
* **V2 (JumpHash):** частковий, керований ребаланс (≈`1/(N+1)` для кроку N→N+1); решта — без змін.
