#!/usr/bin/env python3
"""LeafBridge local engine for Android UI, silent Shazam capture and Spotify playlists."""
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from urllib.parse import urlencode, quote
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError
import hashlib
import json
import os
import re
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
SPOTIFY_CONFIG = DATA / "spotify-config.json"
SEEN_PATH = DATA / "shazam-seen.json"
INBOX = DATA / "shazam-inbox.jsonl"
ADDED_TRACKS = DATA / "spotify-added-tracks.json"


def read_config():
    try:
        return json.loads(SPOTIFY_CONFIG.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}


def write_config(config):
    temp = SPOTIFY_CONFIG.with_suffix(".tmp")
    temp.write_text(json.dumps(config, ensure_ascii=False), encoding="utf-8")
    try:
        os.chmod(temp, 0o600)
    except OSError:
        pass
    temp.replace(SPOTIFY_CONFIG)


def spotify_request(path, token, method="GET", payload=None):
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    req = Request("https://api.spotify.com/v1/" + path, data=data, method=method)
    req.add_header("Authorization", "Bearer " + token)
    if data is not None:
        req.add_header("Content-Type", "application/json")
    with urlopen(req, timeout=12) as response:
        raw = response.read()
        return json.loads(raw.decode("utf-8")) if raw else {}


def valid_config(config):
    return bool(config.get("client_id") and config.get("playlist_id") and
                config.get("access_token") and config.get("refresh_token"))


def get_access_token(config):
    if int(config.get("expires_at", 0)) > int(time.time()) + 90:
        return config["access_token"]
    body = urlencode({
        "client_id": config["client_id"],
        "grant_type": "refresh_token",
        "refresh_token": config["refresh_token"],
    }).encode("utf-8")
    req = Request("https://accounts.spotify.com/api/token", data=body, method="POST")
    req.add_header("Content-Type", "application/x-www-form-urlencoded")
    with urlopen(req, timeout=12) as response:
        refreshed = json.loads(response.read().decode("utf-8"))
    config["access_token"] = refreshed["access_token"]
    config["expires_at"] = int(time.time()) + int(refreshed.get("expires_in", 3600))
    if refreshed.get("refresh_token"):
        config["refresh_token"] = refreshed["refresh_token"]
    write_config(config)
    return config["access_token"]


def extract_track_query(entry):
    title = str(entry.get("title", "")).strip()
    text = str(entry.get("text", "")).strip()
    if title.lower() in {"shazam", "identification shazam", "titre identifié"}:
        candidate = text
    elif text and text.lower() not in {"shazam", "ouvrir", "open"}:
        candidate = title + " " + text
    else:
        candidate = title
    return re.sub(r"\s+", " ", candidate).strip(" -–—|·")[:300]


def add_shazam_track(entry):
    config = read_config()
    if not valid_config(config):
        return False, "Spotify non configuré : titre conservé dans la file locale"
    query = extract_track_query(entry)
    if len(query) < 3:
        return False, "Notification reçue mais titre/artiste non identifiables"
    token = get_access_token(config)
    result = spotify_request("search?" + urlencode({"q": query, "type": "track", "limit": 5}), token)
    tracks = result.get("tracks", {}).get("items", [])
    if not tracks:
        return False, "Aucun résultat Spotify pour : " + query
    chosen = tracks[0]
    uri = chosen.get("uri")
    if not uri:
        return False, "Résultat Spotify sans URI exploitable"
    try:
        added = set(json.loads(ADDED_TRACKS.read_text(encoding="utf-8"))) if ADDED_TRACKS.exists() else set()
    except (OSError, ValueError, TypeError):
        added = set()
    if uri in added:
        return True, "Déjà ajouté précédemment : " + chosen.get("name", query)
    playlist_id = str(config["playlist_id"]).strip()
    playlist_id = playlist_id.rsplit(":", 1)[-1].rsplit("/", 1)[-1]
    spotify_request("playlists/" + quote(playlist_id, safe="") + "/tracks", token,
                    method="POST", payload={"uris": [uri]})
    added.add(uri)
    ADDED_TRACKS.write_text(json.dumps(sorted(added)[-10000:], ensure_ascii=False), encoding="utf-8")
    return True, "Ajouté à Spotify : " + chosen.get("name", query)


def process_pending_entries():
    if not INBOX.exists() or not valid_config(read_config()):
        return 0
    updated, changed = [], 0
    for line in INBOX.read_text(encoding="utf-8").splitlines():
        try:
            entry = json.loads(line)
        except ValueError:
            continue
        if not entry.get("spotify_added"):
            try:
                ok, message = add_shazam_track(entry)
                entry["spotify_added"] = ok
                entry["result"] = message
                if ok:
                    changed += 1
            except (HTTPError, URLError, TimeoutError, KeyError, ValueError) as exc:
                entry["result"] = "En attente Spotify : " + str(exc)[:200]
        updated.append(json.dumps(entry, ensure_ascii=False))
    INBOX.write_text("\n".join(updated) + ("\n" if updated else ""), encoding="utf-8")
    return changed


class Handler(BaseHTTPRequestHandler):
    def _send(self, code, payload):
        raw = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(raw)

    def _read_payload(self):
        size = int(self.headers.get("Content-Length", "0"))
        if size <= 0 or size > 65536:
            raise ValueError("Taille de requête invalide")
        payload = json.loads(self.rfile.read(size))
        if not isinstance(payload, dict):
            raise ValueError("Le JSON doit être un objet")
        return payload

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
        config = read_config()
        return self._send(200, {
            "ok": True, "message": "Moteur LeafBridge actif",
            "spotify_configured": valid_config(config),
            "automations": {key: running.get(key, STATES[key]) for key in sorted(ALLOWED)},
        })

    def do_POST(self):
        try:
            payload = self._read_payload()
        except Exception as exc:
            return self._send(400, {"ok": False, "message": "JSON invalide : " + str(exc)})

        if self.path == "/spotify/config":
            required = ("client_id", "playlist_id", "access_token", "refresh_token")
            if any(not str(payload.get(key, "")).strip() for key in required):
                return self._send(400, {"ok": False, "message": "Configuration Spotify incomplète"})
            safe = {key: str(payload[key]) for key in required}
            safe["expires_at"] = int(payload.get("expires_at", time.time() + 3600))
            write_config(safe)
            processed = process_pending_entries()
            return self._send(200, {"ok": True, "message": "Spotify configuré ; " + str(processed) + " titre(s) en attente ajouté(s) à la playlist"})

        if self.path == "/shazam":
            notification_key = str(payload.get("notification_key", ""))[:512]
            title = str(payload.get("title", ""))[:500]
            text = str(payload.get("text", ""))[:2000]
            fingerprint = hashlib.sha256((notification_key + "\n" + title + "\n" + text).encode("utf-8")).hexdigest()
            try:
                seen = set(json.loads(SEEN_PATH.read_text(encoding="utf-8"))) if SEEN_PATH.exists() else set()
            except (OSError, ValueError, TypeError):
                seen = set()
            if fingerprint in seen:
                return self._send(200, {"ok": True, "message": "Notification déjà traitée"})
            entry = {
                "received_at": int(time.time()),
                "package": str(payload.get("package", "com.shazam.android")),
                "title": title,
                "text": text,
                "post_time": payload.get("post_time"),
                "notification_key": notification_key or None,
            }
            success, message = False, "Titre conservé dans la file locale"
            try:
                success, message = add_shazam_track(entry)
            except (HTTPError, URLError, TimeoutError, KeyError, ValueError) as exc:
                message = "Échec Spotify, titre conservé localement : " + str(exc)[:250]
            entry["spotify_added"] = success
            entry["result"] = message
            with INBOX.open("a", encoding="utf-8") as file:
                file.write(json.dumps(entry, ensure_ascii=False) + "\n")
            seen.add(fingerprint)
            SEEN_PATH.write_text(json.dumps(sorted(seen)[-5000:], ensure_ascii=False), encoding="utf-8")
            return self._send(200, {"ok": True, "spotify_added": success, "message": message})

        if self.path != "/automation":
            return self._send(404, {"ok": False, "message": "Route inconnue"})
        key, command = str(payload.get("id", "")), str(payload.get("command", ""))
        if key not in ALLOWED or command not in {"start", "stop"}:
            return self._send(400, {"ok": False, "message": "Automatisation ou commande non autorisée"})
        if key == "shazam_spotify":
            STATES[key] = "running" if command == "start" else "stopped"
            return self._send(200, {"ok": True, "message": "Automatisation Shazam → Spotify " + ("activée" if command == "start" else "mise en pause")})
        if command == "stop":
            proc = PROCESSES.get(key)
            if proc and proc.poll() is None:
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
        if not script.is_file() or not script.resolve().is_relative_to(AUTOMATIONS.resolve()):
            return self._send(409, {"ok": False, "message": "Module non configuré : ajoute " + str(script)})
        existing = PROCESSES.get(key)
        if existing and existing.poll() is None:
            return self._send(200, {"ok": True, "message": key + " fonctionne déjà"})
        with (DATA / (key + ".log")).open("a", encoding="utf-8") as log_file:
            proc = subprocess.Popen(
                ["/data/data/com.termux/files/usr/bin/bash", str(script)],
                cwd=str(ROOT), stdin=subprocess.DEVNULL, stdout=log_file,
                stderr=subprocess.STDOUT, start_new_session=True,
            )
        PROCESSES[key] = proc
        STATES[key] = "running"
        return self._send(200, {"ok": True, "message": key + " démarrée"})

    def log_message(self, fmt, *args):
        print("[LeafBridge]", fmt % args)


if __name__ == "__main__":
    print("LeafBridge engine: http://127.0.0.1:8765")
    HTTPServer(("127.0.0.1", 8765), Handler).serve_forever()
