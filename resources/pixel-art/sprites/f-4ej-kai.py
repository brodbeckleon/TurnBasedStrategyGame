NAME = "F-4EJ Kai"
KIND = "air"

# Revision 2 (hand-placed rows, outline added at the end, bbox x=0..31, y=6..25, shadow rows 28-29)
#   nose      drooped wedge: top line falls from the windscreen to a low radome tip (dark cone + 2x2 dark tip), flat belly
#   canopy    tandem two panes split by a dark frame, 3px hump over the spine
#   intake    light-lipped slab with dark mouth trapezoid and a dark underside so it stays a distinct box
#   fin       tall, raked 45 deg; only the tip band is team colour; tip stays at y=7 (outline y=6) clear of the HP bar
#   wing      swept paddle from mid-fuselage; lighter outer panel kinks UP with a team stripe at the tip
#   stab      dark plate hanging steeply DOWN-left (anhedral), 1px outline gap to the wing tip
#   roundel   white square with team-coloured centre on the rear fuselage

def rows(c, y, *segs):
    for x, s in segs: c.stamp(x, y, [s])

def draw(c):
    # ---- anhedral stabiliser: small swept plate hanging steeply DOWN-left ----
    rows(c, 20, (4, 'sSSSg'))
    rows(c, 21, (3, 'sSSSg'))
    rows(c, 22, (2, 'sSSSg'))
    rows(c, 23, (1, 'sSSSg'))
    rows(c, 24, (1, 'sSg'))
    # ---- fin (tall, raked; team colour only on the upper band) ----------------
    rows(c, 7, (5, 'TTTT'))
    rows(c, 8, (5, 'ttttt'))
    rows(c, 9, (5, 'uuuuuu'))
    rows(c, 10, (5, 'sSSSSSg'))
    rows(c, 11, (5, 'sSSSSSSg'))
    rows(c, 12, (4, 'sSSSSSSSg'))
    rows(c, 13, (4, 'sSSSSSSSSg'))
    rows(c, 14, (4, 'sSSS'))
    # ---- canopy (two panes + frame) ---------------------------------------------
    rows(c, 11, (16, 'yyyKyy'))
    rows(c, 12, (15, 'yyYYKyyYY'))
    rows(c, 13, (14, 'YYYYYKYYYYY'))
    # ---- fuselage ---------------------------------------------------------------
    rows(c, 14, (8, 'g' * 18))
    rows(c, 15, (4, 'S' * 10), (14, 'G' * 10), (24, 'SSSS'))
    rows(c, 16, (4, 'S' * 10), (14, 'g' * 8), (22, 'KK'), (24, 'gS'), (26, 'ss'), (28, 'S'))
    rows(c, 17, (4, 'S' * 10), (14, 'S' * 7), (21, 'KKK'), (24, 'gs'), (26, 'sss'), (29, 'S'))
    rows(c, 18, (4, 'g' * 10), (14, 's' * 7), (21, 'KKK'), (24, 'gg'), (26, 'sss'), (29, 'KK'))
    rows(c, 19, (4, 'g' * 10), (14, 's' * 6), (20, 'KKKK'), (24, 'gg'), (26, 'sss'), (29, 'KK'))
    # ---- twin exhaust ------------------------------------------------------------
    rows(c, 15, (2, 'KK'))
    rows(c, 16, (1, 'FKK'))
    rows(c, 17, (1, 'fKK'))
    rows(c, 18, (1, 'FKK'))
    # ---- near wing: swept paddle from mid-fuselage, outer panel kinks UP -----
    rows(c, 19, (11, 'GGSggggggG'))
    rows(c, 20, (10, 'GGGSgggggG'))
    rows(c, 21, (10, 'GGSgggggG'))
    rows(c, 22, (11, 'SSSgggG'))
    rows(c, 23, (13, 'SSgG'))
    rows(c, 24, (15, 'SG'))
    # ---- team stripe on the wing tip ----
    rows(c, 20, (10, 'ttt'))
    # ---- roundel (Hinomaru, centre in team colour) ----
    rows(c, 15, (8, 'www')); rows(c, 16, (8, 'wtw')); rows(c, 17, (8, 'www'))
    c.outline('k')
    c.shadow(9, 22, 28)
    c.shadow(11, 20, 29)
