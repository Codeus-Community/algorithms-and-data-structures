# Consistent Hashing Implementation Task 🎯

## Topic Overview

Consistent Hashing is a distributed hashing scheme that operates independently of the number of servers in a distributed hash table. It provides a way to distribute data across nodes in a manner that minimizes reorganization when nodes are added or removed, making it ideal for distributed caching systems and load balancing.

## Why Consistent Hashing? 🤔

Traditional hash-based distribution (like modulo hashing) has significant drawbacks in distributed systems:

* **Massive data redistribution**: When adding/removing nodes, nearly all data needs to be remapped
* **Cache invalidation storms**: Node changes cause widespread cache misses
* **Downtime during scaling**: Systems become unavailable during redistribution
* **Inefficient resource usage**: All nodes must participate in data movement

Consistent Hashing solves these problems by:
- Minimizing data movement to ~K/N keys when nodes change (K = keys, N = nodes)
- Providing predictable, bounded redistribution
- Enabling seamless horizontal scaling
- Maintaining system availability during node changes

## Task Goal 🎯

You're working on an e-commerce platform's product catalog service that currently uses Simple Hashing (modulo-based) for distributing products across cache nodes. The system experiences significant performance degradation during scaling operations.

**Your mission**: Implement Consistent Hashing to optimize the distribution system and minimize data movement during node additions/removals.

**Specifically**: Complete all TODOs in the `ConsistentHashingManager` class to upgrade from the simple hashing API (v0) to the consistent hashing API (v1).

## System Architecture

```
Product Catalog Service (port 8080)
├── Simple Hashing API (v0)
│   ├── POST /api/v0/init?nodes={n}     - Initialize with n nodes
│   ├── POST /api/v0/nodes              - Add a new node
│   └── DELETE /api/v0/nodes/{index}    - Remove node by index
│
└── Consistent Hashing API (v1) [TO IMPLEMENT]
    ├── POST /api/v1/init?nodes={n}     - Initialize with n nodes
    ├── POST /api/v1/nodes              - Add a new node
    └── DELETE /api/v1/nodes/{index}    - Remove node by index
```

## Current Problem ⚠️

Using Simple Hashing (`/api/v0/*` endpoints), observe:
1. **Complete redistribution**: Adding/removing nodes causes ALL products to be checked
2. **Massive data movement**: Most products move to different nodes
3. **Poor scaling characteristics**: Performance degrades with more products/nodes

## Implementation Instructions 📝

### Files to Modify

**Primary Task**: Complete the TODOs in `ConsistentHashingManager.java`

### TODO Breakdown

#### 1. Initialize Hashing (`initializeHashing` method)
```java
// TODO: Create a new instance of ConsistentHashing using Product::getId as key extractor
// Hint: Look at how SimpleHashingManager creates its hashing instance

// TODO: Initialize the ring with the given number of nodes
// Hint: ConsistentHashing has a setNodeCount method similar to SimpleHashing

// TODO: Add all elements to the hashing using the appropriate method
// Hint: Check the addElements method in ConsistentHashing

// TODO: Return InitResponse with proper data
// Use the builder pattern and get data from consistentHashing instance
```

#### 2. Add Node (`addNode` method)
```java
// TODO: Capture the current distribution before modification
// Hint: Use captureDistribution() method from ConsistentHashing

// TODO: Add a new node to the ring
// Hint: ConsistentHashing has an addNode() method

// TODO: Uncomment the movement tracking lines after implementing above TODOs

// TODO: Return AddNodeResponse with all required data
// Include: nodeCount, checkedElements, movedElements, distribution, movements
```

#### 3. Remove Node (`removeNode` method)
```java
// TODO: Capture the current distribution before removal
// Hint: Same as in addNode method

// TODO: Remove a node by its index
// Hint: Use removeNodeByIndex(nodeIndex) method

// TODO: Uncomment the movement tracking lines

// TODO: Return RemoveNodeResponse with all required data
```

## Testing Your Implementation 🧪

### 1. Start the Application
```bash
# Run the Spring Boot application
./mvnw spring-boot:run

# Or using your IDE's run configuration
```

### 2. Compare Simple vs Consistent Hashing

#### Test Simple Hashing (Baseline)
```bash
# Initialize with 3 nodes
curl -X POST "http://localhost:8080/api/v0/init?nodes=3"

# Add a node - observe "checkedElements" (should be ~1000, all products)
curl -X POST "http://localhost:8080/api/v0/nodes"

# Remove a node - observe "checkedElements" and "movedElements"
curl -X DELETE "http://localhost:8080/api/v0/nodes/0"
```

#### Test Your Consistent Hashing Implementation
```bash
# Initialize with 3 nodes
curl -X POST "http://localhost:8080/api/v1/init?nodes=3"

# Add a node - observe "checkedElements" (should be much less than total)
curl -X POST "http://localhost:8080/api/v1/nodes"

# Remove a node - observe minimal "movedElements"
curl -X DELETE "http://localhost:8080/api/v1/nodes/0"
```

### 3. Using Postman or API Tools

Import these endpoints for easier testing:
- `POST http://localhost:8080/api/v1/init?nodes=3`
- `POST http://localhost:8080/api/v1/nodes`
- `DELETE http://localhost:8080/api/v1/nodes/0`

## Expected Behavior Comparison 📊

### Simple Hashing (v0)
- **Node Addition**: Checks ALL products, moves ~67% of products
- **Node Removal**: Checks ALL products, moves ~50% of products
- **Complexity**: O(N) for all operations

### Consistent Hashing (v1) - Your Implementation
- **Node Addition**: Checks only affected products (~1/N of total)
- **Node Removal**: Moves only products from removed node
- **Complexity**: O(K/N) where K is total keys, N is number of nodes

## Success Criteria ✅

Your implementation is correct when:

1. **Initialization works**: `/api/v1/init` creates the specified number of nodes and distributes products
2. **Minimal checking on add**: Adding a node checks significantly fewer elements than the total (typically 1/N of products)
3. **Bounded data movement**: Only products from one node segment move when adding nodes
4. **Correct redistribution**: Products are correctly reassigned to their new nodes
5. **All endpoints return valid responses**: Including distribution maps and movement summaries

## Hints for Implementation 💡

### Hint 1: Understanding the Classes
- `ConsistentHashing<T>` is already fully implemented with the ring-based algorithm
- You just need to wire it up in `ConsistentHashingManager`
- Look at `SimpleHashingManager` for the pattern to follow

### Hint 2: Key Methods in ConsistentHashing
- `new ConsistentHashing<>(Function<T, String>)` - Constructor with key extractor
- `setNodeCount(int)` - Initialize with n nodes
- `addElements(List<T>)` - Add products to the ring
- `addNode()` - Add a new node dynamically
- `removeNodeByIndex(int)` - Remove a specific node
- `captureDistribution()` - Get current product-to-node mapping
- `getDistribution()` - Get node element counts
- `getTotalElements()`, `getCheckedElements()`, `getNodeCount()` - Statistics

### Hint 3: Building Responses
Use the builder pattern for response objects:
```java
return InitResponse.builder()
    .message("Initialized with " + nodeCount + " nodes")
    .nodeCount(consistentHashing.getNodeCount())
    .totalProducts(consistentHashing.getTotalElements())
    .checkedElements(consistentHashing.getCheckedElements())
    .distribution(statisticsCollector.getDistribution(consistentHashing))
    .build();
```

## Understanding the Algorithm 🔍

Consistent Hashing works by:
1. **Hash Ring**: Mapping both nodes and keys to points on a virtual circle (0 to 2^64-1)
2. **Key Assignment**: Each key is assigned to the first node clockwise from its position
3. **Node Addition**: Only keys between the new node and its predecessor move
4. **Node Removal**: Only keys from the removed node relocate to the next node

This ensures minimal disruption during scaling operations, critical for distributed systems.

## Learning Resources 📚

* [Consistent Hashing Original Paper](https://www.cs.princeton.edu/courses/archive/fall09/cos518/papers/chash.pdf)
* [Consistent Hashing and Random Trees](https://dl.acm.org/doi/10.1145/258533.258660)
* [Visual Guide to Consistent Hashing](https://www.toptal.com/big-data/consistent-hashing)

## Bonus Challenge 🌟

After completing the basic implementation, consider:
- What happens to the distribution balance as you add more nodes?
- How does the number of "checkedElements" relate to the total number of nodes?
- Can you calculate the theoretical vs actual data movement percentage?

Good luck! 🚀