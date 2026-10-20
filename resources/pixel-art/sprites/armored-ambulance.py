NAME = "Armored Ambulance"
KIND = "ground"

# Silhouette plan (faces right), body x=2..29, outline x=1..30, topmost pixel y=6
#  1 four round tyres   y=22..27  x=2-7, 9-14, 17-22, 24-29  (underbody + wheel wells show above/between)
#  2 tall armoured box  x=2..15 y=9..21, chamfered rear-top, white inset panel + 7x7 red cross
#  3 low cab            roof y=13 x=16..22, 45deg windscreen, flat bonnet, chamfered nose + bumper
#  4 roof beacons       y=7..8: team-colour dome (left) + neutral amber dome (right)
#  5 team stripe        2px on the lower body y=19..20 (whole length), light row over main row

def draw(c):
    # ---- underbody (visible in the gaps between tyres) ----------------------
    c.rect(2, 21, 29, 23, 'o')
    # ---- armoured box (rear) -------------------------------------------------
    c.rect(2, 11, 15, 21, 'O')
    c.rect(3, 10, 15, 10, 'O')
    c.rect(4, 9, 15, 9, 'O')
    c.hline(4, 15, 9, 'L'); c.px(3, 10, 'L'); c.px(2, 11, 'l')
    c.hline(13, 15, 10, 'l'); c.vline(2, 12, 18, 'l')
    c.vline(15, 10, 21, 'o')
    # ---- cab: roof y=13, 45deg windscreen, flat bonnet, chamfered nose ------
    top = {16: 13, 17: 13, 18: 13, 19: 13, 20: 13, 21: 13, 22: 13,
           23: 14, 24: 15, 25: 16, 26: 17, 27: 17, 28: 18, 29: 19}
    for x, y0 in top.items():
        c.vline(x, y0, 21, 'O')
    for x in range(16, 23): c.px(x, 13, 'L')
    for x in (23, 24, 25): c.px(x, top[x], 'l')
    c.px(26, 17, 'L'); c.px(27, 17, 'L'); c.px(28, 18, 'l'); c.px(29, 19, 'l')
    c.vline(16, 13, 21, 'o')
    # ---- white inset panel with red cross -----------------------------------
    c.rect(4, 10, 13, 18, 'w')
    c.hline(4, 13, 18, 'G'); c.vline(13, 10, 18, 'G')
    cross = set()
    for x in range(7, 10):
        for y in range(11, 18): cross.add((x, y))
    for x in range(5, 12):
        for y in range(13, 16): cross.add((x, y))
    for (x, y) in cross:
        edge = ((x + 1, y) not in cross) or ((x, y + 1) not in cross)
        c.px(x, y, 'R' if edge else 'r')
    # ---- cab windows -----------------------------------------------------------
    for y, x1 in ((14, 20), (15, 21), (16, 22)):
        c.hline(17, x1, y, 'Y')
    c.vline(19, 14, 16, 'O')
    c.px(17, 14, 'y'); c.px(18, 14, 'y'); c.px(20, 14, 'y')
    # ---- clean camo blobs ------------------------------------------------------
    c.hline(21, 24, 17, 'N'); c.hline(20, 26, 18, 'N'); c.px(27, 18, 'n')
    # ---- team stripe (light row over main row) -----------------------------------
    c.hline(3, 29, 19, 'T')
    c.hline(3, 29, 20, 't')
    # ---- wheel wells + tyres -------------------------------------------------
    for x0 in (2, 9, 17, 24):
        c.hline(x0 + 1, x0 + 4, 21, 'k')
    tyre = ['.sKKK.', 'sKKKKK', 'KKgSKK', 'KKSsKK', 'KKKKKK', '.KKKK.']
    for x0 in (2, 9, 17, 24):
        c.stamp(x0, 22, tyre)
    c.stamp(17, 12, ['gSs'])                      # cab roof hatch
    c.vline(19, 17, 18, 'o'); c.px(18, 17, 'g')    # door seam + handle
    # ---- headlamp ------------------------------------------------------------
    c.vline(29, 19, 20, 'f'); c.px(29, 21, 'S')
    # ---- roof beacons ----------------------------------------------------
    c.stamp(5, 7, ['.TT.', 'tttu'])
    c.stamp(11, 7, ['.ff.', 'fFFf'])
    c.outline('k')
    c.vline(21, 8, 11, 's'); c.px(21, 7, 'S')          # whip antenna
    c.shadow(3, 29, 29)
