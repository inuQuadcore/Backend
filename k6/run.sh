#!/bin/bash

if [ -f "$(dirname "$0")/.env" ]; then
  export $(grep -v '^#' "$(dirname "$0")/.env" | xargs)
fi

if [ -z "$PROMETHEUS_URL" ]; then
  echo "❌ PROMETHEUS_URL이 설정되지 않았습니다. k6/.env 파일을 확인해주세요."
  exit 1
fi

SCRIPT=${1:-"scripts/auth.js"}

echo "========================================"
echo "  k6 부하테스트 시작"
echo "  스크립트: $SCRIPT"
echo "  Prometheus: $PROMETHEUS_URL"
echo "========================================"

k6 run \
  --out experimental-prometheus-rw \
  -e K6_PROMETHEUS_RW_SERVER_URL=$PROMETHEUS_URL \
  -e K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM=true \
  "$SCRIPT"
