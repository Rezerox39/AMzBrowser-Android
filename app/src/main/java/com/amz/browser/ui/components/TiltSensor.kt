package com.amz.browser.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val sensor = rotationSensor ?: accelerometer

        val mainHandler = Handler(Looper.getMainLooper())
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var filteredX = 0f
        var filteredY = 0f
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
                val next = TiltState(filteredX, filteredY)
                mainHandler.post { tilt = next }
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
        if (sensor != null) {
            sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager?.unregisterListener(listener)
            mainHandler.removeCallbacksAndMessages(null)
        }
    }

    CompositionLocalProvider(LocalTiltState provides tilt, content = content)
}

@Composable
fun TiltCard(
    modifier: Modifier = Modifier,
    gyroEnabled: Boolean = true,
    maxDegrees: Float = 9f,
    content: @Composable BoxScope.(parallaxX: Float, parallaxY: Float) -> Unit
) {
    val sensorTilt = LocalTiltState.current
    var touchTilt by remember { mutableStateOf(TiltState()) }
    val activeTilt = if (gyroEnabled) sensorTilt else touchTilt

    val rotationX by animateFloatAsState(
        targetValue = -activeTilt.y * maxDegrees,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tiltX"
    )
    val rotationY by animateFloatAsState(
        targetValue = activeTilt.x * maxDegrees,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tiltY"
    )

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
                this.rotationX = rotationX
                this.rotationY = rotationY
                cameraDistance = 14f * density
                translationX = activeTilt.x * 4f
                translationY = activeTilt.y * 4f
            }
    ) {
        content(activeTilt.x, activeTilt.y)
    }
}
