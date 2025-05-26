import 'dart:io';

import 'package:cellular_info/model/nr_signal.dart';

import 'cellular_info_platform_interface.dart';

class CellularInfo {
  static Future<List<SignalNr>> getNrCellInfo() async {
    final list = await CellularInfoPlatform.instance.getNrCellInfo();
    return list;
  }

  /// [enableService] - if ture that android service start.
  static Stream<List<SignalNr>> getNrStream({bool enableService = false}) {
    if (!Platform.isAndroid) {
      throw UnsupportedError('cellular_info is only supported on Android.');
    }
    if (enableService) {
      return CellularInfoPlatform.instance.nrSignalStreamFromService;
    } else {
      return CellularInfoPlatform.instance.nrSignalStream;
    }
  }
}
