NAME = "Naval Base"
KIND = "ground"

def draw(c):
    # ---- warehouse / boat shed (left) ----------------------------------
    c.poly([(0, 13), (7, 8), (14, 13)], 'M'); c.line(7, 8, 0, 13, 'h'); c.line(7, 9, 1, 13, 'h')   # gabled roof
    c.rect(1, 13, 13, 21, 'h'); c.rect(10, 13, 13, 21, 'M'); c.vline(1, 13, 21, 'G')                 # walls
    c.hline(1, 13, 13, 't'); c.hline(1, 13, 14, 'u'); c.hline(1, 5, 13, 'T')                          # team band
    c.rect(4, 16, 9, 21, 'm')                                                                          # big shed door
    for y in (17, 19): c.hline(4, 9, y, 'M')
    c.stamp(11, 16, ['y', 'Y'])
    c.px(7, 10, 'y'); c.px(7, 11, 'Y')                                                                  # gable vent
    # ---- dock crane (right) --------------------------------------------
    c.rect(19, 11, 23, 21, 'S'); c.vline(19, 11, 21, 'g'); c.vline(23, 11, 21, 's')   # crane tower
    for y0 in (11, 15, 19):                                                                # lattice X-bracing
        c.line(20, y0, 22, y0 + 2, 's'); c.line(22, y0, 20, y0 + 2, 's')
    c.rect(18, 21, 24, 21, 's')                                                            # foot
    c.rect(18, 8, 24, 10, 'f'); c.hline(18, 24, 10, 'F')                  # yellow machinery house
    c.stamp(19, 8, ['tTt']); c.hline(19, 21, 9, 'u')                      # team-coloured cab windows
    c.hline(14, 31, 7, 'f'); c.hline(15, 30, 6, 'F')                      # jib boom
    for x in (17, 21, 25, 29): c.px(x, 6, 'f')
    c.px(14, 6, 's'); c.rect(14, 8, 15, 9, 's')                           # counterweight (back)
    c.rect(28, 15, 31, 17, 'N'); c.hline(28, 31, 15, 'b'); c.vline(31, 15, 17, 'n'); c.px(30, 14, 'K')   # slung crate
    # ---- concrete quay ---------------------------------------------------
    c.rect(0, 22, 31, 24, 'g'); c.hline(0, 31, 22, 'G')
    c.hline(0, 31, 25, 'S'); c.hline(0, 31, 26, 's')
    for x in (4, 12, 20, 28): c.vline(x, 25, 26, 'K')                     # expansion joints
    c.rect(26, 20, 27, 21, 'K'); c.hline(25, 28, 20, 'K'); c.px(26, 20, 'S')   # bollard
    c.stamp(16, 25, ['K', 'K'])                                          # tyre fender
    # ---- harbour water at the foot of the quay -----------------------------
    c.rect(0, 27, 31, 29, 'a'); c.hline(0, 31, 29, 'A')
    for x in (2, 9, 17, 24): c.hline(x, x + 2, 28, 'c')
    c.hline(0, 31, 27, 'A')
    c.outline('k')
    c.vline(30, 8, 13, 'K')                                               # hoist cable (after the outline: stays 1px thin)
