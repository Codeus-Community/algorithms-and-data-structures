# HyperLogLog Algorithm Task 📊

## Topic Overview

**HyperLogLog** is a probabilistic data structure used for estimating the cardinality (number of unique elements) in a dataset. It provides a memory-efficient way to count unique items with a small, configurable error rate.

### Why HyperLogLog? 🤔

In distributed systems handling massive amounts of data, counting exact unique users can be:
- **Memory-intensive**: Storing millions of user IDs in sets consumes significant RAM
- **Network-heavy**: Transferring large sets between services is slow
- **Computationally expensive**: Union operations on large sets take time

HyperLogLog solves this by using a fixed-size data structure (typically 12-16 KB) that can estimate billions of unique elements with ~2% error rate.

## Task Goal 🎯

You are working with an analytics platform that collects events from multiple tenants (mobile/web applications). The system currently calculates hourly unique users per tenant, country, and device using exact counting with Redis Sets.

**Your mission**: Optimize the system using Redis and its embedded HyperLogLog data structure.
Less formally: resolve all TODOs in the classes:
- `hyperloglog/consumer/src/main/java/org/codeus/hyperloglog/consumer/EventStorageService.java`
- `hyperloglog/aggregator/src/main/java/org/codeus/hyperloglog/aggregator/BasicAggregationService.java`

## System Architecture

```
Producer Service (port 8080)
    ├──[main]  Simulates events from multiple tenants
    ├──[extra] Provides POST /produce and /produce/many endpoints to manually push a new event
    └──[extra] Provides Get /uniqueUsers and /uniqueUsers/all endpoints to retrieve the exact number of unique users
    
Consumer Services (ports 8081, 8082)
    ├──[main]  Receive POST /event requests
    ├──[main]  Store events in Redis Sets
    └──[extra] Provide DELETE /storage endpoint to clear consumed data from Redis
    
Aggregator Service (port 8083)
    ├──[main]  Fetches data from Redis for aggregation and calculating final unique user counts
    ├──[extra] Has scheduled tasks to automate aggregation
    └──[extra] Provides many GET endpoints to manually trigger aggregation per hour/day/tenant
    

```

**NOTE**: **Each service** has `/swagger-ui/index.html` endpoint for easier manual manipulations on the system.


## Current Problem ⚠️
1. Memory usage growing rapidly as events accumulate
2. Rather slow union operations when merging data from multiple consumers


## Hints 💡

<details>
<summary>Hint 1: Where is the bottleneck?</summary>

Look at the Consumer services. They store every userId in Redis Sets. For millions of users, this becomes a memory problem.

</details>

<details>
<summary>Hint 2: What needs to change?</summary>

Instead of storing exact user IDs in Redis Sets, use HyperLogLog to estimate cardinality. Redis provides built-in HyperLogLog support via `PFADD` and `PFCOUNT` commands.

See their counterpart in the Spring's RedisTemplate: `org.springframework.data.redis.core.RedisTemplate.opsForHyperLogLog`
</details>

<details>
<summary>Hint 3: [Light] How to implement?</summary>

Replace Redis Set commands with Redis HyperLogLog:
- Use `PFADD` to add user IDs to HLL structures
- Use `PFCOUNT` to get unique count estimates
- Use `PFMERGE` to combine HLLs from multiple consumers

See their counterpart in the Spring's RedisTemplate: `org.springframework.data.redis.core.RedisTemplate.opsForHyperLogLog`

</details>

<details>
<summary>Hint 4: [Details] How to implement?</summary>


**EventStorageService**:
1. Change `CACHE_KEY_PREFIX` to `hll`, so final key pattern would look like: `hll:{tenantId}:{country}:{device}:{hour}`
2. Replace `redisTemplate.opsForSet()` with `redisTemplate.opsForHyperLogLog()`(as replacing `SADD` with `PFADD` raw command) for storing a new event into Redis.

**BasicAggregationService**:
1. Change `CACHE_KEY_PREFIX` to `hll`, so final key pattern would look like: `hll:{tenantId}:{country}:{device}:{hour}`
2. Replace `redisTemplate.opsForSet()` with `redisTemplate.opsForHyperLogLog()`(as replacing `SCARD` with `PFCOUNT` raw command) for retrieving size of the collection for key from Redis.
3. Methods with TODOs you need:
   1. retrieve all keys using `redisTemplate.keys()`
   2. merge data for those keys using `redisTemplate.opsForHyperLogLog().union(...)`
   3. retrieve number of merged elements using `redisTemplate.opsForHyperLogLog().size(...)`.

</details>

## Running the System

### Spring profiles

All services have a dedicated Spring profile. Make sure you include it in your Run/Debug configuration.

- Consumer service - `consumer1` and `consumer2` for two instances
- Aggregator service - `aggregator`
- Producer service - `producer`

### Flow

1. Start all services in order: Consumer1, Consumer2, Aggregator, Producer
2. Observe the logs to understand the flow
3. Run tests to ensure correctness: check `hyperloglog/e2e-test` and
   run `org.codeus.hyperloglog.e2e_test.MainFlowE2ETest`

### Extra automation

Additionally, you may use scripts to start and stop all services at once.
Using the provided scripts (Linux/Mac):

```bash
# Initially, you are in the ~<project_dir>\algorithms-and-data-structures
cd hyperloglog

# Make scripts executable
chmod +x run-services.sh stop-services.sh

# Start all services
./start-services.sh

# Stop all services
./stop-services.sh
```

**NOTE**: when using scripts above all logs are redirected to files

## Success Criteria ✅

- Storage memory print is minimal due to use of HyperLogLog data structure
- Unique user counts should be approximately correct (within 2-3% error)
- All tests should pass

## Learning Resources 📚

- [HyperLogLog Paper](http://algo.inria.fr/flajolet/Publications/FlFuGaMe07.pdf)
- [HyperLogLog: A Simple but Powerful Algorithm for Data Scientists](https://towardsdatascience.com/hyperloglog-a-simple-but-powerful-algorithm-for-data-scientists-aed50fe47869/)
- [Redis HyperLogLog Commands](https://redis.io/docs/latest/commands/?group=hyperloglog)
- [Redis HyperLogLog Documentation](https://redis.io/docs/latest/develop/data-types/probabilistic/hyperloglogs/)

Good luck! 🚀