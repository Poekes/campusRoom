#!/bin/bash
set -e
cd "$(dirname "$0")"

# Kompilasi proyek agar perubahan kode terbaru langsung diterapkan
./build.sh

echo "=== Menjalankan CampusRoom — Aplikasi Manajemen Booking Ruangan Kampus ==="
java -cp "bin:lib/*" com.campusroom.CampusRoomApp "$@"
