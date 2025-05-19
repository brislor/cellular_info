class SignalNr {
  String? band;
  double? freq;
  int? arfcn;
  int? csiRsrp;
  int? csiRsrq;
  int? csiSinr;
  int? ssRsrp;
  int? ssRsrq;
  int? ssSinr;
  int? dbm;

  SignalNr({
    this.band,
    this.freq,
    this.arfcn,
    this.csiRsrp,
    this.csiRsrq,
    this.csiSinr,
    this.ssRsrp,
    this.ssRsrq,
    this.ssSinr,
    this.dbm,
  });

  SignalNr.fromJson(Map<String, dynamic> json) {
    band = json["band"];
    freq = json["freq"];
    arfcn = json["arfcn"];
    csiRsrp = json['csiRsrp'];
    csiRsrq = json['csiRsrq'];
    csiSinr = json['csiSinr'];
    ssRsrp = json['ssRsrp'];
    ssRsrq = json['ssRsrq'];
    ssSinr = json['ssSinr'];
    dbm = json['dbm'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> map = {};
    map["band"] = band;
    map['freq'] = freq;
    map['arfcn'] = arfcn;
    map['csiRsrp'] = csiRsrp;
    map['csiRsrq'] = csiRsrq;
    map['csiSinr'] = csiSinr;
    map['ssRsrp'] = ssRsrp;
    map['ssRsrq'] = ssRsrq;
    map['ssSinr'] = ssSinr;
    map['dbm'] = dbm;
    return map;
  }

  @override
  String toString() {
    return toJson().toString();
  }
}
