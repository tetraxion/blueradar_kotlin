# Testing Guide - BlueRadar

Panduan testing lengkap untuk memverifikasi semua fitur aplikasi BlueRadar bekerja dengan baik.

---

## 🎯 Persiapan Testing

### Perangkat yang Dibutuhkan:
1. **HP Android** dengan Bluetooth LE support (API 26+)
2. **Perangkat BLE** untuk di-scan:
   - Earbuds Bluetooth (AirPods, Galaxy Buds, dll)
   - Smartwatch (Apple Watch, Galaxy Watch, dll)
   - Fitness tracker (Mi Band, Fitbit, dll)
   - Bluetooth speaker
   - Mouse/keyboard Bluetooth
   - HP lain dengan Bluetooth aktif

### Setup Awal:
- ✅ App sudah terinstall di HP
- ✅ Bluetooth HP dalam kondisi **ON**
- ✅ Location/GPS dalam kondisi **ON** (required untuk BLE scanning)
- ✅ Ada minimal 2-3 perangkat BLE aktif di sekitar

---

## 📋 Skenario Testing

### **Test 1: First Launch & Permissions**

**Tujuan:** Verifikasi permission flow bekerja dengan baik

**Langkah:**
1. Buka app BlueRadar untuk pertama kali
2. App akan minta permission Bluetooth & Location

**Expected Result:**
- ✅ Muncul dialog permission request
- ✅ Setelah grant permission, masuk ke Dashboard
- ✅ Tombol FAB (Play/Stop) muncul di kanan bawah

**Test Case Alternatif:**
- Tolak permission → App tampilkan pesan "Permission Required" dengan tombol "Grant Permissions"
- Tap "Grant Permissions" → Muncul permission dialog lagi

---

### **Test 2: Dashboard Scanner - Basic Scanning**

**Tujuan:** Verifikasi BLE scanning bekerja

**Langkah:**
1. Di Dashboard, tap tombol **Play (▶️)** di kanan bawah
2. Tunggu beberapa detik

**Expected Result:**
- ✅ Tombol berubah jadi **Stop (⏹️)**
- ✅ Muncul progress bar di atas list
- ✅ Device BLE mulai muncul di list (nama, MAC, RSSI, jarak)
- ✅ Device ter-sort berdasarkan sinyal terkuat (RSSI tertinggi di atas)
- ✅ Warna badge berubah sesuai kategori sinyal:
  - 🟢 Hijau = sinyal kuat (dekat)
  - 🟡 Kuning = sinyal sedang
  - 🟠 Oranye = sinyal lemah
  - 🔴 Merah = sangat lemah

**Test Case Alternatif:**
- Tidak ada device terdeteksi → Tampil "Scanning for devices..."
- Tap Stop → Scanning berhenti, list device tetap ada

---

### **Test 3: Real-time RSSI Update**

**Tujuan:** Verifikasi RSSI update secara real-time

**Langkah:**
1. Scanning aktif
2. Pilih 1 device di list
3. **Jauhkan HP kamu** dari device tersebut (jalan mundur 2-3 meter)
4. Lihat perubahan RSSI dan jarak

**Expected Result:**
- ✅ Nilai RSSI **turun** (semakin negatif, misal dari -40 → -60 dBm)
- ✅ Estimasi jarak **bertambah** (misal dari 1m → 5m)
- ✅ Warna badge **berubah** sesuai kategori (hijau → kuning → oranye)
- ✅ Update terjadi real-time tanpa refresh manual

**Test Kedekatan:**
1. **Dekatkan HP** ke device tersebut lagi
2. **Expected:** RSSI naik, jarak mengecil, warna badge lebih hijau

---

### **Test 4: Search & Filter**

**Tujuan:** Verifikasi fitur search dan RSSI filter

**Langkah A — Search:**
1. Di Dashboard dengan scanning aktif
2. Ketik nama device di search bar (misal: "Galaxy")
3. **Expected:** Hanya device dengan nama "Galaxy" yang muncul

**Langkah B — RSSI Filter:**
1. Geser slider RSSI threshold ke **-70 dBm**
2. **Expected:** 
   - Hanya device dengan sinyal ≥ -70 dBm yang muncul
   - Device lemah (< -70 dBm) otomatis hilang dari list

**Langkah C — Clear Filter:**
1. Hapus text di search bar
2. Geser slider ke **-100 dBm**
3. **Expected:** Semua device muncul lagi

---

### **Test 5: Navigation ke Radar View**

**Tujuan:** Verifikasi tracking intensif 1 device

**Langkah:**
1. Di Dashboard, tap salah satu device di list
2. App pindah ke **Radar View**

**Expected Result:**
- ✅ Top bar menampilkan "Radar View" dengan tombol Back
- ✅ Nama device dan MAC address tampil di atas
- ✅ Visualisasi radar dengan lingkaran konsentris muncul
- ✅ 3 Card info tampil:
  - Signal Strength (RSSI dalam dBm)
  - Estimated Distance (jarak dalam meter)
  - Signal Category (kategori dengan warna)
- ✅ Data update real-time

**Test Real-time Update:**
1. Jauhkan/dekatkan HP dari device
2. **Expected:** 
   - Lingkaran radar berubah ukuran
   - RSSI, jarak, dan kategori update langsung
   - Warna card berubah sesuai sinyal

---

### **Test 6: History Log**

**Tujuan:** Verifikasi penyimpanan riwayat ke database

**Langkah:**
1. Di Dashboard, tap ikon **History (🕒)** di top bar
2. Masuk ke screen **Device History**

**Expected Result:**
- ✅ Tampil list semua device yang pernah di-scan
- ✅ Setiap device menampilkan:
  - Nama device
  - MAC address
  - Last seen (timestamp)
  - Last RSSI
  - Scan count (jumlah kali terdeteksi)

**Test Persistence:**
1. **Tutup app** (swipe dari recent apps)
2. **Buka app lagi**
3. Masuk ke History
4. **Expected:** Data history masih ada (tidak hilang)

---

### **Test 7: Delete History**

**Tujuan:** Verifikasi fitur delete

**Langkah A — Delete Single Device:**
1. Di History screen, tap ikon **Delete (🗑️)** di salah satu device
2. Konfirmasi dialog muncul
3. Tap **Delete**
4. **Expected:** Device hilang dari list

**Langkah B — Clear All:**
1. Tap ikon **Delete** di top bar
2. Konfirmasi dialog "Clear History" muncul
3. Tap **Clear**
4. **Expected:** Semua device hilang, tampil "No device history yet"

---

### **Test 8: Bluetooth On/Off Handling**

**Tujuan:** Verifikasi error handling saat Bluetooth dimatikan

**Langkah:**
1. Di Dashboard dengan scanning **aktif**
2. **Matikan Bluetooth** HP dari Quick Settings
3. Lihat perubahan di app

**Expected Result:**
- ✅ Scanning otomatis **berhenti**
- ✅ Muncul pesan error "Bluetooth has been disabled"
- ✅ Tampil layar warning "Bluetooth is Disabled"

**Recovery Test:**
1. **Nyalakan Bluetooth** lagi
2. Tap tombol Play
3. **Expected:** Scanning berjalan normal lagi

---

### **Test 9: App Lifecycle (Background/Foreground)**

**Tujuan:** Verifikasi scanning berhenti saat app di background

**Langkah:**
1. Di Dashboard dengan scanning **aktif**
2. Tekan tombol **Home** (app ke background)
3. Tunggu 5 detik
4. Buka app lagi

**Expected Result:**
- ✅ Scanning tetap aktif (tidak otomatis stop)
- ✅ List device tetap ada
- ✅ RSSI update kembali normal

**Note:** Android membatasi background BLE scanning untuk hemat baterai. Ini adalah behavior normal.

---

### **Test 10: Screen Rotation**

**Tujuan:** Verifikasi app tidak crash saat rotasi

**Langkah:**
1. Di Dashboard dengan scanning aktif dan ada device di list
2. **Rotasi HP** (portrait → landscape)
3. Rotasi kembali (landscape → portrait)

**Expected Result:**
- ✅ App tidak crash
- ✅ Data tidak hilang
- ✅ Scanning tetap berjalan
- ✅ UI menyesuaikan orientasi

---

### **Test 11: Permission Denial Recovery**

**Tujuan:** Verifikasi app handle permission denied

**Langkah:**
1. **Uninstall app**
2. Install lagi
3. Saat minta permission, tap **Deny**
4. **Expected:** Tampil "Permission Required" dengan tombol "Grant Permissions"
5. Tap "Grant Permissions"
6. Tap **Allow** di dialog permission
7. **Expected:** Masuk ke Dashboard normal

---

### **Test 12: Edge Case - No BLE Devices**

**Tujuan:** Verifikasi tampilan saat tidak ada device

**Langkah:**
1. Matikan semua device BLE di sekitar (earbuds, smartwatch, dll)
2. Di Dashboard, tap Play
3. Tunggu 10 detik

**Expected Result:**
- ✅ Progress bar aktif
- ✅ Tampil text "Scanning for devices..."
- ✅ Tidak ada crash atau error

---

## 🐛 Bug Reporting

Jika menemukan bug saat testing, catat:
1. **Langkah untuk reproduce**
2. **Expected behavior**
3. **Actual behavior**
4. **Screenshot/video** (jika ada)
5. **Device info:**
   - Model HP: _______
   - Android version: _______
   - App version: 1.0

---

## ✅ Final Checklist

Centang jika semua test berhasil:

- [ ] Test 1: Permissions ✅
- [ ] Test 2: Basic Scanning ✅
- [ ] Test 3: Real-time RSSI Update ✅
- [ ] Test 4: Search & Filter ✅
- [ ] Test 5: Radar View ✅
- [ ] Test 6: History Log ✅
- [ ] Test 7: Delete History ✅
- [ ] Test 8: Bluetooth On/Off ✅
- [ ] Test 9: App Lifecycle ✅
- [ ] Test 10: Screen Rotation ✅
- [ ] Test 11: Permission Denial ✅
- [ ] Test 12: No BLE Devices ✅

---

## 📊 Performance Check

Monitor performa app:

- **Memory usage:** Tidak boleh leak (monitor di Android Studio Profiler)
- **Battery drain:** Scanning aktif 5 menit ≈ 1-2% battery (normal)
- **Responsiveness:** UI tidak freeze saat scanning
- **Crash:** 0 crash selama testing

---

## 🎓 Tips Testing

1. **Test dengan real device** — Emulator tidak support BLE dengan baik
2. **Test di berbagai kondisi sinyal** — Dekat, sedang, jauh
3. **Test dengan berbagai jenis device** — Earbuds, smartwatch, speaker
4. **Test permission flow** — Uninstall/install ulang untuk test fresh install
5. **Monitor logcat** — Lihat error/warning di Android Studio

---

**Happy Testing! 🚀**
