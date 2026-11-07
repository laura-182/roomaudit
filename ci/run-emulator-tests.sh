#!/usr/bin/env bash
# Runs the instrumented walkthrough test on the connected emulator and keeps logcat for debugging.
set -u
adb logcat -c || true
# Hide "app isn't responding" system dialogs (slow CI emulators trigger them for the launcher)
adb shell settings put global hide_error_dialogs 1 || true
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null 2>&1 || true
./gradlew --no-daemon connectedDebugAndroidTest
status=$?
adb logcat -d > logcat.txt || true
if [ $status -ne 0 ]; then
  echo "===== Test failure details ====="
  find app/build/outputs/androidTest-results -name '*.xml' -exec sed -n '/<failure/,/<\/failure>/p' {} \; | head -400
  echo "===== App crashes in logcat ====="
  grep -A 25 "FATAL EXCEPTION" logcat.txt | head -60 || true
fi
echo "===== Screenshots saved ====="
find app/build/outputs/connected_android_test_additional_output -name '*.png' | sort || true
exit $status
