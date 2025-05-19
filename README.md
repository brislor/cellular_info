# nr_info_plugin

A Flutter plugin for retrieving 5G NR (New Radio) signal information on Android devices.  
It uses `TelephonyManager.requestCellInfoUpdate()` to provide periodic, real-time cell data via a Dart Stream.

## 📱 Supported Platforms

- ✅ Android (API 29+ / Android 10+)
- ❌ Not supported: iOS / Web / macOS

---

## 🚀 Installation

```yaml
dependencies:
  nr_info_plugin: ^0.0.1

