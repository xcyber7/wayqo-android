#!/usr/bin/env python3
"""Fail closed on unreviewed Open runtime licenses and write bundled attribution."""
# SPDX-License-Identifier: GPL-3.0-only
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET


def metadata(cache, coordinate, seen=None):
    seen = set() if seen is None else seen
    if coordinate in seen:
        raise ValueError("Cyclic Maven parent")
    seen.add(coordinate)
    group, name, version = coordinate.split(":")
    paths = list((cache / group / name / version).glob("*/*.pom"))
    if not paths:
        raise ValueError(f"Missing cached POM: {coordinate}")
    root = ET.parse(paths[0]).getroot()
    licenses = [{"name": p.findtext("{*}name"), "url": p.findtext("{*}url")}
                for p in root.findall("{*}licenses/{*}license")]
    source = root.findtext("{*}scm/{*}url") or root.findtext("{*}url")
    if not licenses:
        parent = root.find("{*}parent")
        if parent is None:
            raise ValueError(f"No license in POM or parent: {coordinate}")
        parent_coord = ":".join(parent.findtext("{*}" + field) or "" for field in ["groupId", "artifactId", "version"])
        inherited = metadata(cache, parent_coord, seen)
        licenses = inherited["licenses"]
        source = source or inherited["source"]
    return {"coordinate": coordinate, "licenses": licenses, "source": source}


def audit(coordinates, cache):
    rows = []
    approved = {"The Apache Software License, Version 2.0", "The Apache License, Version 2.0",
                "Apache License, Version 2.0", "Apache-2.0", "Apache 2.0", "The MIT License", "MIT License",
                "Bouncy Castle Licence", "BSD License"}
    for coordinate in coordinates:
        if coordinate.startswith(("com.google.mlkit:", "com.google.android.gms:", "com.google.firebase:", "junit:", "androidx.test.")):
            raise ValueError(f"Forbidden app runtime dependency: {coordinate}")
        row = metadata(cache, coordinate)
        for license in row["licenses"]:
            if license["name"] not in approved:
                raise ValueError(f"Unreviewed runtime license: {coordinate}: {license['name']}")
        rows.append(row)
    return rows


if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--coordinates", type=Path, required=True)
    p.add_argument("--cache", type=Path, default=Path.home() / ".gradle/caches/modules-2/files-2.1")
    p.add_argument("--output", type=Path, required=True)
    args = p.parse_args()
    rows = audit(args.coordinates.read_text().splitlines(), args.cache)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text("WAYQO Open — resolved runtime dependency attribution\n\n" + "\n\n".join(
        row["coordinate"] + "\n" + "\n".join(f"{l['name']}: {l['url']}" for l in row["licenses"]) +
        f"\nSource: {row['source']}" for row in rows) + "\n")
    print(f"Reviewed {len(rows)} Open runtime coordinates; no unreviewed or proprietary scanner licenses")
