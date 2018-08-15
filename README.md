# agree

This is a web application that hosts a game I like to call "Agree"; the game is specified [here](https://quip.com/jouXACDLERyH). I've approached this project as an opportunity to showcase the design and implementation of a minimum-viable-product web application with a somewhat non-standard architecture.

See it running [here](http://209.177.93.150).

My design goals for an MVP are:
  - Flexibility - Arguably the most critical requirement for an MVP codebase, flexibility determines how much development time is needed to modify, enhance, or scale the product
  - Stability - You can't iterate on the product if you're busy fighting fires, so the application should require little attention once deployed
  - Ease-of-operation - To minimize operational overhead, deploying the server and restoring it to a known-good state should be mostly automated

Below I describe at a high level the application's design, and in detail how it attempts to fulfill these goals. While I believe this design reflects much of my creativity and engineering skill, I feel compelled to mention that I am quite comfortable in standard Ruby/Node.js/PHP/Golang back-end environments as well.

Quick aside: Authentication was unspecified in the document above, so in the interest of completeness I've included a simple token auth scheme. The registration is designed to support an "invite-only beta" phase. An admin registers a token for a new user by visiting `/register/~/desired-username`. The access token for that username will be shown once, in the response to that request, and then stored in the database (not hashed for now since there are no user-provided passwords and a compromise of the server would naturally include access to the plaintext tokens as they come in).

## Design Overview

This project is a Clojure web application, and thus runs in the JVM and listens for HTTP requests on a configurable port. It sits atop an asynchronous web server, [`http-kit`](http://http-kit.org) and uses a popular [routing library](https://github.com/weavejester/compojure) to handle requests.

The back-end uses an embedded SQLite database for durable data storage, and keeps a working set of a few simple in-memory data structures for the newsfeed, scoreboard, and user profiles.

This hybrid database/in-memory storage model allows the application to serve over 12k requests per minute on low-end hardware (the demo above serves dynamically-rendered HTML responses in around 50ms for $10/mo and comfortably supports over 200 requests/second when load-tested using `wrk`), while also offering an extremely expressive and flexible data model interface in the application's programming language (as opposed to a database query language).

In the requirements, the front-end was requested to be a "basic HTML front-end" which I have attempted to provide. The public feed is served as a single HTML page. This page includes some vanilla ES6 JavaScript which (if a user is logged in) loads user-specific vote and score data from the API and decorates the interface. This allows the backend to cache the rendered newsfeed markup between posts/votes, while setting us up for future success (see below).

Finally, this app is packaged into a Docker container for smooth deployment and maintenance.

## Flexibility

The newsfeed view is currently built server-side, using the [Hiccup](https://github.com/weavejester/hiccup) library to render HTML from Clojure data structures (similar to how React et al render HTML from a virtual DOM). The most likely evolution of the view layer would be into a single-page JavaScript or ClojureScript implementation. The component structure of the server-side views mirrors what you would find in a single-page front-end; the Hiccup code can be ported to most ClojureScript view libraries with almost no changes, and the components can be individually rendered as HTML for a low-overhead port to a JSX-based or template-based front-end. The base infrastructure for supporting a JSON API is in place (the user profile is currently served to the client JavaScript as JSON).

By implementing a simple token-based auth scheme, we have the elements to support user accounts. Later authentication enhancements (e.g. user-provided passwords or cryptographic identities) have a clear place to drop in.

By keeping the newsfeed and scoreboard (the application's two most heavily-accessed data structures) in memory and maintaining their state with Clojure application code, we can tweak, or totally overhaul, the way we represent and access data without changing the on-disk schema. Furthermore, the performance enabled by this approach allows a single application server to scale from thousands to tens of thousands of simultaneous users using simple read replicas, removing the need for large complex clusters or dedicated devops staff until the product is extremely popular.

## Stability

There are not many moving pieces, thus little room for exceptional cases. The application doesn't depend on any networked services (storage and caching are embedded). SQLite is well-known as one of the most reliable and thoroughly-tested codebases in the world, and as a result is near-impossible to corrupt. In the interest of expedience, this application's testing is limited to persistence/state operations and the game/score logic.

## Ease-of-operation

This application has been designed such that it can restart at any point in time and quickly load its working set from the database, restoring itself to a known-good state instead of requiring time-consuming administration. Futhermore, the data is stored on a volume separate from the application instance and replicated to multiple locations automatically to prevent data loss. Finally, if manual administration is required, many hosting companies (including Hyper.sh, who host the instance above) offer the ability to open shell sessions on running containers for easy access to the live in-memory state (via Clojure's powerful REPL) or on-disk state (via SQLite). This means we can fix production problems without interrupting service, and quickly redeploy once the fire is out.

To deploy the application, a local Docker image is uploaded to production. The old container is removed (though the database volume is kept), and a new container is started with the new image and the existing db volume.

## Implementation

Implementing this application took 4 hours per day across 3 days, a total of 12 hours, including the time taken to write up this README. I spent the first 3 hours planning, and most of the rest debugging and making a few performance tweaks.

## License

Copyright © 2018 Duncan Smith

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
