import 'package:cellular_info/model/nr_signal.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:cellular_info/cellular_info.dart';
import 'package:cellular_info/cellular_info_platform_interface.dart';
import 'package:cellular_info/cellular_info_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockCellularInfoPlatform
    with MockPlatformInterfaceMixin
    implements CellularInfoPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Stream<SignalNr> get nrSignalStream =>
      Stream.value(SignalNr(ssRsrp: -110, ssRsrq: -11, ssSinr: -22));

  @override
  Future<void> startService()=>Future.value("start");

  @override
  Future<void> stopService()=>Future.value("stop");

  @override
  Future<List?> getNrCellInfo()=>Future.value([1,2,3,4]);
}

void main() {
  final CellularInfoPlatform initialPlatform = CellularInfoPlatform.instance;

  test('$MethodChannelCellularInfo is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelCellularInfo>());
  });

  test('getNrCellInfo', () async {
    CellularInfo cellularInfoPlugin = CellularInfo();
    MockCellularInfoPlatform fakePlatform = MockCellularInfoPlatform();
    CellularInfoPlatform.instance = fakePlatform;
    expect(await cellularInfoPlugin.getNrCellInfo(), [1,2,3,4]);
  });
}
