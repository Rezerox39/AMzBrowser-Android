package com.amz.browser.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.PI

@Immutable
data class TiltState(val x: Float = 0f, val y: Float = 0f)

val LocalTiltState = staticCompositionLocalOf { TiltState() }

@Composable
fun ProvideTiltSensor(
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var tilt by remember { mutableStateOf(TiltState()) }

    DisposableEffect(enabled, lifecycleOwner) {
        if (!enabled) {
            tilt = TiltState(0f, 0f)
            return@DisposableEffect onDispose {}
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var filteredX = 0f
        var filteredY = 0f
        val alpha = 0.14f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rawX: Float
                val rawY: Float
                if (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    rawX = (orientation[2] / (PI.toFloat() / 6f)).coerceIn(-1f, 1f)
                    rawY = ((orientation[1] + 0.65f) / (PI.toFloat() / 6f)).coerceIn(-1f, 1f)
                } else {
                    rawX = (-event.values[0] / 5f).coerceIn(-1f, 1f)
                    rawY = ((event.values[1] - 4.5f) / 5f).coerceIn(-1f, 1f)
                }
                filteredX += alpha * (rawX - filteredX)
                filteredY += alpha * (rawY - filteredY)
                tilt = TiltState(filteredX, filteredY)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && sensor != null) {
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                sensorManager?.unregisterListener(listener)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager?.unregisterListener(listener)
        }
    }

    CompositionLocalProvider(LocalTiltState provides tilt, content = content)
}

@Composable
fun TiltCard(
    modifier: Modifier = Modifier,
    gyroEnabled: Boolean = true,
    maxDegrees: Float = 7f,
    content: @Composable BoxScope.(parallaxX: Float, parallaxY: Float) -> Unit
) {
    val sensorTilt = LocalTiltState.current
    var touchTilt by remember { mutableStateOf(TiltState()) }
    val activeTilt = if (gyroEnabled) sensorTilt else touchTilt

    Box(
        modifier = modifier
            .pointerInput(gyroEnabled) {
                if (!gyroEnabled) {
                    detectDragGestures(
                        onDragEnd = { touchTilt = TiltState() },
                        onDragCancel = { touchTilt = TiltState() },
                        onDrag = { change, _ ->
                            val nx = ((change.position.x / size.width.coerceAtLeast(1)) - 0.5f) * 2f
                            val ny = ((change.position.y / size.height.coerceAtLeast(1)) - 0.5f) * 2f
                            touchTilt = TiltState(nx.coerceIn(-1f, 1f), ny.coerceIn(-1f, 1f))
                        }
                    )
                }
            }
            .graphicsLayer {
                rotationY = activeTilt.x * maxDegrees
                rotationX = -activeTilt.y * maxDegrees
                cameraDistance = 12f * density
            }
    ) {
        content(activeTilt.x, activeTilt.y)
    }
}
