#!/bin/bash

PROMETHEUS_URL="http://13.250.55.151:9090/api/v1/write"
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
