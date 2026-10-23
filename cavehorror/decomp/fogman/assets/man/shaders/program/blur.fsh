#version 150



// settings

const float blur_amount = 2.0;



uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;

uniform vec2 BlurDir;
uniform float Radius;

out vec4 fragColor;

void main() {
    fragColor = vec4(0.0);
    fragColor += texture(DiffuseSampler, texCoord + oneTexel * BlurDir * Radius * -1.0 * blur_amount) * 0.368;
    fragColor += texture(DiffuseSampler, texCoord + oneTexel * BlurDir * Radius * -0.5 * blur_amount) * 0.779;
    fragColor += texture(DiffuseSampler, texCoord + oneTexel * BlurDir * Radius *  0.0 * blur_amount) * 1.000;
    fragColor += texture(DiffuseSampler, texCoord + oneTexel * BlurDir * Radius *  0.5 * blur_amount) * 0.779;
    fragColor += texture(DiffuseSampler, texCoord + oneTexel * BlurDir * Radius *  1.0 * blur_amount) * 0.368;
    fragColor /= 3.294; // total of weights
}
