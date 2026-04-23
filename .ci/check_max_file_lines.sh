#!/usr/bin/env bash
set -euo pipefail

MAX_FILE_LINES="${MAX_FILE_LINES:-300}"

TMP_VIOLATIONS="$(mktemp)"
trap 'rm -f "$TMP_VIOLATIONS"' EXIT

SCANNED_FILES=0
VIOLATIONS=0

is_excluded_path() {
  case "$1" in
    node_modules/*|*/node_modules/*|\
    target/*|*/target/*|\
    dist/*|*/dist/*|\
    build/*|*/build/*|\
    coverage/*|*/coverage/*|\
    vendor/*|*/vendor/*|\
    .state/*|*/.state/*|\
    logs/*|*/logs/*|\
    tmp/*|*/tmp/*|\
    scripts/doc-sync/state/*|*/scripts/doc-sync/state/*)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

is_test_path() {
  case "$1" in
    src/test/*|*/src/test/*|\
    tests/*|*/tests/*|\
    test/*|*/test/*|\
    __tests__/*|*/__tests__/*|\
    *.spec.ts|*.spec.tsx|*.test.ts|*.test.tsx|\
    *Test.java|*Tests.java|*IT.java|*IntegrationTest.java)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

list_candidate_files() {
  if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    git ls-files -z -- '*.java' '*.ts' '*.tsx'
    return
  fi

  find . \
    \( \
      -path './.git' -o \
      -path './node_modules' -o \
      -path './target' -o \
      -path './dist' -o \
      -path './build' -o \
      -path './coverage' -o \
      -path './vendor' -o \
      -path './.state' -o \
      -path './.venv' -o \
      -path './venv' -o \
      -path './tmp' -o \
      -path './logs' \
    \) -prune -o \
    -type f \( -name '*.java' -o -name '*.ts' -o -name '*.tsx' \) -print0
}

while IFS= read -r -d '' file; do
  if is_excluded_path "$file"; then
    continue
  fi
  if is_test_path "$file"; then
    continue
  fi
  if [ ! -f "$file" ]; then
    continue
  fi
  SCANNED_FILES=$((SCANNED_FILES + 1))
  line_count="$(wc -l < "$file" | tr -d ' ')"
  if [ "$line_count" -gt "$MAX_FILE_LINES" ]; then
    VIOLATIONS=$((VIOLATIONS + 1))
    printf "%s\t%s\n" "$line_count" "$file" >> "$TMP_VIOLATIONS"
  fi
done < <(list_candidate_files)

if [ "$VIOLATIONS" -gt 0 ]; then
  echo "ERREUR: fichiers Java/TS (hors tests) superieurs a ${MAX_FILE_LINES} lignes detectes."
  echo "Liste des fichiers a decouper par responsabilites:"
  while IFS=$'\t' read -r line_count path; do
    echo " - ${path#./} (${line_count} lignes)"
  done < <(sort -nr "$TMP_VIOLATIONS")
  echo "Action requise: decouper ces fichiers en sous-composants a responsabilite unique."
  exit 1
fi

echo "OK: controle max ${MAX_FILE_LINES} lignes valide sur ${SCANNED_FILES} fichiers Java/TS."
