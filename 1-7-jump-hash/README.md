# Jump Hash Event Router

This lab compares Kafka's default murmur2 modulo routing with Jump Consistent Hashing by wiring a Spring Boot producer, paired consumers, and a monitoring dashboard.

## Modules
- `common`: shared DTOs and metrics models
- `producer`: emits demo events and exposes routing stats
- `consumer`: processes events and records instance metrics
- `dashboard`: visualises producer + consumer data
- `docker-compose.kafka.yml`: local Kafka + Kafka UI stack (run from this folder)

## Prerequisites
- Docker & Docker Compose
- Java 17+
- Maven 3.9+

## Run The Baseline (default partitioner)
Run all commands from the repository root folder `algorithms-and-data-structures`.

1. Start Kafka locally:
   ```bash
   docker compose -f 1-7-jump-hash/docker-compose.kafka.yml up -d
   ```
2. Create the demo topics (idempotent; ignore "already exists" warnings):
   ```bash
   docker compose -f 1-7-jump-hash/docker-compose.kafka.yml exec kafka \
     sh -c "kafka-topics --create --topic demo.events \
       --bootstrap-server kafka:29092 --partitions 3 --replication-factor 1 \
       || kafka-topics --describe --topic demo.events --bootstrap-server kafka:29092"

   docker compose -f 1-7-jump-hash/docker-compose.kafka.yml exec kafka \
     sh -c "kafka-topics --create --topic demo.stats \
       --bootstrap-server kafka:29092 --partitions 1 --replication-factor 1 \
       || kafka-topics --describe --topic demo.stats --bootstrap-server kafka:29092"
   ```
3. Build only this module (not the whole repo):
   ```bash
   mvn -pl 1-7-jump-hash -am clean verify
   ```
   This compiles `1-7-jump-hash` and its submodules (`common`, `producer`, `consumer`, `dashboard`) only.
4. Start the Spring Boot apps in separate terminals:
   - Dashboard `mvn -pl 1-7-jump-hash/dashboard -am spring-boot:run`
   - Producer `mvn -pl 1-7-jump-hash/producer -am spring-boot:run`
   - Consumer #1 `SERVER_PORT=8091 mvn -pl 1-7-jump-hash/consumer -am spring-boot:run`
   - Consumer #2 `SERVER_PORT=8092 mvn -pl 1-7-jump-hash/consumer -am spring-boot:run`
5. Open the dashboards:
   - Demo dashboard: http://localhost:8081/
   - Kafka UI: http://localhost:8080/
6. Once **Tracked Keys** stabilises near 5 000, grow the partition count and watch the routing metrics:
   ```bash
   docker compose -f 1-7-jump-hash/docker-compose.kafka.yml exec kafka \
     kafka-topics --alter --topic demo.events \
     --bootstrap-server kafka:29092 --partitions 4
   ```
   Note the temporary spike in the dashboard's **Keys Migrated** total and **Migration %** column; these reflect the cost of modulo routing.

## Implement Jump Hash
1. Update `producer/src/main/java/org/codeus/jumphash/producer/CustomPartitioner.java` so the existing Murmur2 hash feeds `Hashing.consistentHash(...)` instead of the modulo operation.
2. In `producer/src/test/java/org/codeus/jumphash/producer/PartitionerMigrationTest.java`, disable `moduloRoutingMigratesManyKeys` with `@Disabled`, remove the annotation from `jumpHashKeepsMigrationsLow`, and run that class (for example `mvn -pl producer -Dtest=PartitionerMigrationTest test`) to confirm the migration rate stays within budget.
3. Recreate the full migration scenario:
   Kafka cannot decrease a topic's partition count. To return `demo.events` to 3 partitions before measuring migrations with Jump Hash, reset the topics, then grow to 4 again.

   - Stop the producer with `Ctrl+C` in its terminal (leave dashboard/consumers running; they will reconnect).
   - Ensure Kafka is up:
     ```bash
     docker compose -f 1-7-jump-hash/docker-compose.kafka.yml up -d
     ```
   - Reset topics to the baseline state (3 partitions for `demo.events`, 1 for `demo.stats`). Choose one:
     - Option A — delete and recreate topics (fast path):
       ```bash
       # Delete topics if they exist (ignore errors)
       docker compose -f 1-7-jump-hash/docker-compose.kafka.yml exec kafka \
         sh -c "kafka-topics --delete --topic demo.events --bootstrap-server kafka:29092 || true"
       docker compose -f 1-7-jump-hash/docker-compose.kafka.yml exec kafka \
         sh -c "kafka-topics --delete --topic demo.stats --bootstrap-server kafka:29092 || true"

       # Recreate with baseline partitioning
       docker compose -f 1-7-jump-hash/docker-compose.kafka.yml exec kafka \
         sh -c "kafka-topics --create --topic demo.events \
           --bootstrap-server kafka:29092 --partitions 3 --replication-factor 1"
       docker compose -f 1-7-jump-hash/docker-compose.kafka.yml exec kafka \
         sh -c "kafka-topics --create --topic demo.stats \
           --bootstrap-server kafka:29092 --partitions 1 --replication-factor 1"
       ```
     - Option B — clean slate (wipes broker data):
       ```bash
       docker compose -f 1-7-jump-hash/docker-compose.kafka.yml down -v
       docker compose -f 1-7-jump-hash/docker-compose.kafka.yml up -d
       # Then (re)create topics as above
       docker compose -f 1-7-jump-hash/docker-compose.kafka.yml exec kafka \
         sh -c "kafka-topics --create --topic demo.events \
           --bootstrap-server kafka:29092 --partitions 3 --replication-factor 1"
       docker compose -f 1-7-jump-hash/docker-compose.kafka.yml exec kafka \
         sh -c "kafka-topics --create --topic demo.stats \
           --bootstrap-server kafka:29092 --partitions 1 --replication-factor 1"
       ```
   - Start the producer again (`mvn -pl 1-7-jump-hash/producer -am spring-boot:run`) and wait until ~5 000 keys are tracked on the dashboard.
   - Increase the partition count from 3 to 4 and observe the low migration spike with Jump Hash enabled:
     ```bash
     docker compose -f 1-7-jump-hash/docker-compose.kafka.yml exec kafka \
       kafka-topics --alter --topic demo.events --bootstrap-server kafka:29092 --partitions 4
     ```

## Verify Jump Hash
1. Repeat the partition increase in Kafka while watching the dashboard.
2. With Jump Hash active, the **Keys Migrated** counter should barely move and the **Migration %** should settle near `1/(N+1)`.
3. Commit on the `1-7-jump-hash` branch, then cherry-pick or merge into `1-7-jump-hash-completed` for your reference solution.

## Tear Down
- Stop Spring Boot apps with `Ctrl+C` in their terminals.
- Stop Kafka stack and remove volumes:
  ```bash
  docker compose -f 1-7-jump-hash/docker-compose.kafka.yml down -v
  ```
