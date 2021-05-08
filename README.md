# campsite
A demo project for an interview

# About the design
In this design, I didn't implement a complicated structure to handle real high volume calls, I only used
a lock to make sure there would be no conflicts on the reseravtion date range. But I have several thoughts
about the improvements:
1. Since each user can reserve only max 3 days so I would use the date as the primary key for the database table
and insert maximum 3 records for each reservation. Hence if there are conflicts, the DB would return exceptions and
the transaction would be rollback, but this will rely on DB capability. Not a very good solution, the only pros is
I don't need the thread lock in the codes, just directly save reservations

2. Have a cache before persisting reservation, each save/update request would go check the cache first, this would
speed a lot, the cons is I need extra codes to handle actions for cache: init, refresh, update, clean

3. I would make the reservation to two steps: a. User/client send a reserve request and get
an unique id immediately, like a token in the cache. b. The request would be sent to a blockqueue or event bus like.
c. There would be some daemon processors will process the requests, by checking availability in an atomic way and return
the results in an output queue. d. The user/client check the status of the reservation by the token

4. If we could use AWS redis as cache or Redshift which supports safety read-write operations


Below are also something I think of for the real scenario design:
1. Separate read and write, e.g. Main DB -> Duplicate Readonly DBs. In most of these system, read requests are more
than write requests, so we could have these services separated and sync between main db to readonly dbs
2. Use cache
3. Use Queue, like MQ, SQS..etc.
4. Split DB, split tables. For example, I have a design is to create 366 tables represent 366 days for this reservation
   then the DB operation would be less conflicts and faster.
5. Use cloud products, AWS or Azure provide some elastic structure which would help a lot.



# The table structure
CREATE TABLE `campsite`.`reservation` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(45) NOT NULL,
  `email` VARCHAR(200) NOT NULL,
  `startFrom` DATETIME NOT NULL,
  `endTo` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) VISIBLE,
  INDEX `username_email` (`username` ASC, `email` ASC) VISIBLE,
  INDEX `daterange` (`startFrom` ASC, `endTo` ASC) VISIBLE);
  

