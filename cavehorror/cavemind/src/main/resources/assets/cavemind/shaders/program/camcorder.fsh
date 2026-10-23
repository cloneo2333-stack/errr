#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D HistorySampler;
uniform vec2 InSize;
uniform float Warp;
uniform float Chroma;
uniform float Grain;
uniform float Night;
uniform float Danger;
uniform float Fear;
uniform float Seed;
uniform float Fade;
uniform float Time;
uniform float Focus;
uniform float Mist;
uniform float Tear;
uniform float TearY;
uniform float Ghost;
uniform float Aura;
uniform float Dim;
uniform float Lcd;
uniform float Ink;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = p * 2.03 + vec2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}

vec2 lens(vec2 c, float k) {
    float r2 = dot(c, c);
    return c * (1.0 + k * r2) / (1.0 + k * 0.5);
}

// soft disc blur, radius in pixels
vec3 blur(vec2 uv, float r) {
    vec3 s = texture(DiffuseSampler, uv).rgb;
    for (int i = 0; i < 12; i++) {
        float a = float(i) * 2.39996;
        float d = sqrt((float(i) + 0.5) / 12.0);
        s += texture(DiffuseSampler, uv + vec2(cos(a), sin(a)) * d * r / InSize).rgb;
    }
    return s / 13.0;
}

void main() {
    vec2 q = texCoord;
    vec3 base = texture(DiffuseSampler, q).rgb;

    // VHS tear: a band of the picture slides sideways and the tape crackles along its edges
    float band = 0.0;
    if (Tear > 0.001) {
        float h = 0.035 + 0.05 * hash(vec2(floor(Time * 7.0), 3.1));
        band = 1.0 - smoothstep(h * 0.6, h, abs(q.y - TearY));
        q.x += band * Tear * (0.035 + 0.05 * (noise(vec2(q.y * 90.0, Time * 30.0)) - 0.5));
        q.x += Tear * 0.004 * (noise(vec2(q.y * 400.0, Time * 50.0)) - 0.5);
    }

    vec2 c = q - 0.5;
    float k = Warp * Fade;
    vec2 d = lens(c, k) * (1.0 - Focus * 0.025);
    float ca = Chroma * Fade * (0.4 + 1.6 * dot(c, c)) + Tear * 0.01;
    vec2 uv = 0.5 + d;
    vec3 col;
    col.r = texture(DiffuseSampler, 0.5 + d * (1.0 + ca)).r;
    col.g = texture(DiffuseSampler, uv).g;
    col.b = texture(DiffuseSampler, 0.5 + d * (1.0 - ca)).b;

    // autofocus hunting: the far end of the corridor (the middle of the frame) smears worst
    if (Focus > 0.001) {
        float r = Focus * (5.0 + 11.0 * (1.0 - smoothstep(0.0, 0.45, length(c))));
        col = mix(col, blur(uv, r), clamp(Focus * 1.4, 0.0, 1.0));
    }

    // frame ghosting: half a second of old picture bleeds through
    if (Ghost > 0.001) {
        vec3 old = texture(HistorySampler, 0.5 + d * 1.01 + vec2(0.006, 0.0)).rgb;
        col = mix(col, max(col, old), Ghost * 0.55);
    }

    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    vec3 nv = vec3(0.20, 1.0, 0.34) * (lum * 1.1 + 0.025) * (1.0 - Dim);
    vec3 grade = mix(col, vec3(lum) * vec3(0.93, 0.96, 1.04), 0.3);
    col = mix(grade, nv, Night);

    // breath on the lens: the two bottom corners go milky, then clear
    if (Mist > 0.001) {
        float edge = fbm(q * vec2(7.0, 5.0) + vec2(Time * 0.03, 0.0));
        float m = max(1.0 - smoothstep(0.0, 0.7, length((q - vec2(0.0, 0.0)) * vec2(1.15, 1.0))),
                      1.0 - smoothstep(0.0, 0.7, length((q - vec2(1.0, 0.0)) * vec2(1.15, 1.0))));
        m = smoothstep(0.1, 0.65, m + (edge - 0.5) * 0.5) * Mist;
        vec3 hazy = blur(uv, 18.0);
        float hl = dot(hazy, vec3(0.299, 0.587, 0.114)) * 0.7 + 0.36;
        vec3 milk = mix(vec3(hl) * vec3(0.78, 0.72, 0.68), mix(vec3(0.20, 1.0, 0.34) * hl, vec3(hl * 0.8), 0.3) * (1.0 - Dim), Night);
        col = mix(col, milk, m * 0.9);
    }

    // the LCD's faint amber spill when night vision is off
    col += vec3(0.07, 0.022, 0.004) * Lcd * (1.0 - smoothstep(0.0, 0.8, length((q - vec2(0.5, -0.15)) * vec2(1.0, 1.5))));

    float n = hash(floor(q * InSize / 1.5) + vec2(Seed * 17.13, Seed * 5.71)) - 0.5;
    col += n * Grain * (mix(0.25, 0.6, Night) + 0.9 * lum);
    if (band > 0.0) col += band * Tear * (hash(vec2(q.x * InSize.x, Time)) - 0.35) * 0.35;

    float vig = smoothstep(0.95, 0.32, length(c * vec2(1.0, 0.86)) * 1.32);
    col *= mix(1.0, vig, 0.7 + 0.2 * Fear);
    float redge = smoothstep(0.22, 0.72, length(c) * 1.45);
    col = mix(col, col * vec3(1.25, 0.32, 0.30) + vec3(0.10, 0.0, 0.0), Danger * redge * 0.85);

    // the Damned's presence: black veins crawl in from the corners, the frame's edge rots inward
    if (Aura > 0.001) {
        vec2 cd = min(q, 1.0 - q) * vec2(InSize.x / InSize.y, 1.0);
        float corner = length(cd);
        float side = min(cd.x, cd.y);
        float f = fbm(q * vec2(8.0, 5.5) + vec2(Time * 0.06, -Time * 0.04));
        float vein = 1.0 - smoothstep(0.0, 0.025 + 0.03 * Aura, abs(f - 0.5));
        float reach = 0.2 + Aura * 0.75;
        float zone = 1.0 - smoothstep(reach * 0.55, reach, corner);
        float rot = 1.0 - smoothstep(0.0, 0.02 + Aura * 0.22, side + (f - 0.5) * 0.05);
        col *= 1.0 - clamp(max(vein * zone, rot * 0.92) * (0.6 + 0.4 * Aura), 0.0, 1.0);
    }

    // ink seeping from the top of the frame
    if (Ink > 0.001) {
        float drip = fbm(vec2(q.x * 14.0, 2.0)) * 0.55 + 0.45 * noise(vec2(q.x * 60.0, 1.0));
        float len = Ink * (0.08 + 0.32 * drip * drip) * (0.8 + 0.2 * sin(Time * 0.7 + q.x * 9.0));
        float ink = 1.0 - smoothstep(len * 0.7, len, 1.0 - q.y);
        float rim = ink * (1.0 - smoothstep(len * 0.55, len * 0.7, 1.0 - q.y));
        col = col * (1.0 - ink * 0.95) + vec3(0.16, 0.03, 0.035) * (ink - rim) * (0.35 + 0.65 * noise(vec2(q.x * 80.0, q.y * 30.0)));
    }

    col *= 1.0 - 0.05 * Night * step(0.5, fract(gl_FragCoord.y * 0.5));
    fragColor = vec4(mix(base, col, Fade), 1.0);
}
