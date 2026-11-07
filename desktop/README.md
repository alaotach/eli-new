# ELI5 Desktop Companion for Arch Linux / Wayland

An instant, floating AI context menu and explanation card for Linux (Hyprland / Wayland), styled to mirror the official ELI5 Android experience.

---

## ✨ Features

- **Instant Cursor Context:** Pops up right at your mouse cursor on any screen or monitor.
- **Auto-Flip Placement:** Automatically flips above or to the left if triggered near screen borders so it never gets clipped.
- **Original ELI5 Segmented Tabs:**
  - `Direct` — Clear, straightforward summary.
  - `ELI5` — Explain like I'm 5 with simple words and fun analogies.
  - `Detailed` — Deep analysis and comprehensive context.
  - `Proofread` — Polish grammar, flow, and tone.
  - `Translate` — Translate highlighted text to natural English.
- **Response Caching:** Switching between tabs is instantaneous—previously fetched modes are stored in memory just like the mobile app.
- **Real-Time Token Streaming:** Streams tokens directly from your Cloudflare Worker proxy (`eli5-api.vpm111964.workers.dev`).
- **Follow-up / Custom Prompt Bar:** Type follow-up questions or custom instructions at the bottom.
- **Clipboard Integration:** One-click `📋 Copy` copies to both standard clipboard and Wayland primary selection buffer.
- **Dismiss on Demand:** Closes on `Escape`, clicking outside, or moving the cursor away.

---

## 🚀 Quick Setup

Run the installer:
```bash
chmod +x install.sh
./install.sh
```

### Hyprland Keybindings
In your Hyprland configuration (e.g. `~/.config/hypr/custom/keybinds.lua`):
```lua
hl.bind("SUPER + SHIFT + E", hl.dsp.exec_cmd(HOME .. "/.local/bin/eli5-menu"), { description = "ELI5: AI Context Menu" })
hl.bind("SUPER + ALT + mouse:273", hl.dsp.exec_cmd(HOME .. "/.local/bin/eli5-menu"), { description = "ELI5: AI Context Menu (Right Click)" })
```

---

## ⌨️ Shortcuts inside the ELI5 Card
- `1` to `5` — Switch between Direct, ELI5, Detailed, Proofread, and Translate tabs.
- `Esc` — Close the popup.
