"use strict";

(() => {
  const SCREENS = [
    { id: "welcome", label: "Welcome", title: "Hello", body: "Welcome to petalOS", next: "Get started", target: "WelcomeActivity", note: "BluetoothSetupActivity runs before welcome in the current native flow. Accessibility remains available throughout setup." },
    { id: "language", label: "Language & region", group: "THE ESSENTIALS", title: "Hello. Bonjour.\nMake yourself at home.", body: "Choose the language and region you want to use on your phone.", target: "LocaleActivity", note: "Use Android’s complete locale list. This browser concept includes a small sample; choosing a language previews selection, not translated copy." },
    { id: "sim", label: "Mobile connection", group: "GET CONNECTED", title: "Out in the world.", body: "Set up your mobile connection, or leave it for later. Wi-Fi works either way.", skip: "Set up later", target: "SimMissingActivity", note: "Conditional screen. Native SIM detection and carrier/eSIM provisioning determine which actions are available." },
    { id: "wifi", label: "Wi-Fi", group: "GET CONNECTED", title: "Find your connection.", body: "Connect to Wi-Fi to restore your things and get ready for what’s next.", skip: "Use offline", target: "NetworkSetupActivity", note: "Native Wi-Fi scanning, authentication, connection errors, and captive portal hand-off. Demo network names and connections here are simulated." },
    { id: "datetime", label: "Date & time", group: "THE ESSENTIALS", title: "Right here.\nRight on time.", body: "Check your time zone, date, and time before we move on.", target: "DateTimeActivity", note: "Wire automatic time and time-zone settings to the existing native activity. Manual fields remain editable when automatic time is off." },
    { id: "restore", label: "Bring your things", group: "FEEL AT HOME", title: "Copy apps & data", body: "Restore with Petal Backup or set up as new.", target: "org.petal.setupwizard.backup.RestoreIntroActivity → org.petal.backup.action.RESTORE", note: "Petal Backup has native folder and server/cloud login pages. Setup opens its UI when installed; the restore engine remains a separate task. Browser restoration is simulated." },
    { id: "location", label: "Location", group: "YOUR CHOICES", title: "A sense of place.", body: "Choose whether your phone can use location. Apps will still ask for their own permission.", target: "LocationSettingsActivity", note: "Retain existing location setting behaviour. This switch only changes the prototype’s state." },
    { id: "device", label: "Device options", group: "THE LITTLE DETAILS", title: "Feels good\nin your hands.", body: "A few finishing touches for the way you use your phone.", target: "DeviceSpecificActivity → org.lineageos.settings.device.SUW_SETTINGS", note: "The existing activity delegates to a device-specific setup screen and skips if none exists. The controls shown here are proposed; expose only supported native options." },
    { id: "recovery", label: "Recovery updates", group: "KEEP IT CURRENT", title: "Ready for\nthe next chapter.", body: "Choose whether recovery should be updated alongside your system.", target: "UpdateRecoveryActivity", note: "Preserve existing recovery updater availability checks. Native flow may skip this screen on devices without that support." },
    { id: "privacy", label: "Privacy & diagnostics", group: "YOUR CHOICES", title: "Your phone.\nYour say.", body: "Review optional diagnostics before you continue. You can change this later in Settings.", target: "LineageSettingsActivity", note: "The existing metrics default is enabled. This concept proposes an off-by-default opt-in; record that decision before native implementation. Link to the actual project privacy policy in the native pass." },
    { id: "lock", label: "Screen lock", group: "A LITTLE PEACE OF MIND", title: "Set a screen lock", body: "Choose a PIN or pattern.", skip: "Set up later", target: "ScreenLockActivity", note: "Native PIN/pattern creation must use Android’s credential setup activities. The concept puts screen lock before biometrics. PIN digits and pattern paths stay in memory and are never persisted or exported." },
    { id: "biometric", label: "Fingerprint", group: "A LITTLE PEACE OF MIND", title: "A familiar touch.", body: "Add a fingerprint to unlock your phone with a touch.", skip: "Not now", target: "BiometricActivity", note: "Conditional on supported biometric hardware and an existing screen lock. This browser demo uses five taps; the native flow must use Android’s enrolment UI." },
    { id: "appearance", label: "Look & feel", group: "DISTINCTLY YOU", title: "Set the mood.", body: "Soft glass. A little colour. Make petalOS feel like your kind of place.", target: "ThemeSettingsActivity + proposed Petal appearance settings", note: "Extend the existing light/dark theme step. Accent, wallpaper, and glass choices are proposed Petal additions; map approved selections to actual supported settings during the native pass." },
    { id: "navigation", label: "Find your way", group: "DISTINCTLY YOU", title: "Your way around.", body: "Swipe through your day, or keep familiar navigation buttons close by.", target: "NavigationSettingsActivity", note: "Retain existing supported navigation modes and gesture tutorial hand-off. Buttons and gestures here demonstrate layout only." },
    { id: "apps", label: "A few Petal extras", group: "PICK YOUR EXTRAS", title: "A little extra Petal.", body: "Choose the extras you want. You can always add or remove them in Petal App Center later.", next: "Review setup", skip: "Skip extras", target: "PROPOSED · Petal App Center setup step", note: "New native step. Selected bundled APKs would be installed through the existing AppPackageInstaller flow. Define failure, retry, and offline behaviour before shipping. All extras start unselected in this concept." },
    { id: "summary", label: "One last look", group: "ALMOST HOME", title: "Feels like you.", body: "Here’s how your phone is shaping up. Tap anything to make a change.", next: "Finish setup", target: "PROPOSED · setup review", note: "New review screen. Native finish should apply choices once, wait for any required setup work, then hand off to FinishActivity. No provisioning flags are changed by the browser." },
    { id: "finish", label: "Welcome home", group: "YOUR NEXT CHAPTER", title: "You’re home.", body: "All set for the everyday, the unexpected, and everything in between. Welcome to petalOS.", next: "Let’s go", target: "FinishActivity", note: "Native completion owns provisioning flags and the launcher hand-off. The browser opens a decorative home-screen preview." },
  ];
  const APP_CHOICES = [
    { id: "assistant", name: "Petal Assistant", detail: "Petal’s optional assistant app.", glyph: "spark" },
    { id: "tuner", name: "PetalTuner", detail: "Game profiles and performance controls.", glyph: "game" },
    { id: "idhelper", name: "petal IDHelper", detail: "Avatar preview and privacy-camera controls.", glyph: "user" },
  ];
  const BACKUP_PAGES = {
    "backup-destination": { id: "backup-destination", label: "Petal Backup", title: "Restore your backup", body: "Where do you keep your backups?", next: "Continue", skip: "Continue without restoring", target: "org.petal.backup.RestoreActivity", note: "Separate Petal Backup setup flow. Back returns to the OOBE restore choice; completion or skip returns a result to SetupWizard." },
    "backup-local": { id: "backup-local", label: "Local backup", title: "Local backup", body: "Choose the folder containing your Petal backups.", next: "Find backups", target: "Petal Backup · local document-tree destination", note: "Native uses Android’s document-tree picker and a persisted URI grant. The browser can select a directory name or accept a sample path; it does not read backup data." },
    "backup-cloud": { id: "backup-cloud", label: "Server / cloud", title: "Choose a provider", body: "Select where your backups are stored.", next: "Continue", target: "Petal Backup · server/cloud provider selection", note: "Choose the provider here, then open its separate connection and login page." },
    "backup-login": { id: "backup-login", label: "Connection & login", title: "Connection & login", body: "Enter your server and account details.", next: "Connect", target: "org.petal.backup.CloudDestinationActivity", note: "Dedicated provider login page. SFTP/SMB use host, port, and account credentials; WebDAV/S3 use endpoints; Drive/OneDrive use OAuth. Native adapters perform real authentication later. Browser fields stay in memory." },
    "backup-snapshots": { id: "backup-snapshots", label: "Choose a backup", title: "Choose a backup", body: "Select what you want to restore.", next: "Restore", target: "Petal Backup · snapshot catalogue", note: "Sample catalogue in the browser. The native provider must list and verify real snapshot manifests." },
    "backup-restoring": { id: "backup-restoring", label: "Restoring", title: "Restoring your data", body: "Keep your phone connected while the restore finishes.", next: "Restoring…", skip: "Cancel restore", target: "Petal Backup · restore job", note: "Browser progress is simulated. Native restore requires the pending engine and truthful per-item results." },
    "backup-complete": { id: "backup-complete", label: "Restore complete", title: "Restore complete", body: "Your sample backup has been restored.", next: "Return to setup", target: "Petal Backup · setup activity result", note: "Return completion to SetupWizard, or the review page if the user opened restore from review. The browser does not restore files." },
  };
  const CLOUD_PROVIDERS = ["WebDAV", "S3-compatible", "SFTP", "SMB", "Google Drive", "OneDrive"];
  const DEFAULT_ANSWERS = {
    language: "English (United Kingdom)", region: "United Kingdom", sim: "later", network: "", automaticTime: true,
    date: "2026-10-16", time: "09:41", timezone: "Europe/London", restore: "fresh", restored: false, backupDestination: "local", backupProvider: "WebDAV",
    location: true, refresh: true, tapToWake: true, recovery: true, metrics: false, lockMethod: "pin", secured: false,
    credentialMethod: "none", fingerprint: 0, theme: "light", wallpaper: "bloom", navigation: "gestures", apps: [], largeText: false, reduceMotion: false,
  };
  const SIMPLE_COPY = {
    language: ["Select language", "Choose your language and region."],
    sim: ["Mobile network", "Insert a SIM card or set up an eSIM."],
    wifi: ["Connect to Wi-Fi", "Choose a network to get connected."],
    datetime: ["Date & time", "Check your date and time zone."],
    restore: ["Copy apps & data", "Restore with Petal Backup or set up as new."],
    location: ["Location services", "Choose whether to use your device’s location."],
    device: ["More settings", "A few options for your phone."],
    recovery: ["Recovery updates", "Keep recovery up to date with your system."],
    privacy: ["Privacy", "Choose whether to share optional diagnostics."],
    lock: ["Set a screen lock", "Choose a PIN or pattern."],
    biometric: ["Add fingerprint", "Unlock your phone with a touch."],
    appearance: ["Choose your style", "Select an appearance for your phone."],
    navigation: ["System navigation", "Choose how to get around."],
    apps: ["Petal apps", "Add the extras you want. You can do this later."],
    summary: ["Ready to go", "Your setup at a glance."],
    finish: ["Welcome to\npetalOS", "You’re all set."]
  };
  for (const screen of SCREENS) {
    if (SIMPLE_COPY[screen.id]) [screen.title, screen.body] = SIMPLE_COPY[screen.id];
    if (screen.id === "apps") screen.next = "Continue";
    if (screen.id === "summary") screen.next = "Finish";
    if (screen.id === "finish") screen.next = "Get started";
  }
  const PAGE_EASING = "cubic-bezier(0.22, 0.8, 0.18, 1)";
  const PAGE_PARALLAX = 0.24;
  const DEFAULT_DESIGN = { schema: 1, accent: "#0969f6", radius: 18, motion: 420, motionStyle: "shared-slide", order: SCREENS.map(s => s.id), copy: {} };
  const STORAGE_KEY = "petal-oobe-simple-v2";
  const $ = selector => document.querySelector(selector);
  const clone = value => JSON.parse(JSON.stringify(value));
  const escape = value => String(value ?? "").replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[char]));
  const paths = {
    globe: '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a18 18 0 0 1 0 18 18 18 0 0 1 0-18Z"/>',
    wifi: '<path d="M3 9a15 15 0 0 1 18 0M6 12a10 10 0 0 1 12 0M9 15a5 5 0 0 1 6 0"/><circle cx="12" cy="18" r=".7"/>',
    sim: '<path d="m8 3-3 4v14h14V3Z"/><rect x="8" y="10" width="8" height="7" rx="1"/><path d="M12 10v7M8 13h8"/>',
    clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v6l4 2"/>',
    restore: '<path d="M4 9a8 8 0 1 1 0 6M4 4v5h5M12 8v5l3 2"/>',
    location: '<path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 0 1 14 0Z"/><circle cx="12" cy="10" r="2.5"/>',
    shield: '<path d="m12 3 8 3v5c0 5-8 10-8 10S4 16 4 11V6Z"/><path d="m8 12 3 3 5-6"/>',
    lock: '<rect x="5" y="10" width="14" height="11" rx="3"/><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3"/>',
    fingerprint: '<path d="M5 10a7 7 0 0 1 14 0v3M3 12v-2a9 9 0 0 1 18 0M8 20c2-3 1-6 1-10a3 3 0 0 1 6 0c0 6 1 8 3 10M5 18c1-3 1-5 1-8M12 10c0 6 0 8-1 11M18 10c0 3 0 5 2 7M15 21l-1-4"/>',
    spark: '<path d="m12 3 2.3 6.7L21 12l-6.7 2.3L12 21l-2.3-6.7L3 12l6.7-2.3ZM19 3v4M17 5h4"/>',
    game: '<path d="M7 7h10c3 0 5 11 3 12s-5-3-6-3h-4c-1 0-4 4-6 3S4 7 7 7Z"/><path d="M8 10v5M5.5 12.5h5M16 11h.1M18 14h.1"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
    phone: '<rect x="6" y="2" width="12" height="20" rx="3"/><path d="M10 18h4M11 5h2"/>',
    update: '<path d="M12 3v12m-4-4 4 4 4-4M5 16v5h14v-5"/>',
    sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.5 1.5M17.5 17.5 19 19M5 19l1.5-1.5M17.5 6.5 19 5"/>',
    check: '<path d="m5 12 4 4L19 6"/>',
  };
  const icon = name => `<svg class="glyph" viewBox="0 0 24 24" aria-hidden="true">${paths[name] || paths.spark}</svg>`;
  const logo = () => '<span class="petal-logo" aria-hidden="true"><img src="assets/petal-logo.png" alt="" draggable="false"></span>';
  let design = clone(DEFAULT_DESIGN);
  let answers = clone(DEFAULT_ANSWERS);
  let current = "welcome";
  let home = false;
  let returnToReview = false;
  let toastTimer;
  let modalTimer;
  let modalGeneration = 0;
  let pageAnimations = [];
  let outgoingPage = null;
  let transitionRun = 0;
  let forcePreviewMotion = new URLSearchParams(location.search).get("motion") === "on";
  let patternDraft = emptyPattern();
  let backupPage = null;
  let backupDraft = emptyBackupDraft();
  let backupBusy = false;
  let backupTimer;
  let backupGeneration = 0;

  function validateDesign(input) {
    if (!input || input.schema !== 1 || !Array.isArray(input.order)) throw new Error("Not a petalOS concept export.");
    const ids = SCREENS.map(s => s.id);
    if (input.order.length !== ids.length || new Set(input.order).size !== ids.length || input.order.some(id => !ids.includes(id))) throw new Error("The export must include each setup screen once.");
    if (input.order[0] !== "welcome" || input.order.at(-1) !== "finish" || input.order.at(-2) !== "summary") throw new Error("Welcome, review, and finish need to stay at the ends of the flow.");
    const result = clone(DEFAULT_DESIGN);
    result.order = [...input.order];
    if (/^#[\da-f]{6}$/i.test(input.accent)) result.accent = input.accent;
    if (Number.isFinite(input.radius)) result.radius = Math.max(12, Math.min(36, input.radius));
    if (Number.isFinite(input.motion)) result.motion = Math.max(0, Math.min(600, input.motion));
    // Upgrade the old nudge's default without binning anyone's copy edits.
    if (!input.motionStyle && input.motion === 240) result.motion = DEFAULT_DESIGN.motion;
    for (const id of [...ids, ...Object.keys(BACKUP_PAGES)]) {
      const entry = input.copy?.[id];
      if (!entry || typeof entry !== "object") continue;
      result.copy[id] = {};
      for (const key of ["title", "body", "next"]) {
        if (typeof entry[key] === "string") result.copy[id][key] = entry[key].slice(0, key === "body" ? 800 : 150);
      }
    }
    return result;
  }

  function readAnswers(input) {
    const clean = clone(DEFAULT_ANSWERS);
    if (!input || typeof input !== "object") return clean;
    for (const key of Object.keys(clean)) {
      if (key === "apps") {
        if (Array.isArray(input.apps)) clean.apps = [...new Set(input.apps.filter(id => APP_CHOICES.some(app => app.id === id)))];
      } else if (typeof input[key] === typeof clean[key]) clean[key] = typeof input[key] === "string" ? input[key].slice(0, 100) : input[key];
    }
    if (!["light", "dark", "system"].includes(clean.theme)) clean.theme = "light";
    if (!["bloom", "mist", "dusk"].includes(clean.wallpaper)) clean.wallpaper = "bloom";
    if (!["gestures", "buttons"].includes(clean.navigation)) clean.navigation = "gestures";
    if (!["pin", "pattern", "none"].includes(clean.lockMethod)) clean.lockMethod = "pin";
    if (!["pin", "pattern", "none"].includes(clean.credentialMethod)) clean.credentialMethod = "none";
    if (clean.secured && !input.credentialMethod) clean.credentialMethod = clean.lockMethod;
    if (!clean.secured || clean.credentialMethod === "none") {
      clean.secured = false;
      clean.credentialMethod = "none";
      clean.fingerprint = 0;
    }
    if (!["local", "cloud"].includes(clean.backupDestination)) clean.backupDestination = "local";
    if (!CLOUD_PROVIDERS.includes(clean.backupProvider)) clean.backupProvider = "WebDAV";
    if (!["fresh", "backup"].includes(clean.restore)) clean.restore = "fresh";
    clean.fingerprint = Math.max(0, Math.min(5, Math.floor(clean.fingerprint) || 0));
    return clean;
  }

  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || "null");
    if (saved) {
      design = validateDesign(saved.design);
      answers = readAnswers(saved.answers);
      forcePreviewMotion ||= saved.forcePreviewMotion === true;
    }
  } catch { /* File previews can block storage; keep the damn preview running. */ }

  function save() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ design, answers, forcePreviewMotion }));
      $("#save-status").textContent = "Saved locally";
    } catch { $("#save-status").textContent = "Export to keep edits"; }
  }

  function step(id = current, includeBackup = true) {
    if (includeBackup && id === current && backupPage) return {
      ...BACKUP_PAGES[backupPage],
      ...(backupPage === "backup-login" ? { title: `${answers.backupProvider} login` } : {}),
      ...design.copy[backupPage],
    };
    const base = SCREENS.find(s => s.id === id) || SCREENS[0];
    return { next: "Continue", ...base, ...design.copy[base.id] };
  }

  function effectiveMotion() {
    if (typeof $("#screen").animate !== "function") return { duration: 0, reason: "Slides unavailable: this browser lacks animation support." };
    if (forcePreviewMotion) return { duration: design.motion || DEFAULT_DESIGN.motion, reason: `Slides enabled · ${design.motion || DEFAULT_DESIGN.motion} ms · preview override` };
    if (design.motion === 0) return { duration: 0, reason: "Slides off: the saved duration is 0 ms." };
    if (answers.reduceMotion) return { duration: 0, reason: "Slides off: Reduce motion is enabled in setup accessibility." };
    if (matchMedia("(prefers-reduced-motion: reduce)").matches) return { duration: 0, reason: "Slides off: Firefox or your operating system requests reduced motion." };
    return { duration: design.motion, reason: `Slides enabled · ${design.motion} ms` };
  }

  function syncMotionStatus() {
    const motion = effectiveMotion();
    $("#motion-status").textContent = motion.reason;
    $("#force-motion").checked = forcePreviewMotion;
    $("#enable-slides").hidden = motion.duration > 0;
  }

  function applyTokens() {
    const root = document.documentElement;
    root.style.setProperty("--accent", design.accent);
    root.style.setProperty("--radius", `${design.radius}px`);
    root.style.setProperty("--motion", `${design.motion}ms`);
    const theme = answers.theme === "system" ? (matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light") : answers.theme;
    $("#phone").dataset.theme = theme;
    $("#phone").dataset.wallpaper = answers.wallpaper;
    document.body.classList.toggle("reduce-motion", answers.reduceMotion);
    syncMotionStatus();
  }

  function choice(key, value, title, detail = "", glyph = "", extra = "") {
    const selected = answers[key] === value;
    return `<button type="button" class="choice${selected ? " selected" : ""}" data-choice="${key}" data-value="${escape(value)}" aria-pressed="${selected}" ${extra}>${glyph ? `<span class="choice-icon">${icon(glyph)}</span>` : ""}<span class="choice-text"><strong>${escape(title)}</strong>${detail ? `<small>${escape(detail)}</small>` : ""}</span><span class="radio-mark" aria-hidden="true"></span></button>`;
  }

  function toggle(key, title, detail) {
    return `<label class="toggle-row"><span class="toggle-copy"><strong>${escape(title)}</strong><small>${escape(detail)}</small></span><input class="switch" type="checkbox" data-key="${key}" ${answers[key] ? "checked" : ""}></label>`;
  }

  function select(key, values, label) {
    return `<label class="field-label" for="field-${key}">${escape(label)}</label><select id="field-${key}" data-key="${key}">${values.map(value => `<option${answers[key] === value ? " selected" : ""}>${escape(value)}</option>`).join("")}</select>`;
  }

  const bigIcon = name => `<div class="big-icon">${icon(name)}</div>`;
  const note = text => `<p class="note">${escape(text)}</p>`;
  const reviewRow = (id, title, value) => `<button class="summary-row" type="button" data-review="${id}"><span><strong>${escape(title)}</strong><small>${escape(value)}</small></span><span aria-hidden="true">↗</span></button>`;

  function emptyPattern() {
    return { first: [], current: [], confirmed: false, pointer: null, moved: false, message: "Connect at least 4 dots." };
  }

  function lockContent() {
    const choices = '<div class="lock-methods">' + choice("lockMethod", "pin", "PIN")
      + choice("lockMethod", "pattern", "Pattern") + '</div>';
    if (answers.lockMethod === "pattern") {
      return choices + `<p class="pattern-status" id="pattern-status" role="status"></p>
        <div id="pattern-pad" class="pattern-pad" role="group" aria-label="Draw a sample unlock pattern">
          <svg viewBox="0 0 300 300" aria-hidden="true"><polyline id="pattern-line"></polyline></svg>
          ${Array.from({ length: 9 }, (_, i) => `<button type="button" class="pattern-dot" data-pattern-dot="${i + 1}" aria-pressed="false" aria-label="Dot ${i + 1}, row ${Math.floor(i / 3) + 1}, column ${i % 3 + 1}"></button>`).join("")}
        </div><div class="pattern-actions"><button class="text-button" type="button" data-action="pattern-reset">Reset</button><button id="pattern-commit" class="text-button" type="button" data-action="pattern-commit">Use this pattern</button></div>`;
    }
    if (answers.lockMethod !== "pin") return choices;
    return choices + `<label class="field-label" for="demo-pin">PIN</label><input id="demo-pin" type="password" inputmode="numeric" pattern="[0-9]*" minlength="4" maxlength="6" autocomplete="off" placeholder="4–6 sample digits" aria-describedby="pin-note"><label class="field-label" for="demo-pin-confirm">Confirm PIN</label><input id="demo-pin-confirm" type="password" inputmode="numeric" maxlength="6" autocomplete="off" placeholder="Enter it again"><p class="note" id="pin-note">Use sample digits. They aren’t saved or exported.</p>`;
  }

  function backupContent() {
    return choice("restore", "backup", "Restore with Petal Backup", "Open Petal Backup to choose a local or cloud backup.", "restore")
      + choice("restore", "fresh", "Set up as new", "Start without restoring a backup.", "spark");
  }

  function emptyBackupDraft() {
    return { folder: "", endpoint: "", host: "", port: answers.backupProvider === "SMB" ? "445" : "22", share: "", domain: "", username: "", password: "", remotePath: answers.backupProvider === "SMB" ? "" : "/PetalBackup", region: "us-east-1", bucket: "", accessKey: "", secretKey: "", account: "", snapshot: "demo-recent", feedback: "", progress: 0 };
  }

  function resetBackupConnection() {
    backupDraft = emptyBackupDraft();
    if (answers.backupProvider === "SMB") { backupDraft.port = "445"; backupDraft.remotePath = ""; }
  }

  function backupField(key, title, placeholder = "", type = "text") {
    return `<label class="field-label" for="backup-${key}">${escape(title)}</label><input id="backup-${key}" data-backup-field="${key}" type="${type}" value="${escape(backupDraft[key])}" placeholder="${escape(placeholder)}" ${type === "number" ? 'min="1" max="65535" step="1"' : ""} autocomplete="off" spellcheck="false">`;
  }

  function backupPageContent() {
    let html = '<p class="backup-badge">Petal Backup · setup preview</p>';
    if (backupPage === "backup-destination") return html
      + choice("backupDestination", "local", "Local storage", "Phone storage, SD card, or USB drive.", "phone")
      + choice("backupDestination", "cloud", "Server / cloud", "A network server or cloud storage account.", "globe");
    if (backupPage === "backup-local") return html
      + backupField("folder", "Backup folder", "/storage/emulated/0/PetalBackup")
      + '<button class="text-button" type="button" data-action="backup-folder">Choose folder →</button><input id="backup-folder-picker" type="file" webkitdirectory multiple hidden>'
      + `<p class="backup-feedback" role="status">${escape(backupDraft.feedback || "The browser previews the folder choice; native setup uses Android’s folder picker.")}</p>`;
    if (backupPage === "backup-cloud") {
      const descriptions = { WebDAV: "WebDAV server", "S3-compatible": "S3 bucket or compatible object storage", SFTP: "SSH file transfer", SMB: "NAS or Windows file share", "Google Drive": "Google account", OneDrive: "Microsoft account" };
      return html + CLOUD_PROVIDERS.map(provider => choice("backupProvider", provider, provider, descriptions[provider], "globe")).join("");
    }
    if (backupPage === "backup-login") {
      if (answers.backupProvider === "WebDAV") html += backupField("endpoint", "Server URL", "https://backup.example.com/dav")
        + backupField("remotePath", "Backup folder on server", "/PetalBackup")
        + backupField("username", "Username", "Your server username") + backupField("password", "Password or app password", "Password", "password");
      else if (answers.backupProvider === "S3-compatible") html += backupField("endpoint", "Endpoint URL", "https://s3.example.com")
        + backupField("region", "Region", "us-east-1") + backupField("bucket", "Bucket", "petal-backups")
        + backupField("accessKey", "Access key ID", "Access key ID") + backupField("secretKey", "Secret access key", "Secret access key", "password");
      else if (["SFTP", "SMB"].includes(answers.backupProvider)) {
        html += backupField("host", "Server hostname or IP", "nas.example.com")
          + backupField("port", "Port", answers.backupProvider === "SFTP" ? "22" : "445", "number");
        if (answers.backupProvider === "SMB") html += backupField("share", "Share name", "Backups") + backupField("domain", "Domain / workgroup (optional)", "WORKGROUP");
        html += backupField("remotePath", answers.backupProvider === "SMB" ? "Folder inside share (optional)" : "Backup folder", answers.backupProvider === "SMB" ? "PetalBackup" : "/home/user/PetalBackup")
          + backupField("username", "Username", "Your server username") + backupField("password", "Password", "Password", "password");
      }
      else html += `<div class="account-card"><p>${backupDraft.account ? `Demo account: ${escape(backupDraft.account)}` : `Connect your ${escape(answers.backupProvider)} account to locate your backups.`}</p><button class="text-button" type="button" data-action="backup-sign-in">${backupDraft.account ? "Change demo account" : "Sign in · preview"}</button></div>`;
      return html + `<p class="backup-feedback" role="status">${escape(backupDraft.feedback || "Connection details are only held in memory in this preview. No server is contacted.")}</p>`;
    }
    if (backupPage === "backup-snapshots") return html + '<p class="section-label">Sample backups</p>'
      + `<button class="choice${backupDraft.snapshot === "demo-recent" ? " selected" : ""}" type="button" data-snapshot="demo-recent" aria-pressed="${backupDraft.snapshot === "demo-recent"}"><span class="choice-text"><strong>macanc · Latest backup</strong><small>Apps, app data, messages, and call logs.</small></span><span class="radio-mark" aria-hidden="true"></span></button>`
      + `<button class="choice${backupDraft.snapshot === "demo-older" ? " selected" : ""}" type="button" data-snapshot="demo-older" aria-pressed="${backupDraft.snapshot === "demo-older"}"><span class="choice-text"><strong>macanc · Previous backup</strong><small>Apps and app data.</small></span><span class="radio-mark" aria-hidden="true"></span></button>`
      + `<p class="backup-feedback connection-detail">${escape(answers.backupDestination === "local" ? backupDraft.folder : `${answers.backupProvider} · ${backupDraft.host || backupDraft.endpoint || backupDraft.account}`)}</p>`;
    if (backupPage === "backup-restoring") return html + `<progress id="backup-progress" value="${backupDraft.progress}" max="100" aria-label="Simulated restore progress"></progress><p id="backup-progress-label" class="backup-feedback" role="status">${backupDraft.progress}% · Restoring sample data…</p>`;
    return html + `<div class="glass-card"><h3>Ready to continue</h3><p>Return to petalOS setup with your restore selection. This browser run used sample data.</p></div>`;
  }

  function cancelBackupWork() {
    clearTimeout(backupTimer);
    backupGeneration++;
    backupBusy = false;
  }

  function showBackupPage(id, backwards = false) {
    cancelBackupWork();
    closeSheet();
    backupPage = id;
    backupDraft.feedback = "";
    backupBusy = id === "backup-restoring";
    try { history.replaceState(null, "", `#restore/backup/${id.replace("backup-", "")}`); } catch {}
    render({ animate: true, backwards, focus: true });
  }

  function leaveBackupPage() {
    cancelBackupWork();
    backupPage = null;
    backupDraft = emptyBackupDraft();
  }

  function finishBackup(restored) {
    answers.restored = restored;
    if (!restored) answers.restore = "fresh";
    leaveBackupPage();
    save();
    if (returnToReview) go("summary");
    else go(design.order[design.order.indexOf("restore") + 1]);
  }

  function backupBack() {
    if (backupBusy) return;
    if (backupPage === "backup-destination") {
      leaveBackupPage();
      render({ animate: true, backwards: true, focus: true });
      try { history.replaceState(null, "", "#restore"); } catch {}
    } else if (backupPage === "backup-local" || backupPage === "backup-cloud") showBackupPage("backup-destination", true);
    else if (backupPage === "backup-login") showBackupPage("backup-cloud", true);
    else if (backupPage === "backup-snapshots") showBackupPage(answers.backupDestination === "local" ? "backup-local" : "backup-login", true);
    else if (backupPage === "backup-complete") finishBackup(true);
  }

  function validServerUrl(value) {
    try {
      const uri = new URL(value);
      return ["https:", "http:"].includes(uri.protocol) && Boolean(uri.hostname) && !uri.username && !uri.password;
    } catch { return false; }
  }

  function validServerHost(value) {
    if (!value || /[\s/@?#]/.test(value)) return false;
    try { return Boolean(new URL(`ssh://${value}:22`).hostname); } catch { return false; }
  }

  function backupNext(skip) {
    if (skip && backupPage === "backup-restoring") { showBackupPage("backup-snapshots", true); return; }
    if (skip) { finishBackup(false); return; }
    if (backupBusy) return;
    if (backupPage === "backup-destination") { showBackupPage(answers.backupDestination === "local" ? "backup-local" : "backup-cloud"); return; }
    if (backupPage === "backup-cloud") { showBackupPage("backup-login"); return; }
    if (backupPage === "backup-complete") { finishBackup(true); return; }
    if (backupPage === "backup-snapshots") {
      backupDraft.progress = 0;
      showBackupPage("backup-restoring");
      const generation = backupGeneration;
      const tick = () => {
        if (generation !== backupGeneration || backupPage !== "backup-restoring") return;
        backupDraft.progress = Math.min(100, backupDraft.progress + 20);
        $("#backup-progress").value = backupDraft.progress;
        $("#backup-progress-label").textContent = `${backupDraft.progress}% · Restoring sample data…`;
        if (backupDraft.progress === 100) { answers.restored = true; save(); showBackupPage("backup-complete"); }
        else backupTimer = setTimeout(tick, 320);
      };
      backupTimer = setTimeout(tick, 320);
      return;
    }
    let error = "";
    if (backupPage === "backup-local" && !backupDraft.folder.trim()) error = "Choose a backup folder or enter a sample path.";
    if (backupPage === "backup-login") {
      if (["WebDAV", "S3-compatible"].includes(answers.backupProvider)) {
        if (!validServerUrl(backupDraft.endpoint.trim())) error = "Enter a server URL starting with https:// or http://.";
        else if (answers.backupProvider === "WebDAV" && (!backupDraft.username.trim() || !backupDraft.password || !backupDraft.remotePath.trim())) error = "Enter the server folder, username, and password.";
        else if (answers.backupProvider === "S3-compatible" && (!backupDraft.region.trim() || !backupDraft.bucket.trim() || !backupDraft.accessKey.trim() || !backupDraft.secretKey)) error = "Enter the region, bucket, and access keys.";
      } else if (["SFTP", "SMB"].includes(answers.backupProvider)) {
        const port = Number(backupDraft.port);
        if (!validServerHost(backupDraft.host.trim())) error = "Enter a hostname or IP address without a protocol or path.";
        else if (!/^\d+$/.test(backupDraft.port) || port < 1 || port > 65535) error = "Enter a port between 1 and 65535.";
        else if (!backupDraft.username.trim() || !backupDraft.password) error = "Enter the server username and password.";
        else if (answers.backupProvider === "SMB" && !backupDraft.share.trim()) error = "Enter the SMB share name.";
        else if (answers.backupProvider === "SFTP" && !backupDraft.remotePath.trim()) error = "Enter the backup folder on the server.";
      } else if (!backupDraft.account) error = "Connect a demo account first.";
    }
    if (error) { $("#form-error").textContent = error; return; }
    backupBusy = true;
    backupDraft.feedback = backupPage === "backup-local" ? "Finding sample backups…" : "Connecting · preview…";
    render();
    const generation = backupGeneration;
    backupTimer = setTimeout(() => { if (generation === backupGeneration && backupPage) showBackupPage("backup-snapshots"); }, 500);
  }

  function updatePatternUI() {
    const pad = $("#pattern-pad");
    if (!pad) return;
    pad.querySelectorAll("[data-pattern-dot]").forEach(dot => {
      const selected = patternDraft.current.includes(Number(dot.dataset.patternDot));
      dot.classList.toggle("selected", selected);
      dot.setAttribute("aria-pressed", selected);
    });
    $("#pattern-line").setAttribute("points", patternDraft.current.map(dot => `${50 + (dot - 1) % 3 * 100},${50 + Math.floor((dot - 1) / 3) * 100}`).join(" "));
    $("#pattern-status").textContent = patternDraft.message;
    $("#pattern-commit").textContent = patternDraft.confirmed ? "Confirmed" : patternDraft.first.length ? "Confirm pattern" : "Use this pattern";
    $("#pattern-commit").disabled = patternDraft.confirmed;
  }

  function appendPatternDot(dot) {
    if (patternDraft.confirmed) patternDraft = emptyPattern();
    const path = patternDraft.current;
    if (path.includes(dot)) return;
    const last = path.at(-1);
    if (last) {
      const rowSum = Math.floor((last - 1) / 3) + Math.floor((dot - 1) / 3);
      const columnSum = (last - 1) % 3 + (dot - 1) % 3;
      if (rowSum % 2 === 0 && columnSum % 2 === 0) {
        const middle = rowSum / 2 * 3 + columnSum / 2 + 1;
        if (!path.includes(middle)) path.push(middle);
      }
    }
    path.push(dot);
    $("#form-error").textContent = "";
    updatePatternUI();
  }

  function commitPattern() {
    if (patternDraft.current.length < 4) patternDraft.message = "Use at least 4 dots.";
    else if (!patternDraft.first.length) {
      patternDraft.first = [...patternDraft.current];
      patternDraft.current = [];
      patternDraft.message = "Draw the same pattern again.";
    } else if (patternDraft.first.join() !== patternDraft.current.join()) {
      patternDraft.current = [];
      patternDraft.message = "Patterns don’t match. Try again.";
    } else {
      patternDraft.confirmed = true;
      patternDraft.message = "Pattern confirmed.";
    }
    updatePatternUI();
  }

  function setupPatternPad() {
    const pad = $("#pattern-pad");
    if (!pad) return;
    const hit = event => {
      const box = pad.getBoundingClientRect();
      const x = (event.clientX - box.left) / box.width * 300;
      const y = (event.clientY - box.top) / box.height * 300;
      for (let dot = 1; dot <= 9; dot++) {
        if (Math.hypot(x - (50 + (dot - 1) % 3 * 100), y - (50 + Math.floor((dot - 1) / 3) * 100)) <= 32) return dot;
      }
      return null;
    };
    pad.addEventListener("pointerdown", event => {
      if (event.button !== 0) return;
      if (patternDraft.confirmed) patternDraft = emptyPattern();
      patternDraft.pointer = event.pointerId;
      patternDraft.moved = false;
      pad.setPointerCapture(event.pointerId);
      const dot = hit(event);
      if (dot) appendPatternDot(dot);
    });
    pad.addEventListener("pointermove", event => {
      if (event.pointerId !== patternDraft.pointer) return;
      const dot = hit(event);
      if (dot && !patternDraft.current.includes(dot)) {
        patternDraft.moved = true;
        appendPatternDot(dot);
      }
    });
    pad.addEventListener("pointerup", event => {
      if (event.pointerId !== patternDraft.pointer) return;
      patternDraft.pointer = null;
      if (patternDraft.moved) commitPattern();
    });
    pad.addEventListener("pointercancel", () => {
      patternDraft.pointer = null;
      patternDraft.current = [];
      patternDraft.message = "Gesture cancelled. Draw again.";
      updatePatternUI();
    });
    updatePatternUI();
  }

  function content(id) {
    switch (id) {
      case "welcome": return `<button class="welcome-language" type="button" data-jump="language">${icon("globe")} ${escape(answers.language)} <span>⌄</span></button>`;
      case "language": return choice("language", "English (United Kingdom)", "English", "United Kingdom", "globe") + choice("language", "English (United States)", "English", "United States") + choice("language", "Français", "Français", "France") + choice("language", "Deutsch", "Deutsch", "Deutschland") + choice("language", "Español", "Español", "España") + select("region", ["United Kingdom", "United States", "France", "Germany", "Spain", "Australia", "Canada", "India"], "Region") + note("Sample language list for the concept. Screen copy stays in English.");
      case "sim": return bigIcon("sim") + choice("sim", "physical", "Use a SIM card", "Preview a detected physical SIM.", "sim") + choice("sim", "esim", "Set up an eSIM", "Carrier setup would open here.", "phone") + choice("sim", "later", "I’ll do this later", "Continue using Wi-Fi.") + note("SIM detection and eSIM provisioning are simulated.");
      case "wifi": return `<p class="section-label">AVAILABLE NETWORKS</p>${["Petal House", "Studio network", "Guest Wi-Fi"].map((name, index) => `<button class="choice${answers.network === name ? " selected" : ""}" type="button" data-network="${name}"><span class="choice-icon">${icon("wifi")}</span><span class="choice-text"><strong>${name}</strong><small>${answers.network === name ? "Connected · demo" : index === 2 ? "Open network · demo" : "Secured network · demo"}</small></span><span aria-hidden="true">${answers.network === name ? "✓" : "⌁"}</span></button>`).join("")}<button class="text-button" type="button" data-action="scan">↻ Scan again</button>${note("These are sample networks. No real Wi-Fi connection is made.")}`;
      case "datetime": return bigIcon("clock") + toggle("automaticTime", "Set automatically", "Use the date and time provided by your network.") + select("timezone", ["Europe/London", "Europe/Paris", "America/New_York", "America/Los_Angeles", "Asia/Kolkata", "Asia/Tokyo", "Australia/Sydney"], "Time zone") + `<div class="input-row"><div><label class="field-label" for="field-date">Date</label><input id="field-date" type="date" data-key="date" value="${escape(answers.date)}" ${answers.automaticTime ? "disabled" : ""}></div><div><label class="field-label" for="field-time">Time</label><input id="field-time" type="time" data-key="time" value="${escape(answers.time)}" ${answers.automaticTime ? "disabled" : ""}></div></div>` + note("The sample date stays fixed so design reviews are repeatable.");
      case "restore": return backupContent();
      case "location": return bigIcon("location") + toggle("location", "Use location", "Allow the system to provide location to apps you permit.") + `<div class="glass-card"><h3>Still your choice, app by app.</h3><p>Turning this on doesn’t grant every app access. Individual location permissions remain in your control.</p></div>`;
      case "device": return bigIcon("phone") + toggle("refresh", "Smooth display", "Prefer a high refresh rate where the device supports it.") + toggle("tapToWake", "Tap to wake", "Wake your screen with a double tap.") + note("Proposed device-specific choices. Unsupported options would be hidden on the phone.");
      case "recovery": return bigIcon("update") + toggle("recovery", "Update recovery with petalOS", "Keep supported recovery components in step with system updates.") + note("This page is only shown when the device supports recovery updates.");
      case "privacy": return bigIcon("shield") + toggle("metrics", "Share optional diagnostics", "Help investigate problems by opting into the project’s supported diagnostics.") + `<div class="glass-card"><h3>An actual choice.</h3><p>This concept leaves optional diagnostics off until you choose otherwise. The native privacy policy and exact data description belong here.</p></div><button class="text-button" type="button" data-action="privacy">About this choice ↗</button>`;
      case "lock": return lockContent();
      case "biometric": return answers.secured ? `<button class="fingerprint-button" type="button" data-action="fingerprint" aria-label="Simulate fingerprint scan, ${answers.fingerprint} of 5 taps">${icon(answers.fingerprint === 5 ? "check" : "fingerprint")}</button><p class="fingerprint-status">${answers.fingerprint === 5 ? "Fingerprint added · demo" : `Tap the sensor ${5 - answers.fingerprint} more ${answers.fingerprint === 4 ? "time" : "times"}`}</p><div class="enrol-progress" role="progressbar" aria-label="Simulated fingerprint enrolment" aria-valuemin="0" aria-valuemax="5" aria-valuenow="${answers.fingerprint}"><span style="width:${answers.fingerprint * 20}%"></span></div>${note("Five taps stand in for Android’s fingerprint enrolment screen.")}` : `${bigIcon("lock")}<div class="glass-card"><h3>A screen lock comes first.</h3><p>Add a PIN or pattern before setting up your fingerprint, or skip this step for now.</p><button class="text-button" type="button" data-jump="lock">Set a screen lock →</button></div>`;
      case "appearance": return `<div class="theme-grid">${[["light", "Light"], ["dark", "Dark"], ["system", "Automatic"]].map(([value, title]) => `<button class="theme-tile${answers.theme === value ? " selected" : ""}" type="button" data-choice="theme" data-value="${value}" aria-pressed="${answers.theme === value}"><div class="mini-phone ${value}" aria-hidden="true"><i></i><i></i><i></i></div>${title}</button>`).join("")}</div><p class="section-label">YOUR ACCENT</p><div class="swatches">${["#7c5cff", "#cb4c84", "#208f80", "#517bc7", "#ad6b35"].map((colour, i) => `<button class="swatch${design.accent === colour ? " selected" : ""}" type="button" style="--swatch:${colour}" data-accent="${colour}" aria-label="${["Petal violet", "Rose", "Lagoon", "Blue", "Warm amber"][i]}" aria-pressed="${design.accent === colour}"></button>`).join("")}</div><p class="section-label">A LITTLE ATMOSPHERE</p><div class="wallpaper-grid">${["bloom", "mist", "dusk"].map(value => `<button class="wallpaper ${value}${answers.wallpaper === value ? " selected" : ""}" type="button" data-choice="wallpaper" data-value="${value}" aria-pressed="${answers.wallpaper === value}">${value[0].toUpperCase() + value.slice(1)}</button>`).join("")}</div>${note("The phone preview updates as you choose. These are concept wallpapers.")}`;
      case "navigation": return `<div class="nav-demo" aria-hidden="true"><div class="demo-card">${logo()}</div><div class="demo-navigation ${answers.navigation}">${answers.navigation === "buttons" ? "◁　　○　　□" : ""}</div></div>` + choice("navigation", "gestures", "Gesture navigation", "Swipe up for home. Swipe from an edge to go back.", "phone") + choice("navigation", "buttons", "Three-button navigation", "Back, home, and recent apps. Familiar and simple.") + `<button class="text-button" type="button" data-action="navigation-help">Show me how →</button>`;
      case "apps": return `<div class="app-selection">${APP_CHOICES.map(app => `<button type="button" class="choice${answers.apps.includes(app.id) ? " selected" : ""}" data-app="${app.id}" aria-pressed="${answers.apps.includes(app.id)}"><span class="choice-icon app-icon">${icon(app.glyph)}</span><span class="choice-text"><strong>${app.name}</strong><small>${app.detail}</small></span><span class="radio-mark" aria-hidden="true"></span></button>`).join("")}</div><div class="glass-card"><h3>${answers.apps.length ? `${answers.apps.length} ${answers.apps.length === 1 ? "extra" : "extras"} selected` : "Just the essentials? That’s fine."}</h3><p>App Center remains available in Petal Hub. Selecting an app here only adds it to this concept’s setup summary.</p></div>${note("Optional apps are not installed by this browser prototype.")}`;
      case "summary": return `<div class="glass-card">${reviewRow("language", "Language & region", `${answers.language} · ${answers.region}`)}${reviewRow("wifi", "Connection", answers.network || "Offline setup")}${reviewRow("restore", "Your things", answers.restore === "fresh" ? "A fresh start" : `Petal Backup · ${answers.backupDestination === "cloud" ? answers.backupProvider : "Local storage"}`)}${reviewRow("lock", "Security", `${answers.secured ? answers.credentialMethod === "pattern" ? "Pattern set" : "PIN set" : "No screen lock"}${answers.fingerprint === 5 ? " · fingerprint added" : ""}`)}${reviewRow("appearance", "Look & feel", `${answers.theme} · ${answers.wallpaper}`)}${reviewRow("navigation", "Navigation", answers.navigation === "gestures" ? "Gestures" : "Three buttons")}${reviewRow("privacy", "Diagnostics", answers.metrics ? "Opted in" : "Off")}${reviewRow("apps", "Petal extras", answers.apps.length ? APP_CHOICES.filter(app => answers.apps.includes(app.id)).map(app => app.name).join(", ") : "No optional extras")}</div>${note("Everything above is a preview selection. No device settings have changed.")}`;
      case "finish": return `<div class="completion-art">${logo()}<span class="completion-check">✓</span></div><span class="pill">Made personal.</span><span class="pill">Made Petal.</span>`;
      default: return "";
    }
  }

  function renderRail() {
    const index = design.order.indexOf(current);
    $("#screen-list").innerHTML = design.order.map((id, i) => `<button type="button" class="flow-item${id === current ? " active" : i < index ? " complete" : ""}" data-jump="${id}" ${id === current ? 'aria-current="step"' : ""}><span>${String(i + 1).padStart(2, "0")}</span>${escape(step(id, false).label)}</button>`).join("");
  }

  function syncEditor() {
    const page = step();
    $("#edit-title").value = page.title;
    $("#edit-body").value = page.body;
    $("#edit-next").value = page.next;
    $("#edit-accent").value = design.accent;
    $("#edit-radius").value = design.radius;
    $("#radius-value").textContent = `${design.radius} px`;
    $("#edit-motion").value = design.motion;
    $("#motion-value").textContent = `${design.motion} ms`;
    $("#edit-mode").value = answers.theme;
    $("#native-note").textContent = page.note;
    $("#native-target").textContent = page.target;
    const index = design.order.indexOf(current);
    $("#move-earlier").disabled = Boolean(backupPage) || index <= 1 || index >= design.order.length - 2;
    $("#move-later").disabled = Boolean(backupPage) || index === 0 || index >= design.order.length - 3;
  }

  function render({ animate = false, backwards = false, focus = false } = {}) {
    stopPageTransition();
    const screen = $("#screen");
    const motion = effectiveMotion();
    const sliding = animate && motion.duration > 0;
    const outgoing = sliding && screen.hasChildNodes() ? screen.cloneNode(true) : null;
    const oldScroll = screen.scrollTop;
    const oldHeight = screen.clientHeight;
    const page = step();
    const index = design.order.indexOf(current);
    applyTokens();
    const hero = "";
    $("#phone").dataset.step = home ? "home" : page.id;
    $("#brand-name").textContent = backupPage ? "Petal Backup" : "petalOS";
    $("#screen").className = `screen ${page.id}${backupPage ? " backup-subflow" : ""}${answers.largeText ? " large-text" : ""}`;
    $("#screen").innerHTML = home ? homeScreen() : `${hero}<h2 id="screen-heading">${escape(page.title)}</h2><p class="intro">${escape(page.body)}</p>${backupPage ? backupPageContent() : content(current)}`;
    $("#screen").setAttribute("aria-labelledby", home ? "home-heading" : "screen-heading");
    $("#progress-fill").style.width = `${(index + 1) / design.order.length * 100}%`;
    $(".progress-track").setAttribute("aria-valuenow", index + 1);
    $("#back").disabled = backupPage ? backupBusy : index === 0 && !home;
    $("#next").disabled = Boolean(backupPage && backupBusy);
    $("#skip").hidden = !page.skip || home;
    $("#skip").textContent = page.skip || "Not now";
    $("#next").innerHTML = `${escape(home ? "Back to welcome" : backupPage ? page.next : current === "restore" && answers.restore === "backup" ? "Open Petal Backup" : returnToReview ? "Back to review" : page.next)} <span aria-hidden="true">${current === "finish" ? "↗" : "→"}</span>`;
    $("#step-caption").innerHTML = `<span>${home ? "HOME SCREEN PREVIEW" : `${String(index + 1).padStart(2, "0")} / ${design.order.length}`}</span><span>${home ? "CONCEPT ONLY" : "YOUR NEXT CHAPTER"}</span>`;
    $("#form-error").textContent = "";
    renderRail();
    syncEditor();
    if (focus) { $("#screen").scrollTop = 0; $("#screen").focus({ preventScroll: true }); }
    setupPatternPad();
    if (backupPage && backupBusy) $("#screen").querySelectorAll("input, select, button").forEach(control => { control.disabled = true; });
    if (outgoing) slidePages(outgoing, oldScroll, oldHeight, backwards, motion.duration);
  }

  function stopPageTransition() {
    transitionRun++;
    pageAnimations.forEach(animation => animation.cancel());
    pageAnimations = [];
    outgoingPage?.remove();
    outgoingPage = null;
    $("#screen").style.removeProperty("z-index");
  }

  function slidePages(outgoing, scroll, height, backwards, duration) {
    const screen = $("#screen");
    if (!screen.animate) return;
    const run = transitionRun;
    const width = screen.clientWidth;
    // The old page is just scenery; it mustn't steal taps or focus.
    outgoing.removeAttribute("id");
    outgoing.removeAttribute("tabindex");
    outgoing.querySelectorAll("[id]").forEach(node => node.removeAttribute("id"));
    outgoing.setAttribute("aria-hidden", "true");
    outgoing.inert = true;
    outgoing.classList.add("page-outgoing");
    outgoing.style.height = `${height}px`;
    outgoing.style.zIndex = backwards ? "3" : "1";
    screen.style.zIndex = "2";
    $(".screen-viewport").appendChild(outgoing);
    outgoing.scrollTop = scroll;
    outgoingPage = outgoing;
    const timing = { duration, easing: PAGE_EASING, fill: "both" };
    const incomingFrom = backwards ? -width * PAGE_PARALLAX : width;
    const outgoingTo = backwards ? width : -width * PAGE_PARALLAX;
    pageAnimations = [
      outgoing.animate([
        { transform: "translateX(0)", opacity: 1 },
        { transform: `translateX(${outgoingTo}px)`, opacity: backwards ? 1 : 0.6 },
      ], timing),
      screen.animate([
        { transform: `translateX(${incomingFrom}px)`, opacity: backwards ? 0.6 : 1 },
        { transform: "translateX(0)", opacity: 1 },
      ], timing),
    ];
    Promise.allSettled(pageAnimations.map(animation => animation.finished)).then(() => {
      if (run === transitionRun) stopPageTransition();
    });
  }

  function homeScreen() {
    return `<h2 id="home-heading" class="visually-hidden">Welcome to your home screen</h2><div class="home-clock">09:41</div><div class="home-date">Friday, 16 October</div><div class="glass-card home-widget"><h3>Good morning.</h3><p>Your next chapter starts here.</p></div><div class="home-grid">${[["phone", "Phone"], ["globe", "Browser"], ["sun", "Camera"], ["spark", "Petal Hub"], ["clock", "Clock"], ["restore", "Files"], ["game", "Music"], ["shield", "Settings"]].map(([glyph, title]) => `<div class="home-app"><i>${icon(glyph)}</i>${title}</div>`).join("")}</div>`;
  }

  function go(id, { review = false } = {}) {
    if (!design.order.includes(id)) return;
    const backwards = design.order.indexOf(id) < design.order.indexOf(current);
    if (current === "lock" && id !== "lock") patternDraft = emptyPattern();
    if (backupPage) leaveBackupPage();
    closeSheet();
    current = id;
    home = false;
    returnToReview = review;
    try { history.replaceState(null, "", `#${id}`); } catch { /* Some file hosts don't like history updates. */ }
    render({ animate: true, backwards, focus: true });
  }

  function validatePage() {
    if (current === "wifi" && !answers.network) return "Choose a sample network, or select Use offline.";
    if (current === "lock") {
      if (answers.lockMethod === "none") { answers.secured = false; answers.credentialMethod = "none"; answers.fingerprint = 0; }
      else if (answers.lockMethod === "pattern") {
        if (!patternDraft.confirmed && !(answers.secured && answers.credentialMethod === "pattern" && !patternDraft.first.length && !patternDraft.current.length)) return "Draw and confirm your pattern first.";
        if (answers.credentialMethod !== "pattern") answers.fingerprint = 0;
        answers.secured = true;
        answers.credentialMethod = "pattern";
      } else {
        const pin = $("#demo-pin").value;
        const confirmation = $("#demo-pin-confirm").value;
        if (answers.secured && answers.credentialMethod === "pin" && !pin && !confirmation) return "";
        if (!/^\d{4,6}$/.test(pin)) return "Use 4–6 sample digits for the demo PIN.";
        if (pin !== confirmation) return "The sample PINs don’t match. Try again.";
        answers.secured = true;
        answers.credentialMethod = "pin";
        answers.fingerprint = 0;
      }
    }
    if (current === "biometric" && answers.fingerprint < 5) return answers.secured ? "Tap the sample sensor five times, or choose Not now." : "Set a screen lock first, or choose Not now.";
    return "";
  }

  function next(skip = false) {
    if (backupPage) { backupNext(skip); return; }
    if (home) { go("welcome"); return; }
    if (!skip) {
      const error = validatePage();
      if (error) { $("#form-error").textContent = error; return; }
    } else {
      if (current === "wifi") answers.network = "";
      if (current === "sim") answers.sim = "later";
      if (current === "lock") { answers.lockMethod = "none"; answers.secured = false; answers.credentialMethod = "none"; answers.fingerprint = 0; }
      if (current === "biometric") answers.fingerprint = 0;
      if (current === "apps") answers.apps = [];
    }
    save();
    if (current === "restore" && !skip && answers.restore === "backup") {
      answers.restored = false;
      save();
      backupDraft = emptyBackupDraft();
      showBackupPage("backup-destination");
      return;
    }
    if (returnToReview) { go("summary"); return; }
    if (current === "finish") { home = true; render({ animate: true, focus: true }); return; }
    go(design.order[design.order.indexOf(current) + 1]);
  }

  function toast(message) {
    clearTimeout(toastTimer);
    $("#toast").textContent = message;
    $("#toast").classList.add("visible");
    toastTimer = setTimeout(() => $("#toast").classList.remove("visible"), 3800);
  }

  function closeSheet() {
    clearTimeout(modalTimer);
    modalGeneration++;
    if ($("#sheet").open) $("#sheet").close();
    $("#sheet-content").replaceChildren();
  }

  function openSheet(markup) {
    closeSheet();
    $("#sheet-content").innerHTML = markup;
    $("#sheet").showModal();
  }

  function networkSheet(name) {
    const secured = name !== "Guest Wi-Fi";
    openSheet(`<h2>${escape(name)}</h2><p>Simulated connection. Use a sample password; it isn’t saved or sent anywhere.</p><form id="network-form"><input name="network" value="${escape(name)}" type="hidden">${secured ? '<label for="network-password">Sample password</label><input id="network-password" type="password" minlength="8" required autocomplete="off" placeholder="At least 8 sample characters">' : ""}<p id="network-status" role="status"></p><button class="primary-button" type="submit">Connect · demo</button></form>`);
  }

  function action(name) {
    if (name === "pattern-reset") { patternDraft = emptyPattern(); updatePatternUI(); }
    if (name === "pattern-commit") commitPattern();
    if (name === "scan") toast("Scan complete · 3 sample networks found.");
    if (name === "fingerprint") {
      answers.fingerprint = Math.min(5, answers.fingerprint + 1); save(); render();
      if (answers.fingerprint === 5) toast("Sample fingerprint added.");
    }
    if (name === "privacy") openSheet("<h2>About diagnostics</h2><p>This is a proposed opt-in screen. During native implementation, this sheet will show the exact supported diagnostics and link to the project’s real privacy policy.</p><p>Nothing is collected by this browser prototype.</p>");
    if (name === "navigation-help") openSheet(`<h2>${answers.navigation === "gestures" ? "A few simple moves." : "Familiar faces."}</h2><p>${answers.navigation === "gestures" ? "Home: swipe up from the bottom.<br><br>Recent apps: swipe up and hold.<br><br>Back: swipe inward from either side." : "Back: go to the previous screen.<br><br>Home: return to the launcher.<br><br>Recent apps: switch between open apps."}</p><p>The final Android version can launch the system’s supported navigation tutorial.</p>`);
    if (name === "restore") {
      showBackupPage("backup-destination");
    }
    if (name === "backup-folder") $("#backup-folder-picker").click();
    if (name === "backup-sign-in") openSheet(`<h2>${escape(answers.backupProvider)}</h2><p>Preview sign-in. No authentication request is sent.</p><form id="backup-account-form"><label for="backup-account-email">Demo account email</label><input id="backup-account-email" type="email" required autocomplete="off" placeholder="you@example.com"><button class="primary-button" type="submit">Use demo account</button></form>`);
  }

  document.addEventListener("click", event => {
    const target = event.target.closest("button");
    if (!target || target.disabled) return;
    if (target.dataset.patternDot) {
      if (event.detail === 0) appendPatternDot(Number(target.dataset.patternDot));
      return;
    }
    if (target.dataset.jump) go(target.dataset.jump);
    if (target.dataset.review) go(target.dataset.review, { review: true });
    if (target.dataset.choice) {
      const key = target.dataset.choice;
      const previous = answers[key];
      answers[key] = target.dataset.value;
      if (key === "backupProvider" && answers[key] !== previous) resetBackupConnection();
      if (key === "lockMethod") patternDraft = emptyPattern();
      if (key === "lockMethod" && answers.lockMethod === "none") { answers.secured = false; answers.credentialMethod = "none"; answers.fingerprint = 0; }
      save(); render();
    }
    if (target.dataset.network) networkSheet(target.dataset.network);
    if (target.dataset.accent) { design.accent = target.dataset.accent; save(); render(); }
    if (target.dataset.app) {
      const id = target.dataset.app;
      answers.apps = answers.apps.includes(id) ? answers.apps.filter(app => app !== id) : [...answers.apps, id];
      save(); render();
    }
    if (target.dataset.action) action(target.dataset.action);
    if (target.dataset.snapshot) { backupDraft.snapshot = target.dataset.snapshot; render(); }
  });

  document.addEventListener("input", event => {
    const field = event.target.dataset.backupField;
    if (backupPage && ["folder", "endpoint", "host", "port", "share", "domain", "username", "password", "remotePath", "region", "bucket", "accessKey", "secretKey"].includes(field)) {
      backupDraft[field] = event.target.value;
      $("#form-error").textContent = "";
    }
  });

  document.addEventListener("change", event => {
    if (event.target.id === "backup-folder-picker") {
      const file = event.target.files[0];
      if (file) backupDraft.folder = file.webkitRelativePath.split("/")[0] || file.name;
      render();
      return;
    }
    const key = event.target.dataset.key;
    if (!key || !Object.hasOwn(DEFAULT_ANSWERS, key)) return;
    answers[key] = event.target.type === "checkbox" ? event.target.checked : event.target.value;
    if (backupPage && key === "backupProvider") resetBackupConnection();
    save();
    render();
  });

  document.addEventListener("submit", event => {
    if (event.target.id === "backup-account-form") {
      event.preventDefault();
      backupDraft.account = $("#backup-account-email").value.trim();
      closeSheet();
      render();
      return;
    }
    if (event.target.id !== "network-form") return;
    event.preventDefault();
    const form = event.target;
    const name = form.elements.network.value;
    form.querySelector("button").disabled = true;
    $("#network-status").textContent = "Connecting to sample network…";
    const password = $("#network-password");
    if (password) password.value = "";
    const generation = modalGeneration;
    modalTimer = setTimeout(() => {
      if (generation !== modalGeneration || !$("#sheet").open) return;
      answers.network = name; save(); closeSheet(); render(); toast(`Connected to ${name} · simulated.`);
    }, 650);
  });

  $("#next").addEventListener("click", () => next());
  $("#skip").addEventListener("click", () => next(true));
  $("#back").addEventListener("click", () => {
    if (backupPage) backupBack();
    else if (home) { home = false; render(); }
    else if (returnToReview) go("summary");
    else go(design.order[Math.max(0, design.order.indexOf(current) - 1)]);
  });
  $("#close-sheet").addEventListener("click", closeSheet);
  $("#sheet").addEventListener("cancel", event => { event.preventDefault(); closeSheet(); });
  $("#sheet").addEventListener("click", event => { if (event.target === $("#sheet")) { const box = event.target.getBoundingClientRect(); if (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom) closeSheet(); } });
  $("#accessibility").addEventListener("click", () => {
    openSheet(`<h2>A comfortable start.</h2><p>Preview accessibility preferences for setup.</p><label><input type="checkbox" id="large-text" ${answers.largeText ? "checked" : ""}> Larger setup text</label><label><input type="checkbox" id="reduce-motion" ${answers.reduceMotion ? "checked" : ""}> Reduce motion</label><p>Screen reader support uses browser semantics here. Android’s accessibility shortcut and service chooser will remain native.</p>`);
    $("#large-text").addEventListener("change", event => { answers.largeText = event.target.checked; save(); render(); });
    $("#reduce-motion").addEventListener("change", event => {
      answers.reduceMotion = event.target.checked;
      if (answers.reduceMotion) forcePreviewMotion = false;
      save(); render();
    });
  });
  $("#restart").addEventListener("click", () => {
    answers = { ...clone(DEFAULT_ANSWERS), theme: answers.theme, largeText: answers.largeText, reduceMotion: answers.reduceMotion };
    save(); go("welcome"); toast("Setup restarted. Your design edits are kept.");
  });
  $("#preview-mode").addEventListener("click", () => {
    const active = document.body.classList.toggle("focus-preview");
    $("#preview-mode").setAttribute("aria-pressed", active);
    toast("Press Escape to return to the design desk.");
  });
  $("#exit-preview").addEventListener("click", () => {
    document.body.classList.remove("focus-preview");
    $("#preview-mode").setAttribute("aria-pressed", "false");
  });
  $("#editor-toggle").addEventListener("click", () => {
    const hidden = document.body.classList.toggle("editor-hidden");
    $("#editor-toggle").setAttribute("aria-expanded", !hidden);
    $("#editor-toggle").textContent = hidden ? "Edit concept" : "Close editor";
  });

  for (const [selector, key, dom] of [["#edit-title", "title", "#screen-heading"], ["#edit-body", "body", ".screen .intro"], ["#edit-next", "next", "#next"]]) {
    $(selector).addEventListener("input", event => {
      const id = step().id;
      design.copy[id] = { ...design.copy[id], [key]: event.target.value };
      const value = event.target.value;
      if ($(dom) && !home) {
        if (key === "next") $(dom).innerHTML = `${escape(value)} <span aria-hidden="true">→</span>`;
        else $(dom).textContent = value;
      }
      save();
    });
  }
  $("#edit-accent").addEventListener("input", event => { design.accent = event.target.value; applyTokens(); save(); });
  $("#edit-radius").addEventListener("input", event => { design.radius = Number(event.target.value); $("#radius-value").textContent = `${design.radius} px`; applyTokens(); save(); });
  $("#edit-motion").addEventListener("input", event => { design.motion = Number(event.target.value); $("#motion-value").textContent = `${design.motion} ms`; applyTokens(); save(); });
  $("#force-motion").addEventListener("change", event => {
    forcePreviewMotion = event.target.checked;
    if (forcePreviewMotion && design.motion === 0) design.motion = DEFAULT_DESIGN.motion;
    save(); render();
  });
  $("#enable-slides").addEventListener("click", () => {
    forcePreviewMotion = true;
    if (design.motion === 0) design.motion = DEFAULT_DESIGN.motion;
    save(); render({ animate: true });
    toast("Slides enabled for this preview.");
  });
  $("#edit-mode").addEventListener("change", event => { answers.theme = event.target.value; save(); render(); });

  function move(direction) {
    if (backupPage) return;
    const i = design.order.indexOf(current);
    const j = i + direction;
    if (i <= 0 || i >= design.order.length - 2 || j <= 0 || j >= design.order.length - 2) return;
    [design.order[i], design.order[j]] = [design.order[j], design.order[i]];
    save(); render(); toast("Screen order updated.");
  }
  $("#move-earlier").addEventListener("click", () => move(-1));
  $("#move-later").addEventListener("click", () => move(1));
  $("#reset-design").addEventListener("click", () => {
    openSheet('<h2>Reset the design?</h2><p>This replaces your copy, palette, motion, and screen order with the original concept. Export first if you want to keep your edits.</p><button class="primary-button" type="button" id="confirm-reset">Reset design</button>');
    $("#confirm-reset").addEventListener("click", () => { design = clone(DEFAULT_DESIGN); save(); closeSheet(); render(); toast("Original design restored."); });
  });

  $("#export-design").addEventListener("click", () => {
    const payload = {
      schema: 1, concept: "petalOS Setup · Simple", exportedAt: new Date().toISOString(), design, selections: answers,
      transition: { style: "shared-slide", durationMs: design.motion, easing: PAGE_EASING, parallax: PAGE_PARALLAX, forward: "enter-right-exit-left", backward: "enter-left-exit-right", reducedMotion: "instant" },
      logoSource: "vendor/petal/apps/PetalHub/res/drawable-nodpi/petal_logo.png",
      backupIntegration: { module: "PetalBackup", package: "org.petal.backup", action: "org.petal.backup.action.RESTORE", fromSetupExtra: "org.petal.backup.extra.FROM_SETUP", readinessMetadata: "org.petal.backup.SETUP_UI_READY", scope: "root-capable", storage: ["local", "cloud"], status: "native-ui-engine-pending" },
      screens: design.order.map(id => ({ ...step(id, false), kind: step(id, false).target.startsWith("PROPOSED") ? "proposed" : "existing-or-extended" })),
      backupPages: Object.keys(BACKUP_PAGES).map(id => ({ ...BACKUP_PAGES[id], ...design.copy[id] })),
      nativeReference: "packages/apps/SetupWizard/res/raw/lineage_wizard_script.xml",
      notes: ["Browser simulation only; no Android provisioning occurs.", "PIN digits, pattern paths, and Wi-Fi passwords are never persisted or exported.", "Lock-before-biometrics and diagnostics-off are proposed native behaviour changes.", "Native mapping and conditional screens require implementation review before shipping."],
    };
    const url = URL.createObjectURL(new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" }));
    const link = document.createElement("a"); link.href = url; link.download = "petalos-oobe-design.json"; link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    toast("Design exported. Keep this JSON with your approved concept.");
  });
  $("#import-design").addEventListener("click", () => $("#import-file").click());
  $("#import-file").addEventListener("change", async event => {
    const file = event.target.files[0];
    if (!file) return;
    try {
      if (file.size > 1024 * 1024) throw new Error("Choose a design JSON smaller than 1 MB.");
      const parsed = JSON.parse(await file.text());
      const nextDesign = validateDesign(parsed.design || parsed);
      const nextAnswers = parsed.selections ? readAnswers(parsed.selections) : answers;
      design = nextDesign; answers = nextAnswers; save(); render(); toast("Design imported.");
    } catch (error) { toast(error.message || "Couldn’t import that design."); }
    event.target.value = "";
  });

  document.addEventListener("keydown", event => {
    if (event.key === "Escape" && !$("#sheet").open) {
      document.body.classList.remove("focus-preview");
      $("#preview-mode").setAttribute("aria-pressed", "false");
    }
    if ($("#sheet").open || event.target.closest("input, textarea, select, button, summary") || event.altKey || event.ctrlKey || event.metaKey) return;
    if (event.key === "ArrowRight") { event.preventDefault(); next(); }
    if (event.key === "ArrowLeft") { event.preventDefault(); $("#back").click(); }
  });
  window.addEventListener("hashchange", () => { const id = location.hash.slice(1); if (design.order.includes(id)) go(id); });
  matchMedia("(prefers-color-scheme: dark)").addEventListener("change", () => { if (answers.theme === "system") render(); });
  matchMedia("(prefers-reduced-motion: reduce)").addEventListener("change", event => {
    if (event.matches && !forcePreviewMotion) stopPageTransition();
    syncMotionStatus();
  });
  if (design.order.includes(location.hash.slice(1))) current = location.hash.slice(1);
  if (location.hash.startsWith("#restore/backup/")) {
    current = "restore";
    answers.restore = "backup";
    const id = "backup-" + location.hash.split("/").at(-1);
    backupPage = ["backup-destination", "backup-local", "backup-cloud", "backup-login"].includes(id) ? id : "backup-destination";
    if (["backup-cloud", "backup-login"].includes(backupPage)) answers.backupDestination = "cloud";
    if (backupPage === "backup-local") answers.backupDestination = "local";
    if (backupPage === "backup-login") resetBackupConnection();
  }
  if (new URLSearchParams(location.search).get("preview") === "1") document.body.classList.add("focus-preview");
  render();
})();
