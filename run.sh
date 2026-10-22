#!/bin/bash
set -e
cd "$(dirname "$0")"

# Jika direktori bin belum ada atau kosong, jalankan build terlebih dahulu
if [ ! -d "bin" ] || [ -z "$(ls -A bin 2>/dev/null)" ]; then
    ./build.sh
fi

echo "=== Menjalankan CampusRoom — Aplikasi Manajemen Booking Ruangan Kampus ==="
java -cp "bin:lib/*" com.campusroom.CampusRoomApp "$@"
