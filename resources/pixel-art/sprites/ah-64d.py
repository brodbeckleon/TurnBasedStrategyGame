NAME = "AH-64D"
KIND = "air"

# AH-64D Apache Longbow, side view, nose RIGHT, olive drab.
#  - Longbow radar dome on top of the mast (y=6..7), main rotor blades as one thin line at y=8
#    (drawn after the outline so it stays 1px), short mast, engine/transmission hump.
#  - stepped tandem canopy: raised pilot seat behind, gunner seat lower in front; pointed nose
#    with the sensor turret (TADS) and the 30mm chin gun under it.
#  - engine nacelle with dark exhaust on the flank, stub wing carrying a rocket pod and a
#    rack of Hellfire missiles; main wheels + tail wheel.
#  - long thin tail boom to a swept fin (team colour) with the tail rotor and the stabiliser.

def draw(c):
    # ---- tail boom: thin, tapering to the left -------------------------------------------
    c.poly([(3, 13), (12, 12), (12, 16), (3, 15)], 'O')
    c.hline(3, 12, 13, 'l'); c.hline(4, 12, 15, 'o')
    # swept vertical fin with team colour
    c.poly([(1, 9), (4, 9), (6, 15), (2, 15)], 'O')
    c.stamp(1, 9, ['TTT', '.ttt', '.ttu', '..tu'])
    c.hline(2, 6, 16, 'o'); c.hline(1, 4, 16, 'l')        # horizontal stabiliser (low, at the tail)
    # ---- fuselage ------------------------------------------------------------------------
    c.poly([(10, 12), (12, 10), (20, 10), (22, 11), (26, 12), (29, 15), (29, 16), (26, 18), (11, 18), (10, 16)], 'O')
    c.hline(12, 20, 10, 'L'); c.hline(11, 22, 11, 'l')      # lit top / hump
    c.hline(11, 26, 17, 'o'); c.hline(12, 25, 18, 'o')      # dark belly
    c.hline(27, 28, 15, 'l')                                # lit nose cone
    # stepped tandem canopy: pilot (rear, raised) + gunner (front, lower)
    c.stamp(19, 9, ['.yyy.',
                    'yywYY',
                    'YYYYY'])
    c.stamp(23, 11, ['yyy.',
                     'ywYY',
                     'YYYY'])
    c.vline(22, 10, 14, 'O')                                # canopy frame between the seats
    c.hline(19, 22, 12, 'O')
    # engine nacelle on the flank with dark exhaust
    c.rect(12, 13, 17, 14, 'l'); c.hline(12, 17, 15, 'o'); c.hline(13, 16, 13, 'L')
    c.stamp(10, 13, ['KK', 'Ks', 'KK'])
    # nose sensor turret (TADS)
    c.stamp(27, 16, ['SgG', 'sSs'])
    # ---- stub wing + stores -----------------------------------------------------------------
    c.hline(14, 21, 16, 'L'); c.hline(14, 21, 17, 'l')
    c.stamp(14, 18, ['gGGg', 'SSSS', 'ssss'])               # rocket pod
    c.px(17, 19, 'K')
    c.stamp(19, 18, ['SSSg', 'KKKK', 'SSSg'])              # Hellfire rack (two rails)
    # ---- landing gear ------------------------------------------------------------------------
    c.vline(23, 19, 20, 's'); c.stamp(22, 21, ['KK', 'KK'])
    c.vline(5, 17, 17, 's'); c.px(5, 18, 'K')

    c.outline('k')

    # ---- thin parts after the outline ---------------------------------------------------------
    # 30mm chin gun under the nose
    c.stamp(25, 19, ['kk....', 'SSgggk', 'kkkkk.'])
    # mast + Longbow dome
    c.vline(16, 7, 9, 'k'); c.px(16, 9, 's')
    c.stamp(14, 5, ['.kkk.', 'kgGgk', 'kSSSk'])
    # main rotor: one thin blade line, light near the hub, dark tips
    c.hline(1, 30, 8, 'K'); c.hline(9, 23, 8, 's'); c.hline(14, 18, 8, 'S')
    # tail rotor: small dark cross on the fin
    c.stamp(1, 7, ['.K.', 'KSK', '.K.'])
    c.shadow(8, 24, 29); c.shadow(10, 22, 30)
