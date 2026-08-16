# Adapt commands to the actual Gradle modules.
# AI agents should prefer `make check` before completing significant work.

.PHONY: format lint detekt test ui-test build check benchmark clean

format:
	@echo "TODO: configure ktlint/Spotless formatting"

lint:
	./gradlew lint

detekt:
	@echo "TODO: configure Detekt task"

test:
	./gradlew test

ui-test:
	@echo "TODO: configure connectedAndroidTest or managed-device tests"

build:
	./gradlew assembleDebug

benchmark:
	@echo "TODO: configure Macrobenchmark task"

check: lint test build
	@echo "Configured checks completed"

clean:
	./gradlew clean
