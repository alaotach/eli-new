#!/usr/bin/env python3
"""
ELI5 Desktop - Official Arch/Linux Companion
Matches the original ELI5 Android BottomSheet design with tabs, caching, streaming, and custom prompt bar.
"""

import sys
import os
import re
import json
import urllib.request
import urllib.error
import subprocess
import threading

import gi
gi.require_version('Gtk', '3.0')
gi.require_version('Gdk', '3.0')
gi.require_version('GtkLayerShell', '0.1')
from gi.repository import Gtk, Gdk, GtkLayerShell, GLib, Pango

API_BASE_URL = "https://eli5-api.vpm111964.workers.dev"

WINDOW_WIDTH = 440
WINDOW_HEIGHT = 380

CSS = b"""
window {
    background-color: transparent;
}

.eli5-card {
    background-color: rgba(26, 26, 28, 0.98);
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: 20px;
    padding: 16px;
    box-shadow: 0 16px 40px rgba(0, 0, 0, 0.7);
}

.header-title {
    color: #ffffff;
    font-size: 15px;
    font-weight: 700;
}

.copy-btn {
    color: #3894e6;
    font-size: 13px;
    font-weight: 600;
    background: transparent;
    border: none;
    padding: 4px 10px;
    border-radius: 6px;
    transition: all 120ms ease;
}

.copy-btn:hover {
    background-color: rgba(56, 148, 230, 0.15);
}

.close-btn {
    color: #888888;
    font-size: 16px;
    font-weight: 500;
    background: transparent;
    border: none;
    padding: 4px 8px;
    border-radius: 6px;
    transition: all 120ms ease;
}

.close-btn:hover {
    color: #ffffff;
    background-color: rgba(255, 255, 255, 0.1);
}

.modes-bar {
    background-color: #121214;
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 12px;
    padding: 3px;
    margin-top: 10px;
    margin-bottom: 12px;
}

.mode-tab {
    background: transparent;
    color: #8e8e93;
    font-size: 12px;
    font-weight: 600;
    border: none;
    border-radius: 8px;
    padding: 6px 10px;
    transition: all 150ms ease;
}

.mode-tab:hover {
    color: #ffffff;
    background-color: rgba(255, 255, 255, 0.06);
}

.mode-tab-active {
    background-color: #007acc;
    color: #ffffff;
}

.mode-tab-active:hover {
    background-color: #0088e3;
    color: #ffffff;
}

.content-box {
    background-color: #212124;
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 14px;
    padding: 12px;
}

.result-text {
    background-color: transparent;
    color: #e2e2e6;
    font-size: 13.5px;
    line-height: 1.35;
}

.refine-bar {
    background-color: #161618;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 10px;
    padding: 2px 4px 2px 10px;
    margin-top: 10px;
}

.refine-entry {
    background: transparent;
    border: none;
    color: #ffffff;
    font-size: 12px;
}

.refine-entry:focus {
    outline: none;
    box-shadow: none;
}

.refine-btn {
    background-color: #007acc;
    color: #ffffff;
    border: none;
    border-radius: 6px;
    padding: 4px 10px;
    font-size: 12px;
    font-weight: 600;
}

.refine-btn:hover {
    background-color: #0088e3;
}
"""

def get_selected_text():
    try:
        res = subprocess.run(['wl-paste', '-p'], stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, timeout=0.5)
        text = res.stdout.decode('utf-8', errors='replace').strip()
        if text:
            return text
    except Exception:
        pass

    try:
        res = subprocess.run(['wl-paste'], stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, timeout=0.5)
        text = res.stdout.decode('utf-8', errors='replace').strip()
        if text:
            return text
    except Exception:
        pass

    return ""

def copy_to_clipboard(text):
    try:
        p1 = subprocess.Popen(['wl-copy'], stdin=subprocess.PIPE)
        p1.communicate(input=text.encode('utf-8'))
        p2 = subprocess.Popen(['wl-copy', '-p'], stdin=subprocess.PIPE)
        p2.communicate(input=text.encode('utf-8'))
    except Exception as e:
        print(f"Copy error: {e}", file=sys.stderr)

class ELI5DesktopWindow(Gtk.Window):
    def __init__(self, selected_text, cx, cy):
        super().__init__(type=Gtk.WindowType.TOPLEVEL)
        self.selected_text = selected_text
        self.cursor_x = cx
        self.cursor_y = cy

        self.cached_explanations = {}
        self.current_mode = "Direct"
        self.current_text_content = ""
        self.is_streaming = False
        self.current_job_id = 0

        self.setup_window()
        self.build_ui()

        # Start default mode automatically (Direct / Straightforward), matching Android
        self.request_explanation("Direct")

    def setup_window(self):
        GtkLayerShell.init_for_window(self)
        GtkLayerShell.set_layer(self, GtkLayerShell.Layer.OVERLAY)
        GtkLayerShell.set_keyboard_mode(self, GtkLayerShell.KeyboardMode.ON_DEMAND)

        GtkLayerShell.set_anchor(self, GtkLayerShell.Edge.TOP, True)
        GtkLayerShell.set_anchor(self, GtkLayerShell.Edge.LEFT, True)

        screen = Gdk.Screen.get_default()
        visual = screen.get_rgba_visual()
        if visual:
            self.set_visual(visual)

        display = Gdk.Display.get_default()
        rel_x = self.cursor_x
        rel_y = self.cursor_y
        mon_w, mon_h = 1920, 1080

        for i in range(display.get_n_monitors()):
            m = display.get_monitor(i)
            geom = m.get_geometry()
            if geom.x <= self.cursor_x < geom.x + geom.width and geom.y <= self.cursor_y < geom.y + geom.height:
                rel_x = self.cursor_x - geom.x
                rel_y = self.cursor_y - geom.y
                mon_w = geom.width
                mon_h = geom.height
                GtkLayerShell.set_monitor(self, m)
                break

        # Smart placement to prevent overflow
        if rel_y + WINDOW_HEIGHT + 24 < mon_h:
            pos_y = rel_y + 4
        else:
            pos_y = max(10, rel_y - WINDOW_HEIGHT - 6)

        if rel_x + WINDOW_WIDTH + 24 < mon_w:
            pos_x = rel_x + 4
        else:
            pos_x = max(10, rel_x - WINDOW_WIDTH - 6)

        GtkLayerShell.set_margin(self, GtkLayerShell.Edge.LEFT, pos_x)
        GtkLayerShell.set_margin(self, GtkLayerShell.Edge.TOP, pos_y)

        self.connect("key-press-event", self.on_key_press)
        self.connect("focus-out-event", self.on_focus_out)
        self.add_events(Gdk.EventMask.FOCUS_CHANGE_MASK)

    def build_ui(self):
        card = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=0)
        card.get_style_context().add_class("eli5-card")
        card.set_size_request(WINDOW_WIDTH, WINDOW_HEIGHT)
        self.add(card)

        # 1. Header (Title, Copy, Close)
        header = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=6)

        title_str = self.selected_text.replace('\n', ' ')
        if len(title_str) > 28:
            title_str = title_str[:26] + "..."
        if not title_str:
            title_str = "ELI5 Assistant"

        title_lbl = Gtk.Label(label=title_str)
        title_lbl.get_style_context().add_class("header-title")
        title_lbl.set_xalign(0)
        title_lbl.set_ellipsize(Pango.EllipsizeMode.END)
        header.pack_start(title_lbl, True, True, 0)

        self.copy_btn = Gtk.Button(label="📋 Copy")
        self.copy_btn.get_style_context().add_class("copy-btn")
        self.copy_btn.connect("clicked", self.on_copy_clicked)
        header.pack_end(self.copy_btn, False, False, 2)

        close_btn = Gtk.Button(label="✕")
        close_btn.get_style_context().add_class("close-btn")
        close_btn.connect("clicked", lambda b: self.close())
        header.pack_end(close_btn, False, False, 0)

        card.pack_start(header, False, False, 0)

        # 2. Segmented Modes Bar (Tabs)
        self.modes_bar = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=2)
        self.modes_bar.get_style_context().add_class("modes-bar")

        self.mode_buttons = {}
        modes = [
            ("Direct", "Direct"),
            ("ELI5", "ELI5"),
            ("Detailed", "Detailed"),
            ("Proofread", "Proofread"),
            ("Translate", "Translate"),
        ]

        for mode_key, label in modes:
            btn = Gtk.Button(label=label)
            btn.get_style_context().add_class("mode-tab")
            btn.connect("clicked", lambda b, k=mode_key: self.request_explanation(k))
            self.modes_bar.pack_start(btn, True, True, 0)
            self.mode_buttons[mode_key] = btn

        card.pack_start(self.modes_bar, False, False, 0)

        # 3. Content Box with Scrollable Text
        content_box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=0)
        content_box.get_style_context().add_class("content-box")

        self.scrolled = Gtk.ScrolledWindow()
        self.scrolled.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        self.scrolled.set_size_request(-1, 210)

        self.text_view = Gtk.TextView()
        self.text_view.set_editable(False)
        self.text_view.set_cursor_visible(False)
        self.text_view.set_wrap_mode(Gtk.WrapMode.WORD_CHAR)
        self.text_view.set_left_margin(4)
        self.text_view.set_right_margin(4)
        self.text_view.get_style_context().add_class("result-text")

        self.text_buffer = self.text_view.get_buffer()
        self.setup_text_tags()

        self.scrolled.add(self.text_view)
        content_box.pack_start(self.scrolled, True, True, 0)
        card.pack_start(content_box, True, True, 0)

        # 4. Refine / Follow-up Input Bar
        refine_box = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=4)
        refine_box.get_style_context().add_class("refine-bar")

        self.refine_entry = Gtk.Entry()
        self.refine_entry.set_placeholder_text("Ask follow-up or custom instructions...")
        self.refine_entry.get_style_context().add_class("refine-entry")
        self.refine_entry.connect("activate", self.on_refine_submit)
        refine_box.pack_start(self.refine_entry, True, True, 0)

        refine_btn = Gtk.Button(label="➜")
        refine_btn.get_style_context().add_class("refine-btn")
        refine_btn.connect("clicked", self.on_refine_submit)
        refine_box.pack_end(refine_btn, False, False, 0)

        card.pack_start(refine_box, False, False, 0)

    def setup_text_tags(self):
        self.tag_bold = self.text_buffer.create_tag("bold", weight=Pango.Weight.BOLD, foreground="#ffffff")
        self.tag_header = self.text_buffer.create_tag("header", weight=Pango.Weight.BOLD, scale=1.12, foreground="#3894e6")
        self.tag_bullet = self.text_buffer.create_tag("bullet", foreground="#3894e6", weight=Pango.Weight.BOLD)
        self.tag_code = self.text_buffer.create_tag("code", font="monospace 11", background="rgba(255,255,255,0.08)")

    def update_tabs(self, active_mode):
        for k, btn in self.mode_buttons.items():
            ctx = btn.get_style_context()
            if k == active_mode:
                ctx.add_class("mode-tab-active")
            else:
                ctx.remove_class("mode-tab-active")

    def request_explanation(self, mode, custom_prompt=None):
        self.current_mode = mode
        self.update_tabs(mode)
        self.copy_btn.set_label("📋 Copy")

        cache_key = f"{mode}:{custom_prompt or ''}"
        if cache_key in self.cached_explanations:
            cached = self.cached_explanations[cache_key]
            self.current_text_content = cached
            self.render_formatted_text(cached)
            return

        self.current_job_id += 1
        job_id = self.current_job_id
        self.is_streaming = True
        self.current_text_content = ""
        self.text_buffer.set_text("Explaining...")

        # Setup endpoint & payload
        if mode == "Proofread":
            endpoint = "/proofread"
            payload = {"text": self.selected_text, "instructions": custom_prompt or ""}
        elif mode == "Translate":
            endpoint = "/proofread"
            payload = {"text": self.selected_text, "instructions": "Translate the text into natural, fluent English."}
        else:
            endpoint = "/explain"
            api_mode = "Straightforward" if mode == "Direct" else mode
            payload = {"text": self.selected_text, "mode": api_mode}

        threading.Thread(
            target=self.fetch_stream,
            args=(endpoint, payload, job_id, cache_key),
            daemon=True
        ).start()

    def fetch_stream(self, endpoint, payload, job_id, cache_key):
        url = f"{API_BASE_URL}{endpoint}"
        data = json.dumps(payload).encode('utf-8')
        req = urllib.request.Request(
            url,
            data=data,
            headers={
                "Content-Type": "application/json",
                "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) ELI5-Desktop/2.2"
            }
        )

        full_response = ""
        is_first = True

        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                for line in resp:
                    if job_id != self.current_job_id or not self.is_streaming:
                        return
                    line_str = line.decode('utf-8', errors='replace').strip()
                    if not line_str.startswith("data:"):
                        continue
                    data_str = line_str[5:].strip()
                    if data_str == "[DONE]":
                        break
                    try:
                        chunk = json.loads(data_str)
                        choices = chunk.get("choices", [])
                        if choices:
                            delta = choices[0].get("delta", {})
                            content = delta.get("content", "")
                            if content:
                                if is_first:
                                    is_first = False
                                    GLib.idle_add(self.clear_buffer, job_id)
                                full_response += content
                                GLib.idle_add(self.append_stream_token, content, job_id)
                    except json.JSONDecodeError:
                        continue
        except Exception as e:
            GLib.idle_add(self.append_stream_token, f"\n[Error: {e}]", job_id)
        finally:
            GLib.idle_add(self.finish_stream, full_response, cache_key, job_id)

    def clear_buffer(self, job_id):
        if job_id == self.current_job_id:
            self.text_buffer.set_text("")
        return False

    def append_stream_token(self, token, job_id):
        if job_id == self.current_job_id:
            end = self.text_buffer.get_end_iter()
            self.text_buffer.insert(end, token)
            adj = self.scrolled.get_vadjustment()
            adj.set_value(adj.get_upper() - adj.get_page_size())
        return False

    def finish_stream(self, full_response, cache_key, job_id):
        if job_id == self.current_job_id:
            self.is_streaming = False
            self.current_text_content = full_response
            if full_response:
                self.cached_explanations[cache_key] = full_response
                self.render_formatted_text(full_response)
        return False

    def render_formatted_text(self, raw_text):
        self.text_buffer.set_text("")
        lines = raw_text.split('\n')

        for i, line in enumerate(lines):
            # Header
            if line.startswith('### ') or line.startswith('## ') or line.startswith('# '):
                h_text = line.lstrip('#').strip()
                iter_end = self.text_buffer.get_end_iter()
                self.text_buffer.insert_with_tags(iter_end, h_text, self.tag_header)
            # Bullet
            elif line.startswith('- ') or line.startswith('* '):
                iter_end = self.text_buffer.get_end_iter()
                self.text_buffer.insert_with_tags(iter_end, "• ", self.tag_bullet)
                self.insert_inline_formatted(line[2:])
            else:
                self.insert_inline_formatted(line)

            if i < len(lines) - 1:
                end = self.text_buffer.get_end_iter()
                self.text_buffer.insert(end, "\n")

    def insert_inline_formatted(self, text):
        # Parse **bold** and `code`
        tokens = re.split(r'(\*\*.*?\*\*|`.*?`)', text)
        for tok in tokens:
            end = self.text_buffer.get_end_iter()
            if tok.startswith('**') and tok.endswith('**') and len(tok) >= 4:
                self.text_buffer.insert_with_tags(end, tok[2:-2], self.tag_bold)
            elif tok.startswith('`') and tok.endswith('`') and len(tok) >= 2:
                self.text_buffer.insert_with_tags(end, tok[1:-1], self.tag_code)
            else:
                self.text_buffer.insert(end, tok)

    def on_refine_submit(self, widget):
        prompt = self.refine_entry.get_text().strip()
        if not prompt:
            return
        self.refine_entry.set_text("")
        self.request_explanation("Proofread", custom_prompt=prompt)

    def on_copy_clicked(self, btn):
        if self.current_text_content:
            copy_to_clipboard(self.current_text_content)
            self.copy_btn.set_label("✓ Copied")

    def on_focus_out(self, widget, event):
        self.close()
        return False

    def on_key_press(self, widget, event):
        keyval = event.keyval
        if keyval == Gdk.KEY_Escape:
            self.close()
            return True

        # Quick tab shortcuts: 1-5
        if not self.refine_entry.is_focus():
            modes = ["Direct", "ELI5", "Detailed", "Proofread", "Translate"]
            if Gdk.KEY_1 <= keyval <= Gdk.KEY_5:
                idx = keyval - Gdk.KEY_1
                if idx < len(modes):
                    self.request_explanation(modes[idx])
                    return True

        return False

def main():
    selected_text = get_selected_text()
    if not selected_text:
        try:
            subprocess.run(['notify-send', '-a', 'ELI5', '-t', '1500', 'ELI5', 'Please highlight text first'], timeout=0.5)
        except Exception:
            pass
        return

    cx, cy = 960, 540
    try:
        out = subprocess.check_output(['hyprctl', 'cursorpos'], timeout=0.4).decode().strip()
        parts = [int(p.strip()) for p in out.split(',')]
        if len(parts) == 2:
            cx, cy = parts
    except Exception:
        pass

    style_provider = Gtk.CssProvider()
    style_provider.load_from_data(CSS)
    Gtk.StyleContext.add_provider_for_screen(
        Gdk.Screen.get_default(),
        style_provider,
        Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION
    )

    win = ELI5DesktopWindow(selected_text, cx, cy)
    win.connect("destroy", Gtk.main_quit)
    win.show_all()
    Gtk.main()

if __name__ == "__main__":
    main()
