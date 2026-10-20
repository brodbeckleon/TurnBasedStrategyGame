NAME = "F-35"
KIND = "air"

# F-35A, nose RIGHT, pseudo-3/4 from slightly above.
#  fuselage x2..31 y15..20 | canopy teardrop x18..25 y12..14 | raked side intake x20..26 y17..20
#  twin canted tails: far (dark, upright) x12..17 tip y7 | near (lit, swept back) x5..13 tip y9
#  near wing: short trapezoid root x14..23 y19, tip x9..14 y24 | small stabiliser at the tail
#  one nozzle at x2..4 | shadow rows 28..29

def draw(c):
    # ---- far tail (behind, darker, more upright) --------------------------
    c.poly([(18, 15), (15, 7), (11, 7), (13, 15)], 's')
    c.line(11, 7, 13, 14, 'K')                       # shaded trailing edge
    c.line(15, 7, 18, 14, 'S')                       # lit leading edge
    c.hline(12, 14, 7, 'g')
    c.hline(12, 14, 8, 't')                          # far fin tip band
    # ---- far stabiliser sliver ---------------------------------------------
    c.poly([(8, 16), (6, 14), (9, 14), (11, 16)], 'K')
    # ---- fuselage: lit top, flank, dark belly ---------------------------------
    c.poly([(31, 18), (28, 16), (25, 15), (20, 14), (6, 14), (3, 15), (2, 16), (2, 19),
            (4, 20), (26, 20), (29, 19)], 's')
    c.hline(5, 24, 15, 'S')                          # lit spine plane
    c.hline(6, 26, 16, 'S')
    c.hline(5, 17, 14, 'S')
    c.hline(26, 28, 16, 'S'); c.hline(27, 30, 17, 'S')   # faceted nose top
    c.hline(5, 19, 19, 'K'); c.hline(5, 19, 20, 'K')     # dark belly
    c.px(28, 16, 'g'); c.px(29, 17, 'g')
    c.hline(8, 15, 14, 'g')                          # single spine highlight
    # ---- near stabiliser ----------------------------------------------------------
    c.poly([(9, 19), (4, 19), (1, 22), (6, 22)], 's')
    c.hline(4, 8, 19, 'S'); c.line(2, 22, 5, 22, 'K')
    # ---- near wing: compact trapezoid, straight trailing edge -------------------
    WING = {19: (14, 23), 20: (13, 21), 21: (12, 19), 22: (11, 18), 23: (10, 16), 24: (9, 14)}
    for y, (tr, le) in WING.items():
        c.hline(tr, le, y, 'S' if y < 23 else 's')
        c.px(le, y, 'g'); c.px(tr, y, 'K')           # lit leading edge, shaded trailing edge
    c.hline(10, 14, 24, 'K'); c.hline(11, 15, 23, 's')   # shadowed tip
    # ---- canopy ---------------------------------------------------------------------
    c.poly([(18, 15), (19, 13), (21, 12), (23, 12), (26, 15)], 'Y')
    c.px(21, 12, 'y'); c.px(22, 12, 'y'); c.px(24, 13, 'y'); c.px(20, 13, 'y')
    # ---- side intake (diverterless, lip raked forward at the top) ----------------
    c.poly([(22, 17), (26, 17), (24, 20), (20, 20)], 'K')
    c.line(26, 17, 24, 20, 'S')
    # ---- near tail (swept, leaning back, team tip) ---------------------------------
    c.poly([(13, 16), (8, 9), (5, 9), (6, 16)], 'S')
    c.line(8, 9, 13, 14, 'g')                        # lit leading edge
    c.line(5, 9, 6, 15, 's')                         # shaded trailing edge
    c.hline(6, 7, 9, 'T'); c.hline(6, 8, 10, 't'); c.hline(6, 9, 11, 'u')
    # ---- nozzle ------------------------------------------------------------------------
    c.rect(2, 16, 4, 19, 'K'); c.px(2, 17, 'F'); c.px(2, 18, 'F')
    c.outline('k')
    c.px(10, 9, 'k')                                  # close the V notch between the fins
    c.shadow(9, 23, 28)
    c.shadow(11, 21, 29)
