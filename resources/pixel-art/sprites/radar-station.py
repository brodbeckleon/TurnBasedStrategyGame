NAME = "Radar Station"
KIND = "ground"

def draw(c):
    # ---- lattice mast ---------------------------------------------------
    c.poly([(11, 15), (16, 15), (18, 21), (9, 21)], 'S')
    c.line(11, 15, 9, 20, 'g'); c.line(16, 15, 18, 20, 's')
    c.line(10, 16, 17, 20, 's'); c.line(17, 16, 10, 20, 's')           # X-bracing
    c.rect(11, 14, 16, 15, 'K'); c.hline(11, 16, 14, 's')               # rotator turntable
    # ---- big parabolic dish, tilted up and to the right ------------------
    c.ellipse(13, 10, 3, 4, 'S')                                         # dish seen from the side, opening right
    c.ellipse(14, 10, 2, 3, 'G'); c.ellipse(15, 10, 1, 2, 'g')            # concave face: lit rim -> shaded centre
    c.vline(15, 9, 11, 'S')
    c.line(15, 6, 22, 9, 's'); c.line(15, 14, 22, 10, 's')              # feed-horn struts
    c.rect(22, 9, 24, 10, 'g'); c.px(24, 9, 'G'); c.px(25, 9, 'r')      # feed horn + red aviation light
    c.rect(9, 9, 10, 12, 'K')                                           # elevation motor behind the dish
    c.stamp(10, 8, ['t', 'T', 't', 't', 'u'])                            # team stripe on the dish back
    c.vline(13, 14, 14, 's')                                            # dish yoke
    # ---- operations bunker ------------------------------------------------
    c.rect(2, 21, 27, 27, 'l'); c.rect(23, 21, 27, 27, 'O'); c.vline(2, 21, 27, 'L')
    c.rect(1, 19, 28, 20, 'g'); c.hline(1, 28, 19, 'w'); c.hline(1, 28, 20, 'S')      # concrete roof slab
    c.hline(2, 27, 21, 't'); c.hline(2, 27, 22, 'u'); c.hline(2, 8, 21, 'T')          # team band
    c.stamp(4, 24, ['yy', 'YY']); c.stamp(19, 24, ['yyy', 'YYY'])                     # screen-lit windows
    c.rect(13, 23, 16, 27, 'K'); c.hline(13, 16, 23, 'S')                              # door
    c.ellipse(24, 18, 3, 3, 'G'); c.ellipse(23, 17, 1, 1, 'w'); c.rect(25, 16, 26, 18, 'g')   # small radome on the roof
    c.px(27, 18, 'S'); c.px(26, 16, 'g'); c.hline(21, 27, 18, 'S')
    c.rect(20, 19, 28, 20, 'g'); c.hline(20, 28, 19, 'w'); c.hline(20, 28, 20, 'S')        # (re-lay roof under the dome)
    # ---- whip antenna + generator ---------------------------------------
    c.vline(4, 11, 18, 's'); c.px(4, 11, 'G'); c.px(3, 13, 's'); c.px(5, 13, 's')
    c.rect(28, 23, 31, 27, 'O'); c.hline(28, 31, 23, 'l'); c.vline(31, 23, 27, 'o')   # generator box
    c.hline(29, 30, 25, 'o'); c.px(29, 22, 's')                                       # vent + exhaust stub
    c.hline(0, 31, 28, 'n')
    c.outline('k')
    c.shadow(1, 30, 29)
