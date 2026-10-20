NAME = "Soryu"
KIND = "sea"

# JMSDF Soryu-class submarine, surfaced and riding low, bow to the RIGHT.
#   long dark charcoal hull barely above the water (y=19..22, waterline y=22), rounded bow,
#   tapering stern with the X-shaped stern planes (upper pair above the water, lower pair seen
#   faintly through it), tall slim sail forward of midships (x=17..21) with sail planes and a
#   team-colour band near its top, periscope masts; foam at the bow, wake behind.

def draw(c):
    # ---- hull: long low cigar, mostly at the waterline ------------------------------------
    c.poly([(3, 21), (7, 19.6), (25, 19.6), (28, 20), (30, 21.5), (30, 23), (3, 23)], 'K')
    c.hline(7, 26, 19, 's')                                       # lit deck casing
    c.hline(9, 24, 19, 'S'); c.px(27, 20, 's'); c.px(28, 20, 's')
    c.hline(4, 29, 22, 'k')
    c.stamp(25, 20, ['ss'])                                       # bow sonar dome highlight
    # ---- X-shaped stern planes (upper pair) ------------------------------------------------
    c.line(5, 19, 3, 16, 'K'); c.line(6, 19, 4, 16, 'K'); c.px(3, 16, 's'); c.px(4, 16, 's')
    c.line(8, 19, 7, 17, 'K'); c.px(7, 17, 's')
    # ---- sail (conning tower): tall slim, team band, sail planes ---------------------------
    c.poly([(17, 19), (17, 11), (18, 10), (21, 10), (22, 11), (22, 19)], 'K')
    c.vline(17, 11, 18, 's'); c.hline(18, 21, 10, 'S')
    c.hline(18, 21, 11, 'T'); c.hline(17, 22, 12, 't'); c.px(22, 12, 'u')
    c.hline(16, 23, 15, 's'); c.hline(16, 23, 16, 'K')            # sail planes
    c.px(23, 15, 'S')
    c.vline(21, 13, 14, 'Y')                                      # bridge window
    c.outline('k')
    # ---- masts on the sail (after the outline) ----------------------------------------------
    c.vline(19, 7, 9, 's'); c.px(19, 6, 'k')                      # periscope
    c.vline(21, 8, 9, 'S')                                       # snorkel / ESM mast
    # ---- water: hull lower edge awash, lower stern planes seen through the water --------------
    for x in range(1, 31):
        c.px(x, 23, 'a')
    c.px(4, 24, 'A'); c.px(3, 25, 'A'); c.px(6, 24, 'A')
    c.hline(5, 28, 24, 'A')
    # foam where the hull meets the water, bow wave, wake behind the stern
    row23 = ".cc.ccwccc.cccwcccc.ccccwcccwwww"
    row25 = "cc..c..cc..c...c...cc...c...ccw."
    for x in range(32):
        if row23[x] != '.': c.px(x, 23, row23[x])
        if row25[x] != '.': c.px(x, 25, row25[x])
    c.stamp(29, 21, ['w', 'ww'])
    c.px(31, 24, 'c'); c.px(0, 24, 'c'); c.px(1, 24, 'c')
