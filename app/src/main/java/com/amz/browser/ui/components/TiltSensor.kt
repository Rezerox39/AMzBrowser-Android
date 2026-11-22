package com.amz.browser.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

/**
 * Stable holder for high-frequency tilt values. Values are read only inside draw /
 * graphics-layer blocks, so sensor updates never trigger recomposition.
 */
class TiltController {
    var x by mutableFloatStateOf(0f)
    var y by mutableFloatStateOf(0f)

    fun update(nx: Float, ny: Float) {
        x = nx
        y = ny
    }
}

val LocalTiltController = staticCompositionLocalOf { TiltController() }

@Composable
fun ProvideTiltSensor(
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { TiltController() }

    DisposableEffect(enabled, lifecycleOwner) {
        if (!enabled) {
            controller.update(0f, 0f)
            return@DisposableEffect onDispose {}
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val sensor = rotationSensor ?: accelerometer

        val mainHandler = Handler(Looper.getMainLooper())
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var filteredX = 0f
        var filteredY = 0f
        var lastSentX = 0f
        var lastSentY = 0f
        var lastPostTime = 0L
        val alpha = 0.10f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rawX: Float
                val rawY: Float
                if (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                    if (event.values.size < 3) return
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    rawX = (orientation[2] / (PI.toFloat() / 5f)).coerceIn(-1f, 1f)
                    rawY = (orientation[1] / (PI.toFloat() / 5f)).coerceIn(-1f, 1f)
                } else {
                    if (event.values.size < 3) return
                    rawX = (-event.values[0] / 6f).coerceIn(-1f, 1f)
                    rawY = (event.values[1] / 6f).coerceIn(-1f, 1f)
                }
                filteredX += alpha * (rawX - filteredX)
                filteredY += alpha * (rawY - filteredY)
                val now = System.currentTimeMillis()
                val changed = kotlin.math.abs(filteredX - lastSentX) > 0.012f ||
                    kotlin.math.abs(filteredY - lastSentY) > 0.012f
                if (now - lastPostTime >= 32L && changed) {
                    lastPostTime = now
                    lastSentX = filteredX
                    lastSentY = filteredY
                    mainHandler.post { controller.update(filteredX, filteredY) }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (sensor != null) {
                    sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                }
                Lifecycle.Event.ON_PAUSE -> sensorManager?.unregisterListener(listener)
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        // Do not register here as well: the ON_RESUME observer already does it,
        // and registering twice delivers every sensor event twice.

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager?.unregisterListener(listener)
            mainHandler.removeCallbacksAndMessages(null)
        }
    }

    CompositionLocalProvider(LocalTiltController provides controller, content = content)
}

@Composable
fun TiltCard(
    modifier: Modifier = Modifier,
    gyroEnabled: Boolean = true,
    maxDegrees: Float = 9f,
    content: @Composable BoxScope.() -> Unit
) {
    val controller = LocalTiltController.current
    var touchX by remember { mutableFloatStateOf(0f) }
    var touchY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(gyroEnabled) {
                if (!gyroEnabled) {
                    detectDragGestures(
                        onDragEnd = {
                            touchX = 0f
                            touchY = 0f
                        },
                        onDragCancel = {
                            touchX = 0f
                            touchY = 0f
                        },
                        onDrag = { change, _ ->
                            touchX = (((change.position.x / size.width.coerceAtLeast(1)) - 0.5f) * 2f).coerceIn(-1f, 1f)
                            touchY = (((change.position.y / size.height.coerceAtLeast(1)) - 0.5f) * 2f).coerceIn(-1f, 1f)
                        }
                    )
                }
            }
            .graphicsLayer {
                val tx = if (gyroEnabled) controller.x else touchX
                val ty = if (gyroEnabled) controller.y else touchY
                rotationX = -ty * maxDegrees
                rotationY = tx * maxDegrees
                cameraDistance = 14f * density
                translationX = tx * 4f
                translationY = ty * 4f
            }
    ) {
        content()
    }
}
