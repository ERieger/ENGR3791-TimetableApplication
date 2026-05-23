#!/usr/bin/env bash
# Compiles and runs the Timetable Application.
# Usage: ./run.sh [path/to/timetable.db]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Ensure sqlite-jdbc is present (copy from data-loader if needed)
if [ ! -f lib/sqlite-jdbc-*.jar ] 2>/dev/null; then
    if [ -f ../data-loader/lib/sqlite-jdbc-*.jar ] 2>/dev/null; then
        mkdir -p lib
        cp ../data-loader/lib/sqlite-jdbc-*.jar lib/
    else
        echo "sqlite-jdbc JAR not found. Run: cd ../data-loader && ./download-deps.sh"
        exit 1
    fi
fi

CP=".:$(echo lib/*.jar | tr ' ' ':')"

echo "Compiling..."
javac -cp "$CP" ./*.java

echo "Starting application..."
java -cp "$CP" TimetableApp "$@"
