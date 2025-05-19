package com.ado.cellular_info

import io.flutter.plugin.common.EventChannel.EventSink

object CellularEventSink {
    private var eventSink: EventSink? = null

    fun setEventSink(eventSink: EventSink?) {
        this.eventSink = eventSink
    }

    fun sendData(data: List<Map<String, Any>>) {
        eventSink?.success(data)
    }

    fun sendError(errorCode: String, message: String) {
        eventSink?.error(errorCode, message, null)
    }
}