# Brewscale

A café whose baristas take a while to brew each coffee, designed to demonstrate
horizontal [scaling](https://en.wikipedia.org/wiki/Scalability) with [load
balancing](<https://en.wikipedia.org/wiki/Load_balancing_(computing)>).

[![build](https://github.com/ArchiDep/brewscale/actions/workflows/build.yml/badge.svg)](https://github.com/ArchiDep/brewscale/actions/workflows/build.yml)
[![license](https://img.shields.io/github/license/ArchiDep/brewscale)](https://opensource.org/licenses/MIT)

Each running instance of the application is a barista, identified by a number
and a colour, who serves one customer at a time. Brewing a drink takes a second,
during which the barista waits without using the CPU, so the next customers wait
in the queue. Running more baristas behind a load balancer makes the queue move
faster, even on a single processor core.

Brewscale is an example application of the [Media Engineering Architecture &
Deployment][archidep] course.

- [Requirements](#requirements)
- [Build the application](#build-the-application)
- [Run the application](#run-the-application)
- [Configuration](#configuration)
- [Load testing](#load-testing)
- [Development](#development)

## Requirements

- [Java][java] 25
- [Maven][maven] 3.9+

The build bundles the application's few dependencies into the jar, so it runs
with Java alone.

## Build the application

```bash
git clone https://github.com/ArchiDep/brewscale.git
cd brewscale
mvn package
```

This runs the automated tests and produces `target/brewscale.jar`.

## Run the application

```bash
java -jar target/brewscale.jar
```

Then visit http://localhost:3000 and order a drink.

The application takes one optional argument, the barista number, a positive
integer:

```bash
java -jar target/brewscale.jar 2
```

The number changes the colour of the page, cycling through eight colours, and is
shown on every page and in every line of the logs, which makes it easy to tell
several running instances apart. Without a number, the café has a single,
unnumbered barista.

The application logs the address it listens on at startup, and each order it
brews:

```
Barista 2 listening on 0.0.0.0:3000, brewing in 1s
Barista 2 brewed a flat white for Alice in 1.001s
```

## Configuration

Further configuration is possible through environment variables:

| Environment variable  | Default value | Description                                                          |
| :-------------------- | :------------ | :------------------------------------------------------------------- |
| `BREWSCALE_HOST`      | `0.0.0.0`     | The IP address or host name to listen on (`0.0.0.0` for any).        |
| `BREWSCALE_PORT`      | `3000`        | The port to listen on, from 0 to 65535 (0 lets the system pick).     |
| `BREWSCALE_BREW_TIME` | `1`           | The time it takes to brew each drink, in seconds (decimals allowed). |

An invalid setting stops the application at startup with a message naming the
setting.

## Load testing

The [`locustfile.py`](locustfile.py) describes a customer for the
[Locust][locust] load testing tool, who orders a flat white, waits one second,
and orders again. Run it from the repository's directory with the application
running:

```bash
locust --host http://localhost:3000
```

Then visit http://localhost:8089 to start the load test.

## Development

The versions of the tools used in development are in
[`.tool-versions`](.tool-versions), for [asdf][asdf]: Java, Node.js for
formatting, and Python for Locust.

```bash
# Run the automated tests and check that they cover 100% of the code.
mvn verify
```

The coverage report is written by [JaCoCo][jacoco] to `coverage/index.html`. The
build fails if any instruction, branch, line, method or class is not covered.

The code, the Markdown and the YAML files are formatted with
[Prettier][prettier] and its [Java plugin][prettier-java]:

```bash
# Install Prettier.
npm ci

# Format all files.
npm run format

# Check the formatting without changing any file.
npm run format:check
```

[archidep]: https://archidep.ch
[asdf]: https://asdf-vm.com
[jacoco]: https://www.jacoco.org/jacoco/
[java]: https://openjdk.org
[locust]: https://locust.io
[maven]: https://maven.apache.org
[prettier]: https://prettier.io
[prettier-java]: https://github.com/jhipster/prettier-java
