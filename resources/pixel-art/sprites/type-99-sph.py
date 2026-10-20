NAME = "Type 99 SPH"
KIND = "ground"

# Silhouette plan (32x32, faces right, columns 0 and 31 stay empty)
#  1. LONG slim 155mm barrel on a clean 3:2 line (~34 deg), 2px tube with a
#     light top edge, slotted 3px muzzle brake at the tip (x=26..29, y=7..10)
#  2. HUGE flat-sided boxy turret x=2..16, y=10..18 -- taller than the hull,
#     dark seam to the hull, rear bustle overhang, framed rear ammo door
#  3. flat low hull, sloped glacis, six road wheels, big rear drive sprocket
#  4. team: pennant on rear antenna + one thin hull stripe
#  camo kept to olive + ONE brown, in a few straight-edged blocks


def draw(c):
    # ---- track band + wheels ------------------------------------------
    c.rect(4, 23, 28, 27, 'K')
    c.px(29, 24, 'K'); c.px(29, 25, 'K'); c.px(29, 26, 'K')
    for x0 in (9, 12, 15, 18, 21, 24):
        c.stamp(x0, 24, ['.S.', 'sKs', '.s.'])
    c.stamp(27, 24, ['SS', 'sK', 'ss'])             # front idler
    # ---- hull (low, flat) -------------------------------------------------
    c.poly([(3, 23), (3, 20), (4, 19), (23, 19), (29, 22), (29, 23)], 'O')
    c.rect(4, 20, 26, 22, 'O')
    c.hline(18, 23, 19, 'l')                         # light deck edge
    c.hline(4, 28, 22, 'o'); c.hline(27, 29, 22, 'o')
    # driver hatch on the glacis deck
    c.rect(21, 17, 23, 18, 'o'); c.hline(21, 23, 17, 'l'); c.px(22, 18, 'K')
    # ---- drive sprocket (rear, big) ----------------------------------------
    c.ellipse(5, 24, 3, 3, 'S')
    c.ellipse(5, 24, 2, 2, 's')
    c.ellipse(5, 24, 1, 1, 'S')
    c.px(5, 24, 'K')
    c.px(3, 22, 'g'); c.px(4, 21, 'g'); c.px(5, 21, 'g'); c.px(2, 23, 'g')
    # ---- turret: HUGE flat-sided box, only a small chamfer at the front corner ----
    c.poly([(2, 18), (2, 10), (3, 9), (14, 9), (16, 11), (16, 18)], 'O')
    c.hline(3, 14, 9, 'L'); c.hline(2, 15, 10, 'l'); c.px(15, 10, 'l')
    c.hline(2, 16, 18, 'o')                          # dark underside row
    # commander cupola + vision slit
    c.rect(10, 7, 13, 8, 'o'); c.hline(10, 13, 7, 'S'); c.px(11, 7, 'g'); c.px(13, 7, 's')
    c.hline(11, 12, 8, 'K')
    # rear ammo door: framed panel with a latch
    c.rect(3, 12, 7, 17, 'o'); c.frame(3, 12, 7, 17, 'n')
    c.px(4, 13, 'K'); c.px(4, 16, 'K'); c.vline(6, 14, 15, 'S')
    # panel seam
    c.vline(8, 11, 17, 'o')
    # camo: olive + one brown, a few straight-edged blocks, none on the roof plane
    c.rect(9, 11, 12, 12, 'N'); c.rect(9, 13, 10, 13, 'N')
    c.rect(12, 16, 15, 17, 'N')
    c.rect(9, 20, 13, 21, 'N')
    c.rect(24, 21, 27, 22, 'N')
    # ---- team colour: one thin hull stripe + rear antenna pennant ---------------
    c.hline(17, 21, 21, 't'); c.px(22, 21, 'u'); c.hline(18, 20, 20, 'T')
    c.stamp(5, 7, ['TTT', 'ttu'])
    # ---- flush mantlet (small) ---------------------------------------------------
    c.rect(15, 14, 16, 17, 'S'); c.hline(15, 16, 14, 'g'); c.hline(15, 16, 17, 's'); c.vline(16, 15, 16, 's')
    # ---- barrel body: 2px tube along a 3:2 line, slotted brake at the tip -----------
    for x in range(17, 30):
        yc = 15.5 - (2.0 / 3.0) * (x - 17)
        bot = int(yc + 0.5); top = bot - 1
        if x >= 26:                                  # muzzle brake: 3 rows, slotted
            c.px(x, top, 'g'); c.px(x, top + 1, 'S'); c.px(x, top + 2, 's')
            if x == 27: c.px(x, top, 'K'); c.px(x, top + 1, 'K'); c.px(x, top + 2, 'K')
        else:
            c.px(x, top, 'g'); c.px(x, bot, 'S')
            if 21 <= x <= 24:                        # bore evacuator: paler sleeve, same thickness
                c.px(x, top, 'G'); c.px(x, bot, 'g')
    # ---- outline ------------------------------------------------------------
    c.outline('k')
    c.vline(4, 7, 8, 's'); c.px(4, 6, 'k'); c.px(3, 7, 'k')             # antenna pole, after the outline
    c.hline(2, 17, 19, 'k')                            # dark seam between turret and hull
    c.shadow(1, 30, 29)
