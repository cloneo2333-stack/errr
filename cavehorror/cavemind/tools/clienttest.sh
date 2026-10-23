#!/usr/bin/env bash
# Usage: tools/clienttest.sh [items|book|seek|maze|all]
# Restores a clean test world, then runs only the chosen client scenario.
set -e
cd "$(dirname "$0")/.."
MODE="${1:-all}"
rm -rf runc/saves/cmtest
cp -r runc/saves_base/cmtest runc/saves/cmtest
rm -f runc/screenshots/cm_*.png
export JAVA_HOME="C:/Users/ACER/.jdks/jdk-17.0.20.1+1"
export CAVEMIND_TEST="$MODE"
timeout 580 ~/.gradle/wrapper/dists/gradle-8.8-bin/dl7vupf4psengwqhwktix4v1/gradle-8.8/bin/gradle --offline runClient > runc/gradle_client.log 2>&1 || true
ls runc/screenshots | grep cm_ || true
grep -a "CLIENTTEST\] scenario" runc/logs/latest.log | tail -1
