#version 150



// settings

const float saturation = 0.4;
const float vibrance = 1.0; // affects saturation of less saturated colors



uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;

uniform vec3 Gray;
uniform vec3 RedMatrix;
uniform vec3 GreenMatrix;
uniform vec3 BlueMatrix;
uniform vec3 Offset;
uniform vec3 ColorScale;
uniform float Saturation;

out vec4 fragColor;

void main() {
    vec4 InTexel = texture(DiffuseSampler, texCoord);

    // Color Matrix
    float RedValue = dot(InTexel.rgb, RedMatrix);
    float GreenValue = dot(InTexel.rgb, GreenMatrix);
    float BlueValue = dot(InTexel.rgb, BlueMatrix);
    vec3 OutColor = vec3(RedValue, GreenValue, BlueValue);

    // Offset & Scale
    OutColor = (OutColor * ColorScale) + Offset;

    // Saturation
    float Luma = dot(OutColor, Gray);
    vec3 Chroma = OutColor - Luma;
    Chroma = pow(abs(Chroma), vec3(2.0 - vibrance)) * sign(Chroma);
    OutColor = Chroma * (Saturation + saturation - 1.0) + Luma;

    fragColor = vec4(OutColor, 1.0);
}
