import glob, os, re, subprocess, sys
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "runc")
SHOTS = os.path.join(ROOT, "screenshots")
OUT = os.path.join(ROOT, "films")
os.makedirs(OUT, exist_ok=True)
fps = int(sys.argv[1]) if len(sys.argv) > 1 else 20

clips = {}
for p in glob.glob(os.path.join(SHOTS, "cm_f_*.png")):
    m = re.match(r"cm_f_(.+)_(\d+)\.png", os.path.basename(p))
    if m:
        clips.setdefault(m.group(1), []).append((int(m.group(2)), p))

for name, frames in clips.items():
    frames.sort()
    tmp = os.path.join(OUT, name + "_tmp")
    os.makedirs(tmp, exist_ok=True)
    for i, (_, p) in enumerate(frames):
        Image.open(p).convert("RGB").resize((960, 540)).save(os.path.join(tmp, "%04d.png" % i))
    mp4 = os.path.join(OUT, name + ".mp4")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", str(fps), "-i", os.path.join(tmp, "%04d.png"),
                    "-pix_fmt", "yuv420p", "-vf", "pad=ceil(iw/2)*2:ceil(ih/2)*2", mp4], check=True)
    pick = [frames[int(i * (len(frames) - 1) / 11)][1] for i in range(12)] if len(frames) >= 12 else [p for _, p in frames]
    tiles = [Image.open(p).convert("RGB").crop((960, 300, 1920, 1080)).resize((320, 260)) for p in pick]
    sheet = Image.new("RGB", (320 * 4, 260 * ((len(tiles) + 3) // 4)))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % 4) * 320, (i // 4) * 260))
    sheet.save(os.path.join(OUT, name + "_sheet.png"))
    for f in glob.glob(os.path.join(tmp, "*.png")):
        os.remove(f)
    os.rmdir(tmp)
    print(name, len(frames), "frames ->", mp4)
