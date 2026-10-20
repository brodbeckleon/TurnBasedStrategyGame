NAME = "Base Ruins"
KIND = "neutral"       # ownerless map object: no team colour, no HP bar

def draw(c):
    # ---- bent, bare flagpole (right, behind everything) ------------------
    c.vline(26, 12, 23, 's'); c.line(26, 12, 28, 9, 's'); c.px(29, 9, 'S')   # snapped and bent over
    # ---- collapsed tent: burnt canvas heap (right) -------------------------
    c.poly([(21, 24), (24, 18), (27, 20), (32, 24)], 'o')
    c.line(24, 18, 27, 20, 'O'); c.line(27, 20, 31, 23, 'O')
    c.px(25, 21, 'K'); c.px(28, 22, 'K'); c.px(23, 22, 'K')                  # burn holes
    # ---- burnt guard tower stump (left): charred legs, cabin roof tipped onto the ground ----
    c.rect(1, 14, 1, 24, 'N'); c.rect(2, 15, 2, 24, 'n'); c.px(1, 13, 'K'); c.px(2, 14, 'K')    # left leg, splintered top
    c.rect(7, 19, 7, 24, 'N'); c.rect(8, 20, 8, 24, 'n'); c.px(7, 18, 'K')                      # right leg, snapped short
    c.line(2, 16, 7, 21, 'n'); c.line(2, 21, 4, 19, 'n')                                       # surviving brace + broken stub
    c.hline(0, 3, 15, 'K'); c.px(4, 15, 'K')                                                   # charred end of the platform
    c.poly([(3, 24), (5, 18), (10, 22)], 'o'); c.line(3, 23, 5, 18, 'O'); c.line(4, 23, 5, 20, 'O')   # tower roof lying tipped over
    c.px(6, 21, 'K'); c.px(5, 22, 'K')                                                          # scorched
    # ---- roofless HQ block (centre) ----------------------------------------
    c.poly([(10, 24), (10, 10), (12, 9), (13, 11), (14, 10), (15, 13), (17, 12), (18, 14),
            (20, 14), (21, 17), (22, 18), (22, 24)], 'O')                     # walls with jagged, broken tops
    c.vline(10, 11, 24, 'L'); c.vline(11, 10, 24, 'l')                        # lit left edge
    for x, y in ((11, 10), (12, 10), (14, 11), (17, 13), (20, 15)):
        c.px(x, y, 'g')                                                       # exposed concrete on the break line
    c.rect(12, 14, 19, 15, 's'); c.hline(12, 17, 14, 'K'); c.px(13, 13, 'K')  # roofless: gutted interior seen over the wall
    c.stamp(12, 17, ['KK', 'Kk'])                                             # blown-out window (the other is gone with the corner)
    c.rect(14, 19, 17, 24, 'k'); c.hline(14, 17, 19, 'K')                     # empty doorway
    c.line(14, 19, 16, 23, 'n')                                               # door hanging from one hinge
    c.stamp(10, 20, ['.K', 'KK', '.K'])                                       # scorch streaks up the wall
    c.stamp(18, 16, ['K.', 'kK', '.K'])
    # fallen roof slab leaning off the broken right wall into the rubble
    c.poly([(16, 15), (18, 14), (25, 22), (23, 23)], 'g'); c.line(18, 14, 25, 22, 'G'); c.line(16, 16, 22, 23, 'S')
    c.poly([(20, 24), (22, 20), (26, 21), (29, 24)], 'S')                     # rubble pile at the collapsed corner
    c.px(23, 21, 'g'); c.px(27, 23, 'g'); c.px(25, 23, 's'); c.px(21, 23, 's'); c.px(26, 22, 'G')
    # ---- gap-toothed sandbag line in front ---------------------------------
    def bag(x0, x1, y, burnt=False):
        c.rect(x0, y, x1, y + 1, 'N' if burnt else 'b'); c.hline(x0, x1, y, 'n' if burnt else 'B'); c.hline(x0, x1, y + 1, 'n' if burnt else 'N')
    bag(0, 3, 26); bag(4, 6, 26, True); bag(1, 4, 24)                         # left: a few bags left standing
    bag(21, 24, 26); bag(26, 29, 26, True); bag(23, 26, 24)                   # right: broken stack
    c.stamp(8, 25, ['gS', 'GgS', 'Ss'])                                       # concrete chunks thrown into the gaps
    c.stamp(29, 25, ['Sg', 'gSs'])
    # ---- ground: scattered rubble, scorch + churned dirt ----------------------
    c.hline(0, 31, 28, 'n')
    c.rect(11, 25, 20, 27, 'N'); c.hline(11, 20, 25, 'b')                     # old driveway
    c.stamp(12, 25, ['KkK', 'kkkK', '.KK'])                                   # scorched crater in the drive
    c.stamp(17, 26, ['gS', 'Ss'])                                             # concrete chunk
    c.stamp(14, 27, ['k'])
    c.stamp(3, 22, ['gS', 'Ss'])                                              # cabin wall chunk under the tower
    c.px(28, 27, 'g'); c.px(3, 28, 'K'); c.px(19, 28, 'K')
    c.outline('k')
    c.shadow(1, 30, 29)
