#!/usr/bin/env python3
"""Generate the how-to-measure diagrams.

Renders labelled anatomical diagrams as PNG into
app/src/main/res/drawable-nodpi/. PNG rather than VectorDrawable because
Android vector drawables cannot render text, and the landmark labels
(navel, nipple line, gluteal fold) are what make the diagram useful.

Layout: a fixed landscape canvas with the figure on the left and a label
gutter on the right, so nothing is ever clipped and every diagram has the
same proportions in the dialog.

Usage:  python3 tools/generate_measure_figures.py <outdir>
Needs:  pip install cairosvg
"""
import os
import sys

BG      = "#FFFFFF"
SKIN    = "#FCFCFD"   # near-white fill so overlapping limbs hide their seams
SKIN_DK = "#F4F6F7"
EDGE    = "#2E3B43"   # firm outline - the figure reads as line art, not a blob
DETAIL  = "#8FA0AA"   # interior anatomy lines
TAPE    = "#E8480F"
MARK    = "#C62828"
GUIDE   = "#7B8E99"
TEXT    = "#22313A"
FONT    = "DejaVu Sans, sans-serif"

# Canvas, figure box and label gutter, in canvas units.
CW, CH = 460, 330
FIG = (18, 14, 210, 280)      # x, y, w, h - where the body is drawn
GUT_X = 244                   # labels start here
CAP_Y = 316                   # caption baseline


# ---------------------------------------------------------------- body parts
# The body is a supplied line-art template (tools/assets/base_figure.png),
# drawn front and back. Overlays are positioned in that image's own pixel
# coordinates, so the landmark table below is measured directly off it.

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "assets", "base_figure.png")
BASE_W, BASE_H = 960, 1090

# Landmarks measured from the front figure, in base-image pixels.
# cx/rx describe the body width at that height, so the tape ellipse matches it.
NECK   = dict(y=196, cx=315, rx=43)
CHEST  = dict(y=288, cx=316, rx=89)
WAIST  = dict(y=412, cx=316, rx=73)
HIP    = dict(y=521, cx=313, rx=102)
BICEP  = dict(y=332, cx=203, rx=26)
THIGH  = dict(y=660, cx=255, rx=45)
CALF   = dict(y=838, cx=227, rx=32)   # calf belly - widest point of the lower leg
KNEE   = (231, 786)                   # narrowest point above it
NAVEL  = (316, 400)
NIPPLES = ((277, 286), (355, 286))
GLUTEAL_FOLD = (255, 630)


def figure_image():
    """Embed the template as a data URI - cairosvg does not resolve file:// hrefs."""
    import base64
    with open(BASE, "rb") as fh:
        b64 = base64.b64encode(fh.read()).decode("ascii")
    return (f'<image xlink:href="data:image/png;base64,{b64}" x="0" y="0" '
            f'width="{BASE_W}" height="{BASE_H}" '
            f'preserveAspectRatio="none"/>')


# ------------------------------------------------------------------ overlays

def wrap(y, cx, rx, squash=0.20):
    """Measuring tape around the body at height y, drawn as an ellipse in
    perspective: dashed where it passes behind, solid across the front."""
    ry = max(rx * squash, 3.0)
    behind = f"M{cx-rx},{y} A{rx},{ry} 0 0,1 {cx+rx},{y}"
    front = f"M{cx-rx},{y} A{rx},{ry} 0 0,0 {cx+rx},{y}"
    return (
        f'<path d="{behind}" fill="none" stroke="{TAPE}" stroke-width="2.4" '
        f'stroke-dasharray="6 5" stroke-linecap="round" opacity="0.85"/>'
        f'<path d="{front}" fill="none" stroke="{TAPE}" stroke-width="5" '
        f'stroke-linecap="round"/>'
    )


def band(y, x0, x1, bow=7):
    """Flat band, kept for callers that want a straight rule."""
    return wrap(y, (x0 + x1) / 2, abs(x1 - x0) / 2)


def dot(x, y, r=5):
    return (f'<circle cx="{x}" cy="{y}" r="{r}" fill="{MARK}" '
            f'stroke="#FFFFFF" stroke-width="1.5"/>')


def dashed(y, x0, x1):
    return (f'<line x1="{x0}" y1="{y}" x2="{x1}" y2="{y}" stroke="{GUIDE}" '
            f'stroke-width="1.6" stroke-dasharray="5 4"/>')


def text(x, y, s, size=13, color=None, anchor="start", weight="normal"):
    return (f'<text x="{x}" y="{y}" font-family="{FONT}" font-size="{size}" '
            f'fill="{color or TEXT}" text-anchor="{anchor}" '
            f'font-weight="{weight}">{s}</text>')


def leader(x0, y0, x1, y1):
    return (f'<line x1="{x0}" y1="{y0}" x2="{x1}" y2="{y1}" stroke="{GUIDE}" '
            f'stroke-width="1.4"/>')


class Fig:
    """Composes a figure: body drawn in its own coordinates, scaled into the
    figure box; labels placed in canvas coordinates so they never clip."""

    def __init__(self, body, focus):
        self.body = body
        fx, fy, fw, fh = FIG
        _x, _y, w, h = focus
        self.s = min(fw / w, fh / h)
        self.tx = fx + (fw - w * self.s) / 2 - _x * self.s
        self.ty = fy + (fh - h * self.s) / 2 - _y * self.s
        self.overlays = []   # drawn inside the scaled group (body coords)
        self.labels = []     # drawn on the canvas (canvas coords)

    def to_canvas(self, x, y):
        return (x * self.s + self.tx, y * self.s + self.ty)

    def add(self, svg_frag):
        self.overlays.append(svg_frag)
        return self

    def callout(self, bx, by, lines, size=12.5, side="right", dy=0):
        """Leader line from a point on the body out to text in the gutter."""
        cx, cy = self.to_canvas(bx, by)
        ty = cy + dy
        if side == "right":
            lx = GUT_X
            self.labels.append(leader(cx + 6, cy, lx - 6, ty - 4))
            anchor = "start"
        else:
            lx = FIG[0] + 4
            self.labels.append(leader(cx - 6, cy, lx + 6, ty - 4))
            anchor = "end"
        for i, line in enumerate(lines):
            self.labels.append(
                text(lx, ty + i * (size + 3), line, size, anchor=anchor)
            )
        return self

    def caption(self, s):
        self.labels.append(text(CW / 2, CAP_Y, s, 12.5, GUIDE, anchor="middle"))
        return self

    def render(self):
        fx, fy, fw, fh = FIG
        return (
            f'<svg xmlns="http://www.w3.org/2000/svg" '
            f'xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 {CW} {CH}" '
            f'width="{CW}" height="{CH}">'
            f'<defs><clipPath id="figbox">'
            f'<rect x="{fx}" y="{fy}" width="{fw}" height="{fh}"/>'
            f'</clipPath></defs>'
            f'<rect width="{CW}" height="{CH}" fill="{BG}"/>'
            f'<g clip-path="url(#figbox)">'
            f'<g transform="translate({self.tx:.3f},{self.ty:.3f}) scale({self.s:.4f})">'
            f'{self.body}{"".join(self.overlays)}</g></g>'
            f'{"".join(self.labels)}</svg>'
        )


# ------------------------------------------------------------------- figures

def build():
    figs = {}
    body = figure_image()

    def W(m):
        return wrap(m["y"], m["cx"], m["rx"])

    # Neck - just below the larynx
    f = Fig(body, (205, 28, 220, 293))
    f.add(W(NECK))
    f.add(dot(315, 180, 6))
    f.callout(358, 180, ["Adam\u2019s apple"], dy=-14)
    f.callout(358, 196, ["tape just below it"], dy=18)
    f.caption("Head straight, tape level \u2014 not sloping down at the front")
    figs["neck"] = f

    # Chest - across the nipple line
    f = Fig(body, (166, 105, 300, 400))
    f.add(W(CHEST))
    for nx, ny in NIPPLES:
        f.add(dot(nx, ny, 6))
    f.callout(CHEST["cx"] + CHEST["rx"], CHEST["y"], ["across the", "nipple line"], dy=0)
    f.caption("Arms relaxed; read at the end of a normal breath out")
    figs["chest"] = f

    # Waist - at the navel
    f = Fig(body, (166, 212, 300, 400))
    f.add(W(WAIST))
    f.add(dot(*NAVEL, 6))
    f.callout(NAVEL[0] + 8, NAVEL[1], ["navel"], dy=34)
    f.callout(WAIST["cx"] + WAIST["rx"], WAIST["y"], ["narrowest point,", "just above it"], dy=-24)
    f.caption("Breathe out; snug, without pulling the tape tight")
    figs["waist"] = f

    # Hip - widest point of the buttocks
    f = Fig(body, (166, 322, 300, 400))
    f.add(W(HIP))
    f.callout(HIP["cx"] + HIP["rx"], HIP["y"], ["widest point", "of the hips"], dy=0)
    f.caption("Feet together; check side-on to find the widest point")
    figs["hip"] = f

    # Arm - midway between shoulder and elbow
    f = Fig(body, (160, 190, 210, 280))
    f.add(W(BICEP))
    f.add(dot(199, 228, 6))
    f.add(dot(186, 425, 6))
    f.callout(199, 228, ["tip of shoulder"], dy=-14)
    f.callout(BICEP["cx"] + BICEP["rx"], BICEP["y"], ["midway between", "the two"], dy=0)
    f.callout(186, 425, ["elbow"], dy=14)
    f.caption("Arm hanging relaxed at your side")
    figs["arm"] = f

    # Thigh - just below the gluteal fold
    f = Fig(body, (172, 520, 240, 320))
    f.add(f'<path d="M212,{GLUTEAL_FOLD[1]} Q255,{GLUTEAL_FOLD[1]+14} 298,{GLUTEAL_FOLD[1]-2}" '
          f'fill="none" stroke="{GUIDE}" stroke-width="2.5" stroke-dasharray="7 5"/>')
    f.add(dot(*GLUTEAL_FOLD, 6))
    f.add(W(THIGH))
    f.callout(298, GLUTEAL_FOLD[1], ["gluteal fold \u2014 where the", "buttock meets the thigh"], dy=-20)
    f.callout(THIGH["cx"] + THIGH["rx"], THIGH["y"], ["tape just below it,", "around the widest part"], dy=18)
    f.caption("Weight even on both feet; tape level with the floor")
    figs["thigh"] = f

    # Calf - widest point of the lower leg
    f = Fig(body, (170, 716, 168, 224))
    f.add(W(CALF))
    f.add(dot(*KNEE, 6))
    f.callout(KNEE[0] + 22, KNEE[1], ["knee"], dy=-12)
    f.callout(CALF["cx"] + CALF["rx"], CALF["y"], ["widest part of", "the calf"], dy=6)
    f.caption("Stand with weight on both feet; tape level with the floor")
    figs["calf"] = f

    return figs


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    preview = os.environ.get("PREVIEW_DIR", out)
    os.makedirs(out, exist_ok=True)
    os.makedirs(preview, exist_ok=True)
    import cairosvg

    scale = 2  # ~920px wide: crisp in the dialog without a heavy bitmap
    for name, fig in build().items():
        doc = fig.render()
        # Only drop .svg previews when explicitly asked - res/ must contain
        # nothing aapt cannot parse.
        if os.environ.get("PREVIEW_DIR"):
            open(os.path.join(preview, f"{name}.svg"), "w").write(doc)
        cairosvg.svg2png(
            bytestring=doc.encode("utf-8"),
            write_to=os.path.join(out, f"measure_{name}.png"),
            output_width=CW * scale,
            output_height=CH * scale,
        )
        print(f"measure_{name}.png  {CW*scale}x{CH*scale}")


if __name__ == "__main__":
    main()
