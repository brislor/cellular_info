import 'dart:convert';
import 'dart:developer';

import 'package:cellular_info/model/lte_band.dart';
import 'package:cellular_info/model/nr_signal.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'cellular_info_platform_interface.dart';
import 'model/nr_band.dart';

/// An implementation of [CellularInfoPlatform] that uses method channels.
class MethodChannelCellularInfo extends CellularInfoPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('cellular_info');

  @visibleForTesting
  final nrEventChannel = const EventChannel('cellular_info_nr_stream');

  @visibleForTesting
  final allCellInfoEventChannel = const EventChannel('cellular_info_all_cell_stream');

  @visibleForTesting
  final serviceEventChannel = const EventChannel('cellular_info_event_service');

  @override
  Future<List<SignalNr>> getCellInfo() async {
    final list = await methodChannel.invokeListMethod("getCellInfo");
    return list?.map((e) {
      return SignalNr.fromJson(Map<String, dynamic>.from(e as Map));
    }).toList() ??
        [];
  }

  @override
  Future<List<SignalNr>> getNrCellInfo() async {
    final list = await methodChannel.invokeListMethod("getNrCellInfo");
    return list?.map((e) {
          return SignalNr.fromJson(Map<String, dynamic>.from(e as Map));
        }).toList() ??
        [];
  }

  @override
  Stream<List<SignalNr>> get allCellInfoStream{
    return allCellInfoEventChannel.receiveBroadcastStream().asyncMap((event) async {
      final rawList = event as List<dynamic>;
      final signals = await Future.wait(rawList.map((e) async {
        var signalNr = SignalNr.fromJson(Map<String, dynamic>.from(e));
        if (signalNr.arfcn != null) {
          if (signalNr.type == 0) {
            signalNr.freq = nrArfcnToFrequency(signalNr.arfcn!);
            signalNr.band =
                convert2Band(await getNrBandForArfcn(signalNr.arfcn!));
          } else if (signalNr.type == 1) {
            final bandInfo = LteFrequencyConverter.getLteBandInfoFromEarfcn(signalNr.arfcn!);
            signalNr.freq = bandInfo.dlFreq;
            signalNr.band = "${bandInfo.band}";
          }
        }
        return signalNr;
      }));
      return signals;
    }).handleError((error) {
      throw Exception("allCellInfoStream...$error");
    });
  }

  @override
  Stream<List<SignalNr>> get nrSignalStream {
    return nrEventChannel.receiveBroadcastStream().asyncMap((event) async {
      final rawList = event as List<dynamic>;
      final signals = await Future.wait(rawList.map((e) async {
        var signalNr = SignalNr.fromJson(Map<String, dynamic>.from(e));
        if (signalNr.arfcn != null) {
          signalNr.freq = nrArfcnToFrequency(signalNr.arfcn!);
          signalNr.band =
              convert2Band(await getNrBandForArfcn(signalNr.arfcn!));
        }
        return signalNr;
      }));
      // final filter = signals.where((e){
      //   if(e.band != null){
      //     return e.band!.contains("77")||e.band!.contains("78");
      //   }
      //   return false;
      // }).toList();
      // return filter;
      return signals;
    }).handleError((error) {
      throw Exception("nrSignalStream error.....$error");
    });
  }

  @override
  Stream<List<SignalNr>> get nrSignalStreamFromService {
    return serviceEventChannel.receiveBroadcastStream().map((event) {
      return (event as List).map((e) {
        return SignalNr.fromJson(Map<String, dynamic>.from(e));
      }).toList();
    }).handleError((error) {
      throw Exception("nrSignalStreamFromService error.....$error");
    });
  }

  @override
  Future<void> startService() async {
    try {
      await methodChannel.invokeListMethod("startService");
    } on PlatformException catch (e) {
      log("start android service failed.${e.message}",
          name: "$runtimeType",
          level: 10,
          error: e,
          stackTrace: StackTrace.current);
    }
  }

  @override
  Future<void> stopService() async {
    try {
      await methodChannel.invokeListMethod("stopService");
    } on PlatformException catch (e) {
      log("stop android service failed.${e.message}",
          name: "$runtimeType",
          level: 10,
          error: e,
          stackTrace: StackTrace.current);
    }
  }

  double nrArfcnToFrequency(int nrArfcn) {
    if (nrArfcn < 0) {
      throw ArgumentError('ARFCN must be non-negative');
    }

    if (nrArfcn < 600000) {
      return 0.005 * nrArfcn;
    } else if (nrArfcn < 2016667) {
      return 3000.0 + 0.015 * (nrArfcn - 600000);
    } else {
      return 24250.08 + 0.06 * (nrArfcn - 2016667);
    }
  }

  Future<List<NrBand>> getNrBandForArfcn(int arfcn) async {
    // 从 assets 加载 JSON 文件
    final jsonText = await rootBundle.loadString('packages/cellular_info/assets/band.json');
    final jsonMap = json.decode(jsonText) as Map<String, dynamic>;
    final mapping = NrBandMapping.fromJson(jsonMap);

    // 计算频率
    final frequency = nrArfcnToFrequency(arfcn);
    final result = <NrBand>[];

    // 检查 FR1 频段
    for (final band in mapping.fr1) {
      if (band.dlRange != null && band.dlRange!.length == 2) {
        if (frequency >= band.dlRange![0] && frequency <= band.dlRange![1]) {
          result.add(band);
        }
      }
    }

    // 检查 FR2 频段
    for (final band in mapping.fr2) {
      if (band.dlRange != null && band.dlRange!.length == 2) {
        if (frequency >= band.dlRange![0] && frequency <= band.dlRange![1]) {
          result.add(band);
        }
      }
    }

    return result;
  }

  String convert2Band(List<NrBand> list) {
    if (list.isEmpty) {
      return "";
    }

    final List<String> processedBands = list
        .map<String>((NrBand e) => e.band.replaceFirst(RegExp(r'^n'), ''))
        .toList();

    return processedBands.join('/');
  }
}
