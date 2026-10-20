"""Minimal stdlib-only PNG read/write + helpers (RGBA8). No PIL available in this sandbox."""
import zlib, struct

def read_png(path):
    d = open(path, 'rb').read()
    assert d[:8] == b'\x89PNG\r\n\x1a\n', 'not a png'
    pos = 8; idat = b''; plte = None; trns = None
    while pos < len(d):
        ln, = struct.unpack('>I', d[pos:pos+4]); typ = d[pos+4:pos+8]; body = d[pos+8:pos+8+ln]; pos += 12 + ln
        if typ == b'IHDR':
            w, h, bd, ct, _, _, il = struct.unpack('>IIBBBBB', body)
        elif typ == b'IDAT': idat += body
        elif typ == b'PLTE': plte = body
        elif typ == b'tRNS': trns = body
    assert bd == 8 and il == 0, f'unsupported bd={bd} interlace={il}'
    ch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    raw = zlib.decompress(idat); stride = w * ch; bpp = ch
    rows = []; prev = bytearray(stride); p = 0
    for y in range(h):
        f = raw[p]; p += 1
        line = bytearray(raw[p:p+stride]); p += stride
        for i in range(stride):
            a = line[i-bpp] if i >= bpp else 0
            b = prev[i]; c = prev[i-bpp] if i >= bpp else 0
            if f == 1: line[i] = (line[i] + a) & 255
            elif f == 2: line[i] = (line[i] + b) & 255
            elif f == 3: line[i] = (line[i] + ((a + b) >> 1)) & 255
            elif f == 4:
                pa = abs(b - c); pb = abs(a - c); pc = abs(a + b - 2*c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        rows.append(line); prev = line
    px = []
    for line in rows:
        row = []
        for x in range(w):
            if ct == 6: row.append(tuple(line[x*4:x*4+4]))
            elif ct == 2: row.append(tuple(line[x*3:x*3+3]) + (255,))
            elif ct == 0: g = line[x]; row.append((g, g, g, 255))
            elif ct == 4: g = line[x*2]; row.append((g, g, g, line[x*2+1]))
            elif ct == 3:
                i = line[x]; r, g_, b_ = plte[i*3:i*3+3]
                a_ = trns[i] if trns and i < len(trns) else 255
                row.append((r, g_, b_, a_))
        px.append(row)
    return w, h, px

def write_png(path, px):
    h = len(px); w = len(px[0])
    raw = bytearray()
    for row in px:
        raw.append(0)
        for r, g, b, a in row: raw += bytes((r, g, b, a))
    def chunk(t, b):
        c = struct.pack('>I', len(b)) + t + b
        return c + struct.pack('>I', zlib.crc32(t + b) & 0xffffffff)
    out = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) \
        + chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b'')
    open(path, 'wb').write(out)

def upscale(px, k):
    out = []
    for row in px:
        r2 = [p for p in row for _ in range(k)]
        for _ in range(k): out.append(list(r2))
    return out

def resize_nn(px, nw, nh):
    h = len(px); w = len(px[0])
    return [[px[y*h//nh][x*w//nw] for x in range(nw)] for y in range(nh)]

def blit(dst, src, ox, oy):
    """alpha-over blit src onto dst (RGBA)."""
    for y, row in enumerate(src):
        for x, (r, g, b, a) in enumerate(row):
            if 0 <= oy+y < len(dst) and 0 <= ox+x < len(dst[0]) and a:
                dr, dg, db, da = dst[oy+y][ox+x]
                af = a / 255.0
                dst[oy+y][ox+x] = (round(r*af + dr*(1-af)), round(g*af + dg*(1-af)), round(b*af + db*(1-af)), 255 if da == 255 or a == 255 else max(a, da))

def canvas(w, h, color=(255, 255, 255, 255)):
    return [[color for _ in range(w)] for _ in range(h)]
