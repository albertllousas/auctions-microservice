# Auction System

## Problem

Design and implement an online auction system that allow buyers to participate on auctions through digital means, 
rather than being at a physical location.

## Functional requirements

- Sellers can post any item for auction at any time from their catalog, with an initial price, a minimal bid and an opening date-time.
- An item can not be for an auction twice, unless it was not sold in a previous action.
- An auction can not be changed, only cancelled, but only after 5 min of creation.
- An auction will be on a preview status for a while, between 1 day to 7 days, allowing potential buyers to take a look beforehand.
- Once an auction is open, bidders can bid for the item.
- A Bidder wins an item if there are no higher bids in the next 20 min.
- An auction will expire if there aren't any bidders in a period of 1h.
- Bidders can bid on any existing item any number of times.
- Bids are only allowed during opening time.
- The platform would allow auto-bidding up to a limit, increasing automatically your bid every time someone outbids you.
- Bidders can subscribe to an auction in order to be notified of any change. 

## Scope

The scope of the system should be auctions, we will work with the assumption that we will have other systems in place for:

- Catalog
- User lifecycle
- Notifications
- ...

Design only the Backend part, let's skip all the FE side.

## Non Functional Requirements

- Low Latency
- High Availability
- Highly consistent

## Estimated workloads

- 10M auctions added every day
- 100M requests to bid on these items


!! --> ReplicatedUsersMongoDBRepository

!! -> autobids

FP, hexa, railway programming, outbox everywhere
not read given the nature of the service, this is a qrite service, write part of cqrs, if you want to read, read service
diagram: auctions part a read service there, that's why we have commands

    - https://www.openn.com/en-au/blogs/what-is-an-online-property-auction-and-how-does-it-work-openn-blog#:~:text=An%20online%20auction%20is%20conducted,or%20company%20facilitating%20the%20sale.
    - preview period (read systems)
    - https://leetcode.com/discuss/interview-question/system-design/1490937/bidding-auction-system-design
    - Auction will be open 
    - https://www.graysauctioneers.com/blog-posts/2020/7/29/how-do-auctions-work
    - https://leetcode.com/discuss/interview-question/792060/Bidding-System%3A-System-Design-Interview
    - https://blog.insiderattack.net/atomic-microservices-transactions-with-mongodb-transactional-outbox-1c96e0522e7c (for active bidding)
    - Check winning action in some kind of outbox, configurable
    - Outbox for notifications, expire , winner 
    - https://stackoverflow.com/questions/35131247/mongodb-remove-document-and-return-it   --> archive completed
    - 1 item per user by index
    - Diff table view for open auctions, then cron to kafka and read an update auction 



https://docs.spring.io/spring-kafka/docs/current/reference/html/#exp-backoff
