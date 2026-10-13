#!/usr/bin/env bash
# Places an order through the gateway and checks it was paid.
#   ./scripts/smoke-test.sh https://api-gateway-payments-dev.apps.<cluster-domain>
set -euo pipefail
BASE_URL="${1:-http://localhost:8080}"

echo "Health: $(curl -fsS "$BASE_URL/actuator/health")"

response=$(curl -fsS -X POST "$BASE_URL/api/orders" \
  -H 'Content-Type: application/json' \
  -d '{"customerEmail":"smoke@example.com","item":"Smoke test","amount":1.00,"currency":"USD",
       "card":{"cardNumber":"4242424242424242","expiry":"12/30","cvv":"123"}}')
echo "Order: $response"

echo "$response" | grep -q '"status":"PAID"' || { echo "Smoke test FAILED"; exit 1; }
echo "Smoke test PASSED"
