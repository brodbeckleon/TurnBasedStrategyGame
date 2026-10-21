NAME = "Kongo"
KIND = "sea"

# JMSDF Kongo-class Aegis destroyer, bow to the RIGHT.  Low, sleek hull (much smaller than Yamato):
# deck y=20 rising to a raked bow (y=18), waterline y=24, flat stern with the helicopter deck.
#   stern flag staff (team ensign) x=1, aft deckhouse x=4..8, aft funnel x=8..10, forward funnel
#   x=11..13, Aegis block x=14..22: flat-sided superstructure with an octagonal SPY-1 radar panel,
#   bridge windows, tall lattice mast on top; VLS hatches + 127mm gun on the foredeck, team stripe
#   along the hull, light bow wave.

def top(x):
    return {25: 19, 26: 19, 27: 18, 28: 18, 29: 18}.get(x, 20)

def bot(x):
    return {1: 23, 25: 23, 26: 23, 27: 22, 28: 21, 29: 20}.get(x, 24)

def draw(c):
    # ---- hull -------------------------------------------------------------------------------
    for x in range(1, 30):
        t, b = top(x), bot(x)
        for y in range(t, b + 1):
            r = y - t
            k = 'G' if r == 0 else ('h' if r <= 2 else 'M')
            if y == b and b - t >= 3: k = 'm'
            c.px(x, y, k)
    c.hline(5, 20, 22, 't')             # team stripe along the hull
    c.px(23, 21, 'w'); c.px(25, 21, 'w')                          # hull number
    c.hline(1, 4, 20, 'g')                                        # helicopter deck
    # ---- aft deckhouse with the rear SPY panel ---------------------------------------------
    c.rect(4, 16, 8, 19, 'g'); c.hline(4, 8, 16, 'G'); c.vline(8, 16, 19, 'S')
    c.stamp(5, 17, ['MM', 'mM'])
    # ---- funnels: dark caps ----------------------------------------------------------
    for x0, y0 in ((8, 13), (11, 12)):
        c.rect(x0, y0, x0 + 2, 19, 'S'); c.vline(x0, y0, 19, 'g'); c.vline(x0 + 2, y0, 19, 's')
        c.hline(x0, x0 + 2, y0, 'K')
    # ---- Aegis superstructure: flat octagonal-ish block -------------------------------------
    c.poly([(14, 19.9), (14, 12), (15, 11), (20, 11), (21, 12), (22, 14), (22, 19.9)], 'g')
    c.hline(15, 20, 11, 'G'); c.vline(14, 12, 19, 'G')
    c.vline(21, 14, 19, 'S'); c.px(21, 13, 'S')
    c.hline(17, 21, 13, 'Y')                                      # bridge windows
    c.stamp(15, 15, ['.MM.', 'MmmM', 'MmmM', '.MM.'])             # octagonal SPY-1 panel
    c.px(16, 15, 'h')
    # ---- foredeck: VLS hatches + 127mm gun ------------------------------------------------------
    c.hline(23, 24, 19, 'S')
    c.stamp(25, 16, ['.Gg', 'GggS', 'SSSs'])
    # ---- lattice mast ---------------------------------------------------------------------------
    c.rect(16, 7, 18, 10, 'S'); c.vline(16, 7, 10, 'g'); c.px(17, 8, 's'); c.px(17, 10, 's')
    c.hline(15, 19, 8, 'g')                                       # yardarm

    c.outline('k')

    # ---- thin parts after the outline -------------------------------------------------------------
    c.hline(28, 30, 16, 'g'); c.hline(28, 30, 15, 'k'); c.hline(28, 30, 17, 'k'); c.px(31, 16, 'k')   # gun barrel
    c.vline(17, 5, 6, 's')                                       # mast-top antenna
    c.vline(1, 13, 19, 's')                                      # stern staff + ensign
    c.stamp(2, 13, ['TTk', 'tuk', 'kk.'])
    c.vline(0, 22, 23, 'k')
    # ---- water: light stern wake, bow wave -------------------------------------------------------
    row25 = ".ccwcccwccccwcccwccccwcccwcwwwc."
    row26 = "...c...cc....c....cc....c..cwc.."
    for x in range(32):
        if row25[x] != '.': c.px(x, 25, row25[x])
        if row26[x] != '.': c.px(x, 26, row26[x])
    c.stamp(27, 24, ['www'])
    c.px(29, 23, 'w')
    c.shadow(2, 28, 27)
