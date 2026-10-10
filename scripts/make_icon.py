"""iOS 版の AppIcon.png（1024x1024）から、アダプティブアイコンの前景を各密度の mipmap に書き出す。

使い方（リポジトリのルートで）:
    python scripts/make_icon.py ../deki-roulette-ios/DekiRoulette/Resources/Assets.xcassets/AppIcon.appiconset/AppIcon.png app/src/main/res

前景は 108dp 四方で、マスクで切られても欠けない範囲（セーフゾーン、中心の直径 66dp）に絵を収める。
絵の背景は単色（#17111F）なので、前景のキャンバスも同じ色で塗り、背景の層（`ic_launcher_background`）にも同じ色を使う。
"""

import math
import sys
from pathlib import Path

from PIL import Image

FOREGROUND_DP = 108
SAFE_ZONE_DIAMETER_DP = 66
BACKGROUND_RGB = (0x17, 0x11, 0x1F)
DENSITY_SCALES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}


def content_radius(image: Image.Image) -> float:
    """背景色でない画素が、画像の中心からどこまで広がっているか（px）。針が盤面の外縁より外に出ている。"""
    rgb = image.convert("RGB")
    pixels = rgb.load()
    width, height = rgb.size
    center_x = width / 2
    center_y = height / 2
    max_radius = 0.0
    for y in range(height):
        for x in range(width):
            if pixels[x, y] == BACKGROUND_RGB:
                continue
            radius = math.hypot(x + 0.5 - center_x, y + 0.5 - center_y)
            max_radius = max(max_radius, radius)
    return max_radius


def make_foreground(source: Image.Image, source_content_radius: float, size_px: int) -> Image.Image:
    """1 つの密度の前景。絵の広がりの半径がセーフゾーンの半径に一致する大きさで中央に置く。"""
    px_per_dp = size_px / FOREGROUND_DP
    safe_radius_px = SAFE_ZONE_DIAMETER_DP / 2 * px_per_dp
    scale = safe_radius_px / source_content_radius
    art_size = round(source.width * scale)
    art = source.convert("RGBA").resize((art_size, art_size), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (size_px, size_px), BACKGROUND_RGB + (255,))
    offset = (size_px - art_size) // 2
    canvas.alpha_composite(art, (offset, offset))
    return canvas


def main() -> None:
    source_path = Path(sys.argv[1])
    res_dir = Path(sys.argv[2])
    source = Image.open(source_path)
    source_content_radius = content_radius(source)
    for density, density_scale in DENSITY_SCALES.items():
        size_px = round(FOREGROUND_DP * density_scale)
        out_dir = res_dir / f"mipmap-{density}"
        out_dir.mkdir(parents=True, exist_ok=True)
        out_path = out_dir / "ic_launcher_foreground.png"
        make_foreground(source, source_content_radius, size_px).save(out_path)
        print(f"wrote {out_path} ({size_px}px)")


if __name__ == "__main__":
    main()
