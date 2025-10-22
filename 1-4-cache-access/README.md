# Cache access semantics (Write-through / write-back / read-through)

## Topic Overview 

**Write-through**, **write-behind** and **read-through** are all startegies which could make developers life easier by helping working with cache.

Though, let's eat a whale 🐳 in chunk :)

**Read-through** 📚 <br>
It's a strategy when: 
<details>
<summary>The user send request to get a data</summary>
</details>
<details>
<summary>If the data is present in cache (cache hit) => <b>return the requested data from cache</b></summary>
</details>
 <details>
<summary>If the data is <b>NOT</b> present in cache (cache miss) => <b>cache will retrieve data from db, store it, and return it to the user</b></summary>
</details>

**Write-through** 📝 <br>
It's a strategy when:
<details>
<summary>The user send request to add a data</summary>
</details>
<details>
<summary>The data will be <b>first added to the cache</b></summary>
</details>
 <details>
<summary>Then <b>the data will be added to db <i>immediately</i></b></summary>
</details>

**Write-behind** 🔙 <br>
It's a strategy when:
<details>
<summary>The user send request to update a data</summary>
</details>
<details>
<summary>The data will be <b>first added to the cache</b></summary>
</details>
 <details>
<summary>Then <b>the data will be added to db <i>asynchronously</i></b></summary>
</details>


## Wonderful! And now, ladies and gentlemen, <br>
## Welcome to Codeus GolfTournament 🏌


### Entities that will be cached: 
 - Golfer 🏌‍♀️ => **golfers** cache
 - GolfTournament 🏆 => **tournaments** cache

### [Technologies](https://youtube.com/shorts/IL9rjdx0xuo?si=BiGSI42fuzc75edh)

- Redis => as a cache storage
- PostgreSQL => as a system of records 

### Key metrics ⚖️

1. Organizers of the tournament want to know the exact number and names of all participants (Golfers).🏌🏌‍♂️🏌‍♀️ => *write-through, read-through
2. During the golf tournament viewers, who made a bet beforehand, want to keep up with golfers results (on which hole the golfer earn how many scores). 🏆🎫 => write-behind

Your task is to implement those strategies. <br>
You will find **TODO**'s inside `/service/GolfTournamentService.java`.

### Running the System

On the **startup** of the application, **new Tournament** with 5 players **will be created.**
```
 Scheduling
    ├──[/services/GameFlowService] Simulates golfers score update every 1 minute
    ├──[/services/GolfBatchUpdateService] Every 5 minutes will simulate database batch update (5 or more records) 
    
```

This will be enough to see the results of caching. 

```
 If you want, you could manually (golf-tournament-controller):
    ├──POST  [/tournaments] add tournament 
    ├──POST  [/tournaments/{tournamentId}/golfers] add golfer to tournament 
             (be aware that to a new tournament you could not add already existing golfers 
             as they are taking part into another tournamnet right now)
    ├──PATCH [/tournaments/{tournamentId}/score] modify golfers progress ( add number of hole and score )
    
```

### Test scenarios ⚙️
<details>
<summary>Scenario 1 => test write-though behavior</summary>
On startup a new Tournament will be created and golfers will be added to it using write-through strategy. <br>
<ul>
<li>Check tournament data in db <b>/data/tournament/fromDb</b></li>
<li>Check tournament data in cache <b>/data/tournament/fromCache</b></li>
</ul>
You should see that the number of golfers, their ids and names from cache are consistent with the data from db.
</details>
<details>
<summary>Scenario 2 => test write-through behavior on your own example</summary>
<ul>
<li>Add new tournament <b>/tournaments</b></li>
<li>Add new golfers to new tournament <br>
( you could not add existing golfers as they are already taking part in another tournament ) <b>/tournaments</b></li>
</ul>
You should see that the number of golfers, their ids and names from cache are consistent with the data from db.
</details>
<details>
<summary>Scenario 3 => test read-through on your own example</summary>
<ul>
<li>Add new tournament <b>/tournaments</b></li>
<li>Check that it appears in db <b>/data/tournament/fromDb</b></li>
<li>Check that it absent in cache <b>/data/tournament/fromCache</b></li>
<li>Use get request to <b>/tournaments/{tournamentId}</b> <br>
This will read tournament from db and cache it</li>
<li>Repeat steps 2 and 3 </li>
</ul>
You should see that the number of golfers, their ids and names from cache are consistent with the data from db.
</details>
<details>
<summary>Scenario 4 => test write-behind</summary>
<ul>
<li>Open <b>http:localhost:8080/dashboard</b></li>
<li>Wait while scheduler made an update to golfers scores, you will see a message in a console</li>
<li>Pay attention how data from golfers cache will differ from the one in db</li>
</ul>
</details>


### Check you implementation  ✅
- You could open [`http://localhost:8080/dashboard`](http://localhost:8080/dashboard) where you will be able to see data which stored in the db and data which stored in tournaments and golfers caches.
- You could open [`http://localhost:8080/swagger-ui`](http://localhost:8080/swagger-ui) where you will find `cache-sor-controller` from which you could send requests to get data from db and from cache to compare.


### Learning sources 🤓
- [Spring Inline Caching example](https://docs.spring.io/spring-boot-data-geode-build/1.6.x/reference/html5/guides/caching-inline.html)
- [Spring Write-Behind Caching example](https://docs.spring.io/spring-boot-data-geode-build/1.6.x/reference/html5/guides/caching-inline-async.html)
- [Caching Strategies and something else](https://www.enjoyalgorithms.com/tags/databases/)
- [Declarative Annotation-based caching](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html)

Good luck! 🚀