package com.amz.browser.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.abs

private const val DEADZONE_DEGREES = 1.5f
private const val TILT_SCALE = 0.35f
private const val MAX_DEGREES = 8f
private const val FILTER_ALPHA = 0.15f
private const val REFERENCE_DRIFT = 0.01f

private val TiltSpring: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 120f)

/**
 * Holds the two animated tilt angles in DEGREES. Sensor events only emit targets;
 * the values themselves live in Animatable, which animations update. No ordinary
 * Compose State is written from the sensor callback, and consumers read [x]/[y]
 * exclusively inside draw/layout-phase lambdas, so tilting never recomposes the UI.
 */
class TiltController {
    val x = Animatable(0f)
    val y = Animatable(0f)

    /** Last raw target, exposed for the hidden debug overlay only. */
    @Volatile
    var rawX = 0f
        private set

    @Volatile
    var rawY = 0f
        private set

    /** False when no usable sensor exists; TiltCard then uses touch drag. */
    @Volatile
    var sensorAvailable = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val targets = MutableSharedFlow<Pair<Float, Float>>(replay = 1, extraBufferCapacity = 1)

    init {
        scope.launch {
            targets.collectLatest { (tx, ty) ->
                launch { x.animateTo(tx, TiltSpring) }
                launch { y.animateTo(ty, TiltSpring) }
            }
        }
    }

    fun update(tx: Float, ty: Float) {
        rawX = tx
        rawY = ty
        targets.tryEmit(tx to ty)
    }

    fun reset() = update(0f, 0f)

    fun close() {
        scope.cancel()
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

    DisposableEffect(Unit) {
        onDispose { controller.close() }
    }

    DisposableEffect(enabled, context, lifecycleOwner) {
        if (!enabled) {
            controller.sensorAvailable = false
            controller.reset()
            return@DisposableEffect onDispose {}
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationVector = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val useRotationVector = rotationVector != null
        val fallbackAvailable = accelerometer != null && magnetometer != null
        controller.sensorAvailable = useRotationVector || fallbackAvailable

        if (!controller.sensorAvailable) {
            return@DisposableEffect onDispose {}
        }

        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        }
        val axisX: Int
        val axisY: Int
        when (display?.rotation ?: Surface.ROTATION_0) {
            Surface.ROTATION_90 -> {
                axisX = SensorManager.AXIS_Y
                axisY = SensorManager.AXIS_MINUS_X
            }
            Surface.ROTATION_180 -> {
                axisX = SensorManager.AXIS_MINUS_X
                axisY = SensorManager.AXIS_MINUS_Y
            }
            Surface.ROTATION_270 -> {
                axisX = SensorManager.AXIS_MINUS_Y
                axisY = SensorManager.AXIS_X
            }
            else -> {
                axisX = SensorManager.AXIS_X
                axisY = SensorManager.AXIS_Y
            }
        }

        val matrix = FloatArray(9)
        val remapped = FloatArray(9)
        val orientation = FloatArray(3)
        val accel = FloatArray(3)
        val magnetic = FloatArray(3)
        var accelReady = false
        var magneticReady = false

        var referenceX = 0f
        var referenceY = 0f
        var referenceReady = false
        var filteredX = 0f
        var filteredY = 0f
        var filterReady = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val ok = if (useRotationVector) {
                    if (event.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR || event.values.size < 3) return
                    SensorManager.getRotationMatrixFromVector(matrix, event.values)
                    true
                } else {
                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            System.arraycopy(event.values, 0, accel, 0, 3)
                            accelReady = true
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            System.arraycopy(event.values, 0, magnetic, 0, 3)
                            magneticReady = true
                        }
                        else -> return
                    }
                    accelReady && magneticReady &&
                        SensorManager.getRotationMatrix(matrix, null, accel, magnetic)
                }
                if (!ok) return

                if (!SensorManager.remapCoordinateSystem(matrix, axisX, axisY, remapped)) return
                SensorManager.getOrientation(remapped, orientation)

                // orientation[1] = pitch (X), orientation[2] = roll (Y), in radians.
                val currentX = orientation[1]
                val currentY = orientation[2]
                if (currentX.isNaN() || currentX.isInfinite() ||
                    currentY.isNaN() || currentY.isInfinite()
                ) return

                if (!referenceReady) {
                    referenceX = currentX
                    referenceY = currentY
                    referenceReady = true
                    return
                }
                // Reference slowly follows the current pose (~3 s time constant at
                // game-rate sampling) so a held phone always returns to neutral.
                referenceX += (currentX - referenceX) * REFERENCE_DRIFT
                referenceY += (currentY - referenceY) * REFERENCE_DRIFT

                fun shape(deltaRadians: Float): Float {
                    var degrees = Math.toDegrees((deltaRadians - 0.0).toDouble()).toFloat()
                    if (abs(degrees) < DEADZONE_DEGREES) degrees = 0f
                    return (degrees * TILT_SCALE).coerceIn(-MAX_DEGREES, MAX_DEGREES)
                }

                val targetX = shape(currentX - referenceX)
                val targetY = shape(currentY - referenceY)
                if (!filterReady) {
                    filteredX = targetX
                    filteredY = targetY
                    filterReady = true
                } else {
                    filteredX += FILTER_ALPHA * (targetX - filteredX)
                    filteredY += FILTER_ALPHA * (targetY - filteredY)
                }
                controller.update(filteredX, filteredY)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        var registered = false
        fun register() {
            if (registered) return
            if (useRotationVector) {
                sensorManager?.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_GAME)
            } else {
                sensorManager?.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
                sensorManager?.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_GAME)
            }
            registered = true
        }

        fun unregister() {
            if (!registered) return
            sensorManager?.unregisterListener(listener)
            registered = false
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> register()
                Lifecycle.Event.ON_PAUSE -> unregister()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) register()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unregister()
            controller.sensorAvailable = false
            controller.reset()
        }
    }

    CompositionLocalProvider(LocalTiltController provides controller, content = content)
}

/** Read in a graphicsLayer block: translates a child opposite to the tilt. */
@Composable
fun tiltParallax(depth: Dp = 4.dp): Modifier {
    val tilt = LocalTiltController.current
    return Modifier.graphicsLayer {
        val nx = (tilt.x.value / MAX_DEGREES).coerceIn(-1f, 1f)
        val ny = (tilt.y.value / MAX_DEGREES).coerceIn(-1f, 1f)
        translationX = -nx * depth.toPx()
        translationY = -ny * depth.toPx()
    }
}

@Composable
fun TiltCard(
    modifier: Modifier = Modifier,
    gyroEnabled: Boolean = true,
    maxDegrees: Float = MAX_DEGREES,
    content: @Composable BoxScope.() -> Unit
) {
    val tilt = LocalTiltController.current
    var touchX by remember { mutableFloatStateOf(0f) }
    var touchY by remember { mutableFloatStateOf(0f) }
    val useSensor = gyroEnabled && tilt.sensorAvailable

    Box(
        modifier = modifier
            .pointerInput(useSensor) {
                if (useSensor) return@pointerInput
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
            .graphicsLayer {
                val rx = if (useSensor) tilt.x.value else touchX * maxDegrees
                val ry = if (useSensor) tilt.y.value else touchY * maxDegrees
                rotationX = -ry
                rotationY = rx
                cameraDistance = 16f * density
            }
    ) {
        content()
    }
}

/** Long-press any title for 3 seconds with this modifier to reveal the debug HUD. */
fun Modifier.tiltDebugLongPress(onActivated: () -> Unit): Modifier = pointerInput(onActivated) {
    awaitEachGesture {
        awaitFirstDown()
        val up = withTimeoutOrNull(3000L) { waitForUpOrCancellation() }
        if (up == null) onActivated()
    }
}

@Composable
fun TiltDebugOverlay(tilt: TiltController) {
    val text by produceState(initialValue = "tilt x 0.00  y 0.00", tilt) {
        while (true) {
            value = "tilt x %.2f°  y %.2f°".format(tilt.x.value, tilt.y.value)
            delay(100)
        }
    }
    Surface(
        color = androidx.compose.ui.graphics.Color(0xCC000000),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
