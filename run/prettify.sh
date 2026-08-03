#!/bin/sh
# Runs Prettify from a separately checked-out Westarps devtools repository.

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_DIR=$(dirname -- "$SCRIPT_DIR")
DEVTOOLS_DIR=${WESTARPS_DEVTOOLS_HOME:-}

if [ -z "$DEVTOOLS_DIR" ]; then
  DEVTOOLS_DIR=$(git -C "$REPO_DIR" config --local --get westarps.devtools.path 2>/dev/null || true)
fi

if [ -z "$DEVTOOLS_DIR" ]; then
  DEVTOOLS_DIR="$(dirname -- "$REPO_DIR")/devtools"
else
  case "$DEVTOOLS_DIR" in
    /*) ;;
    *) DEVTOOLS_DIR="$REPO_DIR/$DEVTOOLS_DIR" ;;
  esac
fi

LAUNCHER="$DEVTOOLS_DIR/run/prettify.sh"
if [ ! -x "$LAUNCHER" ]; then
  echo "Prettify launcher not found at: $LAUNCHER" >&2
  echo "Clone https://github.com/jewesta/devtools.git beside RetroCrawler," >&2
  echo "set WESTARPS_DEVTOOLS_HOME, or configure westarps.devtools.path locally." >&2
  exit 2
fi

exec "$LAUNCHER" --repo "$REPO_DIR" "$@"
