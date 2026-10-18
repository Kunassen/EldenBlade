"""Validate the distributable without Forge or local third-party dependencies."""
from pathlib import Path
import argparse, hashlib, json, zipfile

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('jar', nargs='?', default='releases/EldenBlade-1.20.1-1.0.0.jar')
args = parser.parse_args()
jar = ROOT / args.jar
lore = ['芦苇之地的大剑客“老翁”的武器。', '斩人无数的诅咒刀。',
        '据说“鲜血君王”蒙格在亲身体会', '老翁的剑与疯狂之后，说出了提案：',
        '我赐给你吧──', '那抚平渴望的修罗生存之道。']
with zipfile.ZipFile(jar) as z:
    assert z.testzip() is None
    names = z.namelist()
    assert 'dev/kunas/eldenblade/client/BladeTooltip.class' in names
    assert not any('CombatGameTests' in n or n.endswith('.blend') or 'prinegorerouse' in n for n in names)
    assert 'version="1.0.0"' in z.read('META-INF/mods.toml').decode()
    for locale in ['zh_cn', 'en_us']:
        language = json.loads(z.read(f'assets/eldenblade/lang/{locale}.json'))
        assert [language[f'tooltip.eldenblade.rivers_of_blood.lore.{i}'] for i in range(1, 7)] == lore
    for name in ['rivers_of_blood.obj', 'rivers_of_blood.png']:
        resource = 'assets/eldenblade/model/' + name
        assert z.read(resource) == (ROOT / 'src/main/resources' / resource).read_bytes()
digest = hashlib.sha256(jar.read_bytes()).hexdigest()
jar.with_suffix('.jar.sha256').write_text(digest + '  ' + jar.name + '\n', encoding='ascii')
print(json.dumps({'status': 'passed', 'jar': jar.name, 'bytes': jar.stat().st_size,
                  'sha256': digest, 'lore_lines': 6, 'client_visual_tested': False}, indent=2))
