#!/usr/bin/env python3
"""Fail closed on unreviewed Open runtime licenses and write bundled attribution."""
# SPDX-License-Identifier: GPL-3.0-only
import argparse
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET
from urllib.request import urlopen
from urllib.error import HTTPError


def metadata(cache, coordinate, seen=None, fetch=False):
    seen = set() if seen is None else seen
    if coordinate in seen:
        raise ValueError("Cyclic Maven parent")
    seen.add(coordinate)
    group, name, version = coordinate.split(":")
    if not all(re.fullmatch(r"[A-Za-z0-9_.+-]+", part) for part in [group, name, version]):
        raise ValueError("Unsupported Maven coordinate")
    paths = list((cache / group / name / version).glob("*/*.pom"))
    if not paths:
        if not fetch:
            raise ValueError(f"Missing cached POM: {coordinate}; use --fetch-missing-poms")
        relative = group.replace(".", "/") + f"/{name}/{version}/{name}-{version}.pom"
        repositories = ["https://dl.google.com/dl/android/maven2/", "https://repo.maven.apache.org/maven2/"]
        if not group.startswith(("androidx.", "com.android.")):
            repositories.reverse()
        for repository in repositories:
            try:
                with urlopen(repository + relative, timeout=30) as response:
                    root = ET.fromstring(response.read())
                break
            except HTTPError as error:
                if error.code != 404:
                    raise
        else:
            raise ValueError(f"No POM at approved Maven repositories: {coordinate}")
    else:
        root = ET.parse(paths[0]).getroot()
    licenses = [{"name": p.findtext("{*}name"), "url": p.findtext("{*}url")}
                for p in root.findall("{*}licenses/{*}license")]
    source = root.findtext("{*}scm/{*}url") or root.findtext("{*}url")
    if not licenses:
        parent = root.find("{*}parent")
        if parent is None:
            raise ValueError(f"No license in POM or parent: {coordinate}")
        parent_coord = ":".join(parent.findtext("{*}" + field) or "" for field in ["groupId", "artifactId", "version"])
        inherited = metadata(cache, parent_coord, seen, fetch)
        licenses = inherited["licenses"]
        source = source or inherited["source"]
    return {"coordinate": coordinate, "licenses": licenses, "source": source}


def audit(coordinates, cache, fetch=False):
    rows = []
    approved = {"The Apache Software License, Version 2.0", "The Apache License, Version 2.0",
                "Apache License, Version 2.0", "Apache-2.0", "Apache 2.0", "The MIT License", "MIT License",
                "Bouncy Castle Licence", "BSD License"}
    for coordinate in coordinates:
        if coordinate.startswith(("com.google.mlkit:", "com.google.android.gms:", "com.google.firebase:", "junit:", "androidx.test.")):
            raise ValueError(f"Forbidden app runtime dependency: {coordinate}")
        row = metadata(cache, coordinate, fetch=fetch)
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
    p.add_argument("--fetch-missing-poms", action="store_true", help="Fetch missing license metadata only from Google Maven/Maven Central")
    args = p.parse_args()
    rows = audit(args.coordinates.read_text().splitlines(), args.cache, args.fetch_missing_poms)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text("WAYQO Open — resolved runtime dependency attribution\n\n" + "\n\n".join(
        row["coordinate"] + "\n" + "\n".join(f"{l['name']}: {l['url']}" for l in row["licenses"]) +
        f"\nSource: {row['source']}" for row in rows) + "\n")
    print(f"Reviewed {len(rows)} Open runtime coordinates; no unreviewed or proprietary scanner licenses")
