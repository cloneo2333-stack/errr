#version 150



// settings

const float distortion_amount = 0.1;
const float scanline_amount = 0.2;



uniform sampler2D DiffuseSampler;

in vec2 texCoord;

uniform vec2 InSize;

const float Pi = 3.1415926535;
const float ScanlineScale = 1.0;
const float ScanlineHeight = 1.0;
const float ScanlineBrightScale = 1.0;
const float ScanlineOffset = 0.0;
const vec3 Floor = vec3(0.05, 0.05, 0.05);
const vec3 Power = vec3(0.8, 0.8, 0.8);

out vec4 fragColor;

void main() {
	
	vec2 barrelTexCoord = texCoord * 2.0 - 1.0;
	barrelTexCoord *= distortion_amount * (length(barrelTexCoord) - 1) + 1.0;
	barrelTexCoord = barrelTexCoord * 0.5 + 0.5;
	if (barrelTexCoord != clamp(barrelTexCoord, 0.0, 1.0)) discard;
	
    vec4 InTexel = texture(DiffuseSampler, barrelTexCoord);

    // -- Scanline Simulation --
    float InnerSine = barrelTexCoord.y * InSize.y * ScanlineScale * 0.25;
    float ScanBrightMod = sin(InnerSine * Pi + ScanlineOffset * InSize.y * 0.25);
    float ScanBrightness = mix(1.0, (pow(ScanBrightMod * ScanBrightMod, ScanlineHeight) * ScanlineBrightScale + 1.0) * 0.5, scanline_amount);
    vec3 ScanlineTexel = InTexel.rgb * ScanBrightness;

    // -- Color Compression (increasing the floor of the signal without affecting the ceiling) --
    ScanlineTexel = Floor + (1.0 - Floor) * ScanlineTexel;

    ScanlineTexel.rgb = pow(ScanlineTexel.rgb, Power);

    fragColor = vec4(ScanlineTexel.rgb, 1.0);
}
