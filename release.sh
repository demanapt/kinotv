#!/bin/bash
# Выпуск обновления KinoTV: собирает APK и публикует его в GitHub Releases.
# Перед запуском увеличь versionCode (и при желании versionName) в version.properties.
# Использование: ./release.sh "Что нового в этой версии"
set -euo pipefail
cd "$(dirname "$0")"

REPO="demanapt/kinotv"
NOTES="${1:-}"
CODE=$(grep '^versionCode=' version.properties | cut -d= -f2)
NAME=$(grep '^versionName=' version.properties | cut -d= -f2)
TAG="v$NAME"

if [ -z "${JAVA_HOME:-}" ] && [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi
if [ -z "${ANDROID_HOME:-}" ] && [ -d "$HOME/Library/Android/sdk" ]; then
  export ANDROID_HOME="$HOME/Library/Android/sdk"
fi

if [ -x ./gradlew ]; then ./gradlew assembleRelease; else gradle assembleRelease; fi

mkdir -p dist
cp app/build/outputs/apk/release/app-release.apk dist/KinoTV.apk
python3 - "$CODE" "$NAME" "https://github.com/$REPO/releases/download/$TAG/KinoTV.apk" "$NOTES" > dist/version.json <<'PY'
import json, sys
code, name, url, notes = sys.argv[1:]
print(json.dumps({"versionCode": int(code), "versionName": name, "apkUrl": url, "notes": notes}, ensure_ascii=False))
PY

gh release create "$TAG" dist/KinoTV.apk dist/version.json --repo "$REPO" --title "KinoTV $NAME" --notes "${NOTES:-Обновление}"
echo "Готово: версия $NAME (код $CODE) опубликована. Приставка увидит её при следующем запуске KinoTV."
