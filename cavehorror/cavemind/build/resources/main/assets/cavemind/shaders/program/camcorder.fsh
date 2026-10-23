#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Warp;
uniform float Chroma;
uniform float Grain;
uniform float Night;
uniform float Danger;
uniform float Fear;
uniform float Seed;
uniform float Fade;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

vec2 lens(vec2 c, float k) {
    float r2 = dot(c, c);
    return c * (1.0 + k * r2) / (1.0 + k * 0.5);
}

void main() {
    vec2 c = texCoord - 0.5;
    float k = Warp * Fade;
    vec2 d = lens(c, k);
    float ca = Chroma * Fade * (0.4 + 1.6 * dot(c, c));
    vec3 col;
    col.r = texture(DiffuseSampler, 0.5 + d * (1.0 + ca)).r;
    col.g = texture(DiffuseSampler, 0.5 + d).g;
    col.b = texture(DiffuseSampler, 0.5 + d * (1.0 - ca)).b;
    vec3 base = texture(DiffuseSampler, texCoord).rgb;
    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    vec3 nv = vec3(0.20, 1.0, 0.34) * (lum * 1.1 + 0.025);
    vec3 grade = mix(col, vec3(lum) * vec3(0.93, 0.96, 1.04), 0.3);
    col = mix(grade, nv, Night);
    float n = hash(floor(texCoord * InSize / 1.5) + vec2(Seed * 17.13, Seed * 5.71)) - 0.5;
    col += n * Grain * (0.6 + 0.8 * lum);
    float vig = smoothstep(0.95, 0.32, length(c * vec2(1.0, 0.86)) * 1.32);
    col *= mix(1.0, vig, 0.7 + 0.2 * Fear);
    float edge = smoothstep(0.22, 0.72, length(c) * 1.45);
    col = mix(col, col * vec3(1.25, 0.32, 0.30) + vec3(0.10, 0.0, 0.0), Danger * edge * 0.85);
    col *= 1.0 - 0.05 * Night * step(0.5, fract(gl_FragCoord.y * 0.5));
    fragColor = vec4(mix(base, col, Fade), 1.0);
}
