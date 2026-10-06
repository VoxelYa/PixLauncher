/// <reference path="../preload/preload.ts" />

interface StateResponse {
  account: string | null;
  running: boolean;
  memoryAutoMB: number;
  settings: { memoryMB: number | null; mirrorPreference: string };
}

const $ = <T extends HTMLElement>(id: string) => document.getElementById(id) as T;

let signedIn = false;
let setupDone = false;

function refresh(state: StateResponse): void {
  signedIn = state.account !== null;
  $('account-chip').textContent = signedIn ? state.account! : 'Not signed in';
  $('btn-login').classList.toggle('hidden', signedIn);
  $('btn-logout').classList.toggle('hidden', !signedIn);
  $('login-pill').textContent = signedIn ? 'Signed in' : 'Required';
  $('login-pill').classList.toggle('ok', signedIn);
  (document.getElementById('btn-play') as HTMLButtonElement).disabled = !(signedIn && setupDone);
  $('btn-kill').classList.toggle('hidden', !state.running);
  $('ready-pill').classList.toggle('hidden', !setupDone);
  $('mem-auto').textContent = String(state.memoryAutoMB);
  (document.getElementById('mem-auto-cb') as HTMLInputElement).checked = state.settings.memoryMB === null;
  (document.getElementById('mem-manual') as HTMLInputElement).disabled = state.settings.memoryMB === null;
  (document.getElementById('mem-manual') as HTMLInputElement).value = String(state.settings.memoryMB ?? state.memoryAutoMB);
  ($('mirror-select') as HTMLSelectElement).value = state.settings.mirrorPreference;
}

let accountsCache: { profileId: string; profileName: string; active: boolean }[] = [];

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
    row.textContent = (a.active ? '\u2713 ' : '') + a.profileName;
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
}

updateSetupUi('idle');
void pullState();

document.getElementById('account-chip')?.addEventListener('click', () => {
  renderAccountMenu();
  document.getElementById('account-menu')?.classList.toggle('hidden');
});

function updateSetupUi(state: 'idle' | 'running' | 'done' | 'error'): void {
  const pill = $('setup-pill');
  pill.classList.remove('ok', 'err');
  if (state === 'done') { setupDone = true; pill.textContent = 'Installed'; pill.classList.add('ok'); }
  else if (state === 'running') { pill.textContent = 'Downloading…'; }
  else if (state === 'error') { pill.textContent = 'Failed'; pill.classList.add('err'); }
  else { pill.textContent = 'Not downloaded'; }
  (document.getElementById('btn-setup') as HTMLButtonElement).disabled = state === 'running' || setupDone;
}

window.pix.onSetupProgress((p) => {
  $('setup-log').textContent = p.message;
  const bar = $('setup-bar');
  bar.style.width = Math.min(95, (bar.clientWidth % 100) + 4) + '%'; // indeterminate-ish crawl
});
window.pix.onSetupDone(() => {
  ($('setup-bar') as HTMLElement).style.width = '100%';
  $('setup-log').textContent = 'All game files installed.';
  updateSetupUi('done');
  void pullState();
});
window.pix.onSetupError((p) => {
  $('setup-log').textContent = 'Error: ' + p.message;
  updateSetupUi('error');
});
window.pix.onStateChanged(() => void pullState());

window.pix.onDeviceDone((p) => {
  document.getElementById('device-info')?.remove();
  (document.getElementById('btn-login') as HTMLButtonElement).disabled = false;
  if (p.ok) $('setup-log').textContent = 'Signed in as ' + (p.profileName ?? '');
  else $('setup-log').textContent = 'Device login failed: ' + (p.message ?? '');
});

async function startDeviceLogin(): Promise<void> {
  (document.getElementById('btn-login') as HTMLButtonElement).disabled = true;
  try {
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
      '<button class="btn" id="copy-code" style="padding:6px 14px">' + (copied ? 'Copied \u2713' : 'Copy code') + '</button>' +
      '<button class="btn primary" id="open-page" style="padding:6px 14px">Open login page</button>' +
      '</div>';
    box.appendChild(info);
    (info.querySelector('#copy-code') as HTMLButtonElement).addEventListener('click', async (ev) => {
      const b = ev.currentTarget as HTMLButtonElement;
      try { await navigator.clipboard.writeText(d.userCode); b.textContent = 'Copied \u2713'; } catch { /* noop */ }
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

document.getElementById('btn-login')?.addEventListener('click', () => startDeviceLogin());

$('btn-logout').addEventListener('click', () => void window.pix.logout());

$('btn-setup').addEventListener('click', () => {
  updateSetupUi('running');
  $('setup-progress').classList.remove('hidden');
  void window.pix.runSetup();
});

$('btn-play').addEventListener('click', async () => {
  (document.getElementById('btn-play') as HTMLButtonElement).disabled = true;
  const res = await window.pix.launchGame();
  if (!res.ok) $('setup-log').textContent = 'Launch failed: ' + res.error;
  $('btn-kill').classList.remove('hidden');
});

$('btn-kill').addEventListener('click', () => void window.pix.killGame());

$('mem-auto-cb').addEventListener('change', async (e) => {
  const auto = (e.target as HTMLInputElement).checked;
  (document.getElementById('mem-manual') as HTMLInputElement).disabled = auto;
  await window.pix.setSettings({ memoryMB: auto ? null : Number((document.getElementById('mem-manual') as HTMLInputElement).value) || null });
});

$('mem-manual').addEventListener('change', async (e) => {
  await window.pix.setSettings({ memoryMB: Number((e.target as HTMLInputElement).value) });
});

$('mirror-select').addEventListener('change', async (e) => {
  await window.pix.setSettings({ mirrorPreference: (e.target as HTMLSelectElement).value });
});

$('nav-home').addEventListener('click', () => {
  $('nav-home').classList.add('active');
  $('nav-mods').classList.remove('active');
  $('nav-settings').classList.remove('active');
  $('view-home').classList.remove('hidden');
  $('view-mods').classList.add('hidden');
  $('view-settings').classList.add('hidden');
});
$('nav-mods').addEventListener('click', () => {
  $('nav-mods').classList.add('active');
  $('nav-home').classList.remove('active');
  $('nav-settings').classList.remove('active');
  $('view-mods').classList.remove('hidden');
  $('view-home').classList.add('hidden');
  $('view-settings').classList.add('hidden');
  void refreshModsList();
});
$('nav-settings').addEventListener('click', () => {
  $('nav-settings').classList.add('active');
  $('nav-home').classList.remove('active');
  $('nav-mods').classList.remove('active');
  $('view-settings').classList.remove('hidden');
  $('view-home').classList.add('hidden');
  $('view-mods').classList.add('hidden');
});

async function refreshModsList(): Promise<void> {
  const installed = await window.pix.modsList();
  const box = $('mods-results');
  for (const div of Array.from(box.querySelectorAll('div[data-slug]'))) {
    const slug = div.getAttribute('data-slug')!;
    const btn = div.querySelector('button') as HTMLButtonElement | null;
    if (btn && installed.some((f) => f.toLowerCase().includes(slug.toLowerCase()))) {
      btn.disabled = true;
      btn.textContent = 'Installed';
    }
  }
}

$('mods-search').addEventListener('click', async () => {
  const q = (document.getElementById('mods-query') as HTMLInputElement).value.trim();
  const box = $('mods-results');
  box.innerHTML = '';
  $('mods-status').textContent = 'Searching…';
  try {
    const hits = await window.pix.modsSearch(q);
    $('mods-status').textContent = hits.length ? `${hits.length} results` : 'No results';
    for (const hit of hits) {
      const row = document.createElement('div');
      row.setAttribute('data-slug', hit.slug);
      row.style.cssText = 'border:1px solid var(--border);border-radius:8px;padding:10px 12px;display:flex;justify-content:space-between;gap:12px;align-items:center';
      const left = document.createElement('div');
      left.innerHTML = `<div style="font-weight:600;font-size:13px">${hit.title}</div>` +
        `<div class="muted" style="font-size:12px">${(hit.description || '').slice(0, 110)}</div>` +
        `<div class="muted" style="font-size:11px">${hit.downloads.toLocaleString()} downloads</div>`;
      const btn = document.createElement('button');
      btn.className = 'btn primary';
      btn.textContent = 'Install';
      btn.addEventListener('click', async () => {
        btn.disabled = true;
        btn.textContent = 'Installing…';
        const res = await window.pix.modsInstall(hit.project_id);
        if (res.ok) { btn.textContent = 'Installed'; $('mods-status').textContent = 'Installed ' + res.file; }
        else { btn.textContent = 'Failed'; btn.disabled = false; $('mods-status').textContent = 'Error: ' + res.error; }
      });
      row.appendChild(left);
      row.appendChild(btn);
      box.appendChild(row);
    }
  } catch (err) {
    $('mods-status').textContent = 'Search failed: ' + (err instanceof Error ? err.message : String(err));
  }
});

window.pix.onModsProgress((p) => { $('mods-status').textContent = p.message; });

void pullState();
