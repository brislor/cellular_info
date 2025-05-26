# cellular_info

A Flutter plugin for retrieving 5G NR signal information on Android devices.  
It uses `TelephonyManager.requestCellInfoUpdate()` to provide periodic, real-time cell data via a Dart Stream.

## 📱 Supported Platforms

- ✅ Android (API 29+ / Android 10+)
- ❌ Not supported: iOS / Web / macOS

---

## 🚀 Installation

```yaml
dependencies:
  cellular_info: ^1.0.0
```

## 🔧 Usage
```dart
import 'package:cellular_info/cellular_info.dart';

final subscription = CellularInfo.getNrStream().listen((list) {
  print("5G NR Info: $info");
});
```
Cancel the stream to stop processing:
```dart
subscription.cancel();
```

## 📊 Sample Output
```json
[
  {
    "band": '77/78',
    "freq": 635334.33,
    "arfcn": 3992,
    "ssRsrp": -85,
    "ssRsrq": -11,
    "ssSinr": -7,
    "csiRsrp": -85,
    "csiRsrq": -11,
    "csiSinr": -7,
    "dbm": -100,
  },
  {
    "band": '77/78',
    "freq": 635334.33,
    "arfcn": 3992,
    "ssRsrp": -85,
    "ssRsrq": -11,
    "ssSinr": -7,
    "csiRsrp": -85,
    "csiRsrq": -11,
    "csiSinr": -7,
    "dbm": -100,
  },
  {
    "band": '77/78',
    "freq": 635334.33,
    "arfcn": 3992,
    "ssRsrp": -85,
    "ssRsrq": -11,
    "ssSinr": -7,
    "csiRsrp": -85,
    "csiRsrq": -11,
    "csiSinr": -7,
    "dbm": -100,
  },
]
```