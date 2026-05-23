#!/usr/bin/env bash
# Compiles CsvToSqliteLoader.java and runs it.
# Usage: ./load.sh [csv-dir] [output.db]
#   csv-dir   defaults to "../Spec and CSVs/CSV"
#   output.db defaults to "timetable.db"
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Download sqlite-jdbc if needed
./download-deps.sh

# Build classpath from all JARs in lib/
CP=".:$(echo lib/*.jar | tr ' ' ':')"

# Compile
echo "Compiling..."
javac -cp "$CP" ./*.java

# Run
echo "Running loader..."
java -cp "$CP" CsvToSqliteLoader "$@"
