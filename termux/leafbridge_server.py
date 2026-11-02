#!/usr/bin/env python3
"""LeafBridge local engine. Binds to loopback only; no remote listener."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from datetime import datetime, timezone
import json
import threading

HOST = "127.0.0.1"
PORT = 8765
DATA = Path.home() / ".leafbridge"
INBOX = DATA / "shazam-inbox.jsonl"
LOG = DATA / "engine.log"
LOCK = threading.Lock()
AUTOMATIONS = {
    "shazam_spotify": {"enabled": False, "implemented": False, "label": "Shazam → Spotify"},
    "github_sync": {"enabled": False, "implemented": False, "label": "Synchronisation GitHub"},
    "music_sort": {"enabled": False, "implemented": False, "label": "Classement musical"},
    "routine": {"enabled": False, "implemented": False, "label": "Routine LeafBridge"},
    "shazam_inbox": {"enabled": True, "implemented": True, "label": "Réception Shazam locale"},
}

def now():
    return datetime.now(timezone.utc).isoformat()

def log(event, details=None):
    DATA.mkdir(parents=True, exist_ok=True)
    entry = {"time": now(), "event": event, "details": details or {}}
    with LOCK, LOG.open("a", encoding="utf-8") as stream:
        stream.write(json.dumps(entry, ensure_ascii=False) + "\n")

class Handler(BaseHTTPRequestHandler):
    server_version = "LeafBridgeEngine/1.0"

    def reply(self, code, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def read_json(self):
        length = int(self.headers.get("Content-Length", "0"))
        if length < 1 or length > 65536:
            raise ValueError("Taille JSON invalide")
        return json.loads(self.rfile.read(length).decode("utf-8"))

    def do_GET(self):
        if self.path == "/status":
            self.reply(200, {"ok": True, "message": "Moteur LeafBridge connecté", "service": "LeafBridgeEngine", "version": "1.0", "host": HOST, "port": PORT, "automations": AUTOMATIONS})
        elif self.path == "/automations":
            self.reply(200, {"ok": True, "automations": AUTOMATIONS})
        elif self.path == "/logs":
            DATA.mkdir(parents=True, exist_ok=True)
            lines = LOG.read_text(encoding="utf-8").splitlines()[-50:] if LOG.exists() else []
            self.reply(200, {"ok": True, "logs": [json.loads(line) for line in lines]})
        else:
            self.reply(404, {"ok": False, "message": "Route inconnue"})

    def do_POST(self):
        try:
            data = self.read_json()
        except (ValueError, json.JSONDecodeError, UnicodeDecodeError) as exc:
            self.reply(400, {"ok": False, "message": f"JSON invalide : {exc}"})
            return

        if self.path == "/shazam":
            if not AUTOMATIONS["shazam_inbox"]["enabled"]:
                self.reply(409, {"ok": False, "message": "Réception Shazam en pause"})
                return
            DATA.mkdir(parents=True, exist_ok=True)
            record = {"received_at": now(), "payload": data}
            with LOCK, INBOX.open("a", encoding="utf-8") as stream:
                stream.write(json.dumps(record, ensure_ascii=False) + "\n")
            log("shazam_received", {"title": data.get("title", ""), "text": data.get("text", "")})
            self.reply(200, {"ok": True, "message": "Notification Shazam enregistrée localement"})
            return

        if self.path == "/automation":
            key = data.get("id")
            command = data.get("command")
            item = AUTOMATIONS.get(key)
            if item is None or command not in ("start", "stop"):
                self.reply(400, {"ok": False, "message": "Automatisation ou commande inconnue"})
                return
            if not item["implemented"]:
                self.reply(501, {"ok": False, "message": f"{item['label']} : connecteur à configurer avant activation"})
                return
            item["enabled"] = command == "start"
            log("automation_" + command, {"id": key})
            self.reply(200, {"ok": True, "message": f"{item['label']} : " + ("activée" if item["enabled"] else "arrêtée"), "automation": item})
            return

        self.reply(404, {"ok": False, "message": "Route inconnue"})

    def log_message(self, fmt, *args):
        # Keep Termux output concise; detailed events go to the local log.
        pass

if __name__ == "__main__":
    DATA.mkdir(parents=True, exist_ok=True)
    log("engine_started", {"host": HOST, "port": PORT})
    print(f"LeafBridge Engine actif sur http://{HOST}:{PORT}")
    print(f"Données locales : {DATA}")
    try:
        ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
    except KeyboardInterrupt:
        print("\nArrêt du moteur LeafBridge")
