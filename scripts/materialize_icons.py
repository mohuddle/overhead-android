#!/usr/bin/env python3
"""Copy the Mobitecture Overhead launcher into the Android resource tree.

The artwork matches branding/ in mohuddle/omarchy-overhead: a sky field, the
side-view plane, and a copper wing light. The PNGs in branding/ are that
render. This script installs the adaptive layers and the legacy density icons.
"""
import shutil
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[1]
android = root / "branding" / "android"
res = root / "app" / "src" / "main" / "res"


def require(tool: str) -> None:
    if shutil.which(tool) is None:
        raise SystemExit(f"Missing {tool}")


def resize(src: Path, size: int, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["magick", str(src), "-filter", "Lanczos", "-resize", f"{size}x{size}", str(dest)],
        check=True,
    )
    print(f"Wrote {dest} {dest.stat().st_size} bytes")


require("magick")

for name in ["ic_launcher_background", "ic_launcher_foreground", "ic_launcher_monochrome"]:
    src = android / f"{name}.png"
    dest = res / "drawable" / f"{name}.png"
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src, dest)
    print(f"Wrote {dest}")

xml = (android / "ic_launcher.xml").read_text()
for dest_name in ["ic_launcher.xml", "ic_launcher_round.xml"]:
    dest = res / "mipmap-anydpi-v26" / dest_name
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(xml)
    print(f"Wrote {dest}")

hero = android / "playstore-icon-512.png"
for dens, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    resize(hero, size, res / f"mipmap-{dens}" / "ic_launcher.png")
    resize(hero, size, res / f"mipmap-{dens}" / "ic_launcher_round.png")
