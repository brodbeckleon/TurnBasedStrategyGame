NAME = "Forest"
KIND = "neutral"       # terrain overlay drawn on top of the plain grass tile: no team colour, no HP bar

# A stand of five dark-green conifers filling the cell, transparent between them so the grass shows.
# Two tall trees at the back (tops y=1/2), three in front (trunks resting on y=27..29).  Each tree is
# built from stacked, widening tiers: lit (light) left edge, dark-green body, deep-shadow right side
# and a shadow row under every tier.  Trees are pasted back-to-front with a 1px dark edge between
# overlapping crowns so every tree reads on its own.

def conifer(t, cx, top, h, trunk=2):
    """Conifer with its tip at (cx, top), h foliage rows, then `trunk` rows of brown trunk."""
    for r in range(h):
        hw = min(r // 4 + r % 4, 6)
        y = top + r
        tier_bottom = (r % 4 == 3) or r == h - 1
        for x in range(cx - hw, cx + hw + 1):
            d = x - cx
            if tier_bottom and d > -hw: k = 'v' if d > 0 else 'V'
            elif d <= -hw + 1 and hw >= 2: k = 'X' if d == -hw + 1 else 'x'
            elif d < 0: k = 'x' if d < -hw // 2 else 'V'
            elif d == 0: k = 'V'
            else: k = 'v' if d >= hw // 2 + 1 else 'V'
            t.px(x, y, k)
        if hw == 0: t.px(cx, y, 'X')
    for i in range(trunk):
        t.px(cx, top + h + i, 'N'); t.px(cx + 1, top + h + i, 'n')


def part(c, fn, edge='k'):
    t = type(c)()
    fn(t)
    for y in range(t.h):
        for x in range(t.w):
            if t.g[y][x] != '.':
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if t.inb(nx, ny) and t.g[ny][nx] != '.' and c.g[y][x] not in '.~':
                    c.g[y][x] = edge
                    break
    for y in range(t.h):
        for x in range(t.w):
            if t.g[y][x] != '.':
                c.g[y][x] = t.g[y][x]


def draw(c):
    part(c, lambda t: conifer(t, 9, 1, 16))       # back left, tall
    part(c, lambda t: conifer(t, 23, 2, 15))      # back right
    part(c, lambda t: conifer(t, 4, 12, 15))      # front left, trunk to y=28
    part(c, lambda t: conifer(t, 27, 13, 14))     # front right, trunk to y=28
    part(c, lambda t: conifer(t, 15, 9, 18, 3))   # centre front, trunk to y=29
    c.outline('k')
    c.shadow(1, 30, 30); c.shadow(2, 29, 29)
