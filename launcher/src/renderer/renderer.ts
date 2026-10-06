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

async function pullState(): Promise<void> {
  refresh((await window.pix.getState()) as StateResponse);
}

updateSetupUi('idle');
void pullState();

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

$('btn-login').addEventListener('click', () => void window.pix.login());
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
