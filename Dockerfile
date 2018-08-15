# We start with a standard community-provided Docker container image
FROM clojure:lein-alpine

# This is the application's default port (it's forwarded to 80 in production)
EXPOSE 3000

# This is the local (NON-PUBLIC) port the application's REPL will be available on
# To connect, run `lein repl :connect 1337` from a shell inside a running container
ENV NREPL_PORT 1337

# Here we create a mount point for our database volume and ensure the db path exists
VOLUME /usr/db
RUN mkdir -p /usr/db/agree

# Then we inform the app of the db's location via an environment variable
ENV AGREE_DB_PATH /usr/db/agree/data.db

# Copy the project.clj (which contains dependency information) into the container
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY project.clj /usr/src/app/

# Fetch depdencies (thanks to Docker's cache, this will only be run if project.clj is changed)
RUN lein deps

# Now copy the source code and compile into a self-contained jar
COPY . /usr/src/app
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app-standalone.jar

# Finally, our entrypoint is to run the app's jar (the JVM prefers IPv6 by default, which many hosts do not support and thus requires a flag to circumvent)
CMD ["java", "-jar", "-Djava.net.preferIPv4Stack=true", "app-standalone.jar"]