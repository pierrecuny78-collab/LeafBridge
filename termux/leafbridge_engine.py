#!/usr/bin/env python3
"""LeafBridge local engine for Android UI <-> Termux.

Binds only to 127.0.0.1. Automation scripts are allow-listed and must exist
under ~/LeafBridge/automations. No shell command is accepted from HTTP input.
"""
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
import json
import subprocess
import time

ROOT = Path.home() / "LeafBridge"
AUTOMATIONS = ROOT / "automations"
DATA = ROOT / "data"
DATA.mkdir(parents=True, exist_ok=True)
AUTOMATIONS.mkdir(parents=True, exist_ok=True)

ALLOWED = {"shazam_spotify", "github_sync", "music_sort", "routine"}
PROCESSES = {}
STATES = {key: "stopped" for key in ALLOWED}


class Handler(BaseHTTPRequestHandler):
    def _send(self, code, payload):
        raw = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(raw)

    def do_GET(self):
        if self.path != "/status":
            return self._send(404, {"ok": False, "message": "Route inconnue"})
        running = {}
        for key, proc in list(PROCESSES.items()):
            if proc.poll() is None:
                running[key] = "running"
            else:
                running[key] = "stopped"
                STATES[key] = "stopped"
        return self._send(200, {
            "ok": True,
            "message": "Moteur LeafBridge actif",
            "automations": {key: running.get(key, STATES[key]) for key in sorted(ALLOWED)}
        })

    def do_POST(self):
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if size < 0 or size > 65536:
                return self._send(413, {"ok": False, "message": "Requête trop volumineuse"})
            if size == 0:
                return self._send(400, {"ok": False, "message": "Corps JSON manquant"})
            payload = json.loads(self.rfile.read(size))
            if not isinstance(payload, dict):
                return self._send(400, {"ok": False, "message": "Le JSON doit être un objet"})
        except Exception:
            return self._send(400, {"ok": False, "message": "JSON invalide"})

        if self.path == "/shazam":
            entry = {
                "received_at": int(time.time()),
                "package": payload.get("package", payload.get("source", "unknown")),
                "title": str(payload.get("title", ""))[:500],
                "text": str(payload.get("text", ""))[:2000],
                "post_time": payload.get("post_time"),
            }
            with (DATA / "shazam-inbox.jsonl").open("a", encoding="utf-8") as file:
                file.write(json.dumps(entry, ensure_ascii=False) + "\n")
            return self._send(200, {"ok": True, "message": "Notification Shazam enregistrée"})

        if self.path != "/automation":
            return self._send(404, {"ok": False, "message": "Route inconnue"})

        key = str(payload.get("id", ""))
        command = str(payload.get("command", ""))
        if key not in ALLOWED or command not in {"start", "stop"}:
            return self._send(400, {"ok": False, "message": "Automatisation ou commande non autorisée"})

        if command == "stop":
            proc = PROCESSES.get(key)
            if proc and proc.poll() is None:
                import os
                import signal
                try:
                    os.killpg(proc.pid, signal.SIGTERM)
                except ProcessLookupError:
                    pass
                try:
                    proc.wait(timeout=3)
                except subprocess.TimeoutExpired:
                    try:
                        os.killpg(proc.pid, signal.SIGKILL)
                    except ProcessLookupError:
                        pass
                    proc.wait(timeout=2)
            PROCESSES.pop(key, None)
            STATES[key] = "stopped"
            return self._send(200, {"ok": True, "message": key + " arrêtée"})

        script = AUTOMATIONS / (key + ".sh")
        if not script.is_file():
            return self._send(409, {
                "ok": False,
                "message": "Module non configuré : ajoute " + str(script)
            })
        if not script.resolve().is_relative_to(AUTOMATIONS.resolve()):
            return self._send(400, {"ok": False, "message": "Chemin de script refusé"})
        existing = PROCESSES.get(key)
        if existing and existing.poll() is None:
            return self._send(200, {"ok": True, "message": key + " fonctionne déjà"})
        log_path = DATA / (key + ".log")
        with log_path.open("a", encoding="utf-8") as log_file:
            proc = subprocess.Popen(
                ["/data/data/com.termux/files/usr/bin/bash", str(script)],
                cwd=str(ROOT),
                stdin=subprocess.DEVNULL,
                stdout=log_file,
                stderr=subprocess.STDOUT,
                start_new_session=True,
            )
        PROCESSES[key] = proc
        STATES[key] = "running"
        return self._send(200, {"ok": True, "message": key + " démarrée"})

    def log_message(self, fmt, *args):
        print("[LeafBridge]", fmt % args)


if __name__ == "__main__":
    print("LeafBridge engine: http://127.0.0.1:8765")
    HTTPServer(("127.0.0.1", 8765), Handler).serve_forever()
