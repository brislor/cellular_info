class SignalNr {
  /// 0: 5g-NR,1: 4g-LTE.
  int type = 0;
  bool isRegistered = false;
  String? band;
  double? freq;
  int? arfcn;
  int? pci;
  int? ci;
  int? tac;
  int? eNB;
  int? lcid;
  int? csiRsrp;
  int? csiRsrq;
  int? csiSinr;
  int? ssRsrp;
  int? ssRsrq;
  int? ssSinr;
  int? dbm;

  SignalNr({
    this.type = 0,
    this.isRegistered = false,
    this.band,
    this.freq,
    this.arfcn,
    this.pci,
    this.ci,
    this.tac,
    this.eNB,
    this.lcid,
    this.csiRsrp,
    this.csiRsrq,
    this.csiSinr,
    this.ssRsrp,
    this.ssRsrq,
    this.ssSinr,
    this.dbm,
  });

  SignalNr.fromJson(Map<String, dynamic> json) {
    type = json["type"];
    isRegistered = json["isRegistered"];
    band = json["band"];
    freq = json["freq"];
    arfcn = json["arfcn"];
    pci = json["pci"];
    ci = json["ci"];
    tac = json["tac"];

    /// verify: https://www.cellmapper.net/enbid
    /// eNB = ci / 256;
    /// LCID = ci % 256;
    eNB = ci != null ? ci! ~/ 256 : null;
    lcid = ci != null ? ci! % 256 : null;
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
    map["type"] = type;
    map["isRegistered"] = isRegistered;
    map["band"] = band;
    map['freq'] = freq;
    map['arfcn'] = arfcn;
    map["pci"] = pci;
    map["ci"] = ci;
    map["tac"] = tac;
    map['eNB'] = eNB;
    map['lcid'] = lcid;
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
