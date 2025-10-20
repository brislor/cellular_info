package com.ado.cellular_info.service;

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.ado.cellular_info.CellularEventSink
import com.ado.cellular_info.model.CellInfoModel
import com.ado.cellular_info.model.CellInfoType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class CellInfoService : Service() {
    private val TAG = "CellInfoService"
    private val googleUrl = "https://www.google.com"
    private val CHANNEL_ID = "cell_info_channel"
    private val NOTIFICATION_ID = 1751
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var context: Context
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var cellInfoCallback: TelephonyManager.CellInfoCallback

    private val handlerTask = object : Runnable {
        override fun run() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                telephonyManager.requestCellInfoUpdate(
                    mainExecutor, cellInfoCallback
                )
                CoroutineScope(Dispatchers.IO).launch {
                    val result = httpTest(googleUrl)
                    Log.d(TAG, "Http send result: $result")
                }
                handler.postDelayed(this, 2000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        initCallback()
    }

    private fun initCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cellInfoCallback = object : TelephonyManager.CellInfoCallback() {
                override fun onCellInfo(cellInfo: List<CellInfo>) {
                    // 处理最新基站数据（例如存储或发送到服务器）
                    Log.d(TAG, "CellInfoCallback onCellInfo(): ${cellInfo.size} length cell info")
                    val dataList = parseNrData(cellInfo)
                    CellularEventSink.sendData(dataList)
                }

                override fun onError(errorCode: Int, detail: Throwable?) {
                    super.onError(errorCode, detail)
                    Log.e(TAG, "CellInfoCallback onError(): $errorCode")
                    val errorMessage = if (detail == null)"CellInfoCallback unknown error" else detail.message?:""
                    CellularEventSink.sendError(errorCode = errorCode.toString(), message = errorMessage)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseNrData(cellInfo: List<CellInfo>): List<Map<String,Any>> {
        val nrList = cellInfo.filterIsInstance<CellInfoNr>()
        if (nrList.isNotEmpty()) {
//            val mapList = nrList.map {
//                val strengthNr = it.cellSignalStrength as CellSignalStrengthNr
//                val cellIdentityNr = it.cellIdentity as CellIdentityNr
//
//                val arfcn = cellIdentityNr.nrarfcn
//                val rsrp = strengthNr.ssRsrp
//                val rsrq = strengthNr.ssRsrq
//                val sinr = strengthNr.ssSinr
//                val csiRsrp = strengthNr.csiRsrp
//                val csiRsrq = strengthNr.csiRsrq
//                val csiSnr = strengthNr.csiSinr
//                val dbm = strengthNr.dbm
//                CellInfoModel(
////                                band = convert2Band(
////                                    NrTools.getNrBandForArfcn(
////                                        arfcn,
////                                        context = context
////                                    )
////                                ),
//                    arfcn = arfcn,
////                                freq = NrTools.nrArfcnToFrequency(arfcn),
//                    rsrq,
//                    rsrp,
//                    sinr,
//                    csiRsrp = csiRsrp,
//                    csiRsrq = csiRsrq,
//                    csiSinr = csiSnr,
//                    dbm = dbm
//                ).toMap()
//            }
            val mapList = getCellInfoMap(nrList)
            return mapList
        }else{
            return listOf()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getCellInfoMap(cellInfoList: List<CellInfo>):List<Map<String,Any>>{
        val mapList = cellInfoList.map {
            when(it){
                is CellInfoNr -> {
                    val strengthNr = it.cellSignalStrength as CellSignalStrengthNr
                    val cellIdentityNr = it.cellIdentity as CellIdentityNr

                    val arfcn = cellIdentityNr.nrarfcn
                    val pci = cellIdentityNr.pci
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
                        rsrq,
                        rsrp,
                        sinr,
                        csiRsrp = csiRsrp,
                        csiRsrq = csiRsrq,
                        csiSinr = csiSnr,
                        dbm = dbm
                    ).toMap()
                }
                is CellInfoLte ->{
                    val strengthNr = it.cellSignalStrength
                    val cellIdentityNr = it.cellIdentity

                    val arfcn = cellIdentityNr.earfcn
                    val pci = cellIdentityNr.pci
                    val rsrp = strengthNr.rsrp
                    val rsrq = strengthNr.rsrq
                    val sinr = strengthNr.rssnr
                    val dbm = strengthNr.dbm
                    CellInfoModel(
                        type = CellInfoType.Lte,
                        isRegistered = it.isRegistered,
                        arfcn = arfcn,
                        pci,
                        rsrq,
                        rsrp,
                        sinr,
                        dbm = dbm,
                    ).toMap()
                }
                else->{
                    // Impossible: 3g or 2g.
                    CellInfoModel(type = CellInfoType.Nr, isRegistered = false,arfcn = 0, pci = 0, ssRsrp = 0, ssRsrq = 0, ssSnr = 0, dbm = 0).toMap()
                }
            }
        }
        return mapList
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "cell info Monitor", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "cell info listen channel" }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 创建常驻通知
        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Searching Cellular Tower")
                .setContentText("Cell info monitor running")
                .setSmallIcon(androidx.core.R.drawable.ic_call_answer_low)
                .setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "cell info Monitor", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "cell info listen channel" }
            notificationManager.createNotificationChannel(channel)
        }

        // 启动前台服务（Android 9+ 必须指定 foregroundServiceType）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 开始周期性获取 CellInfo（示例每5分钟请求一次）
        startPeriodicCellInfoUpdate()
        return START_STICKY
    }

    private fun httpTest(url: String): String? {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
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

    private fun startPeriodicCellInfoUpdate() {
        handler.post(handlerTask)
    }

//    fun convert2Band(list: List<NrBand>): String {
//        if (list.isEmpty()) return ""
//        val bandList = list.map {
//            it.band.removePrefix("n")
//        }
//        return bandList.joinToString(separator = "/")
//    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        handler.removeCallbacks(handlerTask)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}