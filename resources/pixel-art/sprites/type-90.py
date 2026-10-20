NAME = "Type 90"
KIND = "ground"

# SILHOUETTE PLAN r3 (facing right) - heavy slab, NOT a howitzer:
#   squarer welded turret (x 10..20) seated on the hull, big stepped rear bustle box that hangs
#   over the hull's back, thin 2px 120mm smoothbore with ONE small evacuator step, thick
#   segmented skirt, short 45-degree glacis, six road wheels.
#   bounding box x 2..31, y 8..29

def draw(c):
    # ---- running gear: dark track band with rounded ends, six 3x3 road wheels --
    c.rect(3, 23, 27, 27, 'K')
    c.erase(3, 23); c.erase(3, 27); c.erase(27, 27)
    for x0 in (4, 8, 12, 16, 20, 24):
        c.stamp(x0, 24, ['gSg', 'SsS', 'sSs'])
    # ---- hull block: skirt + short 45-degree glacis ---------------------------
    c.poly([(4, 23), (4, 19), (5, 18), (20, 18), (21, 17), (27, 23)], 'O')
    c.hline(21, 22, 17, 'l')                                   # front deck lit edge
    c.hline(4, 26, 22, 'o')                                    # skirt lower shadow
    c.hline(5, 25, 23, 'k')                                    # dark gap under the skirt
    c.line(21, 17, 26, 22, 'l')                                # glacis lit edge
    # ---- turret: welded slab, flat roof, vertical front -----------------------
    c.rect(10, 11, 20, 18, 'O')
    c.px(20, 11, '.')                                          # 1px roof chamfer at the front
    c.hline(10, 19, 11, 'L')                                   # roof highlight
    c.hline(10, 20, 18, 'o')                                   # underside shadow
    c.vline(20, 12, 17, 'l')                                   # lit front edge
    c.vline(10, 12, 17, 'o')                                   # rear face (stepped behind the bustle)
    c.vline(15, 12, 17, 'o')                                   # single armour seam
    # ---- bustle: separate box, roof 2 rows lower, hangs over the hull's back ---
    c.rect(2, 13, 9, 18, 'O')
    c.hline(2, 9, 13, 'l')                                     # own lighter top row
    c.hline(2, 9, 18, 'o')                                     # darker bottom row
    c.vline(2, 14, 17, 'o')                                    # rear face shade
    c.vline(9, 14, 17, 'o')                                    # notch between bustle and turret
    # ---- roof gear -------------------------------------------------------------
    c.stamp(12, 9, ['gSg', 'lOo'])                             # commander cupola
    c.stamp(17, 10, ['gy'])                                    # gunner sight
    # ---- gun mantlet -----------------------------------------------------------
    c.stamp(21, 12, ['gg', 'SS', 'Ss', 'ss'])
    # ---- camouflage: chunky organic blotches, two browns -----------------------
    c.stamp(3, 14, ['.NNn..', 'NNnnn.', '.nnn..'])              # bustle
    c.stamp(11, 12, ['NNn.', 'Nnnn', '.nnN'])                   # turret rear
    c.stamp(16, 15, ['.NNn', 'Nnnn', '.nn.'])                   # turret front
    c.stamp(18, 12, ['sK', 'sK'])                               # smoke dischargers (two tube pairs)
    c.stamp(5, 20, ['NNn.', '.nnn'])                            # skirt, rear plate
    c.stamp(11, 20, ['.NNNn', 'nnnn.'])                         # skirt, mid plate
    # skirt plate seams (notch the lower lip)
    for x in (9, 15, 21):
        c.vline(x, 20, 21, 'o'); c.px(x, 22, 'k')
    # ---- team colour -----------------------------------------------------------
    c.stamp(12, 16, ['TTt', 'ttu'])                            # turret side patch
    c.hline(16, 20, 20, 'T'); c.hline(16, 20, 21, 't')         # skirt stripe
    # ---- outline first, then thin things drawn on top --------------------------
    c.outline('k')
    # 120mm smoothbore: thin tube to the cell edge, one small evacuator step
    c.hline(23, 31, 12, 'k')
    c.hline(23, 30, 13, 'g'); c.hline(23, 30, 14, 's')
    c.vline(31, 13, 14, 'k')
    c.stamp(26, 12, ['gGg', 'SSS', 'sss'])                      # evacuator collar (1px above the tube)
    c.hline(26, 28, 11, 'k')
    c.shadow(4, 27, 29)
