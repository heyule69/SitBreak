package com.sitbreak.service

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * 智能暂停：靠计步器判断用户是否已经离座走动。
 *
 * 人离座去开会时计时器还在傻算，回来立刻收到"你已久坐两小时"，
 * 统计数据也随之失真。这里用 TYPE_STEP_COUNTER 做低功耗判定：
 * 该传感器由硬件持续累计，不需要保持 CPU 唤醒。
 */
class SmartPauseDetector(
    private val context: Context,
    private val onWalkDetected: () -> Unit,
) : SensorEventListener {

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val stepSensor: Sensor?
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    /** 传感器累计值的基准，负数表示尚未取到第一帧 */
    private var baseline = -1f
    private var running = false

    /** 设备是否具备计步器 */
    fun isSupported(): Boolean = stepSensor != null

    /** 是否已授予活动识别权限（API 29 起需要） */
    fun hasPermission(): Boolean = hasPermission(context)

    fun isAvailable(): Boolean = isSupported() && hasPermission()

    /** @return 是否真正开始监听 */
    fun start(): Boolean {
        if (running) return true
        val sensor = stepSensor ?: run {
            Log.d(TAG, "no step counter on this device")
            return false
        }
        if (!hasPermission()) {
            Log.d(TAG, "ACTIVITY_RECOGNITION not granted")
            return false
        }
        baseline = -1f
        running = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        Log.d(TAG, "smart pause started=$running")
        return running
    }

    fun stop() {
        if (!running) return
        sensorManager.unregisterListener(this)
        running = false
    }

    /** 新一轮久坐开始时重新取基准，避免把上一轮的步数算进来 */
    fun resetBaseline() {
        baseline = -1f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val total = event.values.firstOrNull() ?: return
        if (baseline < 0f) {
            baseline = total
            return
        }
        // 传感器在设备重启后会归零，此时重新取基准
        if (total < baseline) {
            baseline = total
            return
        }
        if (total - baseline >= STEP_THRESHOLD) {
            Log.d(TAG, "walk detected: ${total - baseline} steps")
            baseline = total
            onWalkDetected()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val TAG = "SitBreakSmartPause"

        /** 连续走够这么多步才算真的离座，避免坐着晃动被误判 */
        const val STEP_THRESHOLD = 40

        /** 供设置界面在不启动监听的前提下判断可用性 */
        fun isSupported(context: Context): Boolean =
            (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
                .getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

        fun hasPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACTIVITY_RECOGNITION,
            ) == PackageManager.PERMISSION_GRANTED
    }
}
