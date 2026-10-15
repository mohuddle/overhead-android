#!/usr/bin/env python3
"""Render the Overhead launcher SVGs to PNG assets.

Uses rsvg-convert and ImageMagick, which are already on this machine.
The background is sky #2F6FE4, the foreground Cessna is limestone #F4F2ED,
and the monochrome Cessna is white.
"""
import shutil
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[1]
branding = root / "branding"
android = branding / "android"
hero_svg = branding / "overhead-icon.svg"


def require(tool: str) -> None:
    if shutil.which(tool) is None:
        raise SystemExit(f"Missing {tool}")


def render_svg(svg: Path, size: int, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["rsvg-convert", "-w", str(size), "-h", str(size), "-o", str(dest), str(svg)],
        check=True,
    )
    print(f"Wrote {dest} {dest.stat().st_size} bytes")


def resize(src: Path, size: int, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["magick", str(src), "-filter", "Lanczos", "-resize", f"{size}x{size}", str(dest)],
        check=True,
    )
    print(f"Wrote {dest} {dest.stat().st_size} bytes")


require("rsvg-convert")
require("magick")

for name in ["ic_launcher_background", "ic_launcher_foreground", "ic_launcher_monochrome"]:
    render_svg(android / f"{name}.svg", 432, android / f"{name}.png")

hero = android / "overhead-icon-512.png"
render_svg(hero_svg, 512, hero)

res = root / "app" / "src" / "main" / "res"
for name in ["ic_launcher_background", "ic_launcher_foreground", "ic_launcher_monochrome"]:
    dest = res / "drawable" / f"{name}.png"
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(android / f"{name}.png", dest)
    print(f"Wrote {dest}")

xml = (android / "ic_launcher.xml").read_text()
for dest_name in ["ic_launcher.xml", "ic_launcher_round.xml"]:
    dest = res / "mipmap-anydpi-v26" / dest_name
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(xml)
    print(f"Wrote {dest}")

for dens, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    resize(hero, size, res / f"mipmap-{dens}" / "ic_launcher.png")
    resize(hero, size, res / f"mipmap-{dens}" / "ic_launcher_round.png")
