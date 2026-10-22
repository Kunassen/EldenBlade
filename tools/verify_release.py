"""Validate the distributable without Forge or local third-party dependencies."""
from pathlib import Path
import argparse, hashlib, json, zipfile

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('jar', nargs='?', default='releases/EldenBlade-1.20.1-1.2.1.jar')
args = parser.parse_args()
jar = ROOT / args.jar
lore = ['芦苇之地的大剑客“老翁”的武器。', '斩人无数的诅咒刀。',
        '据说“鲜血君王”蒙格在亲身体会', '老翁的剑与疯狂之后，说出了提案：',
        '我赐给你吧──', '那抚平渴望的修罗生存之道。']
with zipfile.ZipFile(jar) as z:
    assert z.testzip() is None
    names = z.namelist()
    assert 'dev/kunas/eldenblade/HealthStorage.class' in names
    assert 'dev/kunas/eldenblade/mixin/HealthReadTraceMixin.class' in names
    assert b'eldenblade.mixins.json' in z.read('META-INF/MANIFEST.MF')
    assert json.loads(z.read('eldenblade.mixins.json'))['required']
    resolver = z.read('dev/kunas/eldenblade/HealthStorage.class')
    assert b'revelationfix' not in resolver and b'Apollyon' not in resolver
    assert 'dev/kunas/eldenblade/client/BladeTooltip.class' in names
    assert not any('CombatGameTests' in n or 'HealthStorageGameTests' in n or 'SaVisualCheck' in n or n.endswith('.blend') or 'prinegorerouse' in n for n in names)
    assert 'version="1.2.1"' in z.read('META-INF/mods.toml').decode()
    assert 'versionRange="[1.1.23,1.9.65]"' in z.read('META-INF/mods.toml').decode()
    assert 'dev/kunas/eldenblade/client/BloodSlashRenderer.class' in names
    assert 'dev/kunas/eldenblade/BloodSlashEntity.class' in names
    assert 'dev/kunas/eldenblade/BloodSlashShape.class' in names
    assert 'assets/eldenblade/textures/entity/blood_slash.png' not in names
    motion = 'assets/eldenblade/combostate/corpse_piler.vmd'
    assert z.read(motion) == (ROOT / 'src/main/resources' / motion).read_bytes()
    sounds = json.loads(z.read('assets/eldenblade/sounds.json'))
    for stage in ['first', 'second']:
        key = 'corpse_piler_' + stage
        sound = sounds[key]['sounds'][0]
        assert sound['name'] == 'eldenblade:' + key and not sound['stream']
        resource = f'assets/eldenblade/sounds/{key}.ogg'
        data = z.read(resource)
        assert data.startswith(b'OggS') and b'vorbis' in data
        # Vorbis identification header channel count must be one for positional audio.
        header = data.index(b'\x01vorbis')
        assert data[header + 11] == 1
        assert data == (ROOT / 'src/main/resources' / resource).read_bytes()
    texture = 'assets/eldenblade/textures/entity/blood_sweep.png'
    data = z.read(texture)
    assert data == (ROOT / 'src/main/resources' / texture).read_bytes()
    assert data[25] == 6, 'Blood slash must use RGBA transparency'
    for locale in ['zh_cn', 'en_us']:
        language = json.loads(z.read(f'assets/eldenblade/lang/{locale}.json'))
        assert [language[f'tooltip.eldenblade.rivers_of_blood.lore.{i}'] for i in range(1, 7)] == lore
    for name in ['rivers_of_blood.obj', 'rivers_of_blood.png']:
        resource = 'assets/eldenblade/model/' + name
        assert z.read(resource) == (ROOT / 'src/main/resources' / resource).read_bytes()
digest = hashlib.sha256(jar.read_bytes()).hexdigest()
jar.with_suffix('.jar.sha256').write_text(digest + '  ' + jar.name + '\n', encoding='ascii')
print(json.dumps({'status': 'passed', 'jar': jar.name, 'bytes': jar.stat().st_size,
                  'sha256': digest, 'lore_lines': 6, 'mono_sounds': 2,
                  'visual_check': 'See docs/RELEASE_1.2.1.md; not performed by this script'}, indent=2))
