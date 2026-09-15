#!/usr/bin/env python3
"""Draw the how-to-measure figures as SVG, render to PNG for visual checking,
then emit Android vector drawables from the same path data."""
import os, sys

BODY = "#90A4AE"      # blue grey - readable on light and dark
LIMB = "#7B8E9A"      # slightly darker so limbs read as behind the torso
TAPE = "#FF7043"      # deep orange - vivid against blue grey

# Head overlaps the neck so the two read as one shape.
HEAD = "M100,34 m-22,0 a22,22 0 1,0 44,0 a22,22 0 1,0 -44,0"

# Neck -> shoulders -> lats -> waist -> hips, mirrored about x=100
TORSO = (
    "M87,50 L87,80 "
    "C71,86 57,94 53,112 "
    "C51,132 55,150 59,158 "
    "C63,174 65,182 65,193 "
    "C61,207 57,219 57,236 "
    "C58,253 59,264 61,276 "
    "L139,276 "
    "C141,264 142,253 143,236 "
    "C143,219 139,207 135,193 "
    "C135,182 137,174 141,158 "
    "C145,150 149,132 147,112 "
    "C143,94 129,86 113,80 "
    "L113,50 Z"
)

# Arms start inside the shoulder so they attach rather than float.
ARM_L = (
    "M62,96 "
    "C48,106 41,124 39,146 "
    "C37,168 37,190 38,208 "
    "C38,218 40,226 43,231 "
    "C48,233 53,229 54,221 "
    "C55,202 56,180 59,158 "
    "C61,138 63,116 68,102 Z"
)
ARM_R = (
    "M138,96 "
    "C152,106 159,124 161,146 "
    "C163,168 163,190 162,208 "
    "C162,218 160,226 157,231 "
    "C152,233 147,229 146,221 "
    "C145,202 144,180 141,158 "
    "C139,138 137,116 132,102 Z"
)

# Standalone arm (deltoid -> bicep -> elbow -> forearm -> hand), so the band
# sits squarely on the bicep instead of straddling the torso edge.
ARM_ONLY = (
    "M60,16 "
    "C78,16 90,28 90,46 "
    "C90,70 88,96 84,120 "
    "C80,136 78,142 77,152 "
    "C78,172 76,196 72,220 "
    "C71,232 70,242 68,250 "
    "C64,257 56,257 52,250 "
    "C50,242 49,232 48,220 "
    "C44,196 42,172 43,152 "
    "C42,142 40,136 36,120 "
    "C32,96 30,70 30,46 "
    "C30,28 42,16 60,16 Z"
)

# Pelvis block, then two clearly separated thighs down past the knee.
PELVIS = (
    "M56,30 "
    "C54,52 56,72 62,88 "
    "L138,88 "
    "C144,72 146,52 144,30 Z"
)
THIGH_L = (
    "M58,86 "
    "C52,104 52,128 57,156 "
    "C61,186 65,214 68,242 "
    "L92,242 "
    "C93,212 95,182 95,152 "
    "C95,122 94,100 93,86 Z"
)
THIGH_R = (
    "M142,86 "
    "C148,104 148,128 143,156 "
    "C139,186 135,214 132,242 "
    "L108,242 "
    "C107,212 105,182 105,152 "
    "C105,122 106,100 107,86 Z"
)


def tape(y, x0, x1, bow=9):
    """Front band plus a faded arc suggesting the tape continuing around."""
    mid = (x0 + x1) / 2
    return (
        f'<path d="M{x0},{y} Q{mid},{y-bow} {x1},{y}" fill="none" '
        f'stroke="{TAPE}" stroke-width="5" stroke-linecap="round" opacity="0.35"/>'
        f'<path d="M{x0},{y} L{x1},{y}" fill="none" stroke="{TAPE}" '
        f'stroke-width="9" stroke-linecap="round"/>'
    )


def figure(parts, bands, view):
    body = "".join(f'<path d="{d}" fill="{c}"/>' for d, c in parts)
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" '
        f'viewBox="{view[0]} {view[1]} {view[2]} {view[3]}" '
        f'width="{view[2]}" height="{view[3]}">{body}{"".join(bands)}</svg>'
    )


UPPER = [(ARM_L, LIMB), (ARM_R, LIMB), (HEAD, BODY), (TORSO, BODY)]
LOWER = [(THIGH_L, LIMB), (THIGH_R, LIMB), (PELVIS, BODY)]
ARM = [(ARM_ONLY, BODY)]

FIGURES = {
    "neck":  (UPPER, [tape(74, 83, 117, 6)],   (18, 4, 164, 190)),
    "chest": (UPPER, [tape(122, 53, 147, 10)], (18, 4, 164, 210)),
    "waist": (UPPER, [tape(193, 63, 137, 9)],  (18, 40, 164, 230)),
    "hip":   (UPPER, [tape(240, 56, 144, 10)], (18, 80, 164, 210)),
    "arm":   (ARM, [tape(78, 31, 89, 7)],      (10, 0, 100, 270)),
    "thigh": (LOWER, [tape(122, 62, 96, 7)],   (40, 14, 120, 250)),
}

out = sys.argv[1] if len(sys.argv) > 1 else "."
os.makedirs(out, exist_ok=True)
import cairosvg

for name, (parts, bands, view) in FIGURES.items():
    svg = figure(parts, bands, view)
    p = os.path.join(out, f"{name}.svg")
    open(p, "w").write(svg)
    cairosvg.svg2png(url=p, write_to=os.path.join(out, f"{name}.png"),
                     output_width=view[2] * 2, output_height=view[3] * 2,
                     background_color="#1b1b1b")
print("rendered:", ", ".join(FIGURES))
