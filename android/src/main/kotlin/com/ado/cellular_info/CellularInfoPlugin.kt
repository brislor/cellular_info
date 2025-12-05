package com.ado.cellular_info


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.ado.cellular_info.model.CellInfoModel
import com.ado.cellular_info.model.CellInfoType
import com.ado.cellular_info.service.CellInfoService
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.util.Timer
import java.util.TimerTask

/** CellularInfoPlugin */
class CellularInfoPlugin : FlutterPlugin, MethodCallHandler {
    private val tag = "CellularInfoPlugin"
    private val testUrl = "https://www.google.com"

    //    private val testUrl = "https://www.baidu.com"
    private val timeInternal = 1000
    private lateinit var context: Context
    private lateinit var methodChannel: MethodChannel
    private lateinit var nrEventChannel: EventChannel
    private lateinit var allInfoEventChannel: EventChannel
    private lateinit var serviceEventChannel: EventChannel
    private var nrStreamEventSink: EventChannel.EventSink? = null
    private var allCellInfoStreamEventSink: EventChannel.EventSink? = null
    private var nrTimer: Timer? = null
    private var allCellInfoTimer: Timer? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.i(tag, "Init Plugin")
        context = flutterPluginBinding.applicationContext
        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "cellular_info")
        methodChannel.setMethodCallHandler(this)

        allInfoEventChannel =
            EventChannel(flutterPluginBinding.binaryMessenger, "cellular_info_all_cell_stream")
        allInfoEventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                Log.i(tag, "all cell event stream onListen: ")
                allCellInfoStreamEventSink = events
                startAllInfoTimer()
            }

            override fun onCancel(arguments: Any?) {
                Log.i(tag, "all cell event stream onCancel: ")
                stopAllInfoTimer()
                allCellInfoStreamEventSink = null
            }
        })

        nrEventChannel =
            EventChannel(flutterPluginBinding.binaryMessenger, "cellular_info_nr_stream")
        nrEventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                Log.i(tag, "event stream onListen: ")
                nrStreamEventSink = events
                startNrInfoTimer()
            }

            override fun onCancel(arguments: Any?) {
                Log.i(tag, "event stream onCancel: ")
                stopNrTimer()
                nrStreamEventSink = null
            }
        })

        serviceEventChannel =
            EventChannel(flutterPluginBinding.binaryMessenger, "cellular_info_event_service")
        serviceEventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                events?.let { CellularEventSink.setEventSink(it) }
            }

            override fun onCancel(arguments: Any?) {
                CellularEventSink.setEventSink(null)
            }
        })
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getCellInfo" -> getNrCellInfo(result)
            "getNrCellInfo" -> getNrCellInfo(result)
            "startService" -> startBackgroundService(result)
            "stopService" -> stopBackgroundService(result)
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        nrEventChannel.setStreamHandler(null)
        allInfoEventChannel.setStreamHandler(null)
        serviceEventChannel.setStreamHandler(null)
    }

    private fun startAllInfoTimer() {
        allCellInfoTimer?.cancel()
        allCellInfoTimer = Timer()
        val handler = Handler(Looper.getMainLooper())
        allCellInfoTimer?.schedule(object : TimerTask() {
            override fun run() {
                handler.post {
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = httpTest(testUrl)
                        Log.d(tag, "Http send result: $result")
                    }
                    getAllCellInfoStream()
                }
            }
        }, 0, timeInternal.toLong())
    }

    private fun startNrInfoTimer() {
        nrTimer?.cancel()
        nrTimer = Timer()
        val handler = Handler(Looper.getMainLooper())
        nrTimer?.schedule(object : TimerTask() {
            override fun run() {
                handler.post {
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = httpTest(testUrl)
                        Log.d(tag, "Http send result: $result")
                    }
                    getNrCellInfoStream()
                }
            }
        }, 0, timeInternal.toLong())
    }

    private fun stopNrTimer() {
        Log.d(tag, "stopNrTimer")
        nrTimer?.cancel()
        nrTimer = null
    }

    private fun stopAllInfoTimer() {
        Log.d(tag, "stopAllInfoTimer")
        allCellInfoTimer?.cancel()
        allCellInfoTimer = null
    }

    private fun httpTest(url: String): String? {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = timeInternal
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                "Error: ${connection.responseCode}"
            }
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }

    private fun getNrCellInfoStream() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            nrStreamEventSink?.error("SYSTEM UNSUPPORTED", "5G NR requires Android 11+", null)
            return
        }

        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val phoneStatePermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        )
        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED || coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            nrStreamEventSink?.error(
                "Missing permissions",
                "location permission not allowed.",
                null
            )
            return
        }
        if (phoneStatePermission != PackageManager.PERMISSION_GRANTED) {
            nrStreamEventSink?.error(
                "Missing permissions",
                "READ_PHONE_STATE permission not allowed.",
                null
            )
            return
        }
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.requestCellInfoUpdate(
            context.mainExecutor,
            @RequiresApi(Build.VERSION_CODES.Q)
            object : TelephonyManager.CellInfoCallback() {
                override fun onCellInfo(cellInfoList: List<CellInfo>) {
                    val filters = cellInfoList.filter { it is CellInfoNr }
//                    val nrList = cellInfoList.filterIsInstance<CellInfoNr>()
                    if (filters.isEmpty()) {
                        nrStreamEventSink?.error("GEt_NR_CELL_INFO_NOT_FOUND", "No 5G NR cell info available", null)
                        return
                    }
//                    val mapList = filters.map {
//                        when(it){
//                            is CellInfoNr -> {
//                                val strengthNr = it.cellSignalStrength as CellSignalStrengthNr
//                                val cellIdentityNr = it.cellIdentity as CellIdentityNr
//
//                                val arfcn = cellIdentityNr.nrarfcn
//                                val pci = cellIdentityNr.pci
//                                val rsrp = strengthNr.ssRsrp
//                                val rsrq = strengthNr.ssRsrq
//                                val sinr = strengthNr.ssSinr
//                                val csiRsrp = strengthNr.csiRsrp
//                                val csiRsrq = strengthNr.csiRsrq
//                                val csiSnr = strengthNr.csiSinr
//                                val dbm = strengthNr.dbm
//                                CellInfoModel(
//                                    arfcn = arfcn,
//                                    pci,
//                                    rsrq,
//                                    rsrp,
//                                    sinr,
//                                    csiRsrp = csiRsrp,
//                                    csiRsrq = csiRsrq,
//                                    csiSinr = csiSnr,
//                                    dbm = dbm
//                                ).toMap()
//                            }
//                            is CellInfoLte ->{
//                                val strengthNr = it.cellSignalStrength
//                                val cellIdentityNr = it.cellIdentity
//
//                                val arfcn = cellIdentityNr.earfcn
//                                val pci = cellIdentityNr.pci
//                                val rsrp = strengthNr.rsrp
//                                val rsrq = strengthNr.rsrq
//                                val sinr = strengthNr.rssnr
//                                val dbm = strengthNr.dbm
//                                CellInfoModel(
//                                    arfcn = arfcn,
//                                    pci,
//                                    rsrq,
//                                    rsrp,
//                                    sinr,
//                                    dbm = dbm,
//                                ).toMap()
//                            }
//                            else->{
//                                // Impossible: 3g or 2g.
//                                CellInfoModel(arfcn = 0, pci = 0, ssRsrp = 0, ssRsrq = 0, ssSnr = 0, dbm = 0).toMap()
//                            }
//                        }
//                    }
                    val mapList = getCellInfoMap(filters)
                    nrStreamEventSink?.success(mapList)
                }


                override fun onError(errorCode: Int, detail: Throwable?) {
                    nrStreamEventSink?.error(
                        "GEt_NR_CELL_INFO_ERROR",
                        "Code=$errorCode, message=${detail?.message}",
                        null
                    )
                }
            }
        )
    }

    private fun getAllCellInfoStream() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            allCellInfoStreamEventSink?.error("SYSTEM UNSUPPORTED", "5G NR requires Android 11+", null)
            return
        }

        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val phoneStatePermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        )
        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED || coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            allCellInfoStreamEventSink?.error(
                "Missing permissions",
                "location permission not allowed.",
                null
            )
            return
        }
        if (phoneStatePermission != PackageManager.PERMISSION_GRANTED) {
            allCellInfoStreamEventSink?.error(
                "Missing permissions",
                "READ_PHONE_STATE permission not allowed.",
                null
            )
            return
        }
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.requestCellInfoUpdate(
            context.mainExecutor,
            @RequiresApi(Build.VERSION_CODES.Q)
            object : TelephonyManager.CellInfoCallback() {
                override fun onCellInfo(cellInfoList: List<CellInfo>) {
                    val filters = cellInfoList.filter { it is CellInfoLte || it is CellInfoNr }
                    if (filters.isEmpty()) {
                        allCellInfoStreamEventSink?.error(
                            "GEt_ALL_CELL_INFO_NOT_FOUND",
                            "No 5G NR Or LTE cell info available",
                            null
                        )
                        return
                    }
                    val mapList = getCellInfoMap(filters)
                    allCellInfoStreamEventSink?.success(mapList)
                }

                override fun onError(errorCode: Int, detail: Throwable?) {
                    allCellInfoStreamEventSink?.error(
                        "GEt_ALL_CELL_INFO_NOT_ERROR",
                        "Code=$errorCode, message=${detail?.message}",
                        null
                    )
                }
            }
        )
    }


    private fun getCellInfo(result: Result) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            result.error("SYSTEM UNSUPPORTED", "5G NR requires Android 11+", null)
            return
        }

        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val phoneStatePermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        )
        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED || coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            result.error("Missing permissions", "location permission not allowed.", null)
            return
        }
        if (phoneStatePermission != PackageManager.PERMISSION_GRANTED) {
            result.error("Missing permissions", "READ_PHONE_STATE permission not allowed.", null)
            return
        }

        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val cellInfoList = telephonyManager.allCellInfo ?: emptyList()

        val filters = cellInfoList.filter { it is CellInfoLte || it is CellInfoNr }
        if (filters.isEmpty()) {
            result.error("NOT_FOUND", "No 5G NR OR LTE cell info available", null)
            return
        }
        val mapList = getCellInfoMap(filters)
        result.success(mapList)
    }

    private fun getNrCellInfo(result: Result) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            result.error("SYSTEM UNSUPPORTED", "5G NR requires Android 11+", null)
            return
        }

        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val phoneStatePermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        )
        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED || coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            result.error("Missing permissions", "location permission not allowed.", null)
            return
        }
        if (phoneStatePermission != PackageManager.PERMISSION_GRANTED) {
            result.error("Missing permissions", "READ_PHONE_STATE permission not allowed.", null)
            return
        }

        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val cellInfoList = telephonyManager.allCellInfo ?: emptyList()

        val filters = cellInfoList.filter { it is CellInfoNr }
        if (filters.isEmpty()) {
            result.error("NOT_FOUND", "No 5G NR cell info available", null)
            return
        }
//        val mapList = filters.map {
//            when(it){
//                is CellInfoNr -> {
//                    val strengthNr = it.cellSignalStrength as CellSignalStrengthNr
//                    val cellIdentityNr = it.cellIdentity as CellIdentityNr
//
//                    val arfcn = cellIdentityNr.nrarfcn
//                    val pci = cellIdentityNr.pci
//                    val rsrp = strengthNr.ssRsrp
//                    val rsrq = strengthNr.ssRsrq
//                    val sinr = strengthNr.ssSinr
//                    val csiRsrp = strengthNr.csiRsrp
//                    val csiRsrq = strengthNr.csiRsrq
//                    val csiSnr = strengthNr.csiSinr
//                    val dbm = strengthNr.dbm
//                    CellInfoModel(
//                        arfcn = arfcn,
//                        pci,
//                        rsrq,
//                        rsrp,
//                        sinr,
//                        csiRsrp = csiRsrp,
//                        csiRsrq = csiRsrq,
//                        csiSinr = csiSnr,
//                        dbm = dbm
//                    ).toMap()
//                }
//                is CellInfoLte ->{
//                    val strengthNr = it.cellSignalStrength
//                    val cellIdentityNr = it.cellIdentity
//
//                    val arfcn = cellIdentityNr.earfcn
//                    val pci = cellIdentityNr.pci
//                    val rsrp = strengthNr.rsrp
//                    val rsrq = strengthNr.rsrq
//                    val sinr = strengthNr.rssnr
//                    val dbm = strengthNr.dbm
//                    CellInfoModel(
//                        arfcn = arfcn,
//                        pci,
//                        rsrq,
//                        rsrp,
//                        sinr,
//                        dbm = dbm,
//                    ).toMap()
//                }
//                else->{
//                    // Impossible: 3g or 2g.
//                    CellInfoModel(arfcn = 0, pci = 0, ssRsrp = 0, ssRsrq = 0, ssSnr = 0, dbm = 0).toMap()
//                }
//            }
//        }
        val mapList = getCellInfoMap(filters)
        result.success(mapList)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getCellInfoMap(cellInfoList: List<CellInfo>): List<Map<String, Any>> {
        val mapList = cellInfoList.map {
            when (it) {
                is CellInfoNr -> {
                    val strengthNr = it.cellSignalStrength as CellSignalStrengthNr
                    val cellIdentityNr = it.cellIdentity as CellIdentityNr

                    val arfcn = cellIdentityNr.nrarfcn
                    val pci = cellIdentityNr.pci
                    val ci = cellIdentityNr.nci
                    val tac = cellIdentityNr.tac
                    val rsrp = strengthNr.ssRsrp
                    val rsrq = strengthNr.ssRsrq
                    val sinr = strengthNr.ssSinr
                    val csiRsrp = strengthNr.csiRsrp
                    val csiRsrq = strengthNr.csiRsrq
                    val csiSnr = strengthNr.csiSinr
                    val dbm = strengthNr.dbm
                    CellInfoModel(
                        type = CellInfoType.Nr,
                        isRegistered = it.isRegistered,
                        arfcn = arfcn,
                        pci,
                        ci,
                        tac,
                        rsrq,
                        rsrp,
                        sinr,
                        csiRsrp = csiRsrp,
                        csiRsrq = csiRsrq,
                        csiSinr = csiSnr,
                        dbm = dbm
                    ).toMap()
                }

                is CellInfoLte -> {
                    val strengthLte = it.cellSignalStrength as CellSignalStrengthLte
                    val cellIdentityLte = it.cellIdentity as CellIdentityLte

                    val arfcn = cellIdentityLte.earfcn
                    val pci = cellIdentityLte.pci
                    val ci = cellIdentityLte.ci
                    val tac = cellIdentityLte.tac
                    val rsrp = strengthLte.rsrp
                    val rsrq = strengthLte.rsrq
                    val sinr = strengthLte.rssnr
                    val dbm = strengthLte.dbm
                    CellInfoModel(
                        type = CellInfoType.Lte,
                        isRegistered = it.isRegistered,
                        arfcn = arfcn,
                        pci,
                        ci.toLong(),
                        tac,
                        rsrq,
                        rsrp,
                        sinr,
                        dbm = dbm,
                    ).toMap()
                }

                else -> {
                    // Impossible: 3g or 2g.
                    CellInfoModel(
                        type = CellInfoType.Nr,
                        isRegistered = false,
                        arfcn = 0,
                        pci = 0,
                        ci = 0L,
                        tac = 0,
                        ssRsrp = 0,
                        ssRsrq = 0,
                        ssSnr = 0,
                        dbm = 0
                    ).toMap()
                }
            }
        }
        return mapList
    }

    private fun startBackgroundService(result: Result) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            result.error("UNSUPPORTED", "5G NR requires Android 11+", null)
            return
        }

        val serviceIntent = Intent(context, CellInfoService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        result.success(null)
    }

    private fun stopBackgroundService(result: Result) {
        val serviceIntent = Intent(context, CellInfoService::class.java)
        context.stopService(serviceIntent)
        result.success(null)
    }
}
