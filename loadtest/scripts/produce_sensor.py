import json
import os
import sys
import time
import uuid


def main() -> None:
	sessions = int(os.getenv("SESSIONS", "50"))
	interval_ms = int(os.getenv("INTERVAL_MS", "500"))

	session_ids = [f"session-{i}" for i in range(1, sessions + 1)]

	print(
		f"Generating sensor events: sessions={sessions} interval_ms={interval_ms} (target ~= {sessions * (1000.0 / interval_ms):.1f} msg/s)",
		file=sys.stderr,
		flush=True,
	)

	base = {
		"x": -5.405197,
		"y": -1.435728,
		"z": -0.073432,
		"steer": 0.0,
		"wheel_degree": 0,
		"handle_angle": 0,
		"speed": -0.0,
		"front_dist": 6.841736959814851,
		"left_dist": 6.8549553028926455,
		"right_dist": 6.819816073111288,
		"rear_dist": 6.269557738692705,
	}

	interval_s = interval_ms / 1000.0
	while True:
		cycle_start = time.time()
		for sid in session_ids:
			sent_at_ms = int(time.time() * 1000)
			payload = {
				"time": sent_at_ms / 1000.0,
				**base,
				"event_id": str(uuid.uuid4()),
				"sent_at_epoch_ms": sent_at_ms,
			}
			line = f"{sid}:{json.dumps(payload, separators=(\",\", \":\"))}"
			print(line, flush=True)

		elapsed = time.time() - cycle_start
		sleep_for = interval_s - elapsed
		if sleep_for > 0:
			time.sleep(sleep_for)


if __name__ == "__main__":
	main()
