"""Encode the locally filtered reference recording as two positional mono sounds.

Run after decoding/filtering the supplied recording to art/sa_audio/clean.wav.
The source recording and private paths are not bundled with the mod.
"""
from pathlib import Path
import ctypes as C
import os
import sys
import wave
import numpy as np

library = Path(sys.argv[1]).resolve()
handle = os.add_dll_directory(str(library.parent))
sf = C.CDLL(str(library))

class Info(C.Structure):
    _fields_ = [("frames", C.c_longlong), ("samplerate", C.c_int),
                ("channels", C.c_int), ("format", C.c_int),
                ("sections", C.c_int), ("seekable", C.c_int)]

sf.sf_open.argtypes = [C.c_char_p, C.c_int, C.POINTER(Info)]
sf.sf_open.restype = C.c_void_p
sf.sf_writef_float.argtypes = [C.c_void_p, C.POINTER(C.c_float), C.c_longlong]
sf.sf_writef_float.restype = C.c_longlong
sf.sf_close.argtypes = [C.c_void_p]
sf.sf_strerror.argtypes = [C.c_void_p]
sf.sf_strerror.restype = C.c_char_p
with wave.open("art/sa_audio/clean.wav") as w:
    assert w.getnchannels() == 1 and w.getsampwidth() == 2
    rate = w.getframerate()
    samples = np.frombuffer(w.readframes(w.getnframes()), dtype="<i2").astype(np.float32) / 32768
for name, start, duration, fade_out in [
    ("first", 1.25, .25, .006), ("second", 1.5, 1.1, .35)
]:
    a = samples[round(start*rate):round((start+duration)*rate)].copy()
    n_in, n_out = round(.004*rate), round(fade_out*rate)
    a[:n_in] *= np.linspace(0, 1, n_in)
    a[-n_out:] *= np.linspace(1, 0, n_out)
    path = Path(f"src/main/resources/assets/eldenblade/sounds/corpse_piler_{name}.ogg")
    info = Info(0, rate, 1, 0x200000 | 0x0060, 0, 0)  # Ogg / Vorbis
    f = sf.sf_open(str(path).encode(), 0x20, C.byref(info))
    if not f:
        raise RuntimeError(sf.sf_strerror(None))
    try:
        assert sf.sf_writef_float(f, a.ctypes.data_as(C.POINTER(C.c_float)), len(a)) == len(a)
    finally:
        assert sf.sf_close(f) == 0
    print(path.name, len(a)/rate, "seconds, mono,", path.stat().st_size, "bytes")
