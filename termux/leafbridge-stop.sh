#!/data/data/com.termux/files/usr/bin/bash
DATA="$HOME/.leafbridge"
PIDFILE="$DATA/server.pid"
if [ ! -f "$PIDFILE" ]; then
  echo "LeafBridge n'est pas déclaré démarré."
  exit 0
fi
PID="$(cat "$PIDFILE")"
if kill -0 "$PID" 2>/dev/null; then
  kill "$PID"
  echo "Arrêt demandé pour LeafBridge (PID $PID)."
else
  echo "Le processus n'existe plus."
fi
rm -f "$PIDFILE"
