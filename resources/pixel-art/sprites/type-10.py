NAME = "Type 10"
KIND = "ground"

# Revision 3 (after blind critic round 2).  Final coordinates, no shift.
#  - turret: low, long, flat-roofed wedge in three clean planes: lit roof + 3px front-slope band
#    (constant 2px/row), mid flank, dark lower chamfer; flat vertical rear with a 45-degree top corner,
#    protruding gun mantlet, lit deck ledge + dark ring row separating it from the hull.
#  - gun ends at x=29 (2px clear of the cell edge): 3-row sleeve stepping down to a 2-row barrel,
#    dark muzzle tip, flat lit top line.
#  - camo: tan / brown blotches live on the hull skirt, glacis and the lower turret flank only; the turret
#    roof and lit slope stay clean teal (light / mid / dark).
#  - roof: ONE small gunner sight / RWS block (no twin bumps).
#  - team colour: short stubby flag on the turret rear, solid 3-row team panel in the skirt.

def draw(c):
    # ---- running gear: 5 big road wheels, lighter top run ------------------
    c.rect(2, 23, 25, 27, 'K')
    c.hline(2, 25, 23, 's')                       # upper track run, one tone lighter
    wheel = ['.SS.',
             'SgSs',
             'SSss',
             '.ss.']
    for x0 in (2, 7, 12, 17, 22):
        c.stamp(x0, 24, wheel)
    for x in (2, 25):                              # rounded track ends
        c.erase(x, 23); c.erase(x, 27)

    # ---- hull: deck ledge, thin segmented skirt, 45-degree glacis ------------
    c.hline(4, 23, 20, 'p')                        # lit deck ledge under the turret
    c.hline(3, 24, 21, 'E')
    c.hline(3, 25, 22, 'e')
    c.px(24, 21, 'p'); c.px(25, 22, 'E')           # glacis edge
    c.px(3, 21, 'p')
    for x in (8,):                          # skirt panel joints
        c.px(x, 21, 'e'); c.px(x, 22, 'k')

    # ---- turret: low faceted wedge ----------------------------------------
    rows = {14: (8, 13), 15: (7, 15), 16: (6, 17), 17: (6, 19), 18: (6, 21), 19: (6, 21)}
    for y, (x0, x1) in rows.items():
        c.hline(x0, x1, y, 'E')
    # lit planes: flat roof + the long front slope as a 3px band, constant 2px/row
    c.hline(8, 13, 14, 'p')
    c.px(7, 15, 'p')
    c.hline(13, 15, 15, 'p'); c.hline(15, 17, 16, 'p'); c.hline(17, 19, 17, 'p'); c.hline(19, 21, 18, 'p')
    # facet seam behind the slope band, parallel to it (solid 2px steps), flank shadow, turret ring shadow
    c.hline(7, 17, 18, 'e')                         # lower flank chamfer (shadow plane)
    c.hline(6, 21, 19, 'e')
    c.vline(6, 16, 18, 'e')
    # gun mantlet protruding from the slope
    c.rect(18, 16, 19, 18, 'K')
    c.px(18, 16, 'S'); c.px(19, 16, 's')

    # ---- camouflage: hull + glacis + lower turret flank only -----------------
    c.hline(5, 9, 21, 'b'); c.hline(6, 8, 20, 'B'); c.hline(4, 7, 22, 'N')
    c.hline(10, 12, 22, 'n'); c.px(11, 21, 'N'); c.px(12, 21, 'N')
    c.hline(23, 24, 22, 'N'); c.px(23, 21, 'N')
    c.hline(7, 10, 17, 'N'); c.hline(7, 11, 18, 'N'); c.hline(8, 9, 18, 'n')

    # ---- team colour: solid 3-row skirt panel + stubby flag -----------------
    c.hline(14, 20, 20, 'T'); c.hline(14, 20, 21, 't'); c.hline(14, 20, 22, 'u')
    c.px(13, 20, 'e'); c.px(13, 21, 'e'); c.px(13, 22, 'k')
    c.px(21, 20, 'e'); c.px(21, 21, 'e'); c.px(21, 22, 'k')
    c.stamp(4, 10, ['TTTT', 'tttt', 'uuu.'])

    # ---- roof gear: single gunner sight block -------------------------------
    c.stamp(12, 12, ['gSS',
                     'sSY'])

    c.outline('k')

    # ---- after the outline: thin things ---------------------------------------
    c.vline(8, 10, 13, 'S')                    # short flag pole standing on the roof
    # 120mm smoothbore: 3-row sleeve stepping down to a long 2-row barrel, dark muzzle tip
    for x in range(20, 30):
        c.px(x, 15, 'k')
    for x in range(20, 24):
        c.px(x, 16, 'g'); c.px(x, 17, 'S'); c.px(x, 18, 's'); c.px(x, 19, 'k')
    for x in range(24, 30):
        c.px(x, 16, 'S'); c.px(x, 17, 's'); c.px(x, 18, 'k')
    c.px(24, 19, '.')
    c.px(28, 16, 's'); c.px(29, 16, 'K'); c.px(28, 17, 'K'); c.px(29, 17, 'K')
    c.shadow(3, 26, 29)
