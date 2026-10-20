NAME = "Army Base"
KIND = "ground"

def draw(c):
    # ---- vehicle garage: low arched (Quonset-style) olive roof ----------
    c.ellipse(15.5, 15, 12, 7, 'O')                                                    # arched corrugated roof
    c.ellipse(14, 14, 9, 5, 'l'); c.ellipse(12, 12, 5, 2, 'L')                         # lit curve of the roof
    for x in (8, 13, 18, 23):
        for y in range(8, 14):
            if c.get(x, y) != '.': c.px(x, y, 'o')                                     # corrugation ribs
    c.rect(4, 14, 27, 27, 'l'); c.rect(23, 14, 27, 27, 'O'); c.vline(4, 14, 27, 'L')   # front wall
    c.hline(4, 27, 14, 't'); c.hline(4, 27, 15, 'u'); c.hline(4, 10, 14, 'T')          # team band under the eaves
    # ---- big roll-up door -------------------------------------------------
    c.rect(7, 17, 20, 27, 'g')
    for y in range(18, 27, 2): c.hline(8, 19, y, 'S')                                  # door slats
    c.hline(7, 20, 17, 's'); c.vline(7, 17, 27, 's'); c.vline(20, 17, 27, 's')        # door frame
    c.rect(8, 26, 19, 27, 'K')                                                         # door half-open: dark gap
    c.rect(12, 21, 15, 22, 'w'); c.rect(13, 21, 14, 22, 't')                           # unit number plate
    # ---- side door + window -----------------------------------------------
    c.stamp(22, 18, ['yy', 'YY']); c.stamp(25, 18, ['y', 'Y'])
    c.rect(23, 22, 25, 27, 'K'); c.hline(23, 25, 22, 'S')
    # ---- flag pole (left) -------------------------------------------------
    c.vline(1, 6, 27, 's'); c.px(1, 6, 'G')
    c.stamp(2, 6, ['TTTTT', 'tttttt', 'ttttu.', 'tuu...'])
    # ---- sandbag emplacement (front left) + crates (right) ------------------
    def bags(x0, x1, y):
        for row, off in ((y, 0), (y + 2, 2)):
            x = x0 - off
            while x <= x1:
                bx0, bx1 = max(x, x0), min(x + 3, x1)
                if bx0 <= bx1:
                    c.rect(bx0, row, bx1, row + 1, 'b'); c.hline(bx0, bx1, row, 'B'); c.hline(bx0, bx1, row + 1, 'N')
                x += 4
    bags(0, 6, 24)
    c.rect(27, 23, 30, 27, 'N'); c.hline(27, 30, 23, 'b'); c.vline(30, 23, 27, 'n'); c.line(27, 24, 30, 27, 'n')   # crate
    c.rect(28, 20, 30, 22, 'O'); c.hline(28, 30, 20, 'l')                                                         # ammo box
    c.hline(0, 31, 28, 'n')
    c.outline('k')
    c.shadow(1, 30, 29)
