#!/bin/bash

# SQLite-Vec Android Bindings Build Script
# This script sets up and builds the Android bindings for sqlite-vec

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}SQLite-Vec Android Bindings Build Script${NC}"
echo "========================================"

# Check if Android SDK is installed
if [ -z "$ANDROID_SDK_ROOT" ] && [ -z "$ANDROID_HOME" ]; then
    echo -e "${RED}Error: Android SDK not found. Please set ANDROID_SDK_ROOT or ANDROID_HOME environment variable.${NC}"
    echo "You can install Android SDK using Android Studio or command line tools."
    exit 1
fi

# Check if NDK is installed
if [ -z "$ANDROID_NDK_ROOT" ] && [ -z "$ANDROID_NDK_HOME" ]; then
    echo -e "${YELLOW}Warning: Android NDK not found. Please set ANDROID_NDK_ROOT or ANDROID_NDK_HOME environment variable.${NC}"
    echo "You can install Android NDK using Android Studio SDK Manager."
fi

# Set up environment variables
export ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-$ANDROID_HOME}
export ANDROID_NDK_ROOT=${ANDROID_NDK_ROOT:-$ANDROID_NDK_HOME}

echo "Android SDK: $ANDROID_SDK_ROOT"
echo "Android NDK: $ANDROID_NDK_ROOT"

# Check if we're in the correct directory
if [ ! -f "build.gradle" ]; then
    echo -e "${RED}Error: build.gradle not found. Please run this script from the bindings/android directory.${NC}"
    exit 1
fi

# Create gradle wrapper if it doesn't exist
if [ ! -f "gradlew" ]; then
    echo -e "${YELLOW}Creating Gradle wrapper...${NC}"
    gradle wrapper --gradle-version 8.4
fi

# Make gradlew executable
chmod +x gradlew

# Clean previous build
echo -e "${GREEN}Cleaning previous build...${NC}"
./gradlew clean

# Build the library
echo -e "${GREEN}Building Android library...${NC}"
./gradlew assembleDebug

# Run unit tests
echo -e "${GREEN}Running unit tests...${NC}"
./gradlew testDebugUnitTest

# Generate documentation
echo -e "${GREEN}Generating documentation...${NC}"
./gradlew javadoc

echo -e "${GREEN}Build completed successfully!${NC}"
echo ""
echo "Generated files:"
echo "- Library AAR: build/outputs/aar/"
echo "- Test reports: build/reports/tests/"
echo "- Documentation: build/docs/javadoc/"
echo ""
echo "To use the library in your Android project:"
echo "1. Copy the AAR file to your app's libs/ directory"
echo "2. Add the following to your app's build.gradle:"
echo "   implementation files('libs/sqlite-vec-android-debug.aar')"
echo "   implementation 'androidx.sqlite:sqlite:2.3.1'"
echo ""
echo "Example usage:"
echo "   SQLiteVec.load(database);"
echo "   List<SearchResult> results = SQLiteVec.searchSimilar(database, \"vectors\", \"embedding\", queryVector, 10);"
