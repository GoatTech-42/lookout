#!/usr/bin/env python3
"""Lookout rig proof harness - scores runtime evidence per matrix row.

Source/docs only: this script never launches or talks to a rig. Live mode
takes operator-supplied files; --dry-run scores the committed fixtures.

Usage:
  python3 rig/proof/proof.py --dry-run
  python3 rig/proof/proof.py --client-log run.log --config lookout.json \
      [--server-log server.log] [--arm-counts counts.txt] [--row boot]

Exit 0 only when every scored row is PASS. UNVERIFIED and FAIL are
nonzero, never pass-by-absence.
"""
import argparse, pathlib, sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[2]))
from rig.proof import parsers, score

FIXTURES = pathlib.Path(__file__).resolve().parent / "fixtures"

def read(path):
    """Missing or undecodable evidence is absent, not a crash: the row
    scores UNVERIFIED per the documented contract."""
    if not path:
        return None
    try:
        return pathlib.Path(path).read_text()
    except (OSError, UnicodeDecodeError) as e:
        print(f"note: {path}: unreadable ({e.__class__.__name__}), rows needing it score UNVERIFIED",
              file=sys.stderr)
        return None

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--client-log")
    ap.add_argument("--config")
    ap.add_argument("--server-log")
    ap.add_argument("--arm-counts")
    ap.add_argument("--row", choices=sorted(score.ROWS), action="append")
    ap.add_argument("--dry-run", action="store_true",
                    help="score the committed fixtures instead of operator files")
    args = ap.parse_args()

    if args.dry_run:
        client_text = (FIXTURES / "client-good.log").read_text()
        config_text = (FIXTURES / "lookout-good.json").read_text()
        server_text = (FIXTURES / "server-good.log").read_text()
        counts_text = (FIXTURES / "arm-counts-good.txt").read_text()
    else:
        client_text = read(args.client_log)
        config_text = read(args.config)
        server_text = read(args.server_log)
        counts_text = read(args.arm_counts)
        if not any([args.client_log, args.config, args.server_log, args.arm_counts]):
            ap.error("no inputs; pass evidence files or --dry-run")

    client = parsers.parse_client_log(client_text) if client_text is not None else None
    config = parsers.parse_config(config_text) if config_text is not None else None
    server = parsers.parse_server_log(server_text) if server_text is not None else None
    counts = parsers.parse_arm_counts(counts_text) if counts_text is not None else None
    inputs = {"boot": client, "g2-optin": config, "target-delivery": client,
              "chat-capture": client, "null-gui": client,
              "g3-receipt": server, "g4-coverage": counts}

    rows = args.row or sorted(score.ROWS)
    failed = []
    for name in rows:
        label, fn = score.ROWS[name]
        verdict, detail = fn(inputs[name])
        print(f"{verdict} {name} ({label}): {detail}")
        if verdict != "PASS":
            failed.append(name)
    print("SUITE:", "all pass" if not failed else f"not proven {failed}")
    return 0 if not failed else 1

if __name__ == "__main__":
    sys.exit(main())
