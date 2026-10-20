NAME = "Infantry"
KIND = "ground"

# JGSDF rifle fireteam of three, facing RIGHT: rear rifleman standing (left, feet y=27),
# second rifleman standing (feet y=28), point man kneeling in front (right, ground y=29) aiming forward.
# Olive/brown camo fatigues, olive helmets with a team-colour band, tan faces, black Type 89 rifles.
# Figures are pasted back-to-front; an overlapping figure gets a 1px dark edge so the three read apart.

STAND = ['..oOl.....',
         '.oOlLl....',
         '.uttTT....',
         '..nbBb....',
         '..obb.....',
         '.oOlOl....',
         '.ONlOlB...',
         '.oOOlKKKKK',
         '.oNOOKK...',
         '.onoo.....',
         '.oO.Ol....',
         '.oO.Ol....',
         '.oO..Ol...',
         '.KK..KK...']

KNEEL = ['...oOl.....',
         '..oOlLl....',
         '..uttTT....',
         '...nbBb....',
         '...obb.....',
         '..oOlOlB...',
         '..ONlOKKKKKKK',
         '..oOOlKK...',
         '..onoo.....',
         '..oOlllll..',
         '..oO....Ol.',
         'KKoo....OK.']


def part(c, rows, x, y, edge='k'):
    t = type(c)()
    t.stamp(x, y, rows)
    for yy in range(t.h):
        for xx in range(t.w):
            if t.g[yy][xx] != '.':
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = xx + dx, yy + dy
                if t.inb(nx, ny) and t.g[ny][nx] != '.' and c.g[yy][xx] not in '.~':
                    c.g[yy][xx] = edge
                    break
    for yy in range(t.h):
        for xx in range(t.w):
            if t.g[yy][xx] != '.':
                c.g[yy][xx] = t.g[yy][xx]


def draw(c):
    part(c, STAND, 1, 14)          # rear rifleman, feet y=27
    part(c, STAND, 10, 15)         # second rifleman, feet y=28
    part(c, KNEEL, 18, 18)         # kneeling point man in front, ground y=29
    c.outline('k')
    c.shadow(2, 30, 30); c.shadow(2, 12, 29)
