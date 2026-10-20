NAME = "F-2"
KIND = "air"

# Revision 2 plan (nose RIGHT, 32x32, everything y>=6)
#  fuselage   slim: rows 13..17 mid, tapering to a pointed nose (tip x=29,y=15)
#  fin        swept trapezoid rows 7..12, trailing edge ~vertical, team tip rows 7-8
#  spine      F-16 style hump rows 12 between fin and canopy
#  canopy     blown bubble x15..23, rows 10..12
#  chin intake dark navy mouth under/behind the cockpit, rows 18..19, x19..27
#  near wing  swept trapezoid rows 17..21, root x11..19 -> tip x5..11
#  stabiliser small trapezoid behind the wing, rows 18..20
#  2 ASM-2    pale slim missiles hung under the wing (A inboard/forward, B outboard)


def draw(c):
    # ---- horizontal stabiliser (small trapezoid, tail) ---------------------
    for y, x0, x1 in ((17, 3, 8), (18, 2, 7), (19, 1, 6), (20, 1, 4)):
        c.hline(x0, x1, y, 'h')
    c.px(2, 18, 'M'); c.px(1, 19, 'M'); c.hline(1, 2, 20, 'M')       # trailing edge shade
    c.px(7, 18, 'g'); c.px(6, 19, 'g'); c.px(4, 20, 'g')              # leading edge light

    # ---- vertical fin: swept trapezoid -------------------------------------
    for y, x0, x1 in ((7, 3, 5), (8, 3, 6), (9, 4, 7), (10, 4, 8), (11, 4, 9), (12, 4, 10)):
        c.hline(x0, x1, y, 'h')
    c.vline(4, 9, 12, 'M')                                            # trailing edge shade
    c.px(6, 9, 'g'); c.px(7, 10, 'g'); c.px(8, 11, 'g'); c.px(9, 12, 'g')   # leading edge light
    c.hline(3, 5, 7, 'T'); c.hline(3, 6, 8, 't')                      # team tip

    # ---- fuselage (slim) ---------------------------------------------------
    for y, x0, x1 in ((13, 4, 22), (14, 3, 27), (15, 2, 29), (16, 3, 28), (17, 4, 25)):
        c.hline(x0, x1, y, 'h')
    c.hline(11, 14, 12, 'M')                                          # spine hump joining fin and canopy
    c.hline(4, 22, 13, 'M')                                           # darker top paint
    c.hline(4, 20, 14, 'M')
    c.hline(5, 25, 17, 'g')                                           # pale belly
    c.hline(5, 18, 16, 'g')
    # radome
    c.hline(25, 27, 14, 'G'); c.hline(25, 29, 15, 'g'); c.hline(25, 28, 16, 'g')
    c.px(29, 15, 'S'); c.px(28, 16, 'S')
    c.vline(24, 14, 16, 'S')                                          # radome joint
    # team band
    c.hline(10, 12, 14, 'T'); c.hline(10, 12, 15, 't')

    # ---- near wing: swept trapezoid ---------------------------------------
    for y, x0, x1 in ((17, 11, 19), (18, 9, 17), (19, 8, 15), (20, 6, 13), (21, 5, 11)):
        c.hline(x0, x1, y, 'h')
    c.line(19, 17, 11, 21, 'g')                                       # leading edge light
    c.line(11, 17, 5, 21, 'M')                                        # trailing edge shade
    c.hline(11, 17, 17, 'S')                                          # root shadow

    # ---- chin intake: rounded scoop under/behind the cockpit ---------------
    c.hline(19, 26, 18, 'g')                                          # lip
    c.hline(20, 27, 19, 's')
    c.hline(22, 26, 20, 's')
    c.rect(24, 18, 27, 19, 'm'); c.hline(23, 26, 20, 'm')
    c.hline(24, 27, 18, 'M')                                          # lit upper rim of the mouth
    c.hline(25, 27, 19, 'k')                                          # deep throat

    # ---- canopy ------------------------------------------------------------
    c.hline(17, 20, 10, 'y'); c.hline(16, 22, 11, 'y'); c.hline(15, 23, 12, 'Y')
    c.px(21, 10, 'Y'); c.px(22, 11, 'Y'); c.px(23, 11, 'Y')
    c.px(17, 10, 'w')
    c.px(15, 11, 'Y')

    # ---- missiles (before the outline so they get one clean wrap) ---------
    for mx, my in ((13, 21), (3, 23)):
        c.hline(mx + 1, mx + 7, my, 'w'); c.px(mx + 8, my, 'G'); c.px(mx + 9, my, 's')
        c.hline(mx, mx + 8, my + 1, 'G'); c.px(mx + 9, my + 1, 'K')
        c.px(mx, my + 1, 'S')
        c.px(mx + 1, my - 1, 'S'); c.px(mx + 2, my - 1, 's')          # tail fin
        c.px(mx + 6, my + 1, 'g')                                     # mid fin hint
    # ---- nozzle -------------------------------------------------------------
    c.rect(2, 14, 3, 16, 'S'); c.hline(2, 3, 16, 's')
    c.vline(1, 14, 15, 'F'); c.px(2, 15, 'f')

    c.outline('k')

    c.shadow(8, 23, 28)
    c.shadow(11, 20, 29)
