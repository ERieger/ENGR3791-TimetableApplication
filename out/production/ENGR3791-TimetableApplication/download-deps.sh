#!/usr/bin/env bash
# Downloads the SQLite JDBC driver into lib/ if not already present.
set -euo pipefail

SQLITE_JDBC_VERSION="3.47.1.0"
JAR="sqlite-jdbc-${SQLITE_JDBC_VERSION}.jar"
DEST="lib/${JAR}"
URL="https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/${SQLITE_JDBC_VERSION}/${JAR}"

if [ -f "$DEST" ]; then
    echo "sqlite-jdbc already present: $DEST"
    exit 0
fi

mkdir -p lib

echo "Downloading sqlite-jdbc ${SQLITE_JDBC_VERSION}..."
if command -v curl &>/dev/null; then
    curl -fL "$URL" -o "$DEST"
elif command -v wget &>/dev/null; then
    wget -q "$URL" -O "$DEST"
else
    echo "Error: neither curl nor wget found. Download manually:"
    echo "  $URL"
    echo "and place it in: $DEST"
    exit 1
fi

echo "Saved: $DEST"
