.PHONY: build debug release clean test lint changelog tag push all

# FixVol — local build automation
# Relies on JAVA_HOME pointing to JDK 17+
JAVA_HOME ?= /usr/lib/jvm/java-17-openjdk
export JAVA_HOME

GRADLE = ./gradlew --no-daemon

build: debug

debug:
	@echo "=== Building debug APK ==="
	$(GRADLE) :app:assembleDebug
	@ls -lh app/build/outputs/apk/debug/app-debug.apk

release:
	@echo "=== Building release APK (unsigned locally, signed in CI) ==="
	$(GRADLE) :app:assembleRelease
	@ls -lh app/build/outputs/apk/release/app-release-unsigned.apk

clean:
	@echo "=== Cleaning build artifacts ==="
	$(GRADLE) clean

test:
	@echo "=== Running unit tests ==="
	$(GRADLE) :app:testDebugUnitTest

lint:
	@echo "=== Running lint checks ==="
	$(GRADLE) :app:lintDebug

changelog:
	@echo "=== Updating CHANGELOG.md for v1.5.0 ==="
	bash scripts/patch-changelog.sh

tag:
	@echo "=== Creating v1.5.0 git tag ==="
	git tag v1.5.0
	@echo "Tag v1.5.0 created. Run 'make push' to push."

push: tag
	@echo "=== Pushing to origin (triggers CI release) ==="
	git push origin master v1.5.0
	@echo "CI release workflow triggered. Check GitHub Actions for status."

all: clean lint test release changelog tag
	@echo ""
	@echo "=== FixVol v1.5.0 release pipeline complete ==="
	@echo "Debug APK:  app/build/outputs/apk/debug/app-debug.apk"
	@echo "Release:    app/build/outputs/apk/release/app-release-unsigned.apk (unsigned locally)"
	@echo "Changelog:  CHANGELOG.md updated"
	@echo "Tag:        v1.5.0 created locally"
	@echo ""
	@echo "Next: run 'make push' to push and trigger the signed CI build."
