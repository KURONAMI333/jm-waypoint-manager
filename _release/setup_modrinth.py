"""JM Waypoint Manager - first-release Modrinth setup.

One-shot: creates project metadata via PATCH, uploads the icon, then
uploads v0.1.0 jars and submits for review.
"""

from __future__ import annotations
import json
from pathlib import Path

import requests

API = "https://api.modrinth.com/v2"
SLUG = "jm-waypoint-manager"
VERSION = "0.1.0"

REPO = Path(__file__).resolve().parent.parent
TOKEN_PATH = Path(
    r"C:\Users\naoki\claude-memory\kuronami-mods\tools\.tokens\.modrinth_token"
)
ICON_PATH = REPO / "logo.png"

# v0.1.0 ships NeoForge 1.21.1 only — JM Fabric for 1.21.1 doesn't exist,
# and the Forge 1.20.1 port needs the 5.x API (different package layout)
# which we haven't built yet. Add additional jars here for subsequent
# releases.
JARS = [
    ("jmwaypointmanager-neoforge-1.21.1-0.1.0.jar", "1.21.1", "neoforge"),
]

BODY = """\
# JM Waypoint Manager

A modern UI on top of JourneyMap's waypoint list — search, filter, bulk operations.

## What it does

JourneyMap is great, but its built-in waypoint list is minimal — no search,
limited sorting, no bulk operations. JM Waypoint Manager adds a screen
that lets you actually **manage** a large waypoint collection.

## Features

- **Search** by name (live filter)
- **Sort** by name / distance from player / dimension
- **Filter** by enabled/disabled, dimension
- **Bulk-select** multiple waypoints (checkboxes)
- **Bulk delete** / bulk toggle enabled
- **Click the color dot** to cycle through an 8-color palette
- **Click the row** to toggle enabled
- Works with waypoints from any JM-aware mod (Compass to Map, Ping to Map, manual JM, etc.)

## How to open

Default keybind: **Y** (rebindable in Controls). Avoids `J` / `M` / `B`
(used by JourneyMap itself) and `L` (vanilla advancement screen).

## Dependencies

- **[JourneyMap](https://modrinth.com/mod/journeymap)** (required)

## How it differs from JM's built-in waypoint list

| Feature | JM built-in | JM Waypoint Manager |
|---|:---:|:---:|
| Search by name | ✗ | ✅ |
| Sort by distance | ✗ | ✅ |
| Sort by dimension | ✗ | ✅ |
| Multi-select | ✗ | ✅ |
| Bulk delete | ✗ | ✅ |
| Bulk toggle enabled | ✗ | ✅ |
| Quick color change | ✗ | ✅ |
| Filter by enabled state | ✗ | ✅ |

## Source

[GitHub](https://github.com/KURONAMI333/jm-waypoint-manager)
"""

PROJECT_PATCH = {
    "body": BODY,
    "license_id": "MIT",
    "source_url": "https://github.com/KURONAMI333/jm-waypoint-manager",
    "issues_url": "https://github.com/KURONAMI333/jm-waypoint-manager/issues",
    "categories": ["utility"],
    "client_side": "required",
    "server_side": "unsupported",
    "summary": "A modern UI on top of JourneyMap's waypoint list — search, filter, bulk operations.",
}


def headers(token: str) -> dict:
    return {"Authorization": token, "User-Agent": "kuronami-jmm-setup/1.0"}


def main() -> None:
    token = TOKEN_PATH.read_text(encoding="utf-8").strip()

    print(f"[patch] {API}/project/{SLUG}")
    r = requests.patch(
        f"{API}/project/{SLUG}",
        headers=headers(token),
        json=PROJECT_PATCH,
        timeout=30,
    )
    print(f"  status: {r.status_code}")
    if r.status_code not in (200, 204):
        print(f"  body:   {r.text[:400]}")
        raise SystemExit("project PATCH failed")

    print(f"[icon] {ICON_PATH.name}")
    with ICON_PATH.open("rb") as fh:
        r = requests.patch(
            f"{API}/project/{SLUG}/icon?ext=png",
            headers={**headers(token), "Content-Type": "image/png"},
            data=fh.read(),
            timeout=30,
        )
    print(f"  status: {r.status_code}")
    if r.status_code not in (200, 204):
        print(f"  body:   {r.text[:400]}")
        raise SystemExit("icon upload failed")

    r = requests.get(f"{API}/project/{SLUG}", headers=headers(token), timeout=20)
    r.raise_for_status()
    project_id = r.json()["id"]
    print(f"[project] id={project_id}")

    release_dir = REPO / "_release" / f"v{VERSION}"
    for jar_name, mc, loader in JARS:
        jar = release_dir / jar_name
        vstr = f"{VERSION}+{loader}-{mc}"
        meta = {
            "name": vstr,
            "version_number": vstr,
            "changelog": "Initial release. See https://github.com/KURONAMI333/jm-waypoint-manager/releases/tag/v0.1.0",
            "dependencies": [
                # Required: JourneyMap
                {"project_id": "lfHFW1mp", "dependency_type": "required"}
            ],
            "game_versions": [mc],
            "version_type": "release",
            "loaders": [loader],
            "featured": True,
            "project_id": project_id,
            "file_parts": ["file"],
            "primary_file": "file",
            "status": "listed",
        }
        print(f"[upload] {vstr}")
        with jar.open("rb") as fh:
            files = {
                "data": (None, json.dumps(meta), "application/json"),
                "file": (jar.name, fh, "application/java-archive"),
            }
            r = requests.post(
                f"{API}/version",
                headers=headers(token),
                files=files,
                timeout=120,
            )
        print(f"  status: {r.status_code}")
        if r.status_code not in (200, 201):
            print(f"  body:   {r.text[:400]}")
            raise SystemExit(f"version upload failed for {jar_name}")

    # Submit for review
    print("[submit] PATCH status=processing")
    r = requests.patch(
        f"{API}/project/{SLUG}",
        headers={**headers(token), "Content-Type": "application/json"},
        json={"status": "processing"},
        timeout=30,
    )
    print(f"  status: {r.status_code}")


if __name__ == "__main__":
    main()
