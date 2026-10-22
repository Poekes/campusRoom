#!/bin/bash
set -e
cd "$(dirname "$0")"

echo "=== Mengompilasi Proyek CampusRoom (Java Swing + SQLite) ==="
mkdir -p bin

# Compile dengan javac menggunakan lib/*.jar
javac -d bin -cp "lib/*" $(find src/main/java -name "*.java")

# Salin resource gambar ke folder bin
mkdir -p bin/images
cp -r src/main/resources/images/* bin/images/ 2>/dev/null || true

echo "=== Kompilasi Berhasil! File binary tersimpan di direktori bin/ ==="
