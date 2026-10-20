"""
Checks the dependencies of the mod for new versions on Modrinth, and opens a GitHub issue when a new version changes
one of the files the mod relies on (configured in .github/dependency-watch.json).

Classes are compared through their disassembly (javap -c -p) with the constant pool indices removed, so that a mere
recompilation does not count as a change. Other files are compared byte for byte.

Usage: python check_dependencies.py [--dry-run]
  --dry-run: print the issues instead of creating them (no GitHub CLI needed).
"""
import difflib
import fnmatch
import hashlib
import io
import json
import os
import re
import subprocess
import sys
import tempfile
import urllib.parse
import urllib.request
import zipfile

CONFIG = os.path.join(os.path.dirname(__file__), '..', 'dependency-watch.json')
USER_AGENT = 'charlesgael/ae2-wider-terminal dependency check'
MAX_DIFF_LINES = 60
MAX_BODY_CHARS = 60000


def http_get(url):
    request = urllib.request.Request(url, headers={'User-Agent': USER_AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()


def modrinth_versions(project, game_version, loader):
    query = urllib.parse.urlencode({
        'game_versions': json.dumps([game_version]),
        'loaders': json.dumps([loader]),
    })
    versions = json.loads(http_get(f'https://api.modrinth.com/v2/project/{project}/version?{query}'))
    # Most recent first
    return sorted(versions, key=lambda v: v['date_published'], reverse=True)


def primary_file_url(version):
    files = version['files']
    primary = next((f for f in files if f.get('primary')), files[0])
    return primary['url']


def jar_entries(data):
    """All entries of a jar, with the nested jars (META-INF/jarjar) flattened in."""
    entries = {}
    with zipfile.ZipFile(io.BytesIO(data)) as jar:
        for name in jar.namelist():
            if name.endswith('/'):
                continue
            content = jar.read(name)
            if name.startswith('META-INF/jarjar/') and name.endswith('.jar'):
                entries.update(jar_entries(content))
            else:
                entries[name] = content
    return entries


def relevant(entries, patterns):
    return {name: content for name, content in entries.items()
            if any(fnmatch.fnmatchcase(name, pattern) for pattern in patterns)}


def disassemble(content, workdir):
    path = os.path.join(workdir, 'Entry.class')
    with open(path, 'wb') as f:
        f.write(content)
    output = subprocess.run(['javap', '-c', '-p', path], capture_output=True, text=True, check=True).stdout
    lines = []
    for line in output.splitlines():
        if line.startswith('Compiled from'):
            continue
        # Constant pool indices change whenever anything is added to the class: keep the resolved comment only
        lines.append(re.sub(r'#\d+(:\d+)?', '#', line).rstrip())
    return lines


def text_lines(name, content, workdir):
    if name.endswith('.class'):
        return disassemble(content, workdir)
    if name.endswith(('.json', '.txt', '.toml')):
        return content.decode('utf-8', errors='replace').splitlines()
    return None


def compare(old, new, workdir):
    """Returns (changes, diffs): the changed file names with their kind, and a diff per text file."""
    changes = []
    diffs = {}
    for name in sorted(set(old) | set(new)):
        if name not in new:
            changes.append((name, 'removed'))
            continue
        if name not in old:
            changes.append((name, 'added'))
            continue
        if hashlib.sha256(old[name]).digest() == hashlib.sha256(new[name]).digest():
            continue
        old_lines = text_lines(name, old[name], workdir)
        new_lines = text_lines(name, new[name], workdir)
        if old_lines is None:
            changes.append((name, 'changed (binary)'))
            continue
        if old_lines == new_lines:
            continue  # only recompiled
        changes.append((name, 'changed'))
        diffs[name] = list(difflib.unified_diff(old_lines, new_lines, 'old', 'new', lineterm='', n=2))
    return changes, diffs


def issue_body(dependency, checked, latest, changes, diffs):
    lines = [
        f"A new version of **{dependency['name']}** changes files that Wide AE2 Terminals relies on.",
        '',
        f"- Checked version: `{checked}`",
        f"- New version: `{latest['version_number']}` ({latest['version_type']}, "
        f"published {latest['date_published'][:10]})",
        f"- Modrinth: https://modrinth.com/mod/{dependency['modrinth']}/version/{latest['id']}",
        '',
        '### Changed files',
        '',
    ]
    lines += [f'- `{name}`: {kind}' for name, kind in changes]
    lines += ['', '### Differences', '']
    for name, diff in diffs.items():
        shown = diff[:MAX_DIFF_LINES]
        lines += [f'<details><summary><code>{name}</code> ({len(diff)} diff lines)</summary>', '', '```diff']
        lines += shown
        if len(diff) > len(shown):
            lines.append(f'... {len(diff) - len(shown)} more lines')
        lines += ['```', '', '</details>', '']
    lines += [
        '### To do',
        '',
        '- [ ] Check that the mixins and the layout still match these changes',
        f"- [ ] Update `checked` to `{latest['version_number']}` in `.github/dependency-watch.json`",
        '- [ ] Update the supported version range if needed (`gradle.properties`, README)',
    ]
    body = '\n'.join(lines)
    if len(body) > MAX_BODY_CHARS:
        body = body[:MAX_BODY_CHARS] + '\n\n... (truncated)'
    return body


def issue_exists(title):
    output = subprocess.run(['gh', 'issue', 'list', '--state', 'all', '--limit', '100', '--search',
                             f'"{title}" in:title', '--json', 'title'],
                            capture_output=True, text=True, check=True).stdout
    return any(issue['title'] == title for issue in json.loads(output))


def create_issue(title, body):
    subprocess.run(['gh', 'issue', 'create', '--title', title, '--body', body], check=True)


def main():
    dry_run = '--dry-run' in sys.argv
    with open(CONFIG, encoding='utf-8') as f:
        config = json.load(f)

    with tempfile.TemporaryDirectory() as workdir:
        for dependency in config['dependencies']:
            name = dependency['name']
            checked = dependency['checked']
            versions = modrinth_versions(dependency['modrinth'], config['gameVersion'], config['loader'])
            latest = versions[0]
            if latest['version_number'] == checked:
                print(f'{name}: {checked} is the latest version')
                continue

            checked_version = next((v for v in versions if v['version_number'] == checked), None)
            if checked_version is None:
                print(f'{name}: checked version {checked} not found on Modrinth, skipped')
                continue

            title = f"{name} {latest['version_number']}: files used by the mod changed"
            if not dry_run and issue_exists(title):
                print(f'{name}: issue already exists for {latest["version_number"]}')
                continue

            print(f"{name}: comparing {checked} with {latest['version_number']}")
            old = relevant(jar_entries(http_get(primary_file_url(checked_version))), dependency['files'])
            new = relevant(jar_entries(http_get(primary_file_url(latest))), dependency['files'])
            if not old:
                print(f'{name}: no file matches the patterns in {checked}, check the configuration')
                continue
            changes, diffs = compare(old, new, workdir)
            if not changes:
                print(f'{name}: no change in the {len(old)} files used by the mod')
                continue

            for file_name, kind in changes:
                print(f'  {file_name}: {kind}')
            body = issue_body(dependency, checked, latest, changes, diffs)
            if dry_run:
                print(f'--- issue: {title}\n{body}\n---')
            else:
                create_issue(title, body)
                print(f'{name}: issue created')


if __name__ == '__main__':
    main()
