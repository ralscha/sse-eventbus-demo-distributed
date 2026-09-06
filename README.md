# Distributed SSE EventBus demo

This demo runs two Spring Boot nodes connected through Valkey Pub/Sub. A
browser connects to both nodes and shows that an event sent to either node is
delivered to clients connected to both.

## Development

Install [Task](https://taskfile.dev/) and Docker, then run:

```
task demo
```

Open `http://localhost:5173`. The task starts Valkey, both Spring Boot nodes,
and the Vite development server.

## Packaged application

Build a self-contained executable jar that includes the frontend:

```
task package
java -jar target/sse-eventbus-demo-distributed.jar
```

The packaged application runs one node on `http://localhost:8080`. Run a
second jar with the `node-b` profile to demonstrate distribution:

```
java -jar target/sse-eventbus-demo-distributed.jar --spring.profiles.active=node-b
```

Valkey must be available on `localhost:6379`.

## License

Code released under the [Apache License](http://www.apache.org/licenses/).
