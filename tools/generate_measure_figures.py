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
# Outline figure in the style of a tailor's measurement chart: near-white fill
# with a firm outline, limbs drawn under the torso so the joints hide.

def _p(d, w=2.0, fill=None):
    return (f'<path d="{d}" fill="{fill or SKIN}" stroke="{EDGE}" '
            f'stroke-width="{w}" stroke-linejoin="round"/>')


def legs():
    left = _p(
        "M112,312 C106,342 104,374 106,406 C108,440 112,472 114,496 "
        "C115,508 116,516 118,522 L142,522 "
        "C143,508 144,488 145,464 C146,430 147,394 148,358 "
        "C149,342 150,330 150,324 Z"
    )
    right = _p(
        "M188,312 C194,342 196,374 194,406 C192,440 188,472 186,496 "
        "C185,508 184,516 182,522 L158,522 "
        "C157,508 156,488 155,464 C154,430 153,394 152,358 "
        "C151,342 150,330 150,324 Z"
    )
    return left + right


def arms():
    left = _p(
        "M100,120 C88,130 82,150 80,174 C78,200 78,226 80,248 "
        "C81,262 83,274 86,284 C90,291 98,290 101,283 "
        "C102,262 104,236 108,210 C111,184 115,152 119,132 Z"
    )
    right = _p(
        "M200,120 C212,130 218,150 220,174 C222,200 222,226 220,248 "
        "C219,262 217,274 214,284 C210,291 202,290 199,283 "
        "C198,262 196,236 192,210 C189,184 185,152 181,132 Z"
    )
    return left + right


def torso():
    return _p(
        "M118,98 C108,104 100,116 96,136 C93,158 96,182 104,204 "
        "C101,222 99,244 102,266 C104,288 108,304 112,316 L188,316 "
        "C192,304 196,288 198,266 C201,244 199,222 196,204 "
        "C204,182 207,158 204,136 C200,116 192,104 182,98 Z"
    )


def neck():
    return _p("M136,68 L134,100 L166,100 L164,68 Z")


def head():
    return (f'<ellipse cx="150" cy="44" rx="25" ry="31" fill="{SKIN}" '
            f'stroke="{EDGE}" stroke-width="2"/>')


def detail():
    """Interior anatomy lines: collarbone, pectoral crease, centre line."""
    d = lambda p, w=1.4: (f'<path d="{p}" fill="none" stroke="{DETAIL}" '
                          f'stroke-width="{w}" stroke-linecap="round"/>')
    return (
        d("M120,110 Q150,120 180,110")                 # clavicles
        + d("M106,146 Q128,166 148,156")               # left pec
        + d("M194,146 Q172,166 152,156")               # right pec
        + d("M150,158 L150,214", 1.2)                  # centre line
        + d("M134,182 Q150,186 166,182", 1.1)          # rib/ab hint
        + d("M136,198 Q150,202 164,198", 1.1)
    )


def upper_body():
    return legs() + arms() + torso() + neck() + head() + detail()


def upper_arm():
    """Single arm, shoulder to just past the elbow, for the arm guide."""
    return _p(
        "M150,26 C192,26 216,54 216,96 C216,150 210,208 200,264 "
        "C194,300 190,318 188,340 C188,360 186,378 182,394 "
        "C172,404 128,404 118,394 C114,378 112,360 112,340 "
        "C110,318 106,300 100,264 C90,208 84,150 84,96 "
        "C84,54 108,26 150,26 Z", 2.4
    ) + (f'<path d="M150,60 Q168,110 162,170" fill="none" stroke="{DETAIL}" '
         f'stroke-width="1.4" stroke-linecap="round"/>')


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
    f = Fig(upper_body(), (66, 8, 168, 140))
    f.add(f'<path d="M144,84 Q150,77 156,84" fill="none" stroke="{EDGE}" stroke-width="1.8"/>')
    f.add(wrap(96, 150, 17))
    f.add(dot(150, 83, 4))
    f.callout(157, 83, ["Adam\u2019s apple"], dy=-12)
    f.callout(167, 97, ["tape just below it"], dy=16)
    f.caption("Head straight, tape level \u2014 not sloping down at the front")
    figs["neck"] = f

    # Chest - across the nipple line
    f = Fig(upper_body(), (66, 70, 168, 140))
    f.add(wrap(152, 150, 54))
    f.add(dot(126, 161, 4.5) + dot(174, 161, 4.5))
    f.callout(204, 152, ["across the", "nipple line"], dy=0)
    f.caption("Arms relaxed; read at the end of a normal breath out")
    figs["chest"] = f

    # Waist - at the navel
    f = Fig(upper_body(), (66, 140, 168, 140))
    f.add(dot(150, 232, 4.5))
    f.add(dashed(232, 108, 192))
    f.add(wrap(210, 150, 46))
    f.callout(156, 232, ["navel"], dy=16)
    f.callout(196, 210, ["narrowest point,", "just above it"], dy=-14)
    f.caption("Breathe out; snug, without pulling the tape tight")
    figs["waist"] = f

    # Hip - widest point of the buttocks
    f = Fig(upper_body(), (66, 200, 168, 150))
    f.add(wrap(276, 150, 50))
    f.callout(200, 276, ["widest point", "of the hips"], dy=0)
    f.caption("Feet together; check side-on to find the widest point")
    figs["hip"] = f

    # Arm - midway between shoulder and elbow
    f = Fig(upper_arm(), (70, 10, 160, 410))
    f.add(dot(150, 40))
    f.add(dot(150, 350))
    f.add(wrap(196, 150, 64))
    f.callout(150, 40, ["tip of shoulder"], dy=-8)
    f.callout(150, 350, ["elbow"], dy=8)
    f.callout(214, 196, ["measure midway", "between the two"], dy=0)
    f.caption("Arm hanging relaxed at your side")
    figs["arm"] = f

    # Thigh - just below the gluteal fold
    f = Fig(upper_body(), (96, 296, 118, 180))
    f.add(f'<path d="M108,330 Q128,342 148,332" fill="none" stroke="{DETAIL}" '
          f'stroke-width="1.8" stroke-dasharray="6 4"/>')
    f.add(dot(127, 337, 4.5))
    f.add(wrap(362, 127, 21))
    f.callout(148, 333, ["gluteal fold \u2014 where the", "buttock meets the thigh"], dy=-18)
    f.callout(148, 362, ["tape just below it,", "around the widest part"], dy=16)
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
