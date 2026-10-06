export {};

declare global {
  interface Window {
    pix: {
      getState: () => Promise<unknown>;
      login: () => Promise<{ profileName: string; profileId: string }>;
      deviceStart: () => Promise<{ userCode: string; url: string }>;
      onDeviceDone: (cb: (p: { ok: boolean; profileName?: string; message?: string }) => void) => void;
      logout: () => Promise<void>;
      setSettings: (patch: Record<string, unknown>) => Promise<unknown>;
      runSetup: () => Promise<{ ok: boolean; error?: string }>;
      launchGame: () => Promise<{ ok: boolean; error?: string }>;
      killGame: () => Promise<void>;
      accountsList: () => Promise<Array<{ profileId: string; profileName: string; active: boolean }>>;
      accountsSwitch: (profileId: string) => Promise<{ ok: boolean }>;
      modsSearch: (query: string) => Promise<Array<{ project_id: string; slug: string; title: string; description: string; downloads: number; icon_url: string }>>;
      modsInstall: (projectId: string) => Promise<{ ok: boolean; file?: string; error?: string }>;
      modsList: () => Promise<string[]>;
      onModsProgress: (cb: (p: { message: string }) => void) => void;
      onSetupProgress: (cb: (p: { message: string }) => void) => void;
      onSetupDone: (cb: (p: { forgeId: string }) => void) => void;
      onSetupError: (cb: (p: { message: string }) => void) => void;
      onStateChanged: (cb: () => void) => void;
    };
  }
}
