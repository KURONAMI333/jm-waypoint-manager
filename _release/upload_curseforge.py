"""JM Waypoint Manager - CurseForge upload of v0.1.0 jars."""

from __future__ import annotations
import json
import os
from pathlib import Path

import requests

# Filled in by caller after CF project is created on the web.
CF_PROJECT_ID = int(os.environ.get("CF_PROJECT_ID", "0"))
VERSION = "0.1.0"

CF_MC_1_21_1 = 11779
CF_MC_1_20_1 = 9990
CF_LOADER_FORGE = 7498
CF_LOADER_FABRIC = 7499
CF_LOADER_NEOFORGE = 10150
CF_JAVA_17 = 8326
CF_JAVA_21 = 11135

REPO = Path(__file__).resolve().parent.parent
TOKEN_PATH = Path(str(Path.home() / "dev/projects/minecraft-mod-dev/kuronami-mods/tools/.tokens" / ".cf_token"))

# (filename, mc, loader, java)
JARS = [
    (
        "jmwaypointmanager-neoforge-1.21.1-0.1.0.jar",
        CF_MC_1_21_1,
        CF_LOADER_NEOFORGE,
        CF_JAVA_21,
    ),
]

CHANGELOG = """## v0.1.0 - Initial release

A modern UI on top of JourneyMap's waypoint list — search, filter, bulk
operations.

Press **Y** in-game to open. JourneyMap required.

See https://github.com/KURONAMI333/jm-waypoint-manager/releases/tag/v0.1.0
"""


def main() -> None:
    if CF_PROJECT_ID == 0:
        raise SystemExit("Set CF_PROJECT_ID env var (e.g. CF_PROJECT_ID=1234567)")
    token = TOKEN_PATH.read_text(encoding="utf-8").strip()
    headers = {"X-Api-Token": token}
    base = f"https://minecraft.curseforge.com/api/projects/{CF_PROJECT_ID}/upload-file"
    release_dir = REPO / "_release" / f"v{VERSION}"

    for jar_name, mc_id, loader_id, java_id in JARS:
        jar = release_dir / jar_name
        meta = {
            "changelog": CHANGELOG,
            "changelogType": "markdown",
            "displayName": jar.stem,
            "gameVersions": [mc_id, loader_id, java_id],
            "releaseType": "release",
        }
        print(f"[upload] {jar.name}")
        with jar.open("rb") as fh:
            files = {
                "metadata": (None, json.dumps(meta)),
                "file": (jar.name, fh, "application/java-archive"),
            }
            r = requests.post(base, headers=headers, files=files, timeout=120)
        print(f"  status: {r.status_code}")
        if r.status_code not in (200, 201):
            print(f"  body:   {r.text[:400]}")
            raise SystemExit(f"upload failed for {jar_name}")
        print(f"  id: {r.json().get('id')}")


if __name__ == "__main__":
    main()
