#!/bin/bash

DEVICE=$(adb devices | awk 'NR>1 && $2=="device" {print $1}')

if [[ -n "$DEVICE" ]]; then
    echo "Usando dispositivo: $DEVICE"

    ./gradlew assembleRelease &&
    adb install -r app/build/outputs/apk/release/Lune-release.apk &&
    adb shell monkey -p com.demonlab.lune -c android.intent.category.LAUNCHER 1
else
    echo "No hay dispositivos ADB autorizados."
fi
