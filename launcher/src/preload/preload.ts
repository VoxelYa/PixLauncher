import { contextBridge, ipcRenderer } from 'electron';

const api = {
  getState: () => ipcRenderer.invoke('state:get'),
  login: () => ipcRenderer.invoke('auth:login'),
  deviceStart: () => ipcRenderer.invoke('auth:deviceStart'),
  onDeviceDone: (cb: (p: { ok: boolean; profileName?: string; message?: string }) => void) =>
    ipcRenderer.on('auth:deviceDone', (_e, p) => cb(p)),
  logout: () => ipcRenderer.invoke('auth:logout'),
  setSettings: (patch: Record<string, unknown>) => ipcRenderer.invoke('settings:set', patch),
  runSetup: () => ipcRenderer.invoke('setup:run'),
  launchGame: () => ipcRenderer.invoke('game:launch'),
  killGame: () => ipcRenderer.invoke('game:kill'),
  accountsList: () => ipcRenderer.invoke('accounts:list'),
  accountsSwitch: (profileId: string) => ipcRenderer.invoke('accounts:switch', profileId),
  modsSearch: (query: string) => ipcRenderer.invoke('mods:search', query),
  modsInstall: (projectId: string) => ipcRenderer.invoke('mods:install', projectId),
  modsList: () => ipcRenderer.invoke('mods:list'),
  onModsProgress: (cb: (p: { message: string }) => void) => ipcRenderer.on('mods:progress', (_e, p) => cb(p)),
  onSetupProgress: (cb: (p: { message: string }) => void) =>
    ipcRenderer.on('setup:progress', (_e, p) => cb(p)),
  onSetupDone: (cb: (p: { forgeId: string }) => void) => ipcRenderer.on('setup:done', (_e, p) => cb(p)),
  onSetupError: (cb: (p: { message: string }) => void) => ipcRenderer.on('setup:error', (_e, p) => cb(p)),
  onStateChanged: (cb: () => void) => ipcRenderer.on('state:changed', () => cb())
};

contextBridge.exposeInMainWorld('pix', api);

export type PixApi = typeof api;
