package com.ado.cellular_info.model

enum class CellInfoType{
    Nr,Lte;
}
data class CellInfoModel(
//    val band: String,
    val type: CellInfoType,
    val isRegistered: Boolean,
    val arfcn: Int,
//    val freq: Double,
    val pci: Int,
    val ci: Long,
    val tac: Int,
    val ssRsrq: Int,
    val ssRsrp: Int,
    val ssSnr: Int,
    val csiRsrq: Int = 0,
    val csiRsrp: Int = 0,
    val csiSinr: Int = 0,
    val dbm: Int
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "type" to type.ordinal,
            "isRegistered" to isRegistered,
//            "band" to band,
            "arfcn" to arfcn,
//            "freq" to freq,
            "pci" to pci,
            "ci" to ci,
            "tac" to tac,
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