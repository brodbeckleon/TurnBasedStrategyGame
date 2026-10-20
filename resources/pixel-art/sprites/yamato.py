NAME = "Yamato"
KIND = "sea"

# Layout, bow to the RIGHT.  Deck y=20 (waist y=21), waterline y=24, long wedge bow, square stern.
#   stern staff x=1 + ensign, aft turret C x=2..5, funnel x=7..12 (raked, team band), pagoda x=13..19,
#   turret B (superfiring, raised) x=20..24, turret A x=24..28, barrel bundles to x=31,
#   side sponsons on the hull, heavy bow wave / thin stern wake.

def top(x):
    if x <= 9: return 20
    if x <= 21: return 21
    return 20

def bot(x):
    if x == 1: return 23
    if x <= 24: return 24
    return {25: 23, 26: 23, 27: 22, 28: 22, 29: 21, 30: 20, 31: 20}[x]

def draw(c):
    # ---- hull: tan deck line, haze-grey topsides, navy waterline ---------
    for x in range(1, 32):
        t, b = top(x), bot(x)
        for y in range(t, b + 1):
            r = y - t
            k = 'b' if r == 0 else ('h' if r == 1 else 'M')
            if y == b and b - t >= 2: k = 'm'
            c.px(x, y, k)
    for x in range(3, 29, 4): c.px(x, top(x), 'B')
    for x in range(29, 32): c.px(x, 20, 'h')                       # prow rim
    for x in range(5, 25, 3):
        if top(x) + 2 < bot(x): c.px(x, top(x) + 2, 'm')           # portholes

    # ---- side sponsons: secondary turrets / AA tubs ----------------------------
    for x0 in (3, 7, 22, 26):
        c.stamp(x0, 22, ['gSg', 'SSs'])
        c.hline(x0, x0 + 2, 24, 'm')

    # ---- funnel (raked back): rim, team band, steel body -------------------
    c.poly([(7, 11), (11, 11), (11, 13), (12, 13), (12, 21), (9, 21), (9, 17), (8, 17), (8, 14), (7, 14)], 'M')
    c.stamp(7, 11, ['gSSs'])
    c.stamp(7, 12, ['TTTT'])
    c.stamp(7, 13, ['ttt.'])
    c.stamp(8, 13, ['ttu'])
    c.stamp(8, 14, ['SMMs', 'SMMs', 'SMMs'])
    c.stamp(9, 17, ['SMMs', 'SMMs', 'SMms', 'SMms'])
    c.vline(12, 17, 20, 's')

    # ---- aft turret C (x=2..5) --------------------------------------------
    c.stamp(2, 16, ['.GGg', 'GgSS', 'SSSs', 'ssss'])

    # ---- pagoda (shaft x=14..18, platforms x=13..19) -------------------------
    c.stamp(13, 6, ['...s...',
                    '...sTt.',
                    '..gGg..',
                    '..SYS..',
                    'GgggggS',
                    '.sYSYs.',
                    '.SSSSs.',
                    'GgggggS',
                    '.SYSYs.',
                    '.SSSSs.',
                    'ggggggs',
                    '.sYSYs.',
                    '.SSSSs.',
                    '.SSSSs.',
                    '.sssss.'])

    # ---- turret B (raised, superfiring) ----------------------------------
    c.rect(20, 15, 22, 19, 'M'); c.vline(20, 15, 19, 'S'); c.vline(22, 15, 19, 's')
    c.stamp(20, 10, ['.GGg.', 'GGggS', 'SSSSs', 'SSSss', 'ssss.'])
    # ---- turret A ----------------------------------------------------------
    c.stamp(24, 16, ['.GGg.', 'GGggS', 'SSSSs', 'ssss.'])

    c.outline('k')

    # ---- barrel bundles (after the outline): two lit barrels + dark gap ------
    c.hline(25, 31, 11, 'k'); c.hline(25, 31, 12, 'g'); c.hline(25, 31, 13, 'k'); c.hline(25, 31, 14, 'S'); c.hline(25, 31, 15, 'k')
    c.px(31, 12, 'G'); c.px(31, 14, 'g')
    c.hline(29, 31, 16, 'g'); c.hline(29, 31, 17, 'k'); c.hline(29, 31, 18, 'S'); c.hline(29, 30, 19, 'k')
    c.px(31, 16, 'G'); c.px(31, 18, 'g'); c.px(31, 19, 'k')
    c.hline(6, 7, 16, 'k'); c.hline(6, 7, 17, 'g'); c.hline(6, 7, 18, 's'); c.hline(6, 7, 19, 'k')

    # ---- close tiny see-through slots between structures ------------------
    for (x, y) in ((12, 11), (12, 12), (24, 15)):
        if c.get(x, y) == '.': c.px(x, y, 'k')

    # ---- stern staff + ensign ------------------------------------------
    c.vline(1, 11, 19, 's')
    c.stamp(2, 11, ['TTt', 'ttu'])

    # ---- wake & foam: thin at the stern, heavy at the bow --------------------
    row25 = ".wwcwwwcwwwwcwwwwcwwwwcwwwwwwwww"
    row26 = "..cc...cc....cc...cc...ccwwcwwcc"
    for x in range(32):
        if row25[x] != '.': c.px(x, 25, row25[x])
        if row26[x] != '.': c.px(x, 26, row26[x])
    c.stamp(27, 24, ['ww'])
    c.stamp(29, 23, ['ww', 'ww'])
    c.stamp(30, 22, ['ww'])
    c.px(31, 21, 'k')
    c.px(29, 27, 'c'); c.px(31, 27, 'c')
    c.shadow(2, 28, 27)
