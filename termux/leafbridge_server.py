#!/usr/bin/env python3
"""Moteur local LeafBridge pour Android <-> Termux.

Le serveur écoute uniquement sur 127.0.0.1. Les états des automatisations
sont conservés dans ~/.leafbridge/automations.json entre les redémarrages.
"""
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import json
import os
import threading

HOST = "127.0.0.1"
PORT = 8765
DATA = Path.home() / ".leafbridge"
INBOX = DATA / "shazam-inbox.jsonl"
LOG = DATA / "engine.log"
STATE_FILE = DATA / "automations.json"
LOCK = threading.RLock()

DEFAULT_AUTOMATIONS = {
    "shazam_spotify": {
        "enabled": False, "implemented": False, "label": "Shazam → Spotify"
    },
    "github_sync": {
        "enabled": False, "implemented": False, "label": "Synchronisation GitHub"
    },
    "music_sort": {
        "enabled": False, "implemented": False, "label": "Classement musical"
    },
    "routine": {
        "enabled": False, "implemented": False, "label": "Routine LeafBridge"
    },
    "shazam_inbox": {
        "enabled": True, "implemented": True, "label": "Réception Shazam locale"
    },
}


def now():
    return datetime.now(timezone.utc).isoformat()


def load_automations():
    """Recharge les états persistants sans faire confiance au fichier."""
    states = {
        key: dict(value) for key, value in DEFAULT_AUTOMATIONS.items()
    }
    if STATE_FILE.exists():
        try:
            saved = json.loads(STATE_FILE.read_text(encoding="utf-8"))
            if isinstance(saved, dict):
                for key, default in states.items():
                    item = saved.get(key)
                    if isinstance(item, dict) and default["implemented"]:
                        states[key]["enabled"] = bool(item.get("enabled", default["enabled"]))
        except (OSError, json.JSONDecodeError):
            pass
    return states


AUTOMATIONS = load_automations()


def save_automations():
    """Écriture atomique pour éviter un fichier d'état partiellement écrit."""
    DATA.mkdir(parents=True, exist_ok=True)
    temporary = STATE_FILE.with_suffix(".json.tmp")
    temporary.write_text(
        json.dumps(AUTOMATIONS, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    os.replace(temporary, STATE_FILE)


def log(event, details=None):
    DATA.mkdir(parents=True, exist_ok=True)
    entry = {"time": now(), "event": event, "details": details or {}}
    with LOCK:
        with LOG.open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(entry, ensure_ascii=False) + "\n")


class Handler(BaseHTTPRequestHandler):
    server_version = "LeafBridgeEngine/1.1"

    def reply(self, code, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def read_json(self):
        raw_length = self.headers.get("Content-Length", "0")
        try:
            length = int(raw_length)
        except (TypeError, ValueError):
            raise ValueError("Longueur de requête invalide")
        if length < 1 or length > 65536:
            raise ValueError("Taille JSON invalide")
        value = json.loads(self.rfile.read(length).decode("utf-8"))
        if not isinstance(value, dict):
            raise ValueError("Un objet JSON est attendu")
        return value

    def do_GET(self):
        if self.path in ("/status", "/automations"):
            with LOCK:
                snapshot = {key: dict(value) for key, value in AUTOMATIONS.items()}
            payload = {"ok": True, "automations": snapshot}
            if self.path == "/status":
                payload.update({
                    "message": "Moteur LeafBridge connecté",
                    "service": "LeafBridgeEngine",
                    "version": "1.1",
                    "host": HOST,
                    "port": PORT,
                })
            self.reply(200, payload)
            return

        if self.path == "/logs":
            DATA.mkdir(parents=True, exist_ok=True)
            with LOCK:
                lines = LOG.read_text(encoding="utf-8").splitlines()[-50:] if LOG.exists() else []
            entries = []
            for line in lines:
                try:
                    entries.append(json.loads(line))
                except json.JSONDecodeError:
                    continue
            self.reply(200, {"ok": True, "logs": entries})
            return

        self.reply(404, {"ok": False, "message": "Route inconnue"})

    def do_POST(self):
        try:
            data = self.read_json()
        except (ValueError, json.JSONDecodeError, UnicodeDecodeError) as exc:
            self.reply(400, {"ok": False, "message": f"JSON invalide : {exc}"})
            return

        if self.path == "/shazam":
            with LOCK:
                if not AUTOMATIONS["shazam_inbox"]["enabled"]:
                    self.reply(409, {"ok": False, "message": "Réception Shazam en pause"})
                    return
                DATA.mkdir(parents=True, exist_ok=True)
                record = {"received_at": now(), "payload": data}
                with INBOX.open("a", encoding="utf-8") as stream:
                    stream.write(json.dumps(record, ensure_ascii=False) + "\n")
            log("shazam_received", {
                "title": str(data.get("title", ""))[:500],
                "text": str(data.get("text", ""))[:2000],
            })
            self.reply(200, {"ok": True, "message": "Notification Shazam enregistrée localement"})
            return

        if self.path == "/automation":
            key = data.get("id")
            command = data.get("command")
            with LOCK:
                item = AUTOMATIONS.get(key) if isinstance(key, str) else None
                if item is None or command not in ("start", "stop"):
                    self.reply(400, {"ok": False, "message": "Automatisation ou commande inconnue"})
                    return
                if not item["implemented"]:
                    self.reply(501, {
                        "ok": False,
                        "message": f"{item['label']} : connecteur à configurer avant activation",
                        "automation": dict(item),
                    })
                    return
                item["enabled"] = command == "start"
                save_automations()
                result = dict(item)
            log("automation_" + command, {"id": key})
            state = "activée" if result["enabled"] else "arrêtée"
            self.reply(200, {
                "ok": True,
                "message": f"{result['label']} : {state}",
                "automation": result,
            })
            return

        self.reply(404, {"ok": False, "message": "Route inconnue"})

    def log_message(self, fmt, *args):
        # Les événements détaillés sont conservés dans engine.log.
        pass


if __name__ == "__main__":
    DATA.mkdir(parents=True, exist_ok=True)
    with LOCK:
        save_automations()
    log("engine_started", {"host": HOST, "port": PORT, "version": "1.1"})
    print(f"LeafBridge Engine actif sur http://{HOST}:{PORT}")
    print(f"Données locales : {DATA}")
    try:
        ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
    except KeyboardInterrupt:
        print("\nArrêt du moteur LeafBridge")
