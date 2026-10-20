NAME = "Type 81 (C)"
KIND = "ground"

# Silhouette plan (facing right). Body x2..29, outline x1..30, y6..27
#  1 radar dish  x2..10 y6..13  oval scanning disc on a 2px pedestal at the rear of the bed (THE landmark)
#  2 missile pod x12..19 low rack (~17 deg) on a hinge cradle + strut, two pale missile noses forward
#  3 cargo bed   x2..20 y16..19, team stripe, camo blotches
#  4 cab         x21..29: boxy cab (roof y12), sloped screen, glass, hood step down to y17, bumper + lamp
#  5 tyres       7x7 tyres: rear tandem x3..9 / x11..17, front x23..29, y21..27, fender caps above
#  6 whip antenna + team pennant on the cab roof

PX0, PX1 = 12, 19
PTOP = {12: 9, 13: 9, 14: 9, 15: 8, 16: 8, 17: 8, 18: 7, 19: 7}
PODROWS = 'LlOkloo'      # tube1: light/mid/dark, gap, tube2: light/mid/dark  (7 rows)

TYRE = ['.sSKKK.',
        'sKKKKKK',
        'SKgGSKK',
        'KKgSsKK',
        'KKSssKK',
        'KKKKKKK',
        '.KKKKK.']

DISH = ['..GGGgS..',
        '.GgggggS.',
        'GwggggggS',
        'GgggSSgsS',
        'GggSkkgsS',
        'GgggSSssS',
        '.SgggsssS'[:9],
        '..SsssS..']

def draw(c):
    # ---- chassis rail --------------------------------------------------------
    c.rect(3, 20, 29, 20, 's')
    # ---- tyres -----------------------------------------------------------------
    for x0 in (3, 11, 23):
        c.stamp(x0, 21, TYRE)
    # ---- fender caps over the tyres ---------------------------------------------
    for x0 in (3, 11, 23):
        c.hline(x0 - 1, x0 + 7, 20, 'o'); c.hline(x0, x0 + 6, 20, 'O')
    # ---- bed ----------------------------------------------------------------------
    c.rect(2, 16, 20, 19, 'O')
    c.hline(2, 20, 16, 'l'); c.hline(2, 20, 19, 'o')
    # ---- cab + hood ---------------------------------------------------------------
    c.poly([(21, 19), (21, 12), (24, 12), (25, 13), (26, 14), (27, 15), (27, 17), (29, 17), (29, 19)], 'O')
    c.hline(21, 24, 12, 'l'); c.px(25, 13, 'l'); c.px(26, 14, 'l'); c.px(27, 15, 'l')
    c.hline(27, 29, 17, 'l')
    c.hline(21, 29, 19, 'o')
    # glass: pale grey-blue
    c.rect(22, 13, 24, 15, 'h'); c.rect(25, 14, 25, 15, 'h'); c.px(26, 15, 'h')
    c.hline(22, 24, 13, 'G'); c.px(25, 14, 'G'); c.hline(22, 26, 15, 'M')
    c.vline(24, 17, 18, 'o'); c.px(23, 17, 'S')                  # door seam + handle
    c.px(29, 18, 'f'); c.hline(28, 29, 19, 'S')                  # headlamp + bumper
    c.px(28, 18, 's')                                            # grille
    # ---- camouflage ------------------------------------------------------------------
    c.rect(3, 17, 6, 18, 'N'); c.hline(4, 6, 19, 'n'); c.px(3, 19, 'N')
    c.rect(9, 18, 10, 19, 'n'); c.px(9, 19, 'N')
    c.rect(18, 17, 20, 18, 'N'); c.hline(19, 20, 19, 'n')
    c.rect(21, 18, 22, 19, 'N')
    c.rect(27, 18, 27, 18, 'N')
    # ---- team stripe on the bed side ---------------------------------------------------
    c.hline(12, 16, 17, 'T'); c.hline(12, 16, 18, 't'); c.px(16, 18, 'u')
    c.px(2, 18, 'R')
    # ---- dish pedestal ------------------------------------------------------------------
    c.rect(5, 14, 6, 15, 'S'); c.vline(6, 14, 15, 's')
    c.hline(4, 7, 16, 'S')
    # ---- radar dish ----------------------------------------------------------------------
    c.stamp(2, 6, DISH)
    # ---- launcher cradle -----------------------------------------------------------------
    c.rect(12, 15, 15, 15, 's')
    # ---- missile pod ----------------------------------------------------------------------
    for x in range(PX0, PX1 + 1):
        t0 = PTOP[x]
        for dy, k in enumerate(PODROWS):
            c.px(x, t0 + dy, k)
    for bx in (PX0, 16, PX1):
        t0 = PTOP[bx]
        c.rect(bx, t0, bx, t0 + 6, 'S'); c.px(bx, t0, 'g'); c.px(bx, t0 + 3, 'k'); c.px(bx, t0 + 6, 's')
    # ---- whip antenna + pennant ----------------------------------------------------------
    c.stamp(25, 6, ['TTT', 'ttt', 'ttu'])
    c.vline(24, 6, 11, 'S')
    c.outline('k')
    # thin things after the outline
    c.line(17, 16, 18, 13, 'S')
    t0 = PTOP[PX1]
    for ty in (t0 + 1, t0 + 4):
        c.stamp(PX1 + 1, ty - 1, ['kkk', 'wGk', 'gSk', 'kkk'][:4])
    c.shadow(3, 29, 29)
