# cirno-phi-chart-snapshot

A fan-made chart image generator for mobile rhythm game [Phigros](https://www.taptap.cn/app/165287)
This project was created using the [Ktor Project Generator](https://start.ktor.io).

## Features

Here's a list of features included in this project:

| Name                                       | Description                        |
|--------------------------------------------|------------------------------------|
| [Routing](https://start.ktor.io/p/routing) | Provides a structured routing DSL  |
| [OpenAPI](https://start.ktor.io/p/openapi) | Serves OpenAPI documentation       |
| [Swagger](https://start.ktor.io/p/swagger) | Serves Swagger UI for your project |

## Building & Running

### Build from source

To build or run the project from source, clone this repo and use one of the following tasks:

| Task                          | Description                                                          |
|-------------------------------|----------------------------------------------------------------------|
| `./gradlew test`              | Run the tests                                                        |
| `./gradlew build`             | Build everything                                                     |
| `buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `buildImage`                  | Build the docker image to use with the fat JAR                       |
| `publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `run`                         | Run the server                                                       |
| `runDocker`                   | Run using the local docker image                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

### Using GitHub Action

To get build from GitHub Action, open Action view (fork if necessary), run "Build fat jar", then download
the artifact