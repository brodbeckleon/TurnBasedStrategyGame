NAME = "F-15"
KIND = "air"

# Final design notes (revision 6)
# Revision 1 plan (32x32, nose right, pseudo-3/4 from above-side, airborne):
#  fuselage thin slab y11..16, x3..29 ; nose cone x25..29
#  twin tails: far x11..17,y6..11 (dark) ; near x5..10,y7..12 (light) -> 2px V gap
#  big swept near wing: root x9..18 at y14 -> clipped tip x5..10 at y25
#  small stabiliser + two stacked nozzles with orange glow at the rear
#  rectangular intake ahead of wing root, bubble canopy over it
#  shadow rows 28-29

def draw(c):
    # ---- far tail (behind the fuselage, darker) ---------------------------
    c.poly([(17, 11), (11, 11), (12, 6), (14, 6)], 'M')
    c.line(14, 6, 17, 11, 'S')
    c.vline(12, 7, 10, 'm')
    c.hline(12, 14, 8, 'T'); c.hline(12, 15, 9, 'u')
    # ---- fuselage ----------------------------------------------------------
    c.poly([(3, 12), (5, 11), (24, 11), (24, 16), (3, 16)], 'h')
    c.hline(6, 22, 11, 'g')                       # sunlit top deck
    c.hline(3, 24, 16, 'S')                       # belly shadow
    c.hline(4, 21, 15, 'g')                       # belly light strip
    # nose radome
    c.hline(25, 27, 12, 'g')
    c.stamp(25, 13, ['gSSSS',
                     'SSSss',
                     'sss..'])
    # camo blotches (dark blue-grey on top)
    c.rect(12, 12, 15, 12, 'M'); c.rect(13, 13, 14, 13, 'M'); c.hline(6, 8, 14, 'M')
    # ---- rear: two stacked nozzles ----------------------------------------
    c.rect(2, 11, 5, 13, 's'); c.hline(3, 5, 11, 'S')
    c.rect(2, 14, 5, 16, 'S'); c.hline(3, 5, 14, 'g')
    c.vline(2, 11, 16, 'K')
    # ---- near tail ---------------------------------------------------------
    c.poly([(10, 12), (5, 12), (5, 6), (7, 6)], 'h')
    c.line(7, 6, 10, 12, 'g')
    c.vline(5, 7, 12, 'M')
    c.hline(5, 7, 8, 'T'); c.hline(5, 8, 9, 't')
    # ---- stabiliser (near): small swept plate beside the nozzles ----------
    c.stamp(3, 17, ['SggG'])
    c.stamp(2, 18, ['SggG'])
    c.stamp(1, 19, ['SggG'])
    c.stamp(1, 20, ['sgG'])
    # ---- near wing: big clipped delta, root at y14, tip at y25 ------------
    c.poly([(18, 14), (9, 14), (5, 25), (10, 25)], 'g')
    c.line(18, 14, 10, 25, 'G')                     # sunlit leading edge
    c.line(9, 15, 5, 24, 'S')                      # trailing edge shade
    c.hline(9, 17, 14, 'h')                         # wing root seam
    c.hline(5, 10, 25, 's')
    c.rect(8, 22, 10, 24, 't'); c.hline(8, 10, 22, 'T')
    c.rect(11, 17, 14, 19, 'h'); c.hline(12, 13, 20, 'h'); c.hline(12, 13, 18, 'M')
    # ---- intake -------------------------------------------------------------
    c.rect(17, 12, 24, 17, 'S')
    c.hline(17, 24, 12, 'G')
    c.rect(21, 13, 23, 16, 'K')
    c.vline(24, 13, 17, 'g')
    c.hline(17, 23, 17, 's')
    # ---- canopy -------------------------------------------------------------
    c.poly([(19, 11), (20, 9), (23, 8), (25, 9), (26, 11)], 'y')
    c.rect(22, 10, 26, 11, 'Y')
    c.hline(21, 23, 9, 'w')
    c.outline('k')
    c.px(1, 12, 'F'); c.px(1, 15, 'F')
    c.shadow(8, 24, 29)
    c.shadow(11, 21, 30)
