"""Prepare local-only flattened/remappable integration dependencies; never redistribute them.

Usage: python tools/prepare_health_test_mods.py <modpack mods directory> <yuusha|lmq>
The source modpack is read-only. Outputs go to ignored libs/health-<label>-*-local.jar.
"""
from pathlib import Path
import argparse
import io
import tomllib
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("mods", type=Path)
parser.add_argument("label", choices=["yuusha", "lmq"])
args = parser.parse_args()
out = Path(__file__).resolve().parents[1] / "libs"
seen = set()

def flatten(data):
    z = zipfile.ZipFile(io.BytesIO(data))
    if "META-INF/mods.toml" not in z.namelist():
        return
    metadata = tomllib.loads(z.read("META-INF/mods.toml").decode())
    mod_id = metadata["mods"][0]["modId"]
    if mod_id in seen:
        return
    seen.add(mod_id)
    target = out / f"health-{args.label}-{mod_id}-local.jar"
    if mod_id == "mixinextras":
        # Its nested implementation library has no mod metadata; preserve the original JIJ structure.
        target.write_bytes(data)
        return
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as output:
        for name in z.namelist():
            if not name.startswith("META-INF/jarjar/"):
                output.writestr(name, z.read(name))
    for name in z.namelist():
        if name.endswith(".jar"):
            flatten(z.read(name))

for file in args.mods.glob("*.jar"):
    if file.name.lower().startswith(("goety-", "goetyrevelation", "goety-revelation",
                                    "endinglibrary", "curios-forge", "geckolib-forge")):
        flatten(file.read_bytes())
print("Prepared local integration dependencies:", ", ".join(sorted(seen)))
