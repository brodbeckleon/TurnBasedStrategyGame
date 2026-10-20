"""pxart - tiny stdlib-only pixel-art toolkit for the TurnBasedStrategyGame sprites (see ../build.py).

A sprite is a Python file that defines
    NAME = "Type 90"                 # exact in-game unit name -> file name  images/units/playerN/<NAME>.png
    KIND = "ground" | "air" | "sea"  # decides which validation rules apply
    def draw(c): ...                 # c is a Canvas (32x32); draw facing RIGHT

Coordinates: x grows right, y grows down, 0-based, inclusive end points. Colours are single-character palette keys (see PALETTE).
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pngio import read_png, write_png, upscale, blit, canvas

W = H = 32

def _h(s): return (int(s[1:3], 16), int(s[3:5], 16), int(s[5:7], 16), 255)

# key: (rgba, description).  TEAM keys (t T u) are recoloured per player at render time.
PALETTE = {
 '.': (None,           'transparent'),
 'k': (_h('#201829'),  'outline / deepest shadow (same colour the terrain rocks are outlined with)'),
 'K': (_h('#3b3447'),  'dark violet-grey: tracks, tyres, rubber, deep recesses'),
 's': (_h('#444d5c'),  'steel shadow'),
 'S': (_h('#66727f'),  'steel mid'),
 'g': (_h('#8e9aa6'),  'light steel / grey paint'),
 'G': (_h('#bcc6cf'),  'pale grey highlight'),
 'w': (_h('#f2f6f8'),  'near-white: specular, white paint, foam'),
 'o': (_h('#3d4a2b'),  'olive shadow'),
 'O': (_h('#5a6b36'),  'olive mid (JGSDF green)'),
 'l': (_h('#7d8f48'),  'olive light'),
 'L': (_h('#a6b865'),  'olive highlight'),
 'n': (_h('#4b3626'),  'dark brown (camo patch, wood shadow)'),
 'N': (_h('#75553a'),  'brown (camo patch, wood)'),
 'b': (_h('#a07b4f'),  'tan / sand'),
 'B': (_h('#c9a577'),  'tan highlight'),
 'e': (_h('#2b4f45'),  'teal-green shadow (Type 10 camo)'),
 'E': (_h('#4c7a68'),  'teal-green mid'),
 'p': (_h('#7aa592'),  'teal-green light'),
 'm': (_h('#2f3d54'),  'navy shadow'),
 'M': (_h('#4a5d78'),  'navy / dark haze grey'),
 'h': (_h('#7b8fa6'),  'haze grey (ship hull light)'),
 'a': (_h('#1e7cb8'),  'water (same as terrain water)'),
 'A': (_h('#165c88'),  'deep water shadow'),
 'c': (_h('#5aaee4'),  'light water / wake'),
 'y': (_h('#7fd6ea'),  'glass / canopy light'),
 'Y': (_h('#2f6f8f'),  'glass / canopy dark'),
 'f': (_h('#ffd23f'),  'yellow: muzzle flash, lamps'),
 'F': (_h('#ff8a2a'),  'orange: exhaust glow, flame'),
 'r': (_h('#d9382c'),  'fixed red (red cross, warning marks) - NOT the team colour'),
 'R': (_h('#8e1f26'),  'dark fixed red'),
 't': (_h('#2f6fe0'),  'TEAM main  (P1 blue / P2 red)'),
 'T': (_h('#78a6ff'),  'TEAM light'),
 'u': (_h('#1c3f9a'),  'TEAM dark'),
 '~': ((0, 0, 0, 80),  'soft ground / water shadow (only semi-transparent colour allowed)'),
}
TEAM_COLORS = {
 1: {'t': _h('#2f6fe0'), 'T': _h('#78a6ff'), 'u': _h('#1c3f9a')},
 2: {'t': _h('#e0402f'), 'T': _h('#ff8a72'), 'u': _h('#8f1d1d')},
}
TEAM_KEYS = 'tTu'

class Canvas:
    def __init__(self, w=W, h=H):
        self.w, self.h = w, h
        self.g = [['.'] * w for _ in range(h)]

    # ---- single pixels -------------------------------------------------
    def inb(self, x, y): return 0 <= x < self.w and 0 <= y < self.h
    def get(self, x, y): return self.g[y][x] if self.inb(x, y) else '.'
    def px(self, x, y, k):
        assert k in PALETTE, f"unknown palette key {k!r}"
        if self.inb(x, y): self.g[y][x] = k
    def erase(self, x, y): self.px(x, y, '.')

    # ---- primitives ----------------------------------------------------
    def rect(self, x0, y0, x1, y1, k):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for x in range(min(x0, x1), max(x0, x1) + 1): self.px(x, y, k)
    def frame(self, x0, y0, x1, y1, k):
        self.hline(x0, x1, y0, k); self.hline(x0, x1, y1, k); self.vline(x0, y0, y1, k); self.vline(x1, y0, y1, k)
    def hline(self, x0, x1, y, k): self.rect(x0, y, x1, y, k)
    def vline(self, x, y0, y1, k): self.rect(x, y0, x, y1, k)
    def line(self, x0, y0, x1, y1, k):
        dx, dy = abs(x1 - x0), -abs(y1 - y0); sx = 1 if x0 < x1 else -1; sy = 1 if y0 < y1 else -1; err = dx + dy
        while True:
            self.px(x0, y0, k)
            if x0 == x1 and y0 == y1: break
            e2 = 2 * err
            if e2 >= dy: err += dy; x0 += sx
            if e2 <= dx: err += dx; y0 += sy
    def ellipse(self, cx, cy, rx, ry, k, filled=True):
        """Pixel ellipse centred on the *pixel* (cx,cy) with half-widths rx, ry (so total width = 2*rx+1)."""
        for y in range(int(cy - ry), int(cy + ry) + 1):
            for x in range(int(cx - rx), int(cx + rx) + 1):
                v = ((x - cx) / (rx + .5)) ** 2 + ((y - cy) / (ry + .5)) ** 2
                if (v <= 1.0) if filled else (0.62 <= v <= 1.0): self.px(x, y, k)
    def poly(self, pts, k):
        """Filled polygon (even-odd, sampled at pixel centres). pts = [(x,y),...] in pixel coordinates."""
        ys = [p[1] for p in pts]; n = len(pts)
        for y in range(int(min(ys)), int(max(ys)) + 1):
            yc = y + 0.5; xs = []
            for i in range(n):
                (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % n]
                if (y0 <= yc < y1) or (y1 <= yc < y0):
                    xs.append(x0 + (yc - y0) * (x1 - x0) / (y1 - y0))
            xs.sort()
            for i in range(0, len(xs) - 1, 2):
                for x in range(math.ceil(xs[i] - 0.5), math.ceil(xs[i + 1] - 0.5)): self.px(x, y, k)
    def fill(self, x, y, k):
        """4-connected flood fill of the region containing (x,y)."""
        if not self.inb(x, y): return
        old = self.g[y][x]
        if old == k: return
        st = [(x, y)]
        while st:
            a, b = st.pop()
            if self.inb(a, b) and self.g[b][a] == old:
                self.g[b][a] = k; st += [(a + 1, b), (a - 1, b), (a, b + 1), (a, b - 1)]
    def stamp(self, x, y, rows, skip='. '):
        """Paste an ASCII patch with its top-left at (x,y). Characters in `skip` leave the canvas untouched; every other char is a palette key."""
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch not in skip: self.px(x + i, y + j, ch)
    def replace(self, old, new, box=None):
        x0, y0, x1, y1 = box or (0, 0, self.w - 1, self.h - 1)
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if self.get(x, y) == old: self.px(x, y, new)

    # ---- whole-sprite operations --------------------------------------
    def outline(self, k='k', diagonals=False):
        """Add a 1px outline in colour k around every opaque pixel (never around the soft '~' shadow)."""
        add = []
        nb = [(1, 0), (-1, 0), (0, 1), (0, -1)] + ([(1, 1), (1, -1), (-1, 1), (-1, -1)] if diagonals else [])
        for y in range(self.h):
            for x in range(self.w):
                if self.g[y][x] != '.': continue
                for dx, dy in nb:
                    if self.inb(x + dx, y + dy) and self.g[y + dy][x + dx] not in '.~': add.append((x, y)); break
        for x, y in add: self.g[y][x] = k
    def shadow(self, x0, x1, y, k='~'):
        """Soft ground shadow: only paints on transparent pixels of row y."""
        for x in range(x0, x1 + 1):
            if self.inb(x, y) and self.g[y][x] == '.': self.g[y][x] = k
    def hflip(self):
        self.g = [row[::-1] for row in self.g]
    def shift(self, dx, dy):
        n = [['.'] * self.w for _ in range(self.h)]
        for y in range(self.h):
            for x in range(self.w):
                if self.g[y][x] != '.' and self.inb(x + dx, y + dy): n[y + dy][x + dx] = self.g[y][x]
        self.g = n
    def dump(self):
        """The sprite as text (one palette key per pixel) with a column ruler - paste-able back into stamp()."""
        out = ['    ' + ''.join(str(i // 10) if i % 10 == 0 else ' ' for i in range(self.w)), '    ' + ''.join(str(i % 10) for i in range(self.w))]
        for y, row in enumerate(self.g): out.append(f'{y:2d}  ' + ''.join(row))
        return '\n'.join(out)

    def pixels(self, team=1):
        tc = TEAM_COLORS[team]
        out = []
        for row in self.g:
            r = []
            for k in row:
                if k in tc: r.append(tc[k])
                else:
                    v = PALETTE[k][0]; r.append(v if v else (0, 0, 0, 0))
            out.append(r)
        return out

# ---------------------------------------------------------------------------
def load_sprite(path):
    ns = {'__file__': path, '__name__': 'sprite'}
    exec(compile(open(path).read(), path, 'exec'), ns)
    c = Canvas(); ns['draw'](c)
    return ns.get('NAME', os.path.splitext(os.path.basename(path))[0]), ns.get('KIND', 'ground'), c

def components(c):
    """8-connected components of opaque (non-shadow) pixels -> list of sets."""
    seen = set(); comps = []
    for y in range(c.h):
        for x in range(c.w):
            if c.g[y][x] in '.~' or (x, y) in seen: continue
            st = [(x, y)]; comp = set()
            while st:
                a, b = st.pop()
                if (a, b) in seen or not c.inb(a, b) or c.g[b][a] in '.~': continue
                seen.add((a, b)); comp.add((a, b))
                st += [(a + i, b + j) for i in (-1, 0, 1) for j in (-1, 0, 1) if i or j]
            comps.append(comp)
    return comps

def validate(c, kind):
    """Return (errors, warnings) - errors must be fixed, warnings should be looked at."""
    err, warn = [], []
    opaque = [(x, y) for y in range(c.h) for x in range(c.w) if c.g[y][x] not in '.~']
    if not opaque: return ['sprite is empty'], []
    xs = [p[0] for p in opaque]; ys = [p[1] for p in opaque]
    bw, bh = max(xs) - min(xs) + 1, max(ys) - min(ys) + 1
    if bw < 22: warn.append(f'silhouette only {bw}px wide - sprites should fill the cell (target 26-31px wide)')
    if bh < 11: warn.append(f'silhouette only {bh}px tall')
    top = min(ys)
    if kind in ('ground', 'sea') and top < 5: warn.append(f'topmost pixel at y={top}: rows 0-4 are covered by the HP bar - keep detail below y=5')
    if kind == 'air' and top < 5: warn.append(f'topmost pixel at y={top}: rows 0-4 are covered by the HP bar - keep the aircraft below y=5')
    bot = max(ys)
    if kind == 'ground' and not (26 <= bot <= 30): warn.append(f'lowest opaque pixel at y={bot}: ground vehicles should rest on y=27..29 (wheels/tracks), shadow on y=28..30')
    if kind == 'sea' and not (24 <= bot <= 30): warn.append(f'lowest opaque pixel at y={bot}: ships should sit with the waterline around y=25..29')
    team = sum(1 for p in opaque if c.g[p[1]][p[0]] in TEAM_KEYS)
    if team < 6: err.append(f'only {team} team-colour pixels (t/T/u) - need at least 6 so the owner is readable at 1:1')
    if team > 90: warn.append(f'{team} team-colour pixels - team colour should be an accent, not the main paint')
    if not any(c.g[y][x] == 'k' for y in range(c.h) for x in range(c.w)): err.append("no outline pixels: call c.outline('k') (or draw an outline)")
    comps = components(c)
    if len(comps) > 1:
        small = sorted([len(s) for s in comps])[:-1]
        stray = [s for s in comps if len(s) <= 2]
        if stray: warn.append(f'{len(stray)} stray 1-2px fragments disconnected from the body, e.g. at {sorted(stray[0])[0]} (intentional? flag tips/antennas are fine only if connected)')
    # a lone 'k' that is NOT adjacent to a colour is an outline artifact
    return err, warn

# ---------------------------------------------------------------------------
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))   # repo root (resources/pixel-art/tools/..)
IMG = os.path.join(ROOT, 'app', 'src', 'main', 'resources', 'images') + os.sep
def _tile(name, size=32):
    w, h, px = read_png(IMG + 'terrain/' + name + '.png')
    k = size // w
    return upscale(px, k) if k > 1 else px

def preview(c, name, kind, out_png, label=''):
    """One compact sheet: P1 x8 / P2 (mirrored, as the game shows it) x8 on a checker, then in-context at x3 on grass, on a shoreline, with the game's HP bar."""
    S = 8; P = 10
    sheet_w = P + 32 * S + P + 32 * S + P + 32 * 3 * 2 + P * 2 + P
    sheet_h = P + 32 * S + P
    bg = (44, 46, 54, 255)
    sh = canvas(sheet_w, sheet_h, bg)
    def checker(ox, oy, k):
        for y in range(32 * k):
            for x in range(32 * k):
                v = 86 if ((x // (k * 2) + y // (k * 2)) % 2) else 72
                sh[oy + y][ox + x] = (v, v, v + 6, 255)
    p1 = c.pixels(1); p2 = [r[::-1] for r in c.pixels(2)]
    x = P; checker(x, P, S); blit(sh, upscale(p1, S), x, P)
    x += 32 * S + P; checker(x, P, S); blit(sh, upscale(p2, S), x, P)
    # in-context: 2 tiles wide x 1 tall of terrain at x3 -> sprite on tile 1, enemy mirrored on tile 2
    x += 32 * S + P
    ctx_y = P
    row_tiles = [[_tile('PLAIN_PPPP_PPPP'), _tile('PLAIN_PPPP_PPPP')], [_tile('PLAIN_PPWP'), _tile('WATER')]]
    if kind == 'sea': row_tiles = [[_tile('WATER'), _tile('WATER')], [_tile('WATER'), _tile('PLAIN_WPPP')]]
    for ry, rowt in enumerate(row_tiles):
        for rx, tpx in enumerate(rowt):
            blit(sh, upscale(tpx, 3), x + rx * 96, ctx_y + ry * 96)
    # units: p1 on tile (0,0); mirrored p2 on tile (1,0); p1 on tile (0,1) shore/second tile; p2 on (1,1)
    bar_w = 28
    def hp_bar(ox, oy, frac, col):
        for yy in range(5 * 3):
            for xx in range(bar_w * 3):
                sh[oy + yy][ox + 2 * 3 + xx] = (60, 60, 60, 255) if xx >= frac * bar_w * 3 else col
    cells = [(0, 0, p1, 1.0, (0, 128, 0, 255)), (1, 0, p2, 0.55, (255, 165, 0, 255)), (0, 1, p1, 0.2, (255, 0, 0, 255)), (1, 1, p2, 1.0, (0, 128, 0, 255))]
    for cx, cy, spx, frac, col in cells:
        ox, oy = x + cx * 96, ctx_y + cy * 96
        blit(sh, upscale(spx, 3), ox, oy)
        hp_bar(ox, oy, frac, col)
    # tiny 1:1 strip under the context block
    tiny_y = ctx_y + 2 * 96 + 8
    blit(sh, p1, x, tiny_y); blit(sh, p2, x + 40, tiny_y)
    blit(sh, upscale(p1, 2), x + 90, tiny_y)
    write_png(out_png, sh)
    return out_png

def render_all(sprite_path, outdir, make_preview=True):
    os.makedirs(outdir, exist_ok=True)
    name, kind, c = load_sprite(sprite_path)
    err, warn = validate(c, kind)
    final = os.path.join(os.path.dirname(os.path.abspath(outdir)), 'final')     # named PNGs live next to (not inside) the blind-review dir
    os.makedirs(final, exist_ok=True)
    base = os.path.join(final, name)
    write_png(base + '.p1.png', c.pixels(1)); write_png(base + '.p2.png', c.pixels(2))
    if make_preview: preview(c, name, kind, os.path.join(outdir, 'preview.png'))
    return name, kind, c, err, warn

if __name__ == '__main__':
    import argparse
    ap = argparse.ArgumentParser(description='Render + validate a sprite script.')
    ap.add_argument('sprite'); ap.add_argument('--out', required=True); ap.add_argument('--dump', action='store_true')
    a = ap.parse_args()
    name, kind, c, err, warn = render_all(a.sprite, a.out)
    print(f'{name} ({kind}) -> {a.out}/preview.png ; final PNGs in {os.path.join(os.path.dirname(os.path.abspath(a.out)), "final")}/')
    for e in err: print('ERROR  :', e)
    for w in warn: print('WARNING:', w)
    if a.dump: print(c.dump())
    if not err and not warn: print('validation: clean')
    sys.exit(1 if err else 0)
