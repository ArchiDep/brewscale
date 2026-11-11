# Checks the project: make
#
# Every target installs what it needs into the repository, ignored by Git:
# Prettier into node_modules, Locust into .venv. The versions of Java, Node.js
# and Python come from .tool-versions when asdf is installed.

LOCUST_VERSION := 2.47.0
LOCUST_PORT ?= 4290
LOCUSTFILE ?= locustfile.py

VENV := .venv
LOCUST ?= $(VENV)/bin/locust

# Maven runs on JAVA_HOME, not on the java command that asdf provides.
ifneq ($(shell command -v asdf),)
export JAVA_HOME := $(shell asdf where java)
endif
JAVA := $(if $(JAVA_HOME),$(JAVA_HOME)/bin/java,java)

MVN := mvn --batch-mode

.PHONY: all check clean doctoc doctoc-check format format-check locust package verify

all: check

## Run every check: table of contents, formatting, build, tests, coverage,
## nullability, Locust.
check: doctoc-check format-check verify locust

## Check that the README's table of contents is up to date.
doctoc-check: node_modules
	npm run doctoc:check

## Update the README's table of contents with doctoc.
doctoc: node_modules
	npm run doctoc

## Check that every file is formatted with Prettier.
format-check: node_modules
	npm run format:check

## Format every file with Prettier.
format: node_modules
	npm run format

## Build, run the tests, check the coverage and the nullability.
verify:
	$(MVN) verify -P nullaway

## Build the jar without running the tests.
package:
	$(MVN) package -DskipTests

## Run the locustfile against the jar for a few seconds, and check that it made
## requests without any failure. Locust exits with a non-zero code if any
## request failed, but a run that made no request at all would pass, hence the
## count.
locust: package $(LOCUST)
	@rm -f target/locust_*.csv
	@set -e; \
	BREWSCALE_HOST=127.0.0.1 BREWSCALE_PORT=$(LOCUST_PORT) \
		$(JAVA) -jar target/brewscale.jar > target/locust-brewscale.log 2>&1 & \
	pid=$$!; \
	trap 'kill $$pid; echo "Brewscale logs:"; cat target/locust-brewscale.log' EXIT; \
	for attempt in $$(seq 30); do \
		curl --silent --fail --output /dev/null http://127.0.0.1:$(LOCUST_PORT)/ && break; \
		sleep 1; \
	done; \
	$(LOCUST) --locustfile $(LOCUSTFILE) --headless --host http://127.0.0.1:$(LOCUST_PORT) \
		--users 2 --spawn-rate 2 --run-time 10s --only-summary --csv target/locust; \
	count=$$(awk -F, '$$2 == "Aggregated" { print $$3 }' target/locust_stats.csv); \
	echo "Locust made $$count requests"; \
	test "$$count" -gt 0

clean:
	$(MVN) clean
	rm -rf coverage

node_modules: package.json package-lock.json
	npm ci
	@touch node_modules

$(VENV)/bin/locust:
	python3 -m venv $(VENV)
	$(VENV)/bin/pip install --quiet locust==$(LOCUST_VERSION)
