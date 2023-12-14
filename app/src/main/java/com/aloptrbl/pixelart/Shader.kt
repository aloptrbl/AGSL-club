package com.aloptrbl.pixelart

const val SHADER_SOURCE = """
  uniform shader composable;
  uniform float2 resolution;
  float radius = 30.0;
  
  float random (float2 st) {
      return fract(sin(dot(st.xy,vec2(12.9898,78.233)))*43758.5453123);
  }
  
  
  float roundRect(float2 position, float2 box, float radius) {
      vec2 q = abs(position) - box + radius;
      return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;   
  }
  
  half4 avgColor(float2 coord) {
      half4 color = half4(0.0);
      for(float x = -1; x < 2.0; x++) {
          for(float y = -1.0; y < 2.0; y++) {
              float2 offset = float2(x, y);
              color += composable.eval(coord);
          }
      }
      
      return color / 9.0;
  }
  
  half4 main(float2 coord) {
      float2 rectCenter = float2(resolution.x, resolution.y) / 2.0;
      float2 adjustedCoord = coord - rectCenter;
      float distanceFromEdge = roundRect(adjustedCoord, rectCenter, radius);
     
      half4 color = composable.eval(coord);
      if (distanceFromEdge > 0.0) {
          return color;
      }
      
      float2 normCoord = coord / resolution.xy;
      float rand = random(normCoord);
      half4 black = half4(0.3, 0.3, 0.3, 1.0);
      float4 texture = mix(black, float4(float3(rand), 1.0), 0.4);
      color = mix(color, texture, 0.5);
        
//      color = avgColor(coord);

      
      return color;
  }
"""

const val BLUR_SHADER_SOURCE = """
  uniform shader composable;
  uniform float2 resolution;
  uniform float time;

  float random (in vec2 _st) {
    return fract(sin(dot(_st.xy, vec2(0.9,-0.5)))*757.153);
}

// Based on Morgan McGuire @morgan3d
// https://www.shadertoy.com/view/4dS3Wd
float noise (in vec2 _st) {
    vec2 i = floor(_st);
    vec2 f = fract(_st);

    // Four corners in 2D of a tile
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));

    vec2 u = f * f * (3. - 2. * f);

    return mix(a, b, u.x) + (c - a)* u.y * (1. - u.x) + (d - b) * u.x * u.y;
}

float fbm ( in vec2 _st) {
    float v = sin(time*0.3)*0.1;
    float a = 0.1;
    vec2 shift = vec2(100.);
    // Rotate to reduce axial bias
    mat2 rot = mat2(cos(0.5), sin(1.0), -sin(0.5), acos(0.5));
    for (float i = 0.0; i < 3.0; ++i) {
        v += a * noise(_st);
        _st = rot * _st * 2.0 + shift;
        a *= 2.8;
    }
    return v;
}

  half4 main(float2 fragCoord) {
	 vec2 st = (fragCoord * 2.0 - resolution.xy) / min(resolution.x, resolution.y) * 0.8;
   
    vec2 coord = st;
    coord.x += 0.2*time;
    coord.y += 0.2*time;
    
    float len = length(coord) - 3.;     
    
    vec3 color = vec3(0.);

    vec2 q = vec2(0.);
    q.x = fbm( st + 1.0);
    q.y = fbm( st + vec2(-0.450,0.650));

    vec2 r = vec2(0.);
    r.x = fbm( st + 1.0*q + vec2(0.570,0.520)+ 0.1*time );
    r.y = fbm( st + 1.0*q + vec2(0.340,-0.570)+ 0.07*time);
    
    color = mix(color, cos(len + vec3(0.2, 0.0, 0.5)), 1.0);
    color = mix(vec3(0.730,0.237,0.003), vec3(0.667,0.295,0.005), color);
    
    float f = fbm(st+r);
    return vec4(2.0*(f*f*f+.6*f*f+.5*f)*color,1.);
  }

"""

const val WATER_SHADER_SOURCE = """
  uniform shader composable;
  uniform float2 resolution;
  uniform float time;
  
  const float TAU = 6.28318530718;
  const float MAX_ITER = 5.0;

  
  half4 main(float2 fragCoord) {
    
    	float time = time * .5+23.0;
    // uv should be the 0-1 uv of texture...
	vec2 uv = fragCoord.xy / resolution.xy;
    
	vec2 p = mod(uv*TAU*2.0, TAU)-250.0;

	vec2 i = vec2(p);
	float c = 1.0;
	float inten = .005;

	for (float n = 0.0; n < MAX_ITER; n++) 
	{
		float t = time * (1.0 - (3.5 / float(n+1)));
		i = p + vec2(cos(t - i.x) + sin(t + i.y), sin(t - i.y) + cos(t + i.x));
		c += 1.0/length(vec2(p.x / (sin(i.x+t)/inten),p.y / (cos(i.y+t)/inten)));
	}
	c /= float(MAX_ITER);
	c = 1.17-pow(c, 1.4);
	vec3 colour = vec3(pow(abs(c), 8.0));
    colour = clamp(colour + vec3(0.1, 0.1, 0.1), 0.0, 1.0);

    
	return vec4(colour, 1.0);
  }
"""

const val WAVE_SHADER_SOURCE = """
   const float WAVES = 3.0;    
  uniform shader composable;
  uniform float2 resolution;
  uniform float time;
  
    half4 main(float2 fragCoord) {
        vec2 uvNorm = fragCoord.xy / resolution.xy;
	vec2 uv = -1.0 + 2.0 * uvNorm;
    float time = time * 10.3;
       
  	vec4 color = vec4(0.0);    
    vec3 colorLine = vec3(1.0, 1.0, 1.0);
    float epaisLine = 0.002;     

    for(float i=0.0; i<WAVES; i++){
		float sizeDif = (i * 4.0);
        colorLine = vec3(1.0 - (i*0.2));
        
        
		//SiriWave	
        float K = 4.0;
        float B = 10.0;//Nb waves
        float x = uv.x * 2.5;
        float att = (1.0 - (i*0.2)) * 0.3;//Force waves
        float posOnde = uv.y + (att*pow((K/(K+pow(x, K))), K) * cos((B*x)-(time+(i*2.5))));
      
        //Line
        float difEpais = epaisLine + ((epaisLine/WAVES)*i);
        vec3 line = smoothstep( 0.0, 1.0, abs(epaisLine / posOnde)) * colorLine;
        color = vec4(line, 1.0);
    }
    
      if(color.r <= 0.02 && color.g <= 0.02 && color.b <= 0.02) {
        color.a = 0.0;
    }

    return color;
    }
  
  
"""