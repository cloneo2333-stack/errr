#!/usr/bin/env python3
"""Synthesizes the maze sounds (mono OGG) into src/main/resources/assets/cavemind/sounds/maze.

Run: python3 tools/gen_sounds.py   (needs numpy and ffmpeg with libvorbis)
Everything is generated from fixed seeds so the files are reproducible.
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "cavemind", "sounds", "maze")


def t_axis(sec):
    return np.arange(int(SR * sec)) / SR


def lowpass(x, cut):
    a = np.exp(-2 * np.pi * cut / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def bandpass(x, lo, hi):
    return lowpass(x, hi) - lowpass(x, lo)


def resonator(x, f, bw):
    # two-pole resonant filter, used as a formant
    r = np.exp(-np.pi * bw / SR)
    c1, c2 = 2 * r * np.cos(2 * np.pi * f / SR), -r * r
    y = np.zeros_like(x)
    y1 = y2 = 0.0
    g = 1 - r
    for i, v in enumerate(x):
        y0 = g * v + c1 * y1 + c2 * y2
        y[i] = y0
        y2, y1 = y1, y0
    return y


def env(n, attack, release):
    e = np.ones(n)
    a, r = int(SR * attack), int(SR * release)
    if a:
        e[:a] = np.linspace(0, 1, a)
    if r:
        e[-r:] *= np.linspace(1, 0, r)
    return e


def reverb(x, wet=0.3, taps=((0.031, 0.5), (0.047, 0.4), (0.071, 0.3), (0.113, 0.22), (0.173, 0.15))):
    y = x.copy()
    for d, g in taps:
        k = int(SR * d)
        y[k:] += x[:-k] * g * wet * 2
    return y


def norm(x, peak=0.9):
    m = np.max(np.abs(x)) or 1.0
    return x / m * peak


def write(name, x):
    os.makedirs(OUT, exist_ok=True)
    pcm = (np.clip(x, -1, 1) * 32767).astype(np.int16)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, name + ".wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        dst = os.path.join(OUT, name + ".ogg")
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4", dst], check=True)
    print("wrote", name)


def hum():
    # 6 s seamless loop: 19 Hz pressure plus audible 38/57 Hz partials and a slow swell
    rng = np.random.default_rng(19)
    t = t_axis(6.0)
    swell = 0.75 + 0.25 * np.sin(2 * np.pi * 0.5 * t)
    x = 0.55 * np.sin(2 * np.pi * 19 * t) + 0.45 * np.sin(2 * np.pi * 38 * t + 0.6) + 0.22 * np.sin(2 * np.pi * 57 * t + 1.1)
    x += 0.08 * np.sin(2 * np.pi * 76 * t + 2.0) * (0.5 + 0.5 * np.sin(2 * np.pi * (1 / 3) * t))
    noise = lowpass(lowpass(rng.standard_normal(len(t)), 90), 90)
    fade = int(SR * 0.5)
    noise[:fade] = noise[:fade] * np.linspace(0, 1, fade) + noise[-fade:] * np.linspace(1, 0, fade)
    noise = noise[: len(t)]
    x = x * swell + norm(noise, 0.35)
    write("hum", norm(x, 0.95))


def focus():
    # lens motor hunting: "rẹt... rẹt"
    rng = np.random.default_rng(7)
    out = np.zeros(int(SR * 0.5))
    for start, dur, f0 in ((0.0, 0.13, 210), (0.2, 0.17, 260)):
        t = t_axis(dur)
        saw = 2 * ((t * f0 * (1 + 0.15 * t / dur)) % 1) - 1
        gear = (np.sin(2 * np.pi * 95 * t) > 0.2).astype(float) * 0.6 + 0.4
        s = saw * gear + 0.25 * bandpass(rng.standard_normal(len(t)), 1500, 6000)
        s = bandpass(s, 120, 3500) * env(len(t), 0.01, 0.03)
        i = int(SR * start)
        out[i:i + len(s)] += s
    write("focus", norm(out, 0.8))


def voice(f0_curve, dur, formants, breath=0.25, seed=1):
    rng = np.random.default_rng(seed)
    t = t_axis(dur)
    jitter = 1 + 0.012 * lowpass(rng.standard_normal(len(t)), 8) * 30
    f = f0_curve(t) * jitter
    phase = np.cumsum(f / SR)
    pulse = (phase % 1 < 0.35).astype(float) - 0.35
    src = pulse + breath * rng.standard_normal(len(t))
    y = sum(g * resonator(src, fr, bw) for fr, bw, g in formants)
    return y


def sob(seed, name):
    rng = np.random.default_rng(seed)
    parts = []
    for k in range(3):
        # quick catching inhales: "hức hức"
        for _ in range(2 + k % 2):
            n = int(SR * 0.09)
            h = bandpass(rng.standard_normal(n), 900, 4200) * env(n, 0.01, 0.05) * 0.5
            parts.append(h)
            parts.append(np.zeros(int(SR * 0.06)))
        base = 430 + rng.uniform(-30, 40)
        dur = 0.55 + rng.uniform(0, 0.35)
        wail = voice(lambda t: base * (1.08 - 0.22 * t / dur) * (1 + 0.025 * np.sin(2 * np.pi * 6.5 * t)), dur,
                     [(900, 160, 1.0), (1500, 200, 0.6), (3200, 300, 0.25)], breath=0.35, seed=seed + k)
        parts.append(wail * env(len(wail), 0.06, 0.25))
        parts.append(np.zeros(int(SR * rng.uniform(0.25, 0.45))))
    x = np.concatenate(parts)
    x = reverb(bandpass(x, 220, 5200), wet=0.45)
    write(name, norm(x, 0.85))


def cough():
    rng = np.random.default_rng(3)
    out = np.zeros(int(SR * 0.9))
    for start, amp, dur in ((0.0, 1.0, 0.22), (0.3, 0.75, 0.18), (0.55, 0.45, 0.14)):
        n = int(SR * dur)
        burst = rng.standard_normal(n)
        voiced = voice(lambda t: 170 + 0 * t, dur, [(500, 200, 1.0), (1400, 300, 0.5)], breath=0.9, seed=int(start * 100))
        s = resonator(burst, 700, 500) * 1.2 + resonator(burst, 1800, 800) * 0.6 + voiced * 0.8
        e = np.exp(-t_axis(dur) * 18) * env(n, 0.004, 0.02)
        i = int(SR * start)
        out[i:i + n] += s * e * amp
    write("cough", norm(reverb(out, 0.25), 0.95))


def scrape():
    # nails dragged along plaster: stick-slip bursts of filtered noise
    rng = np.random.default_rng(11)
    t = t_axis(1.8)
    stick = np.zeros(len(t))
    pos = 0
    while pos < len(t):
        gap = int(SR / rng.uniform(25, 70))
        stick[pos:pos + gap // 3] = rng.uniform(0.4, 1.0)
        pos += gap
    stick = lowpass(stick, 400)
    grain = bandpass(rng.standard_normal(len(t)), 1200, 7000)
    squeal = np.sin(2 * np.pi * np.cumsum(1900 + 300 * np.sin(2 * np.pi * 0.7 * t)) / SR) * 0.15
    shape = np.interp(t, [0, 0.15, 0.7, 1.1, 1.8], [0, 1, 0.6, 0.9, 0])
    write("scrape", norm(reverb((grain * stick + squeal * stick) * shape, 0.3), 0.8))


def tape():
    # VHS tear: dropout crackle, mains hum and a pitch warble
    rng = np.random.default_rng(5)
    t = t_axis(0.55)
    crackle = (rng.random(len(t)) > 0.985) * rng.standard_normal(len(t)) * 2.5
    hiss = bandpass(rng.standard_normal(len(t)), 2000, 9000) * 0.5
    mains = np.sin(2 * np.pi * 60 * t) * 0.3 + np.sin(2 * np.pi * 120 * t) * 0.15
    warble = np.sin(2 * np.pi * np.cumsum(440 * (1 + 0.3 * np.sin(2 * np.pi * 9 * t))) / SR) * 0.12
    shape = np.interp(t, [0, 0.03, 0.35, 0.55], [0, 1, 0.7, 0])
    write("tape", norm((crackle + hiss + mains + warble) * shape, 0.8))


def top():
    # spinning top: a wobbling whine that slows down
    rng = np.random.default_rng(2)
    t = t_axis(1.6)
    f = 760 - 240 * t / 1.6
    wob = 1 + 0.06 * np.sin(2 * np.pi * (14 - 6 * t / 1.6) * t)
    s = np.sin(2 * np.pi * np.cumsum(f * wob) / SR) + 0.3 * np.sin(2 * np.pi * np.cumsum(2 * f * wob) / SR)
    s += 0.2 * bandpass(rng.standard_normal(len(t)), 2000, 6000) * (0.5 + 0.5 * np.sin(2 * np.pi * 14 * t))
    write("top", norm(reverb(s * env(len(t), 0.05, 0.5), 0.4), 0.7))


def harmonica():
    t = t_axis(1.4)
    s = np.zeros(len(t))
    for f in (392, 494, 587):
        for h in range(1, 6):
            s += np.sin(2 * np.pi * f * h * t * (1 + 0.003 * np.sin(2 * np.pi * 5 * t))) / (h * 1.4)
    s *= 0.6 + 0.4 * np.sin(2 * np.pi * 3.2 * t) ** 2
    write("harmonica", norm(reverb(bandpass(s, 250, 4000) * env(len(t), 0.12, 0.6), 0.5), 0.6))


def twang():
    t = t_axis(0.9)
    f = 150 * (1 + 0.4 * np.exp(-t * 12))
    s = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 5)
    s += 0.4 * np.sin(2 * np.pi * np.cumsum(2.02 * f) / SR) * np.exp(-t * 7)
    write("twang", norm(reverb(s, 0.4), 0.7))


if __name__ == "__main__":
    hum()
    focus()
    sob(21, "sob1")
    sob(42, "sob2")
    cough()
    scrape()
    tape()
    top()
    harmonica()
    twang()
