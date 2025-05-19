class NrBandMapping {
  final List<NrBand> fr1;
  final List<NrBand> fr2;

  NrBandMapping({required this.fr1, required this.fr2});

  factory NrBandMapping.fromJson(Map<String, dynamic> json) {
    return NrBandMapping(
      fr1: List<NrBand>.from(json['fr1'].map((x) => NrBand.fromJson(x))),
      fr2: List<NrBand>.from(json['fr2'].map((x) => NrBand.fromJson(x))),
    );
  }
}

class NrBand {
  final String band;
  final List<double>? ulRange;
  final List<double>? dlRange;
  final String? duplex;

  NrBand({required this.band, this.ulRange, this.dlRange, this.duplex});

  factory NrBand.fromJson(Map<String, dynamic> json) {
    return NrBand(
      band: json['band'],
      dlRange: json['dlRange'] != null
          ? List<double>.from(json['dlRange'].map((x) => x.toDouble()))
          : null,
    );
  }
}
