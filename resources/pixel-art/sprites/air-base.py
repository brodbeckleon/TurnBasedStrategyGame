NAME = "Air Base"
KIND = "ground"

def draw(c):
    # ---- arched aircraft hangar (left) ---------------------------------
    c.ellipse(9, 25, 9, 13, 'S')                                        # half-round steel shell
    c.ellipse(8, 25, 7, 12, 'g'); c.ellipse(6, 25, 3, 11, 'G')          # lit curve (barrel shading)
    for x in (3, 9, 15):
        for y in range(10, 25):
            if c.get(x, y) in 'gGS': c.px(x, y, 'S' if x < 15 else 's')  # ribs
    c.ellipse(9, 25, 5, 8, 'K')                                         # open hangar mouth
    c.hline(6, 12, 17, 's')                                             # door rail
    c.hline(6, 13, 22, 'G'); c.hline(7, 12, 23, 'S'); c.px(14, 22, 'g'); c.stamp(10, 21, ['yY'])   # parked jet nose inside
    c.vline(7, 19, 21, 'S')                                             # its tail fin
    c.hline(3, 15, 14, 't'); c.hline(4, 14, 15, 'u'); c.hline(3, 7, 14, 'T')   # team stripe over the doors
    c.px(3, 15, 'S'); c.px(15, 15, 's')
    c.rect(0, 25, 18, 31, '.')
    # ---- control tower (right of centre) --------------------------------
    c.rect(20, 15, 24, 24, 'g'); c.vline(20, 15, 24, 'G'); c.vline(24, 15, 24, 'S')
    c.stamp(21, 18, ['y', 'Y']); c.stamp(21, 21, ['y', 'Y'])           # stair windows
    c.rect(18, 13, 26, 14, 'S'); c.hline(18, 26, 14, 's')               # catwalk
    c.rect(18, 9, 26, 12, 'y'); c.hline(18, 26, 12, 'Y')                # glass cab
    for x in (18, 21, 23, 26): c.vline(x, 9, 12, 'Y')                   # mullions
    c.px(19, 9, 'w')
    c.rect(17, 7, 27, 8, 'g'); c.hline(17, 27, 7, 'G')                 # roof
    c.px(22, 6, 'r')                                                    # beacon
    c.hline(21, 23, 15, 't'); c.hline(21, 23, 16, 'u')                  # team band on the shaft
    # ---- windsock (far right) -----------------------------------------
    c.vline(30, 12, 24, 's'); c.px(30, 12, 'G')
    c.stamp(27, 13, ['TwT', 'twt', '.u.'])
    # ---- runway strip ---------------------------------------------------
    c.rect(0, 25, 31, 28, 'K'); c.hline(0, 31, 25, 'S'); c.hline(0, 31, 28, 's')
    for x in range(1, 31, 6): c.hline(x, x + 2, 27, 'w')               # centre-line dashes
    c.outline('k')
    c.shadow(1, 30, 29)
