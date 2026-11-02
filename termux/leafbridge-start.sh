#!/data/data/com.termux/files/usr/bin/bash
set -e
BASE="$HOME/LeafBridge-upload"
ENGINE="$BASE/termux/leafbridge_server.py"
DATA="$HOME/.leafbridge"
PIDFILE="$DATA/server.pid"
LOGFILE="$DATA/server.out"
mkdir -p "$DATA"
if [ ! -f "$ENGINE" ]; then
  echo "Moteur introuvable : $ENGINE"
  exit 1
fi
if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
  echo "LeafBridge tourne déjà (PID $(cat "$PIDFILE"))."
  exit 0
fi
command -v python >/dev/null 2>&1 || { echo "Installe Python avec : pkg install python"; exit 1; }
nohup python "$ENGINE" >>"$LOGFILE" 2>&1 &
echo $! > "$PIDFILE"
sleep 1
if kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
  echo "LeafBridge démarré. Test : curl http://127.0.0.1:8765/status"
else
  echo "Échec du démarrage. Consulte $LOGFILE"
  rm -f "$PIDFILE"
  exit 1
fi
