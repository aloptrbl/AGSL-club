package com.aloptrbl.pixelart

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Bundle
import android.text.Layout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aloptrbl.pixelart.ui.theme.PixelArtTheme
import kotlinx.coroutines.delay


class CardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelArtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CardApp()
                }
            }
        }
    }
}

@Composable
fun CardApp() {
    var shader = RuntimeShader(SHADER_SOURCE)
    var blurShader = RuntimeShader(BLUR_SHADER_SOURCE)
    var waterShader = RuntimeShader(WATER_SHADER_SOURCE)
    var waveShader = RuntimeShader(WAVE_SHADER_SOURCE)
    var time by remember { mutableStateOf(59.0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        while (true) {
            time += 0.01f // Update time value
            delay(16L) // Delay to match approximately 60 frames per second
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x252c3e50)).verticalScroll(scrollState)) {
  //      Image(painterResource(id = R.drawable.sample2), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp), verticalArrangement = Arrangement.Bottom) {
            // Box 1
            Box(
                Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .height(250.dp)
                    .width(500.dp)
                    .border(BorderStroke(1.dp, color = Color(0x25FFFFFF)))
                    .shadow(elevation = 0.dp)
                    .background(Color(0xFFFFFF))
                    .graphicsLayer {
                        blurShader.setFloatUniform("time", time)
                        val blur = RenderEffect.createBlurEffect(
                            10f,
                            10f,
                            Shader.TileMode.REPEAT
                        )
                        val background = RenderEffect
                            .createRuntimeShaderEffect(blurShader, "composable")
                        renderEffect = RenderEffect
                            .createChainEffect(blur, background)
                            .asComposeRenderEffect()

                    }
                    .onSizeChanged {
                        blurShader.setFloatUniform(
                            "resolution",
                            it.width.toFloat(),
                            it.height.toFloat()
                        )
                    }) {
                Box(Modifier.matchParentSize()) {
                    Text("Hello Universe")
                }
            }
            Spacer(Modifier.height(16.dp))
            // Box 2
            Box(
                Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFFFFFF))
                    .height(150.dp)
                    .width(500.dp)
                    .border(BorderStroke(1.dp, color = Color(0x25FFFFFF)))
                    .shadow(elevation = 0.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .onSizeChanged {
                        shader.setFloatUniform(
                            "resolution",
                            it.width.toFloat(),
                            it.height.toFloat()
                        )
                    }) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .alpha(1f)
                        .blur(
                            radius = 28.dp,
                            edgeTreatment = BlurredEdgeTreatment.Unbounded
                        )
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0x12FFFFFF),
                                    Color(0xDFFFFFF),
                                    Color(0x9FFFFFFF)

                                ),
                                radius = 2200f,
                                center = Offset.Infinite
                            )
                        )) {
                }
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "hello", modifier = Modifier.padding(16.dp), fontSize = 20.sp, color = Color.White)
                }
            }
            Spacer(Modifier.height(16.dp))
            // Box 3
            Box(
                Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .height(100.dp)
                    .width(500.dp)
                    .border(BorderStroke(2.dp, color = Color(0x25FFFFFF)))
                    .shadow(elevation = 0.dp)
                    .drawBehind {
                        waterShader.setFloatUniform("time", time)
                        val brush = ShaderBrush(waterShader)
                        val paint = Paint().apply {
                            this.isAntiAlias = true
                            this.color = Color.White.toArgb()
                            this.maskFilter = BlurMaskFilter(60f, BlurMaskFilter.Blur.NORMAL)
                        }
                        drawRect(brush, blendMode = BlendMode.Overlay)
                    }
                    .onSizeChanged {
                        waterShader.setFloatUniform(
                            "resolution",
                            it.width.toFloat(),
                            it.height.toFloat()
                        )
                    }) {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val blur = RenderEffect.createBlurEffect(
                                    100f,
                                    100f,
                                    Shader.TileMode.REPEAT
                                )
                                this.renderEffect = blur.asComposeRenderEffect()
                            })
                    Text("Hello Universe", fontSize = 23.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            // Box 4
            Box(
                Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .height(100.dp)
                    .width(500.dp)
                    .border(BorderStroke(2.dp, color = Color(0x25FFFFFF)))
                    .shadow(elevation = 0.dp)
                    .graphicsLayer {
                        waveShader.setFloatUniform("time", time)
                        val wave = RenderEffect.createRuntimeShaderEffect(waveShader, "composable")
                        renderEffect = wave.asComposeRenderEffect()

                    }
                    .onSizeChanged {
                        waveShader.setFloatUniform(
                            "resolution",
                            it.width.toFloat(),
                            it.height.toFloat()
                        )
                    }) {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.fillMaxSize())
                    Text("Hello Universe", fontSize = 23.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}