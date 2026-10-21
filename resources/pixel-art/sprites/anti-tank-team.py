NAME = "Anti-Tank Team"
KIND = "ground"

# JGSDF anti-tank team, facing RIGHT: a kneeling gunner (centre-right, ground y=29) aiming a
# Type 01 LMAT: long olive launch tube on the shoulder, flared rear venturi, fat front end with a dark
# mouth, boxy sight unit in front of his face and a pistol grip in his hand.  Behind him (left, feet
# y=27) the loader stands ready with a spare missile canister.  Team colour on both helmet bands
# and on a band round the launcher.

LOADER = ['..oOl.....',
          '.oOlLl....',
          '.uttTT....',
          '..nbBb....',
          '..obb.....',
          '.oOlOl....',
          '.ONlOlB...',
          '.oOOlOlLLg',
          '.oNOOOllls',
          '.onoooOOos',
          '.oO.Ol....',
          '.oO.Ol....',
          '.oO..Ol...',
          '.KK..KK...']

GUNNER = ['...oOl.....',
          '..oOlLl....',
          '..uttTT....',
          '...nbBb....',
          '...obb.....',
          '..oOlOl....',
          '..ONlOlB...',
          '..oOOlO....',
          '..onoo.....',
          '..oOlllll..',
          '..oO....Ol.',
          'KKoo....OK.']


def part(c, rows, x, y, edge='k'):
    t = type(c)()
    t.stamp(x, y, rows) if isinstance(rows, list) else rows(t)
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


def launcher(t):
    # main tube body x=12..26, rows 23 (lit) / 24 (mid) / 25 (shadow)
    t.hline(12, 26, 23, 'l'); t.hline(12, 26, 24, 'O'); t.hline(12, 26, 25, 'o')
    t.hline(14, 25, 23, 'L')
    # flared rear venturi (left) with dark opening
    t.stamp(10, 22, ['.oo', 'oOl', 'KOO', 'oOo', '.oo'])
    # fat front end (right) with a dark mouth
    t.stamp(26, 22, ['lLLl', 'OllOK', 'OOOOK', 'oooOK', 'oooo'])
    # team band round the tube (behind the gunner's head)
    t.vline(14, 23, 25, 't'); t.vline(15, 23, 25, 'u'); t.px(14, 23, 'T')
    # sight unit in front of the gunner's face: steel box, glass lens towards the eye
    t.stamp(19, 20, ['SgG', 'YSS', 'sss'])
    # pistol grip + gunner's hand
    t.stamp(20, 26, ['K', 'B'])


def draw(c):
    part(c, LOADER, 0, 14)          # loader, feet y=27
    part(c, GUNNER, 11, 18)         # kneeling gunner, ground y=29
    part(c, launcher, 0, 0)
    c.outline('k')
    c.shadow(3, 30, 30); c.shadow(1, 11, 28)
