NAME = "Type 16"
KIND = "ground"

# Silhouette plan (32x32, faces right)  - r6 after blind review round 2
#  1. FOUR big separate 6x6 tyres (own k ring, dark tread, clear 2x2 steel hub), two close pairs with a
#     grass gap between the pairs; the hull belly is lifted so the ground shows between the wheels: WHEELED, not tracked
#  2. low boxy hull, shallow 45deg glacis, wheel arches
#  3. LOW faceted wedge turret: flat roof, bevelled front facet, chamfered rear, 3 planes of tone
#  4. 105mm gun: 3x4 mantlet, 2px sleeve, 1px barrel, boxy muzzle brake. Tip at x=29 (2px margin)
#  5. roof RWS / sight box with a lens and stub gun, whip antenna with team pennant, team patch on the turret, hull stripe
#  6. camo: ONE olive base, two clean patch colours (brown N, tan b), blobs >= 3x2, no speckle

WHEEL = ['.kkkk.',
         'kKsKKk',
         'kKggKk',
         'kKSsKk',
         'kKKKKk',
         '.kkkk.']

def draw(c):
    # ---- hull -----------------------------------------------------------
    c.poly([(2, 23), (2, 20), (3, 19), (24, 19), (27, 22), (27, 23)], 'O')
    c.hline(3, 23, 19, 'l'); c.hline(4, 9, 19, 'L'); c.hline(17, 22, 19, 'L'); c.line(24, 19, 27, 22, 'l')   # lit deck edge + glacis edge
    c.hline(2, 27, 23, 'o')                                         # fender / underside shade
    c.hline(3, 26, 22, 'o')                                         # lower flank shade
    c.px(2, 20, 'l')
    # camo blotches on the hull flank: two patch colours, solid blobs
    c.stamp(3, 20, ['nnn', 'nnnnn', '.nnn'])
    c.stamp(9, 20, ['.bbb', 'bbbbb', '.bbb'])
    c.stamp(21, 20, ['.nnnn', 'nnnnnn', '.nnnnn'])
    # team stripe on the hull flank (compact, 6x3)
    c.hline(14, 19, 20, 'T'); c.hline(14, 19, 21, 't'); c.hline(14, 19, 22, 'u')

    # ---- turret: LOW faceted wedge, 45deg facets -------------------------
    c.stamp(6, 12, [
        '...LLLLLLLL....',
        '..lllllllllL...',
        '.lTTObbbbOllL..',
        'lOttObbbbnnllLl',
        'lOOOOOOOnnnnSKl',
        'ooooooooooooooo'])
    c.hline(6, 20, 18, 'k')                                          # turret ring seam
    # roof RWS / sight box: attached to the roof, lens facing forward
    c.stamp(9, 9, ['gGSs', 'SSsY', 'ssss'])

    c.outline('k')

    # whip antenna + team pennant (after outline: thin lines)
    c.vline(4, 8, 18, 's')
    c.stamp(5, 6, ['TTT', 'ttt', '.tu'])
    c.px(4, 6, 's'); c.px(4, 7, 's')
    # RWS stub gun
    c.hline(13, 14, 10, 'S')

    # ---- mantlet + gun (own outline, drawn after) -----------------------
    c.rect(21, 13, 24, 18, 'k')
    c.rect(21, 14, 23, 17, 'S'); c.vline(23, 14, 17, 's'); c.hline(21, 23, 14, 'g')
    # barrel: 2px sleeve (x=24..26), 1px neck (x=27), boxy muzzle brake (x=28..29)
    c.hline(24, 26, 14, 'k'); c.hline(24, 26, 15, 'S'); c.hline(24, 26, 16, 's'); c.hline(24, 26, 17, 'k')
    c.hline(27, 27, 15, 'k'); c.hline(27, 27, 16, 'S'); c.hline(27, 27, 17, 'k')
    c.stamp(28, 14, ['kk', 'gS', 'ss', 'kk'])
    c.px(24, 18, '.')

    # ---- wheels (after outline: own k ring) ---------------------------
    for x0 in (1, 8, 16, 23):
        c.stamp(x0, 23, WHEEL)
    c.shadow(2, 28, 29)
