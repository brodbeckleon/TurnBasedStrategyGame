NAME = "Base"
KIND = "ground"

def draw(c):
    # ---- tent (right, behind the wall) ---------------------------------
    c.poly([(22, 24), (27, 14), (32, 24)], 'l')
    c.poly([(27, 14), (32, 24), (28, 24)], 'O')                  # shaded right slope
    c.line(27, 14, 22, 24, 'L'); c.line(27, 15, 23, 23, 'L')     # lit left edge
    c.hline(23, 31, 23, 'o')
    c.poly([(26, 24), (27, 19), (28, 24)], 'k')                  # entrance flap opening
    c.vline(27, 11, 13, 's')                                     # tent pole
    c.stamp(28, 10, ['TT', 'tt', 'tu'])                          # pennant
    # ---- guard tower (left) --------------------------------------------
    c.rect(1, 17, 1, 24, 'N'); c.rect(7, 17, 7, 24, 'N'); c.rect(2, 17, 2, 24, 'n'); c.rect(8, 17, 8, 24, 'n')
    c.line(2, 18, 7, 23, 'n'); c.line(7, 18, 2, 23, 'n')         # cross braces
    c.rect(0, 14, 9, 16, 'b'); c.hline(0, 9, 14, 'B'); c.hline(0, 9, 16, 'N')    # platform / cabin base
    c.rect(1, 11, 8, 13, 'S'); c.hline(1, 8, 11, 'G'); c.hline(2, 7, 12, 'K')     # cabin with window slit
    c.poly([(0, 11), (5, 6), (10, 11)], 'O'); c.line(5, 6, 0, 11, 'l'); c.hline(1, 9, 10, 'o')   # roof
    c.rect(4, 7, 5, 9, 't'); c.px(4, 7, 'T')                      # team emblem on the roof
    # ---- HQ block ------------------------------------------------------
    c.rect(10, 15, 22, 24, 'l'); c.rect(20, 15, 22, 24, 'O'); c.vline(10, 15, 24, 'L')
    c.rect(9, 12, 23, 14, 'g'); c.hline(9, 23, 12, 'w'); c.hline(9, 23, 14, 'S')      # concrete roof slab
    c.hline(10, 22, 15, 't'); c.hline(10, 22, 16, 'u')                              # team band under the roof
    c.hline(10, 14, 15, 'T')
    for x0 in (11, 18):
        c.stamp(x0, 18, ['yy', 'YY'])                              # windows
    c.rect(14, 19, 17, 24, 'K'); c.frame(14, 19, 17, 24, 'k'); c.hline(14, 17, 19, 'S')   # door
    # ---- stepped comms level, radio mast + flag ---------------------------
    c.rect(12, 8, 19, 11, 'l'); c.rect(18, 8, 19, 11, 'O'); c.vline(12, 8, 11, 'L')
    c.rect(11, 7, 20, 7, 'g'); c.hline(11, 20, 7, 'w'); c.hline(12, 19, 11, 'O')
    c.stamp(14, 9, ['yy', 'YY']); c.stamp(17, 9, ['y', 'Y'])           # windows
    c.hline(13, 17, 6, 'g'); c.px(15, 6, 'r')                          # antenna cross-bar with a red aviation light
    c.vline(21, 6, 11, 's')
    c.stamp(22, 6, ['TTTTT', 'tttttt', 'ttttu.', 'tuuu..'])               # flag
    # ---- sandbag wall in front (gap for the gate) -----------------------
    def bags(x0, x1, y):
        for row, off in ((y, 0), (y + 2, 2)):
            x = x0 - off
            while x <= x1:
                bx0, bx1 = max(x, x0), min(x + 3, x1)
                if bx0 <= bx1:
                    c.rect(bx0, row, bx1, row + 1, 'b'); c.hline(bx0, bx1, row, 'B'); c.hline(bx0, bx1, row + 1, 'N')
                x += 4
    bags(0, 11, 24); bags(20, 31, 24)
    c.rect(12, 25, 19, 27, 'N'); c.hline(12, 19, 25, 'b'); c.hline(12, 19, 27, 'n')   # gate ramp / driveway
    c.hline(0, 31, 28, 'n')
    c.outline('k')
    c.shadow(1, 30, 29)
