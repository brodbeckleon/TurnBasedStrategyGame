"""Compose terrain tiles from the existing hand-drawn 16x16 shoreline pieces by quadrant (8x8 corner blocks).
Tile name = PLAIN_<N><E><S><W>[_<NE><SE><SW><NW>] with P/W per neighbour (see GameView.getTerrainImagePath)."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pngio import read_png, write_png
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
T = os.path.join(ROOT, 'app', 'src', 'main', 'resources', 'images', 'terrain') + os.sep
_cache = {}
def ld(n):
    if n not in _cache:
        w, h, px = read_png(T + n + '.png'); assert (w, h) == (16, 16), (n, w, h); _cache[n] = px
    return _cache[n]
BASE = 'PLAIN_PPPP_PPPP'
# quadrant -> {(edgeA_water, edgeB_water): source}; for quadrants with no adjacent water edge: diag source
QUAD = {   # (x0,y0) of the 8x8 block, adjacent edges
 'NW': ((0, 0), 'NW'), 'NE': ((8, 0), 'NE'), 'SE': ((8, 8), 'SE'), 'SW': ((0, 8), 'SW')}
def source_for(q, N, E, S, W, NE, SE, SW, NW):
    if q == 'NW':
        a, b = N, W; both, onlyA, onlyB, diag = 'PLAIN_WPPW', 'PLAIN_WPPP', 'PLAIN_PPPW', ('PLAIN_PPPP_PPPW', NW)
    elif q == 'NE':
        a, b = N, E; both, onlyA, onlyB, diag = 'PLAIN_WWPP', 'PLAIN_WPPP', 'PLAIN_PWPP', ('PLAIN_PPPP_WPPP', NE)
    elif q == 'SE':
        a, b = E, S; both, onlyA, onlyB, diag = 'PLAIN_PWWP', 'PLAIN_PWPP', 'PLAIN_PPWP', ('PLAIN_PPPP_PWPP', SE)
    else:
        a, b = S, W; both, onlyA, onlyB, diag = 'PLAIN_PPWW', 'PLAIN_PPWP', 'PLAIN_PPPW', ('PLAIN_PPPP_PPWP', SW)
    if a and b: return both
    if a: return onlyA
    if b: return onlyB
    return diag[0] if diag[1] else BASE
def compose(name):
    parts = name.split('_')
    e = parts[1]; d = parts[2] if len(parts) > 2 else 'PPPP'
    N, E, S, W = [c == 'W' for c in e]; NE, SE, SW, NW = [c == 'W' for c in d]
    out = [list(r) for r in ld(BASE)]
    for q, ((x0, y0), _) in QUAD.items():
        src = ld(source_for(q, N, E, S, W, NE, SE, SW, NW))
        for y in range(y0, y0 + 8):
            for x in range(x0, x0 + 8): out[y][x] = src[y][x]
    return out
