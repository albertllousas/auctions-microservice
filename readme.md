# Auction System

Keywords: `Tactical DDD`, `microservice`, `kotlin`, `spring-boot`, `Hexagonal-Architecture`, `SOLID`, `Domain-Driven Design`, `functional-programming`,
`Testing`, `Event-Driven Architecture`, `Domain-Events`, `Kafka`, `MongoDB`, `Transactional-outbox`, `optimistic-locking`

## Problem

Design and implement an online auction system that allows buyers to participate on auctions through digital means, 
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

## Scope

The scope of the system should be auctions, we will work with the assumption that we will have other systems in place for:

- Catalog
- User lifecycle
- ...

Design only the Backend part, let's skip all the FE side.

## Non Functional Requirements

- Low Latency
- High Availability
- Highly consistent

## Estimated workloads

- 10M auctions added every day
- 100M requests to bid on these items
