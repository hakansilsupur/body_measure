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

BG      = "#F4F6F7"
SKIN    = "#B9C6CD"
SKIN_DK = "#9DAEB7"
EDGE    = "#6E828D"
TAPE    = "#F4511E"
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

def head():
    return f'<ellipse cx="150" cy="48" rx="27" ry="33" fill="{SKIN}" stroke="{EDGE}" stroke-width="2"/>'


def torso():
    return (
        f'<path d="M136,72 L136,100 '
        f'C118,107 100,116 94,133 C89,150 91,171 98,188 '
        f'C104,204 108,209 110,219 C106,234 102,247 102,264 '
        f'C102,283 104,298 108,311 L192,311 '
        f'C196,298 198,283 198,264 C198,247 194,234 190,219 '
        f'C192,209 196,204 202,188 C209,171 211,150 206,133 '
        f'C200,116 182,107 164,100 L164,72 Z" '
        f'fill="{SKIN}" stroke="{EDGE}" stroke-width="2"/>'
    )


def arms():
    return (
        f'<path d="M96,126 C80,137 71,159 69,186 C67,213 67,240 68,260 '
        f'C68,273 70,282 74,289 C80,292 86,288 87,278 '
        f'C88,255 90,228 94,201 C97,176 101,149 105,134 Z" '
        f'fill="{SKIN_DK}" stroke="{EDGE}" stroke-width="2"/>'
        f'<path d="M204,126 C220,137 229,159 231,186 C233,213 233,240 232,260 '
        f'C232,273 230,282 226,289 C220,292 214,288 213,278 '
        f'C212,255 210,228 206,201 C203,176 199,149 195,134 Z" '
        f'fill="{SKIN_DK}" stroke="{EDGE}" stroke-width="2"/>'
    )


def legs():
    """Hips and legs as one connected shape with a crotch notch."""
    return (
        f'<path d="M104,300 '
        f'C100,340 100,378 104,414 C108,450 112,486 114,516 '
        f'L148,516 C149,470 150,420 150,372 C150,340 150,322 150,312 '
        f'L150,306 L150,312 C150,322 150,340 150,372 '
        f'C150,420 151,470 152,516 L186,516 '
        f'C188,486 192,450 196,414 C200,378 200,340 196,300 Z" '
        f'fill="{SKIN_DK}" stroke="{EDGE}" stroke-width="2"/>'
    )


def lower_body():
    """Pelvis + both thighs as one silhouette, with a wide crotch notch so the
    legs read as two limbs rather than a pair of trousers."""
    return (
        f'<path d="M96,60 '
        f'C92,104 94,142 100,170 C96,216 96,262 100,312 '
        f'C104,348 108,382 110,412 L138,412 '
        f'C139,360 140,300 141,244 C141,212 141,196 142,186 '
        f'L150,172 L158,186 '
        f'C159,196 159,212 159,244 C160,300 161,360 162,412 '
        f'L190,412 C192,382 196,348 200,312 '
        f'C204,262 204,216 200,170 C206,142 208,104 204,60 Z" '
        f'fill="{SKIN}" stroke="{EDGE}" stroke-width="2.5"/>'
    )


def upper_arm():
    """Shoulder down to just past the elbow."""
    return (
        f'<path d="M150,26 C192,26 216,54 216,96 '
        f'C216,150 210,208 200,264 C194,300 190,318 188,340 '
        f'C188,360 186,378 182,394 C172,404 128,404 118,394 '
        f'C114,378 112,360 112,340 C110,318 106,300 100,264 '
        f'C90,208 84,150 84,96 C84,54 108,26 150,26 Z" '
        f'fill="{SKIN}" stroke="{EDGE}" stroke-width="2.5"/>'
    )


# ------------------------------------------------------------------ overlays

def band(y, x0, x1, bow=7):
    mid = (x0 + x1) / 2
    return (
        f'<path d="M{x0},{y} Q{mid},{y-bow} {x1},{y}" fill="none" stroke="{TAPE}" '
        f'stroke-width="4" stroke-linecap="round" opacity="0.4"/>'
        f'<path d="M{x0},{y} L{x1},{y}" fill="none" stroke="{TAPE}" '
        f'stroke-width="9" stroke-linecap="round"/>'
    )


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
            f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {CW} {CH}" '
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

    # Neck - just below the larynx
    f = Fig(arms() + head() + torso(), (60, 10, 180, 150))
    f.add(f'<path d="M143,84 Q150,77 157,84" fill="none" stroke="{EDGE}" stroke-width="2"/>')
    f.add(dot(150, 83, 4.5))
    f.add(band(98, 131, 169, 5))
    f.callout(157, 83, ["Adam’s apple"], dy=-10)
    f.callout(169, 98, ["tape sits just", "below it"], dy=14)
    f.caption("Head straight, tape level — not sloping down at the front")
    figs["neck"] = f

    # Chest - across the nipple line
    f = Fig(arms() + head() + torso(), (60, 70, 180, 150))
    f.add(dashed(150, 98, 202))
    f.add(band(150, 94, 206, 8))
    f.add(dot(126, 150, 4.5) + dot(174, 150, 4.5))
    f.callout(206, 150, ["across the", "nipple line"], dy=0)
    f.caption("Arms relaxed; read at the end of a normal breath out")
    figs["chest"] = f

    # Waist - at the navel
    f = Fig(arms() + head() + torso(), (60, 150, 180, 150))
    f.add(dot(150, 212))
    f.add(dashed(212, 106, 194))
    f.add(band(199, 107, 193, 7))
    f.callout(156, 212, ["navel"], dy=16)
    f.callout(193, 199, ["narrowest point,", "just above it"], dy=-12)
    f.caption("Breathe out; snug, without pulling the tape tight")
    figs["waist"] = f

    # Hip - widest point of the buttocks
    f = Fig(arms() + legs() + head() + torso(), (60, 230, 180, 150))
    f.add(band(300, 101, 199, 8))
    f.callout(199, 300, ["widest point", "of the hips"], dy=0)
    f.caption("Feet together; check side-on in a mirror to find the widest point")
    figs["hip"] = f

    # Arm - midway between shoulder and elbow
    f = Fig(upper_arm(), (70, 10, 160, 410))
    f.add(dot(150, 40))
    f.add(dot(150, 350))
    f.add(dashed(196, 86, 214))
    f.add(band(196, 88, 212, 6))
    f.callout(150, 40, ["tip of shoulder"], dy=-6)
    f.callout(150, 350, ["elbow"], dy=6)
    f.callout(212, 196, ["measure midway", "between the two"], dy=0)
    f.caption("Arm hanging relaxed at your side")
    figs["arm"] = f

    # Thigh - just below the gluteal fold
    f = Fig(lower_body(), (88, 52, 124, 372))
    f.add(f'<path d="M101,184 Q121,196 140,186" fill="none" stroke="{EDGE}" '
          f'stroke-width="2" stroke-dasharray="6 4"/>')
    f.add(dot(121, 191))
    f.add(band(214, 99, 141, 6))
    f.callout(140, 187, ["gluteal fold \u2014 where the", "buttock meets the thigh"], dy=-16)
    f.callout(141, 214, ["tape just below it,", "around the widest part"], dy=18)
    f.caption("Weight even on both feet; tape level with the floor")
    figs["thigh"] = f

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
