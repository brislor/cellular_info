package com.ado.cellular_info


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.ado.cellular_info.model.CellInfoModel
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
    private val timeInternal = 2000
    private lateinit var context: Context
    private lateinit var methodChannel: MethodChannel
    private lateinit var normalEventChannel: EventChannel
    private lateinit var serviceEventChannel: EventChannel
    private var normalEventSink: EventChannel.EventSink? = null
    private var timer:Timer? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.i(tag, "Init Plugin")
        context = flutterPluginBinding.applicationContext
        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "cellular_info")
        methodChannel.setMethodCallHandler(this)

        normalEventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "cellular_info_event")
        normalEventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                Log.i(tag, "event stream onListen: ")
                normalEventSink = events
                startTimer()
            }

            override fun onCancel(arguments: Any?) {
                Log.i(tag, "event stream onCancel: ")
                stopTimer()
                normalEventSink = null
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
            "getNrCellInfo" -> getNrCellInfo(result)
            "startService" -> startBackgroundService(result)
            "stopService" -> stopBackgroundService(result)
            "getPlatformVersion" -> result.success("Android ${Build.VERSION.RELEASE}")
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        normalEventChannel.setStreamHandler(null)
        serviceEventChannel.setStreamHandler(null)
    }

    private fun startTimer(){
        timer?.cancel()
        timer = Timer()
        val handler = Handler(Looper.getMainLooper())
        timer?.schedule(object : TimerTask(){
            override fun run() {
                handler.post {
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = httpTest(testUrl)
                        Log.d(tag, "Http send result: $result")
                    }
                    getNrCellInfo()
                }
            }
        },0,timeInternal.toLong())
    }

    private fun stopTimer(){
        Log.d(tag, "stopTimer")
        timer?.cancel()
        timer = null
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

    private fun getNrCellInfo(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            normalEventSink?.error("SYSTEM UNSUPPORTED","5G NR requires Android 11+",null)
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
            normalEventSink?.error("Missing permissions", "location permission not allowed.", null)
            return
        }
        if (phoneStatePermission != PackageManager.PERMISSION_GRANTED) {
            normalEventSink?.error("Missing permissions", "READ_PHONE_STATE permission not allowed.", null)
            return
        }
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.requestCellInfoUpdate(
            context.mainExecutor,
            @RequiresApi(Build.VERSION_CODES.Q)
            object : TelephonyManager.CellInfoCallback(){
                override fun onCellInfo(cellInfoList: List<CellInfo>) {
                    val nrList = cellInfoList.filterIsInstance<CellInfoNr>()
                    if (nrList.isNotEmpty()) {
                        val mapList = nrList.map {
                            val strengthNr = it.cellSignalStrength as CellSignalStrengthNr
                            val cellIdentityNr = it.cellIdentity as CellIdentityNr

                            val arfcn = cellIdentityNr.nrarfcn
                            val rsrp = strengthNr.ssRsrp
                            val rsrq = strengthNr.ssRsrq
                            val sinr = strengthNr.ssSinr
                            val csiRsrp = strengthNr.csiRsrp
                            val csiRsrq = strengthNr.csiRsrq
                            val csiSnr = strengthNr.csiSinr
                            val dbm = strengthNr.dbm
                            CellInfoModel(
                                arfcn = arfcn,
                                rsrq,
                                rsrp,
                                sinr,
                                csiRsrp = csiRsrp,
                                csiRsrq = csiRsrq,
                                csiSinr = csiSnr,
                                dbm = dbm
                            ).toMap()
                        }
                        normalEventSink?.success(mapList)
                        return
                    }
                    normalEventSink?.error("NOT_FOUND", "No 5G NR cell info available", null)
                }


                override fun onError(errorCode: Int, detail: Throwable?) {
                    normalEventSink?.error("GEt_CELL_INFO_ ERROR", detail?.message,null)
                }
            }
        )
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
        val nrList = cellInfoList.filterIsInstance<CellInfoNr>()
        if (nrList.isNotEmpty()) {
            val mapList = nrList.map {
                val strengthNr = it.cellSignalStrength as CellSignalStrengthNr
                val cellIdentityNr = it.cellIdentity as CellIdentityNr

                val arfcn = cellIdentityNr.nrarfcn
                val rsrp = strengthNr.ssRsrp
                val rsrq = strengthNr.ssRsrq
                val snr = strengthNr.ssSinr
                val csiRsrp = strengthNr.csiRsrp
                val csiRsrq = strengthNr.csiRsrq
                val csiSnr = strengthNr.csiSinr
                val dbm = strengthNr.dbm
                CellInfoModel(
//                                band = convert2Band(
//                                    NrTools.getNrBandForArfcn(
//                                        arfcn,
//                                        context = context
//                                    )
//                                ),
                    arfcn = arfcn,
//                                freq = NrTools.nrArfcnToFrequency(arfcn),
                    rsrq,
                    rsrp,
                    snr,
                    csiRsrp = csiRsrp,
                    csiRsrq = csiRsrq,
                    csiSinr = csiSnr,
                    dbm = dbm
                ).toMap()
            }
            result.success(mapList)
            return
        }
        result.error("NOT_FOUND", "No 5G NR cell info available", null)
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
