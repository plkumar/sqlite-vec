# Makefile for SQLiteVec Swift Bindings

.PHONY: build test clean demo example lint format install

# Build the package
build:
	swift build

# Run tests
test:
	swift test

# Run tests with verbose output
test-verbose:
	swift test --verbose

# Clean build artifacts
clean:
	swift package clean

# Run the demo example
demo:
	cd Examples && swift SimpleDemo.swift demo

# Build and run a complete example
example: build
	@echo "Building example..."
	swift build --target SQLiteVec
	@echo "Example built successfully. See Examples/ directory for usage examples."

# Install dependencies (if needed)
install:
	swift package resolve

# Check package for issues
lint:
	swift package diagnose-api-breaking-changes baseline

# Generate Xcode project (for development)
xcode:
	swift package generate-xcodeproj

# Format code (requires swift-format)
format:
	@if command -v swift-format >/dev/null 2>&1; then \
		find Sources Tests -name "*.swift" -exec swift-format -i {} \; ; \
		echo "Code formatted"; \
	else \
		echo "swift-format not found. Install with: brew install swift-format"; \
	fi

# Show package information
info:
	swift package show-dependencies
	swift package dump-package

# Run performance benchmarks (if any)
benchmark: build
	@echo "Running performance tests..."
	swift test --filter ".*Performance.*"

# Help
help:
	@echo "Available targets:"
	@echo "  build         - Build the Swift package"
	@echo "  test          - Run tests"
	@echo "  test-verbose  - Run tests with verbose output"
	@echo "  clean         - Clean build artifacts"
	@echo "  demo          - Run the demo example"
	@echo "  example       - Build example projects"
	@echo "  install       - Install/resolve dependencies"
	@echo "  lint          - Check for API breaking changes"
	@echo "  xcode         - Generate Xcode project"
	@echo "  format        - Format Swift code (requires swift-format)"
	@echo "  info          - Show package information"
	@echo "  benchmark     - Run performance tests"
	@echo "  help          - Show this help message"

# Default target
all: build test
