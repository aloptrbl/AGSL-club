package com.aloptrbl.pixelart

import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aloptrbl.pixelart.ui.theme.PixelArtTheme
import kotlinx.coroutines.delay

const val BLIP_SHADER_SOURCE = """
   uniform shader composable;
   uniform float2 resolution;
   uniform float time;

   half4 main(float2 fragCoord) {
   float x = fragCoord.x / 800.0; // normalized x coordinate
   float alpha = smoothstep(0.0, 1.0, sin(time * 20)); // smooth fade using sine function
   half3 color = composable.eval(fragCoord).rgb;
   return half4(color,alpha);
   }
   
"""

const val FILTER_1987_SHADER_SOURCE = """
   uniform shader composable;
   uniform float2 resolution;
   uniform float time;

   half4 main(float2 fragCoord) {
   // get the color of the composable shader
   half4 color = composable.eval(fragCoord);

   // apply the contrast, brightness, and saturation filters
   color.rgb = mix(vec3(1), mix(vec3(dot(vec3(0.2126, 0.7152, 0.0722), color.rgb)), color.rgb, 1.3), 1.1);
   color.rgb *= 1.1;

   // apply the screen blend mode with the background color
   half4 background = half4(0.9529, 0.4157, 0.7373,1);
   color.rgb = 1.0 - (1.0 - color.rgb) * (1.0 - background.rgb);

   // set the opacity
   color.a = 1;

   // return the final color
   return color;
   }
"""

const val MIX_SHADER_SOURCE = """
   uniform shader composable;
   uniform float2 resolution;
   uniform float time;
   uniform float red;
   uniform float green;
   uniform float blue;
   uniform float alpha;

   half4 main(float2 fragCoord) {
   half4 color = composable.eval(fragCoord);
   // color blend mode with background
   half4 background = half4(red, green, blue, alpha);
   color.rgb = 1.0 - (1.0 - color.rgb) * (1.0 - background.rgb);
   return color;
   }
   
"""

const val MOLECULE_SHADER_SOURCE = """
   uniform shader composable;
   uniform float2 resolution;
   uniform float time;
   uniform shader itexture;
   uniform float offsetX;
   uniform float offsetY;
   

float ltime;

float noise(vec2 p)
{
  return sin(p.x*10.) * sin(p.y*(3. + sin(ltime/11.))) + .2; 
}

mat2 rotate(float angle)
{
  return mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
}


float fbm(vec2 p)
{
  p *= 1.1;
  float f = 0.;
  float amp = .5;
  for( int i = 0; i < 3; i++) {
    mat2 modify = rotate(ltime/50. * float(i*i));
    f += amp*noise(p);
    p = modify * p;
    p *= 2.;
    amp /= 2.2;
  }
  return f;
}

float pattern(vec2 p, out vec2 q, out vec2 r) {
  q = vec2( fbm(p + vec2(1.)),
	    fbm(rotate(.1*ltime)*p + vec2(3.)));
  r = vec2( fbm(rotate(.2)*q + vec2(0.)),
	    fbm(q + vec2(0.)));
  return fbm(p + 1.*r);

}

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

   half4 main(float2 fragCoord) {
   vec2 p = fragCoord.xy / resolution.xy;
  ltime = time;
  float ctime = time + fbm(p/8.)*40.;
  float ftime = fract(ctime/6.);
  ltime = floor(ctime/6.) + (1.-cos(ftime*3.1415)/2.);
  ltime = ltime*6.;
  vec2 q;
  vec2 r;
  float f = pattern(p, q, r);
  vec3 col = hsv2rgb(vec3(q.x/10. + ltime/100. + .4, abs(r.y)*3. + .1, r.x + f));
  float vig = 1. - pow(4.*(p.x - .5)*(p.x - .5), 10.);
  vig *= 1. - pow(4.*(p.y - .5)*(p.y - .5), 10.);
  
   half4 color = composable.eval(fragCoord);
   vec2 offset = vec2(offsetX, offsetY);
   float dist = distance(offset, fragCoord);
   color.rgb = 1.0 - (1.0 - color.rgb) * (1.0 - col*vig);
  
  return color;
   }
   
"""

const val MOLECULE2_SHADER_SOURCE = """
   uniform shader composable;
   uniform float2 resolution;
   uniform float time;
   uniform shader itexture;
   uniform float offsetX;
   uniform float offsetY;
  
  float SCurve (float x) {
		x = x * 2.0 - 1.0;
		return -x * abs(x) * 0.5 + x + 0.5;
}

  
   half4 main(float2 fragCoord) {
   // normalize 0 to 1
   vec2 uv = fragCoord / resolution;
   half4 image = composable.eval(fragCoord);

   // Modify the background color to make it glass-like
   vec3 backgroundCol = vec3(uv.xyy);
   half4 background = half4(backgroundCol, 0.4); // Decreased alpha for a more transparent background

   // Blend the image and background with an alpha blend
   image.rgb = mix(background.rgb, image.rgb, background.a);

  return half4(image);
   }
   
"""

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelArtTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                   App(this.resources)
                }
            }
        }
    }
}

@Composable
fun App(resources: Resources) {
    var scrollState = rememberScrollState()
    val context = LocalContext.current
    var time by remember { mutableStateOf(0.0f) }
    val photo1 = BitmapFactory.decodeResource(resources, R.drawable.sample3)
    var photo2 = BitmapFactory.decodeResource(resources, R.drawable.sample2)
    var photo3 = BitmapFactory.decodeResource(resources, R.drawable.sample1)

    var blipShader = RuntimeShader(BLIP_SHADER_SOURCE)
    var filter1987Shader = RuntimeShader(FILTER_1987_SHADER_SOURCE)
    var mixShader = RuntimeShader(MIX_SHADER_SOURCE)
    var moleculeShader = RuntimeShader(MOLECULE_SHADER_SOURCE)
    var molecule2Shader = RuntimeShader(MOLECULE2_SHADER_SOURCE)

    var bitmapShader = BitmapShader(photo2, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    var redValue by remember { mutableStateOf(0.0f) }
    var greenValue by remember { mutableStateOf(0.0f) }
    var blueValue by remember { mutableStateOf(0.0f) }
    var alphaValue by remember { mutableStateOf(0.0f) }


    LaunchedEffect(Unit) {
        while (true) {
            time += 0.01f // Update time value
            delay(16L) // Delay to match approximately 60 frames per second
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
        // Image #1
        Image(bitmap = photo1.asImageBitmap(), contentDescription = "", modifier = Modifier
            .height(250.dp)
            .onSizeChanged {
                blipShader.setFloatUniform(
                    "resolution",
                    it.width.toFloat(),
                    it.height.toFloat()
                )
            }
            .graphicsLayer {
                clip = true
                blipShader.setFloatUniform("time", time)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(blipShader, "composable")
                    .asComposeRenderEffect()
            })

        Spacer(Modifier.height(16.dp))
        // Image #2
        Image(bitmap = photo2.asImageBitmap(), contentDescription = "", modifier = Modifier
            .height(250.dp)
            .onSizeChanged {
                filter1987Shader.setFloatUniform(
                    "resolution",
                    it.width.toFloat(),
                    it.height.toFloat()
                )
            }
            .graphicsLayer {
                clip = true
                filter1987Shader.setFloatUniform("time", time)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(filter1987Shader, "composable")
                    .asComposeRenderEffect()
            })

        Spacer(Modifier.height(16.dp))
        // Image #3
        Image(bitmap = photo3.asImageBitmap(), contentDescription = "", modifier = Modifier
            .height(250.dp)
            .onSizeChanged {
                mixShader.setFloatUniform(
                    "resolution",
                    it.width.toFloat(),
                    it.height.toFloat()
                )
            }
            .graphicsLayer {
                clip = true
                mixShader.setFloatUniform("time", time)
                mixShader.setFloatUniform("red", redValue)
                mixShader.setFloatUniform("green", greenValue)
                mixShader.setFloatUniform("blue", blueValue)
                mixShader.setFloatUniform("alpha", alphaValue)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(mixShader, "composable")
                    .asComposeRenderEffect()
            })
        Spacer(Modifier.height(16.dp))
        Text("Red")
        Slider(
            modifier = Modifier.padding(16.dp),
            value = redValue,
            onValueChange = {value-> redValue = value },
            valueRange =  0.0f..1.0f
        )
        Text("Green")
        Slider(
            modifier = Modifier.padding(16.dp),
            value = greenValue,
            onValueChange = {value-> greenValue = value },
            valueRange =  0.0f..1.0f
        )
        Text("Blue")
        Slider(
            modifier = Modifier.padding(16.dp),
            value = blueValue,
            onValueChange = {value-> blueValue = value },
            valueRange =  0.0f..1.0f
        )
        Text("Alpha")
        Slider(
            modifier = Modifier.padding(16.dp),
            value = alphaValue,
            onValueChange = {value-> alphaValue = value },
            valueRange =  0.0f..1.0f
        )

        Spacer(Modifier.height(16.dp))
        // Image #4
        Image(bitmap = photo2.asImageBitmap(), contentDescription = "", modifier = Modifier
            .height(250.dp)
            .fillMaxWidth()
            .onSizeChanged {
                moleculeShader.setFloatUniform(
                    "resolution",
                    it.width.toFloat(),
                    it.height.toFloat()
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { },
                    onDoubleTap = { },
                    onLongPress = { },
                    onTap = { it ->
                        moleculeShader.setFloatUniform(
                            "offsetX",
                            it.x
                        ); moleculeShader.setFloatUniform("offsetY", it.y)
                    }
                )
            }
            .graphicsLayer {
                clip = true
                moleculeShader.setInputBuffer("itexture", bitmapShader);
                moleculeShader.setFloatUniform("time", time)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(moleculeShader, "composable")
                    .asComposeRenderEffect()
            })

        Box(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(100.dp)
                .background(Color.Unspecified)
                .onSizeChanged {
                    molecule2Shader.setFloatUniform(
                        "resolution",
                        it.width.toFloat(),
                        it.height.toFloat()
                    )
                }
                .graphicsLayer {
                    clip = true
                    molecule2Shader.setFloatUniform("time", time)
                    renderEffect = RenderEffect
                        .createRuntimeShaderEffect(molecule2Shader, "composable")
                        .asComposeRenderEffect()
                }) {
            Text("New Notification", Modifier.padding(25.dp))
        }

        Button(onClick = {
            val intent = Intent(context, CardActivity::class.java)
            // Start the new activity with the intent
            context.startActivity(intent)
        }) {
            Text("Card View")
        }
    }
}