import 'package:cellular_info/model/nr_signal.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'cellular_info_method_channel.dart';

abstract class CellularInfoPlatform extends PlatformInterface {
  /// Constructs a CellularInfoPlatform.
  CellularInfoPlatform() : super(token: _token);

  static final Object _token = Object();

  static CellularInfoPlatform _instance = MethodChannelCellularInfo();

  /// The default instance of [CellularInfoPlatform] to use.
  ///
  /// Defaults to [MethodChannelCellularInfo].
  static CellularInfoPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [CellularInfoPlatform] when
  /// they register themselves.
  static set instance(CellularInfoPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<List<SignalNr>> getCellInfo() {
    throw UnimplementedError('getCellInfo() has not been implemented.');
  }

  Future<List<SignalNr>> getNrCellInfo() {
    throw UnimplementedError('getNrCellInfo() has not been implemented.');
  }

  Stream<List<SignalNr>> get allCellInfoStream {
    throw UnimplementedError('nrOrLteSignalStream() has not been implemented.');
  }

  Stream<List<SignalNr>> get nrSignalStream {
    throw UnimplementedError('nrSignalStream() has not been implemented.');
  }

  Stream<List<SignalNr>> get nrSignalStreamFromService {
    throw UnimplementedError('nrSignalStreamFromService() has not been implemented.');
  }

  Future<void> startService() async {
    throw UnimplementedError('startService() has not been implemented.');
  }

  Future<void> stopService() async {
    throw UnimplementedError('stopService() has not been implemented.');
  }
}
