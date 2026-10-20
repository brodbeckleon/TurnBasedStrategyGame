NAME = "A-10"
KIND = "air"

# A-10 Thunderbolt II, nose RIGHT, pseudo-3/4 from above (depth axis = down-left).
#  - fuselage side-on (bright spine, mid flank, dark belly), bubble canopy, grey chin gun bay,
#    fat GAU-8 housing tapering to a 2-row muzzle that touches the nose
#  - near wing: LONG wide-chord straight wing drawn OVER the lower fuselage and projecting down-left,
#    lighter than the body, olive-grey camo blotches, team-colour tip stripe, stores off the leading edge
#  - two dark engine pods hugging the upper rear fuselage (near pod in front, far pod peeking above)
#  - H-tail: near fin + far fin with team-coloured rudders, small stabiliser trapezoid under the near fin
#  - fins/pods start at y=7 (outline y=6) so the HP bar (y=0..4) stays clear; soft shadow on rows 29-30

WC, WD, WS, WX, WY = 10, 8, 1.25, 13, 19    # wing chord, depth rows, shear px/row, root left x, root top y


def part(c, fn, edge='k'):
    """Draw fn on a scratch canvas and paste it over c.  Where it covers earlier parts, a 1px `edge`
    line is drawn on the pixels just outside it (so overlaps get ONE clean line); the final
    c.outline('k') then handles the silhouette."""
    t = type(c)()
    fn(t)
    if edge:
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


def wing_x(d):
    sh = int(d * WS + 0.5)
    return WX - sh, WX + WC - 1 - sh


def far_fin(t):
    for i, r in enumerate(['TTol', 'ttol', 'ttol', 'oool', 'ooool']):
        t.stamp(8, 7 + i, [r])


def far_pod(t):
    t.stamp(12, 7, ['.SgGgSs.',
                    'KSSSSssK',
                    '.sssssK.'])


def store(t, x, y):
    t.stamp(x, y, ['KSSSSg', 'KsssGg'])


def wing(t):
    for d in range(WD):
        y = WY + d
        x0, x1 = wing_x(d)
        t.hline(x0, x1, y, 'l')
        t.px(x1, y, 'L')                                # leading edge highlight
        t.hline(x0, x0 + 1, y, 'O')                     # trailing-edge shade
    x0, x1 = wing_x(WD - 1)
    t.hline(x0, x1, WY + WD - 1, 't')                   # team wing tip stripe
    for d, a, b in ((1, 4, 7), (2, 3, 6), (5, 4, 7), (6, 3, 5)):             # one olive camo blotch
        x0, _ = wing_x(d)
        t.hline(x0 + a, x0 + b, WY + d, 'O')


def fuselage(t):
    rows = {14: (19, 24), 15: (7, 25), 16: (5, 25), 17: (4, 26), 18: (4, 26), 19: (6, 26), 20: (22, 25)}
    for y, (x0, x1) in rows.items():
        t.hline(x0, x1, y, 'O')
    t.hline(19, 24, 14, 'L'); t.hline(7, 25, 15, 'l'); t.hline(5, 25, 16, 'l')
    t.hline(4, 25, 17, 'O')
    t.hline(4, 21, 18, 'o'); t.hline(6, 21, 19, 'o')
    t.hline(22, 26, 18, 'S'); t.hline(22, 26, 19, 'S'); t.hline(22, 25, 20, 's')  # grey gun bay under the nose
    t.hline(23, 25, 19, 's')
    t.hline(21, 24, 16, 'L'); t.px(25, 15, 'L')                                   # lit cheek behind the nose


def near_fin(t):
    rows = ['TTOl', 'ttOl', 'ttOl', 'ttOOl', 'ttOOl', 'ttOOl', 'ttOOOl', 'ttOOOl', 'uuOOOl']
    for i, r in enumerate(rows):
        t.stamp(2, 9 + i, [r])


def tailplane(t):
    # small trapezoid at the tail, in front of the fin base
    t.hline(4, 10, 18, 'l'); t.hline(3, 9, 19, 'O'); t.hline(2, 8, 20, 'o')


def near_pod(t):
    t.stamp(8, 11, ['.SgGGgSs.',
                    'KSSSSSgGK',
                    'KsssssssK',
                    '.KKKKKK..'])


def canopy(t):
    t.stamp(18, 10, ['..yyyy..',
                     '.yywYYY.',
                     'yyyYsYYY',
                     'YYYYsYYY'])


def draw(c):
    part(c, far_fin)
    part(c, far_pod)
    for d in (2, 5):
        x1, y = wing_x(d)[1], WY + d
        part(c, lambda t, x1=x1, y=y: store(t, x1 - 2, y + 1))
    part(c, fuselage)
    part(c, wing, edge='o')
    part(c, near_fin)
    part(c, tailplane)
    part(c, near_pod)
    part(c, canopy)
    c.outline('k')
    # close 1px pinholes left between neighbouring outlines
    for y in range(c.h):
        for x in range(c.w):
            if c.g[y][x] == '.' and all(c.get(x + dx, y + dy) not in '.~' for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                c.g[y][x] = 'k'
    # GAU-8: fat 3-row cannon housing tapering to a 2-row muzzle, touching the nose directly
    c.stamp(27, 16, ['kkk..',
                     'Gggkk',
                     'gSSGg',
                     'ssssK',
                     'kkkkk'])
    c.shadow(9, 22, 29); c.shadow(11, 20, 30)
