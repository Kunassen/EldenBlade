"""Derive the dedicated SA motion from the installed SlashBlade dependency.

Only sword-chain translation changes. Native animation keyframes, interpolation,
rotations and sheath movement remain intact. Run from any working directory.
"""
from pathlib import Path
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "libs/SlashBladeResharped-1.20.1-1.1.23.jar"
TARGET = ROOT / "src/main/resources/assets/eldenblade/combostate/corpse_piler.vmd"
with zipfile.ZipFile(SOURCE) as archive:
    data = bytearray(archive.read("assets/slashblade/combostate/motion.vmd"))
assert data.startswith(b"Vocaloid Motion Data 0002")
count = struct.unpack_from("<I", data, 50)[0]
changed = 0
for index in range(count):
    offset = 54 + index * 111
    if data[offset:offset + 15].split(b"\0")[0] == b"JointA1":
        z = struct.unpack_from("<f", data, offset + 27)[0]
        # Native layer uses 0.125 block/MMD unit; negative Z faces forward.
        struct.pack_into("<f", data, offset + 27, z - 6.4)
        changed += 1
assert changed == 371
TARGET.parent.mkdir(parents=True, exist_ok=True)
TARGET.write_bytes(data)
print(f"Shifted {changed} sword keyframes forward 0.8 blocks.")
