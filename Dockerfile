# syntax=docker/dockerfile:1

# Build
# =====
# The jar is the same on every architecture, so it is built once, on the
# builder's own, even for an image of another architecture.
FROM --platform=$BUILDPLATFORM maven:3.9.16-eclipse-temurin-25 AS build

WORKDIR /build

COPY pom.xml ./
COPY src ./src/

# Runs the tests before packaging, as it does on a server: a failing test stops
# the build. The cache keeps Maven's downloads between local builds.
RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode package

# Runtime
# =======
FROM eclipse-temurin:25.0.5_8-jre-resolute

RUN useradd --system --no-create-home --shell /usr/sbin/nologin brewscale

WORKDIR /brewscale

# Owned by root, so the user the application runs as cannot replace it.
COPY --from=build /build/target/brewscale.jar ./

USER brewscale

EXPOSE 3000

# A barista serves one request at a time, so an HTTP check would wait in the
# queue behind every order being brewed, and time out under load. Opening a
# connection is enough: the kernel accepts it into the listening socket's
# backlog while the barista is busy. The image has bash but no wget or curl.
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/${BREWSCALE_PORT:-3000}"]

# The JVM's performance data is written under /tmp, which is not writable when
# the container's filesystem is read-only, and is only read by monitoring tools
# such as jstat. The barista number is given as the command, e.g. "2".
ENTRYPOINT ["java", "-XX:-UsePerfData", "-jar", "/brewscale/brewscale.jar"]
