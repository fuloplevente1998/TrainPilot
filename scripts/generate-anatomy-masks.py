"""Derive six aligned muscle tint masks from the v1.7.7 anatomy illustration.

The manually bounded regions restrict the color extraction to the corresponding
muscle groups. Gray fascia, skin, hair, and the warm background remain neutral.
Run: python3 scripts/generate-anatomy-masks.py
"""
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageOps

ROOT = Path(__file__).resolve().parents[1] / "www"
SOURCE = ROOT / "progress-anatomy-177.webp"
IMAGE = Image.open(SOURCE).convert("RGB")
W, H = IMAGE.size
assert (W, H) == (1536, 1024)

# Anatomical territories follow the actual contours in the front and rear art.
# Several polygons per group keep the shoulders, pecs, arms, trunk, and legs
# separate. Pixel chroma then clips the tint to the illustrated muscle tissue.
REGIONS = {
    "chest": [
        [(275,331),(325,295),(384,289),(442,316),(447,409),(418,425),(341,413),(295,386),(266,352)],
        [(466,315),(532,290),(595,303),(645,333),(649,356),(616,392),(546,414),(476,425),(457,407)],
    ],
    "back": [
        [(1019,185),(1060,220),(1103,257),(1112,337),(1102,480),(1069,588),(1000,625),(928,611),(917,542),(942,440),(958,332)],
        [(1125,259),(1169,220),(1213,183),(1274,325),(1310,438),(1323,547),(1292,612),(1230,627),(1160,590),(1127,479),(1116,337)],
    ],
    "shoulders": [
        [(222,334),(250,294),(301,281),(337,300),(322,363),(287,411),(237,431),(211,397)],
        [(601,301),(651,280),(697,300),(728,346),(710,399),(669,428),(630,399)],
        [(853,339),(890,297),(948,280),(984,307),(964,367),(931,407),(884,430),(847,397)],
        [(1234,307),(1277,280),(1337,300),(1369,342),(1361,397),(1311,431),(1269,400)],
    ],
    "arms": [
        [(209,383),(258,387),(277,450),(266,538),(230,590),(196,570),(187,494)],
        [(190,537),(230,588),(225,658),(195,751),(152,781),(139,736),(152,650)],
        [(646,383),(698,391),(727,489),(721,568),(689,591),(656,539),(639,452)],
        [(694,552),(730,521),(760,635),(775,731),(750,779),(714,756),(694,664)],
        [(798,431),(837,382),(885,402),(910,470),(896,551),(862,599),(822,580)],
        [(818,555),(859,588),(857,661),(836,750),(799,772),(778,734),(791,627)],
        [(1320,397),(1368,380),(1404,430),(1423,543),(1384,602),(1343,561)],
        [(1381,553),(1422,533),(1440,632),(1454,734),(1426,779),(1393,748),(1375,650)],
    ],
    "core": [
        [(334,413),(387,428),(450,409),(456,654),(425,723),(391,714),(348,629),(315,505)],
        [(461,410),(533,427),(581,412),(608,504),(563,669),(499,731),(462,655)],
        [(1000,615),(1099,602),(1111,683),(1057,719),(987,681)],
        [(1124,603),(1240,616),(1255,684),(1166,718),(1119,684)],
    ],
    "legs": [
        [(283,639),(335,655),(408,692),(425,825),(382,966),(325,1009),(283,964),(254,825)],
        [(507,691),(572,652),(626,640),(660,813),(633,963),(578,1009),(525,967),(497,827)],
        [(951,635),(1003,630),(1106,660),(1126,766),(1088,845),(990,854),(928,801)],
        [(1143,661),(1233,630),(1291,643),(1332,802),(1267,854),(1163,845),(1120,766)],
        [(963,819),(1033,838),(1092,840),(1120,1009),(930,1009)],
        [(1164,840),(1241,840),(1311,820),(1328,1009),(1137,1009)],
    ],
}

rgb = np.asarray(IMAGE, dtype=np.float32)
red, green, blue = rgb.transpose(2, 0, 1)
# Warm illustrated fibers carry red chroma; gray tissue and white fascia do not.
chroma = np.clip((red - green - 11) / 48, 0, 1)
chroma *= np.clip((green - blue - 2) / 28, 0, 1)
chroma *= np.clip((red - 64) / 80, 0, 1)
# The source has a warm vignette behind the figure. Its darker, smooth pixels
# must not be mistaken for muscle just because their hue is orange.
chroma *= np.clip(((red + green + blue) / 3 - 103) / 34, 0, 1)

masks = {}
for key, polygons in REGIONS.items():
    region = Image.new("L", (W, H), 0)
    draw = ImageDraw.Draw(region)
    for polygon in polygons:
        draw.polygon(polygon, fill=255)
    region = region.filter(ImageFilter.GaussianBlur(2))
    alpha = np.uint8(np.clip(np.asarray(region, dtype=np.float32) * chroma * 0.77, 0, 210))
    mask = Image.fromarray(alpha, "L")
    mask.save(ROOT / f"progress-muscle-{key}-177.png", optimize=True)
    masks[key] = mask

if __name__ == "__main__":
    # Preview is intentionally outside www; it never ships in the APK.
    preview = ImageOps.grayscale(IMAGE).convert("RGBA")
    colors = {"chest":"#f28a28", "back":"#e84e4e", "shoulders":"#f28a28",
              "arms":"#e84e4e", "core":"#e84e4e", "legs":"#f28a28"}
    for key, color in colors.items():
        layer = Image.new("RGBA", (W, H), color)
        layer.putalpha(masks[key])
        preview = Image.alpha_composite(preview, layer)
    preview.convert("RGB").save(ROOT.parent / "issue61-mask-preview.jpg", quality=88)
