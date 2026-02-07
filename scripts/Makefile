# Scripts Makefile. Delegates to shell scripts for deps, test, lint, fmt, clean.
ROOT := $(dir $(lastword $(MAKEFILE_LIST)))/..
deps:
	$(ROOT)/scripts/deps.sh
test:
	$(ROOT)/scripts/test.sh
lint:
	$(ROOT)/scripts/lint.sh
fmt:
	$(ROOT)/scripts/fmt.sh
clean:
	$(ROOT)/scripts/clean.sh
