# Aminmart Password Manager - Makefile

.PHONY: help clean build dev install run release release-signed devices logs uninstall test lint check

GRADLE = ./gradlew
ADB = adb
PACKAGE = com.aminmart.passwordmanager.debug
APK_DEBUG_DIR = app/build/outputs/apk/debug
APK_RELEASE_DIR = app/build/outputs/apk/release

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
	@echo "✓ Debug APKs (per-ABI):"
	@ls -1 $(APK_DEBUG_DIR)/*.apk

# Builds are split per ABI; pick the APK matching the connected device
install:
	@ABI=$$($(ADB) shell getprop ro.product.cpu.abi | tr -d '\r'); \
	APK="$(APK_DEBUG_DIR)/app-$$ABI-debug.apk"; \
	if [ ! -f "$$APK" ]; then echo "✗ $$APK tidak ditemukan. Jalankan 'make build' dulu."; exit 1; fi; \
	$(ADB) install -r "$$APK"; \
	echo "✓ Installed $$APK"

run:
	$(ADB) shell am start -n $(PACKAGE)/com.aminmart.passwordmanager.MainActivity
	@echo "✓ App launched"

dev: build install run

# Release build; the gradle config signs automatically when
# keystore.properties + the keystore file exist, else produces unsigned APKs
release:
	$(GRADLE) assembleRelease
	@echo ""
	@echo "✓ Release APKs (per-ABI):"
	@ls -1 $(APK_RELEASE_DIR)/*.apk

release-signed:
	@if [ ! -f keystore.properties ]; then \
		echo "✗ keystore.properties tidak ditemukan di root project"; \
		exit 1; \
	fi
	$(GRADLE) assembleRelease
	@echo ""
	@echo "✓ Signed release APKs (per-ABI):"
	@ls -1 $(APK_RELEASE_DIR)/*.apk

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
