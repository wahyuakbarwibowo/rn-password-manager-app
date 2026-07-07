# Aminmart Password Manager - Makefile

.PHONY: help clean build dev install run release release-signed devices logs uninstall test lint check

GRADLE = ./gradlew
ADB = adb
PACKAGE = com.aminmart.passwordmanager.debug
APK_DEBUG = app/build/outputs/apk/debug/app-debug.apk
APK_RELEASE = app/build/outputs/apk/release/app-release.apk

help:
	@echo "Aminmart Password Manager - Available Commands"
	@echo "==============================================="
	@echo ""
	@echo "Development:"
	@echo "  make build          - Build debug APK"
	@echo "  make dev            - Build + install + run on device"
	@echo "  make install        - Install debug APK on device"
	@echo "  make run            - Launch app on device"
	@echo "  make clean          - Clean build artifacts"
	@echo ""
	@echo "Release:"
	@echo "  make release        - Build release APKs (signed if keystore.properties exists)"
	@echo "  make release-signed - Build signed release APKs (fails without keystore.properties)"
	@echo ""
	@echo "Quality:"
	@echo "  make test           - Run unit tests"
	@echo "  make lint           - Run lint checks"
	@echo "  make check          - Run tests + lint"
	@echo ""
	@echo "Device:"
	@echo "  make devices        - List connected devices"
	@echo "  make logs           - View app logs"
	@echo "  make uninstall      - Uninstall app from device"

clean:
	$(GRADLE) clean

build:
	$(GRADLE) assembleDebug
	@echo ""
	@echo "✓ Debug APK ready: $(APK_DEBUG)"

install:
	$(ADB) install -r $(APK_DEBUG)
	@echo "✓ App installed"

run:
	$(ADB) shell am start -n $(PACKAGE)/com.aminmart.passwordmanager.MainActivity
	@echo "✓ App launched"

dev: build install run

# Release build; the gradle config signs automatically when
# keystore.properties + the keystore file exist, else produces an unsigned APK
release:
	$(GRADLE) assembleRelease
	@echo ""
	@echo "✓ Release APK:"
	@ls -1 app/build/outputs/apk/release/*.apk

release-signed:
	@if [ ! -f keystore.properties ]; then \
		echo "✗ keystore.properties tidak ditemukan di root project"; \
		exit 1; \
	fi
	$(GRADLE) assembleRelease
	@echo ""
	@echo "✓ Signed release APK:"
	@ls -1 app/build/outputs/apk/release/*.apk

test:
	$(GRADLE) test

lint:
	$(GRADLE) lint

check: test lint

devices:
	$(ADB) devices

logs:
	@PID=$$($(ADB) shell pidof -s $(PACKAGE)); \
	if [ -z "$$PID" ]; then \
		echo "App not running. Showing crash-focused logs..."; \
		$(ADB) logcat -v time AndroidRuntime:E ActivityManager:E libc:F DEBUG:F *:S; \
	else \
		$(ADB) logcat --pid=$$PID -v time; \
	fi

uninstall:
	$(ADB) uninstall $(PACKAGE)
	@echo "✓ App uninstalled"
