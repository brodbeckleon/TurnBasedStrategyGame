NAME = "Type 87"
KIND = "ground"

# Type 87 SPAAG (Gepard-like): Type 74 chassis (six road wheels), big boxy turret,
# TWIN 35mm barrels = two clean 2px bars on a 2:1 diagonal sharing one dark divider line,
# big elliptical search dish on a mast at the turret rear, small round tracking dish front-top.
# Barrel tips end at x=29 (+ outline at x=30) so nothing touches the cell border.

def draw(c):
    # ---- running gear: six road wheels, calm two-tone, light only on the top row ----
    c.rect(4, 23, 27, 27, 'K')
    for y in (24, 25, 26): c.px(3, y, 'K'); c.px(28, y, 'K')
    c.hline(4, 27, 23, 's')                                  # lighter track-guard top edge
    for x0 in (5, 9, 13, 17, 21, 25):
        c.stamp(x0, 24, ['SSS', 'sKs', 'sss'])
    # ---- hull (low, sloped glacis) + side skirt ----------------------
    c.poly([(3, 23), (3, 21), (4, 20), (24, 20), (29, 22), (29, 23)], 'O')
    c.hline(5, 23, 20, 'l')
    c.rect(4, 21, 26, 22, 'o'); c.hline(4, 26, 21, 'O'); c.hline(4, 20, 22, 'k')
    c.stamp(4, 20, ['SsS'])                                    # engine deck grille
    c.stamp(8, 21, ['NNN']); c.stamp(13, 21, ['nnN']); c.stamp(18, 21, ['NNN'])
    # team patch on the hull front 5x2
    c.stamp(22, 21, ['TTTtt', 'uuuuu'])
    c.hline(27, 28, 22, 'k')
    # ---- turret: big boxy block, lit roof ----------------------------
    c.poly([(6, 19), (6, 14), (7, 13), (19, 13), (20, 14), (20, 19)], 'O')
    c.hline(7, 19, 13, 'L'); c.hline(6, 20, 14, 'l'); c.hline(6, 20, 15, 'l')
    c.hline(6, 20, 18, 'o')
    # camo: 2-3px chunks
    c.stamp(7, 14, ['NN'])
    c.stamp(7, 15, ['nN'])
    c.stamp(16, 14, ['NNN', 'NNn'])
    c.stamp(17, 16, ['OO'])
    c.stamp(16, 17, ['nnn'])
    # rear vent grille
    c.rect(7, 17, 8, 18, 'o'); c.hline(7, 8, 17, 'K')
    # team badge on the turret side: 5x3 inside a dark frame
    c.rect(9, 14, 15, 18, 'k')
    c.stamp(10, 15, ['TTTTT', 'ttttt', 'uuuuu'])
    # ---- search radar: wide elliptical dish on a mast at the turret rear ----
    c.stamp(4, 6, ['..GGGGG..',
                   '.GsssssS.',
                   'GsssgsssS',
                   '.GsssssS.',
                   '..SSSSS..'])
    c.vline(7, 11, 12, 'S'); c.vline(8, 11, 12, 's')
    c.hline(6, 9, 12, 'K')                                     # rotating pedestal
    # ---- tracking radar: small round dish, front-top -------------------
    c.stamp(15, 9, ['.GGG.',
                    'GsgsS',
                    '.SSS.'])
    c.px(16, 12, 'S'); c.px(17, 12, 's')
    # ---- gun mount plate at the turret front --------------------------
    c.rect(19, 13, 20, 18, 'S'); c.vline(20, 14, 18, 's'); c.px(19, 13, 'g')
    c.outline('k')
    # ---- twin barrels: 45deg, two 2px bars (light edge + mid body) split by a 1px dark divider ----
    S0, XMIN, D = 32, 21, 22          # band offset (x+y), first column, tip cut (x-y)
    COL = {0: 'k', 1: 'G', 2: 'S', 3: 'k', 4: 'g', 5: 'S', 6: 'k'}
    MUZ = {1: 's', 2: 'K', 4: 's', 5: 'K'}
    for y in range(5, 20):
        for x in range(XMIN, 32):
            o, d = x + y - S0, x - y
            if not 0 <= o <= 6 or d > D + 1: continue
            if d == D + 1:
                if 1 <= o <= 5: c.px(x, y, 'k')
                continue
            col = COL[o]
            if d >= D - 1 and o in MUZ: col = MUZ[o]
            c.px(x, y, col)
    c.shadow(5, 28, 29)
