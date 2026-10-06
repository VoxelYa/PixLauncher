import io, os

base = r"D:\PixLauncher\launcher\src"

# ---------- renderer.ts: account dropdown + device-info cleanup + polished texts
p = os.path.join(base, "renderer", "renderer.ts")
s = io.open(p, encoding="utf-8").read()

s = s.replace('''async function pullState(): Promise<void> {
  refresh((await window.pix.getState()) as StateResponse);
}''', '''let accountsCache: { profileId: string; profileName: string; active: boolean }[] = [];

async function pullState(): Promise<void> {
  refresh((await window.pix.getState()) as StateResponse);
  try {
    accountsCache = await window.pix.accountsList();
    renderAccountChip();
  } catch { /* list is cosmetic */ }
}

function renderAccountChip(): void {
  const active = accountsCache.find((a) => a.active) ?? null;
  const any = accountsCache.length > 0;
  const chip = document.getElementById('account-chip');
  if (chip) {
    const name = document.getElementById('account-name');
    if (name) name.textContent = any ? ((active ? active.profileName : accountsCache[0].profileName)) : 'Not signed in';
  }
}

function renderAccountMenu(): void {
  const menu = document.getElementById('account-menu');
  if (!menu) return;
  menu.innerHTML = '';
  for (const a of accountsCache) {
    const row = document.createElement('button');
    row.className = 'account-item' + (a.active ? ' active' : '');
    row.textContent = (a.active ? '\\u2713 ' : '') + a.profileName;
    row.addEventListener('click', async () => {
      menu.classList.add('hidden');
      if (!a.active) {
        await window.pix.accountsSwitch(a.profileId);
        await pullState();
      }
    });
    menu.appendChild(row);
  }
  const add = document.createElement('button');
  add.className = 'account-item add';
  add.textContent = '+ Add account';
  add.addEventListener('click', () => {
    menu.classList.add('hidden');
    document.getElementById('nav-home')?.click();
    startDeviceLogin();
  });
  menu.appendChild(add);
}''')

s = s.replace('''function updateSetupUi(state: 'idle' | 'running' | 'done' | 'error'): void {''', '''document.getElementById('account-chip')?.addEventListener('click', () => {
  renderAccountMenu();
  document.getElementById('account-menu')?.classList.toggle('hidden');
});

function updateSetupUi(state: 'idle' | 'running' | 'done' | 'error'): void {''')

s = s.replace("$('btn-login').addEventListener('click', async () => {\n  // PCL2-style device code login: show the code, open the browser, poll.\n  (document.getElementById('btn-login') as HTMLButtonElement).disabled = true;",
"function startDeviceLogin(): void {\n  (document.getElementById('btn-login') as HTMLButtonElement).disabled = true;")

old_dev_start = '''  try {
    const d = await window.pix.deviceStart();
    $('setup-log').textContent = '';
    const box = document.getElementById('login-card')!;
    const old = document.getElementById('device-info');
    if (old) old.remove();
    const info = document.createElement('div');
    info.id = 'device-info';
    info.className = 'muted';
    info.style.marginTop = '10px';
    const loginUrl = 'https://www.microsoft.com/link';
    // auto-copy the code the moment it is shown
    let copied = false;
    try { await navigator.clipboard.writeText(d.userCode); copied = true; } catch { /* manual copy via button */ }
    info.innerHTML =
      '1. Click <b>Open login page</b> (re-open any time)<br>' +
      '2. Paste the code and sign in with the Microsoft account that owns Minecraft<br>' +
      '3. Your code: <b id="device-code" style="font-size:18px;color:var(--text)">' + d.userCode + '</b> ' +
      '<button class="btn" id="copy-code" style="padding:4px 10px">' + (copied ? 'Copied!' : 'Copy code') + '</button>';
    box.appendChild(info);
    const openBtn = document.createElement('button');
    openBtn.className = 'btn primary';
    openBtn.textContent = 'Open login page';
    openBtn.style.marginTop = '8px';
    openBtn.addEventListener('click', () => { window.open(loginUrl, '_blank'); });
    info.appendChild(openBtn);
    (info.querySelector('#copy-code') as HTMLButtonElement).addEventListener('click', async (ev) => {
      const b = ev.currentTarget as HTMLButtonElement;
      try {
        await navigator.clipboard.writeText(d.userCode);
        b.textContent = 'Copied!';
      } catch { b.textContent = d.userCode; }
      setTimeout(() => { b.textContent = 'Copy code'; }, 2000);
    });
  } catch (err) {
    $('setup-log').textContent = 'Device login error: ' + (err instanceof Error ? err.message : String(err));
    (document.getElementById('btn-login') as HTMLButtonElement).disabled = false;
  }
}'''
new_dev_start = '''  try {
    const d = await window.pix.deviceStart();
    const box = document.getElementById('login-card')!;
    document.getElementById('device-info')?.remove();
    const info = document.createElement('div');
    info.id = 'device-info';
    info.style.marginTop = '12px';
    const loginUrl = 'https://www.microsoft.com/link';
    let copied = false;
    try { await navigator.clipboard.writeText(d.userCode); copied = true; } catch { /* fallback: Copy button */ }
    info.innerHTML =
      '<div style="font-size:13px;line-height:1.6">Open the login page, sign in with your Microsoft account, ' +
      'and paste your code when asked.</div>' +
      '<div class="row" style="align-items:center;margin-top:10px">' +
      '<span style="font-size:22px;font-weight:700;letter-spacing:2px;color:var(--text)">' + d.userCode + '</span>' +
      '<button class="btn" id="copy-code" style="padding:6px 14px">' + (copied ? 'Copied \\u2713' : 'Copy code') + '</button>' +
      '<button class="btn primary" id="open-page" style="padding:6px 14px">Open login page</button>' +
      '</div>';
    box.appendChild(info);
    (info.querySelector('#copy-code') as HTMLButtonElement).addEventListener('click', async (ev) => {
      const b = ev.currentTarget as HTMLButtonElement;
      try { await navigator.clipboard.writeText(d.userCode); b.textContent = 'Copied \\u2713'; } catch { /* noop */ }
      setTimeout(() => { b.textContent = 'Copy code'; }, 2000);
    });
    (info.querySelector('#open-page') as HTMLButtonElement).addEventListener('click', () => {
      window.open(loginUrl, '_blank');
    });
  } catch (err) {
    $('setup-log').textContent = 'Device login error: ' + (err instanceof Error ? err.message : String(err));
    (document.getElementById('btn-login') as HTMLButtonElement).disabled = false;
  }
}

document.getElementById('btn-login')?.addEventListener('click', () => startDeviceLogin());'''
assert old_dev_start in s, 'dev start block not found'
s = s.replace(old_dev_start, new_dev_start)

s = s.replace('''window.pix.onDeviceDone((p) => {
  if (p.ok) {
    $('setup-log').textContent = 'Signed in as ' + (p.profileName ?? '');
  } else {
    $('setup-log').textContent = 'Device login failed: ' + (p.message ?? '');
  }
});''', '''window.pix.onDeviceDone((p) => {
  document.getElementById('device-info')?.remove();
  (document.getElementById('btn-login') as HTMLButtonElement).disabled = false;
  if (p.ok) $('setup-log').textContent = 'Signed in as ' + (p.profileName ?? '');
  else $('setup-log').textContent = 'Device login failed: ' + (p.message ?? '');
});''')
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('renderer ok')

# ---------- index.html: account chip with arrow + menu
p = os.path.join(base, 'renderer', 'index.html')
s = io.open(p, encoding='utf-8').read()
s = s.replace('<div id="account-chip" class="account-chip">Not signed in</div>',
'''<div class="account-wrap">
        <button class="account-chip" id="account-chip"><span id="account-name">Not signed in</span><span class="arrow">\u25be</span></button>
        <div class="account-menu hidden" id="account-menu"></div>
      </div>''')
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('html ok')

# ---------- styles.css: dropdown styles (accent already black)
p = os.path.join(base, 'renderer', 'styles.css')
s = io.open(p, encoding='utf-8').read()
if '.account-wrap' not in s:
    s = s.replace('.sidebar-footer { margin-top: auto; }', '''.sidebar-footer { margin-top: auto; }
.account-wrap { position: relative; }
.account-chip {
  width: 100%; display: flex; justify-content: space-between; align-items: center;
  font-size: 12px; color: var(--text);
  border: 1px solid var(--border); border-radius: 8px; padding: 8px 10px;
  background: var(--card); cursor: pointer; text-align: left;
}
.account-chip .arrow { color: var(--muted); font-size: 10px; }
.account-menu {
  position: absolute; bottom: calc(100% + 6px); left: 0; right: 0;
  background: var(--card); border: 1px solid var(--border); border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12); padding: 4px; z-index: 10;
}
.account-item {
  display: block; width: 100%; text-align: left; border: 0; background: transparent;
  padding: 8px 10px; border-radius: 7px; font-size: 12px; color: var(--text); cursor: pointer;
}
.account-item:hover { background: #f0f2f7; }
.account-item.active { color: var(--accent); font-weight: 600; }
.account-item.add { color: var(--muted); border-top: 1px solid var(--border); border-radius: 0 0 7px 7px; }''')
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('css ok')

# ---------- preload + d.ts: accounts APIs
p = os.path.join(base, 'preload', 'preload.ts')
s = io.open(p, encoding='utf-8').read()
if 'accountsList' not in s:
    s = s.replace("  killGame: () => ipcRenderer.invoke('game:kill'),",
"""  killGame: () => ipcRenderer.invoke('game:kill'),
  accountsList: () => ipcRenderer.invoke('accounts:list'),
  accountsSwitch: (profileId: string) => ipcRenderer.invoke('accounts:switch', profileId),""")
    io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('preload ok')

p = os.path.join(base, 'renderer', 'pix.d.ts')
s = io.open(p, encoding='utf-8').read()
if 'accountsList' not in s:
    s = s.replace("      killGame: () => Promise<void>;",
"""      killGame: () => Promise<void>;
      accountsList: () => Promise<Array<{ profileId: string; profileName: string; active: boolean }>>;
      accountsSwitch: (profileId: string) => Promise<{ ok: boolean }>;""")
    io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('dts ok')

# ---------- index.ts: IPC handlers
p = os.path.join(base, 'main', 'index.ts')
s = io.open(p, encoding='utf-8').read()
if 'accounts:list' not in s:
    s = s.replace("import { saveAccount } from './auth/store';",
"import { saveAccount, listAccounts, switchActive } from './auth/store';")
    s = s.replace("  ipcMain.handle('mods:list', () => modrinth.listInstalled());",
"""  ipcMain.handle('mods:list', () => modrinth.listInstalled());

  ipcMain.handle('accounts:list', () => {
    try { return listAccounts(); } catch { return []; }
  });
  ipcMain.handle('accounts:switch', (_e, profileId: string) => {
    const ok = switchActive(profileId);
    send('state:changed', null);
    return { ok };
  });""")
    io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('index ok')
