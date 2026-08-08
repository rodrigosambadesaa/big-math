#!/usr/bin/env bash
set -euo pipefail

export DISPLAY="${DISPLAY:-:99}"
export XVFB_WHD="${XVFB_WHD:-1440x900x24}"

Xvfb "$DISPLAY" -screen 0 "$XVFB_WHD" >/tmp/xvfb.log 2>&1 &
XVFB_PID=$!

fluxbox >/tmp/fluxbox.log 2>&1 &
FLUXBOX_PID=$!

x11vnc -display "$DISPLAY" -forever -shared -rfbport 5900 -nopw >/tmp/x11vnc.log 2>&1 &
X11VNC_PID=$!

/usr/share/novnc/utils/novnc_proxy --vnc localhost:5900 --listen 8080 >/tmp/novnc.log 2>&1 &
NOVNC_PID=$!

cleanup() {
  kill "$NOVNC_PID" "$X11VNC_PID" "$FLUXBOX_PID" "$XVFB_PID" 2>/dev/null || true
}

trap cleanup EXIT

java -cp "/opt/big-math/classes" ch.obermuhlner.math.big.example.BigMathCalculatorApp
