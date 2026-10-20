NAME = "Type 89"
KIND = "ground"

# JGSDF Type 89 IFV (round 3): tall boxy troop hull with a long flat roof, SMALL turret set
# behind a front deck (driver hatch), overlapping angular ATGM pod, short thick 35mm cannon,
# sloped glacis (right), rear ramp door (left), six road wheels with idler + sprocket.
# bbox x=2..30, y=10(flag)..29.  Everything important at y>=10.

def draw(c):
    # ---- running gear: track band, six 3x3 road wheels (rear idler / front sprocket have hubs)
    c.rect(3, 24, 27, 27, 'K')
    for y in (25, 26):
        c.px(2, y, 'K'); c.px(28, y, 'K')
    for x0 in (8, 12, 16, 20):
        c.stamp(x0, 24, ['KgK', 'SKS', 'KsK'])            # round road wheel, dark hub
    for x0 in (4, 24):
        c.stamp(x0, 24, ['gSS', 'SgS', 'sSs'])            # idler / sprocket: full ring

    # ---- hull: tall flat-roofed box, vertical rear, sloped glacis --------------------
    c.poly([(2, 23), (2, 19), (3, 18), (23, 18), (28, 22), (28, 23)], 'O')
    c.hline(3, 23, 18, 'l')                                      # roof light edge
    c.rect(7, 22, 25, 22, 'o'); c.hline(7, 26, 23, 'k')          # skirt shade + bottom edge
    # sloped glacis: light upper plate, mid nose plate, dark chin, headlamp
    c.hline(24, 26, 19, 'l'); c.hline(25, 27, 20, 'l')
    c.hline(26, 27, 21, 'O'); c.px(27, 22, 'o')
    c.px(27, 21, 'S')                                            # headlamp
    # rear ramp door: recessed panel, frame, handle, hinge line
    c.rect(3, 19, 6, 22, 'o'); c.hline(3, 6, 19, 'O')
    c.rect(4, 20, 5, 22, 'O'); c.px(5, 21, 'g')
    c.vline(7, 19, 23, 'k')
    # roof: driver hatch on the front deck (+ periscope), small troop hatch at the rear
    c.rect(20, 16, 22, 17, 'O'); c.hline(20, 22, 16, 'l'); c.px(22, 17, 'S')
    c.rect(5, 16, 6, 17, 'O'); c.hline(5, 6, 16, 'l')
    # camouflage (flat brown patches, >=2px thick)
    c.rect(12, 20, 15, 21, 'N'); c.rect(16, 21, 17, 22, 'n')
    c.rect(20, 20, 22, 21, 'N'); c.px(19, 21, 'N')
    c.rect(8, 22, 9, 22, 'N')
    # team colour: skirt tag at the rear
    c.hline(8, 10, 20, 't'); c.px(8, 20, 'T'); c.hline(8, 10, 21, 't'); c.px(10, 21, 'u')

    # ---- turret + ATGM pod ------------------------------------------------------------
    # turret: one slab, light roof row, mid body, dark bottom row, vertical gun face
    c.poly([(11, 17), (11, 14), (17, 14), (19, 15), (19, 17)], 'O')
    c.hline(12, 17, 14, 'L'); c.px(18, 14, 'l'); c.hline(11, 18, 15, 'l')
    c.hline(11, 19, 17, 'o')
    c.rect(14, 15, 15, 16, 'N')                                  # camo blotch
    c.rect(16, 12, 17, 13, 'S'); c.hline(16, 17, 12, 'g')        # gunner sight
    c.rect(12, 13, 13, 13, 'o')                                  # commander hatch
    c.rect(18, 16, 19, 17, 't'); c.hline(18, 19, 17, 'u')        # turret team tag
    # ATGM pod: angular box on the rear of the turret
    c.rect(7, 14, 11, 17, 'o')
    c.hline(7, 11, 14, 'O')
    c.px(7, 14, '.')                                             # chamfer
    c.rect(8, 15, 9, 16, 'S'); c.hline(8, 9, 15, 'g')            # steel housing
    c.px(10, 15, 'K'); c.px(10, 16, 'K')                         # tube mouths
    c.vline(11, 14, 17, 'k')                                     # seam pod | turret

    c.outline('k')

    # ---- thin things after the outline ---------------------------------------------------
    c.vline(3, 11, 17, 'k')                                      # 1px whip antenna (hull rear)
    c.stamp(4, 10, ['TTTk', 'ttuk', 'kkk.'])                     # team pennant
    # 35mm cannon: mantlet, thick sleeve, slimmer barrel, bright muzzle tip
    c.stamp(20, 14, ['.kkkkkkkkk',
                     'KKgggggggk',
                     'KKSSSSSkkk',
                     '.kkkkkk...'])
    c.px(28, 15, 'G')
    c.shadow(4, 26, 29)
