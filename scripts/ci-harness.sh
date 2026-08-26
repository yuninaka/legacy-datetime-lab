#!/usr/bin/env bash
#
# CI guardrail harness for AI-driven autonomous fixes.
#
# Guards against an agent "passing" by editing the test suite instead of
# fixing production code, then runs the four gates a real fix must clear:
# formatting, compile, static analysis, and unit tests + mutation testing.

set -e

echo "=== Guardrail: src/test/ must not be modified ==="
if ! git diff --quiet -- src/test/ || ! git diff --cached --quiet -- src/test/; then
  echo "ERROR: src/test/ の変更は禁止されています"
  exit 1
fi

echo "=== 1/4 Code Formatter: mvn spotless:check ==="
mvn spotless:check

echo "=== 2/4 Type Check & Build: mvn compile -DskipTests ==="
mvn compile -DskipTests

echo "=== 3/4 Linter: mvn spotbugs:check ==="
mvn spotbugs:check

echo "=== 4/4 Unit Test & Mutation: mvn test pitest:mutationCoverage ==="
mvn test pitest:mutationCoverage

echo "ALL CI GUARDRAILS PASSED!"
