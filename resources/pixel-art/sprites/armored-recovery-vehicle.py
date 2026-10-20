NAME = "Armored Recovery Vehicle"
KIND = "ground"

# Silhouette plan (faces right), all coordinates final.
#  bbox x0..31, y5..28 (+shadow y29)
#  1 tracks      x1..25 y23..27, six 3x3 road wheels
#  2 hull        x1..25 y19..22, low and long, short glacis
#  3 crane       lattice boom (two rails + steel rungs) from a pivot-hub stand
#                rising over the rear at 45deg, steel sheave head, hook dangling,
#                topping cable to the cab roof (closes the A-frame triangle)
#  4 winch drum  steel spool on the rear deck under the boom
#  5 cab         tall flat-roofed boxy cab x16..23 y11..18, dark window band,
#                roof beacon, tiny MG cupola, pennant mast
#  6 dozer blade steel leaning plate x26..30 y16..27 in front of the hull

def draw(c):
    # ---- running gear ----------------------------------------------------
    c.rect(1, 23, 25, 27, 'K')
    for x0 in (2, 6, 10, 14, 18, 22):
        c.stamp(x0, 24, ['gSS', 'SsS', 'sSs'])
    # ---- hull (low): light deck edge, mid side, shadow, seam --------------
    c.poly([(1, 23), (1, 20), (2, 19), (22, 19), (25, 22), (25, 23)], 'O')
    c.hline(2, 22, 19, 'l'); c.px(23, 20, 'l'); c.px(24, 21, 'l')
    c.hline(2, 24, 21, 'o'); c.hline(1, 25, 22, 'k')
    # ---- winch drum on the rear deck --------------------------------------
    c.stamp(4, 14, ['GgGgG',
                    'SsSsS',
                    'SsSsS',
                    'sKsKs',
                    'kkkkk'])
    # ---- crane boom: lattice rails + rungs, pivot (12,14) -> head (4,6) -----
    for i in range(0, 8):
        x = 12 - i; y = 14 - i
        c.px(x - 1, y, 'L'); c.px(x, y, 'l')
        c.px(x + 1, y, 'g' if i % 3 == 1 else 'o')
        c.px(x + 2, y, 'O'); c.px(x + 3, y, 'o')
    # steel sheave head over the boom tip
    c.stamp(2, 6, ['.GGg.',
                   'gSkSs',
                   'sSs..'])
    # ---- A-frame pivot stand: olive housing + steel pivot hub ------------------
    c.poly([(9, 18), (10, 14), (15, 14), (15, 18)], 'O')
    c.hline(10, 15, 14, 'L'); c.vline(9, 16, 18, 'o'); c.vline(10, 15, 17, 'l')
    c.rect(11, 15, 14, 18, 'o'); c.hline(11, 14, 18, 'k')
    c.vline(14, 15, 17, 'O'); c.vline(11, 15, 17, 'o')
    c.stamp(10, 12, ['GgGg', 'gkSs', 'SsSs'])             # pivot hub, boom base
    # ---- cab ---------------------------------------------------------------
    c.rect(16, 11, 23, 18, 'O')
    c.hline(16, 23, 11, 'L'); c.hline(16, 23, 12, 'l'); c.hline(16, 23, 13, 'o')
    c.vline(16, 14, 18, 'o'); c.hline(16, 23, 18, 'o')
    c.hline(19, 23, 14, 'y'); c.hline(19, 23, 15, 'Y'); c.px(23, 14, 'Y')
    c.hline(17, 22, 17, 't')
    # ---- roof: beacon + cupola ---------------------------------------------
    c.rect(18, 9, 20, 10, 'o'); c.px(18, 9, 'S'); c.px(19, 9, 'g')
    c.px(16, 10, 'F'); c.px(19, 10, 'Y')
    # ---- dozer blade: leaning plate, forward cutting edge (before the outline) --
    for y in range(16, 28):
        x = 26 if y < 20 else (27 if y < 24 else 28)
        c.px(x, y, 's'); c.px(x + 1, y, 'S'); c.px(x + 2, y, 'g')
    c.hline(26, 28, 16, 'g'); c.px(26, 16, 'G'); c.px(27, 16, 'G')
    c.hline(28, 30, 27, 'G'); c.hline(28, 30, 26, 'g')
    c.px(27, 20, 'S'); c.px(28, 20, 'g')
    # ---- camo blotch on the hull side + panel seams --------------------------
    c.hline(3, 7, 20, 'N'); c.hline(4, 7, 21, 'n'); c.px(8, 20, 'N')
    c.px(9, 20, 'o'); c.px(16, 20, 'o')
    # ---- team stripe on the hull -------------------------------------------
    c.hline(18, 24, 20, 'T'); c.hline(18, 24, 21, 't'); c.px(25, 21, 'u')
    c.px(25, 20, 'f')
    # ---- outline -----------------------------------------------------------
    c.outline('k')
    c.hline(25, 28, 28, 'k')
    # ---- thin things, drawn on top of the outline --------------------------
    c.hline(21, 22, 9, 's')                                  # MG barrel
    c.vline(23, 6, 10, 's'); c.stamp(24, 6, ['TTT', 'ttt', '.tu'])   # pennant
    c.line(10, 8, 15, 10, 's')                                # topping cable
    c.vline(2, 9, 10, 'S')                                   # hook cable
    c.stamp(1, 11, ['kSk', 'kgk', '.S.', '.S.', 'SS.'])      # hook block + hook
    c.shadow(2, 27, 29)
