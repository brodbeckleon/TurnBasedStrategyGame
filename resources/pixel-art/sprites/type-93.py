NAME = "Type 93"
KIND = "ground"

# Type 93 Kin-SAM: 6x6 truck, steeply raised boxy 2x2 launcher on an elevation
# cradle with a ram, EO ball on a cab-roof mast.  No dish, no flag.

# raised launcher box, hand drawn (origin x=4, y=6).  '.'=empty.
BOX = [
    #  0123456789012345678
    '.........SS.........',   # y6
    '........',
]


def nose(c, x, y, far=False):
    # base collar at (x..x+1, y), cone narrows up-right
    if far:
        c.px(x, y, 's'); c.px(x + 1, y, 's')
        c.px(x, y - 1, 'S'); c.px(x + 1, y - 1, 's')
        c.px(x + 1, y - 2, 'g'); c.px(x + 2, y - 2, 'S')
        c.px(x + 2, y - 3, 'G')
    else:
        c.px(x, y, 'S'); c.px(x + 1, y, 's')
        c.px(x, y - 1, 'g'); c.px(x + 1, y - 1, 'S')
        c.px(x + 1, y - 2, 'G'); c.px(x + 2, y - 2, 'g')
        c.px(x + 2, y - 3, 'w')


def draw(c):
    c.rect(3, 22, 29, 23, 'K')
    # raised box: rows y10..y19 (below 19 hidden by bed)
    rows = {
        9:  (13, 14),
        10: (12, 15),
        11: (11, 16),
        12: (10, 16),
        13: (10, 15),
        14: (9, 14),
        15: (8, 14),
        16: (8, 13),
        17: (7, 12),
        18: (6, 12),
        19: (6, 11),
    }
    for y, (a, b) in rows.items():
        for x in range(a, b + 1):
            ch = 'O'
            if x <= a + 1: ch = 'l'
            if x >= b - 1: ch = 'o'
            c.px(x, y, ch)
    # noses: far row first
    nose(c, 12, 8, far=True); nose(c, 15, 10, far=True)
    nose(c, 11, 10); nose(c, 14, 12)
    # bed
    c.rect(3, 19, 20, 22, 'O')
    c.hline(3, 20, 19, 'l'); c.hline(3, 20, 22, 'o')
    c.vline(20, 20, 22, 'o')
    for x in (9, 16): c.vline(x, 20, 21, 'o')
    c.rect(4, 20, 7, 20, 'N'); c.rect(11, 21, 14, 21, 'n'); c.rect(17, 20, 19, 21, 'N')
    c.px(5, 21, 'N'); c.px(6, 21, 'n')
    c.hline(11, 15, 20, 't'); c.hline(12, 15, 21, 'u')
    cab = [
        '.llllll...',
        'oYYYoyYS..',
        'oYYYYoyYS.',
        'oYYYYYoyYS',
        'OOOOOOOOOl',
        'OttttOOOOf',
        'OuuuuOOOKK',
        'ooooooOOKK',
    ]
    c.stamp(21, 15, cab)
    c.hline(28, 30, 22, 'S')
    wheel = ['.KsKK.',
             'KsKKKK',
             'KKSsKK',
             'KKssKK',
             'KKKKKK',
             '.KKKK.']
    for x0 in (3, 10, 22):
        c.stamp(x0, 22, wheel)
    c.outline('k')
    c.shift(0, 1)
    c.shadow(4, 29, 30)
