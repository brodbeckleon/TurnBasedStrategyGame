#!/usr/bin/env python3
"""Regenerate the generated pixel art (stdlib only, no PIL):

    python3 resources/pixel-art/build.py              # sprites + missing terrain tiles
    python3 resources/pixel-art/build.py --check      # only validate the sprites

Sprites  resources/pixel-art/sprites/*.py  ->  app/src/main/resources/images/units/player1|2/<NAME>.png
         (one 32x32 sprite per unit, facing right; player 1 gets blue team accents, player 2 red; the game mirrors player 2)
Tiles    PLAIN_* shoreline combinations that GameView can ask for but that were never hand-drawn are composed from the
         hand-drawn pieces (8x8 corner blocks) -> app/src/main/resources/images/terrain/

Sprite file contract and the drawing API are documented in tools/pxart.py.
"""
import argparse, glob, itertools, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, 'tools'))
from pngio import write_png
import pxart
from compose_tiles import compose, T as TERRAIN

UNITS = os.path.join(pxart.IMG, 'units')

# Hand-drawn terrain tiles (never overwritten); every other requestable PLAIN_* name is generated.
HAND_DRAWN = {
    'PLAIN_PPPP', 'PLAIN_PPPW', 'PLAIN_PPWP', 'PLAIN_PPWW', 'PLAIN_PWPP', 'PLAIN_PWPW', 'PLAIN_PWWP', 'PLAIN_WPPP', 'PLAIN_WPPW', 'PLAIN_WPWP', 'PLAIN_WWPP',
    'PLAIN_PPPP_PPPP', 'PLAIN_PPPP_PPPW', 'PLAIN_PPPP_PPWP', 'PLAIN_PPPP_PPWW', 'PLAIN_PPPP_PWPP', 'PLAIN_PPPP_PWWP', 'PLAIN_PPPP_WPPP', 'PLAIN_PPPP_WPPW', 'PLAIN_PPPP_WWPP',
}

def requested_plain_tiles():
    """Every PLAIN_* name GameView.getTerrainImagePath can produce (neighbours P/W/M/off-map; M and off-map count as plain)."""
    names = set()
    for n in itertools.product('PWMX', repeat=4):
        s = ''.join(n)
        if len(set(s)) == 1:
            for d in itertools.product('PWMX', repeat=4):
                names.add('PLAIN_' + s.replace('X', 'P').replace('M', 'P') + '_' + ''.join(d).replace('X', 'P').replace('M', 'P'))
        else:
            names.add('PLAIN_' + s.replace('X', 'P').replace('M', 'P'))
    return names

def build_sprites(check_only):
    errors = 0
    for path in sorted(glob.glob(os.path.join(HERE, 'sprites', '*.py'))):
        name, kind, canvas = pxart.load_sprite(path)
        err, warn = pxart.validate(canvas, kind)
        for e in err: print(f'ERROR   {os.path.basename(path)}: {e}')
        for w in warn: print(f'warning {os.path.basename(path)}: {w}')
        errors += len(err)
        if check_only or err: continue
        for team in (1, 2):
            out_dir = os.path.join(UNITS, f'player{team}'); os.makedirs(out_dir, exist_ok=True)
            write_png(os.path.join(out_dir, name + '.png'), canvas.pixels(team))
        print(f'sprite  {name}')
    return errors

def build_tiles():
    for name in sorted(requested_plain_tiles() - HAND_DRAWN):
        write_png(os.path.join(TERRAIN, name + '.png'), compose(name))
    print(f'tiles   {len(requested_plain_tiles() - HAND_DRAWN)} composed PLAIN_* tiles')

if __name__ == '__main__':
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--check', action='store_true', help='validate sprites only, write nothing')
    a = ap.parse_args()
    bad = build_sprites(a.check)
    if not a.check: build_tiles()
    sys.exit(1 if bad else 0)
