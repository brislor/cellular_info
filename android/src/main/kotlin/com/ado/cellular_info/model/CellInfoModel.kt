package com.ado.cellular_info.model

data class CellInfoModel(
//    val band: String,
    val arfcn: Int,
//    val freq: Double,
    val ssRsrq: Int,
    val ssRsrp: Int,
    val ssSnr: Int,
    val csiRsrq: Int,
    val csiRsrp: Int,
    val csiSinr: Int,
    val dbm: Int
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
//            "band" to band,
            "arfcn" to arfcn,
//            "freq" to freq,
            "ssRsrq" to ssRsrq,
            "ssRsrp" to ssRsrp,
            "ssSinr" to ssSnr,
            "csiRsrq" to csiRsrq,
            "csiRsrp" to csiRsrp,
            "csiSinr" to csiSinr,
            "dbm" to dbm,
        )
    }
}


//@Serializable
//data class NrBand(
//    val band: String,
//    // 下行频率范围，以 MHz 表示，[min, max]，若未定义则为 null
//    val dlRange: List<Double>? = null,
//    // 上行频率范围，以 MHz 表示，[min, max]，对于 TDD 频段可为 null
//    val ulRange: List<Double>? = null,
//    val duplex: String? = null
//)
//
//@Serializable
//data class NrBandMapping(
//    val FR1: List<NrBand>,
//    val FR2: List<NrBand>
//)