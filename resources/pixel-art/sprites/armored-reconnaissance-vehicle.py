NAME = "Armored Reconnaissance Vehicle"
KIND = "ground"

# Silhouette plan (32x32, faces right)  -- revision 3
#   overall box      x 2..29, y 6..28  (rows 0-5 stay clear for the HP bar)
#   mast             slim telescoping pole x=8 y 9..12, collar, 3x2 base bracket on the rear deck,
#                    4x2 sensor head x 7..10 y 7..8 with one cyan lens pixel facing front
#   rear whip        x=4, y 8..14
#   turret + 25mm    small turret x 14..23 y 11..14, thin barrel x 24..29 (k cap at x=29)
#   pennant whip     x=15, y 6..10, pennant x 16..19 y 7..8
#   hull             x 3..27, y 15..24, sloped glacis on the right, sill above the axle line
#   three tyres      5px discs (7px with outline) x0 = 4 / 13 / 22, y 23..27, wide gaps


def tyre(c, x0, top, mid):
    c.ellipse(x0 + 2, 25, 3, 3, 'k', filled=True)          # wheel arch ring
    c.stamp(x0, 23, [
        '.sKK.',
        's' + 'K' + top + 'KK',
        'K' + mid + 'K',
        'KKsKk',
        '.KkK.',
    ])


def draw(c):
    # ---- hull ---------------------------------------------------------------
    c.hline(5, 21, 15, 'L')                       # deck top edge (lit)
    c.hline(4, 23, 16, 'l')
    c.hline(3, 25, 17, 'O')
    c.rect(3, 18, 27, 24, 'O')
    c.hline(22, 23, 16, 'L'); c.hline(24, 25, 17, 'l'); c.hline(26, 27, 18, 'l')   # glacis, lit
    c.rect(3, 20, 27, 24, 'o')                    # shadowed lower side / sill
    c.rect(26, 19, 27, 22, 'O')                   # nose plate
    c.hline(25, 27, 23, 'S')                      # bumper
    c.px(27, 21, 'f')                             # headlamp
    c.px(24, 18, 'Y'); c.px(25, 18, 'y')          # driver vision block
    # camo blobs (few, big)
    c.stamp(5, 16, ['.NNNn', 'NNNnn', '.nnn.'])
    c.stamp(19, 16, ['.NNN', 'NNNn', '.nnn'])
    # engine deck louvres + side panel seam
    c.stamp(11, 16, ['sSs'])
    c.vline(11, 17, 19, 'o')
    # team id patch on the hull side (4x2)
    c.stamp(14, 18, ['TTTT', 'tttu'])

    # ---- wheels -------------------------------------------------------------
    tyre(c, 4, 'S', 'SgS')
    tyre(c, 13, 'g', 'SGS')
    tyre(c, 22, 'S', 'SgS')

    # ---- turret (small) -----------------------------------------------------
    c.poly([(14, 14), (14, 12), (15, 11), (20, 11), (21, 12), (22, 13), (22, 14)], 'O')
    c.hline(15, 20, 11, 'L'); c.hline(15, 21, 12, 'l')
    c.hline(14, 22, 14, 'o')
    c.stamp(16, 12, ['NNNN', '.nnN'])
    c.px(21, 12, 'Y'); c.px(21, 13, 'K')                                    # gunner sight, smoke launcher

    # ---- mast (slim, telescoping) --------------------------------------------
    c.rect(7, 13, 9, 14, 'S'); c.hline(7, 9, 13, 'g'); c.hline(7, 9, 14, 's')   # base bracket
    c.vline(8, 9, 12, 'S')                                                      # pole
    c.hline(7, 9, 11, 'g')                                                      # telescoping collar
    c.stamp(7, 7, ['gGgg', 'SSSy'])                                             # sensor head + lens
    # pennant on the turret whip
    c.stamp(16, 7, ['TTTt', 'ttu.'])

    c.outline('k')

    # ---- thin things after the outline --------------------------------------
    c.vline(4, 8, 14, 's'); c.px(4, 8, 'S')      # rear whip
    c.vline(15, 6, 10, 's'); c.px(15, 6, 'S')    # pennant whip
    # 25mm cannon: one thin steel line (outlined above/below) + small fume extractor near the breech
    c.stamp(23, 12, [
        'kkkkkkk',
        'SSSSSSk',
        'kkkkkkk',
    ])
    c.stamp(24, 11, ['kk', 'Sg'])
    c.shadow(5, 27, 29)
