#!/usr/bin/env bash
# Runs the instrumented walkthrough test on the connected emulator and keeps logcat for debugging.
set -u
adb logcat -c || true
./gradlew --no-daemon connectedDebugAndroidTest
status=$?
adb logcat -d > logcat.txt || true
exit $status
