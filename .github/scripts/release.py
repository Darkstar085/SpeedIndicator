#!/usr/bin/env python3

import os
import re
import shutil
import subprocess
import sys
from pathlib import Path
from urllib import request


def run(command, *, env=None):
    print("$ " + " ".join(command), flush=True)
    subprocess.run(command, check=True, env=env)


def output(command, *, env=None):
    return subprocess.check_output(command, text=True, env=env).strip()


def version_info():
    text = Path("app/build.gradle.kts").read_text(encoding="utf-8")
    match = re.search(r'versionName = "([^"]+)"', text)
    if not match:
        raise SystemExit("Could not determine versionName.")
    version = match.group(1)
    return version, f"v{version}", f"SpeedIndicator_v{version}.apk"


def apk_path():
    _, _, name = version_info()
    return Path(os.environ.get("GITHUB_WORKSPACE", ".")) / name


def validate(requested):
    version, _, _ = version_info()
    if not requested:
        raise SystemExit("Release version is required.")
    if version != requested:
        raise SystemExit(
            f"Requested version {requested} does not match app version {version}."
        )
    print(f"Release version validated: {version}")


def build():
    workspace = Path(os.environ.get("GITHUB_WORKSPACE", "."))
    target = apk_path()
    run(["chmod", "+x", "./gradlew"])
    run(["./gradlew", "assembleRelease", "--stacktrace"])

    apk_dir = Path("app/build/outputs/apk/release")
    candidates = sorted(apk_dir.glob("*.apk"))
    if not candidates:
        raise SystemExit("No release APK was produced.")

    shutil.move(str(candidates[0]), target)
    print(f"Renamed release APK: {target}")
    print(f"Release APK size: {target.stat().st_size} bytes")


def verify():
    target = apk_path()
    if not target.is_file():
        raise SystemExit(f"Release APK is missing: {target}")

    sdk_root = os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME")
    if not sdk_root or not (Path(sdk_root) / "build-tools").is_dir():
        raise SystemExit("Android SDK build-tools directory was not found.")

    signers = sorted(
        (Path(sdk_root) / "build-tools").glob("*/apksigner"),
        key=lambda path: tuple(int(part) if part.isdigit() else part for part in re.split(r"([0-9]+)", str(path))),
    )
    if not signers or not signers[-1].is_file():
        raise SystemExit("Android Build Tools apksigner was not found.")

    print(f"Using apksigner: {signers[-1]}")
    run([str(signers[-1]), "verify", "--verbose", str(target)])


def previous_tag(tag):
    repo = os.environ["GITHUB_REPOSITORY"]
    return output(
        [
            "gh",
            "release",
            "list",
            "--repo",
            repo,
            "--limit",
            "100",
            "--json",
            "tagName",
            "--jq",
            ".[].tagName",
        ]
    ).splitlines()


def notes():
    version, tag, _ = version_info()
    tags = [item for item in previous_tag(tag) if item != tag]
    previous = tags[0] if tags else ""
    release_notes = ["## What's new", ""]

    range_value = f"{previous}..HEAD" if previous else "HEAD"
    commits = output(
        ["git", "log", range_value, "--no-merges", "--pretty=format:%s"]
    ).splitlines()

    seen = set()
    for commit in commits:
        if ":" not in commit:
            continue
        text = commit.split(":", 1)[1].strip()
        if not text or text in seen:
            continue
        seen.add(text)
        release_notes.append(f"- {text[:1].upper()}{text[1:]}")

    if previous:
        release_notes.extend(
            [
                "",
                f"**Full Changelog:** https://github.com/{os.environ['GITHUB_REPOSITORY']}/compare/{previous}...{tag}",
            ]
        )

    Path("RELEASE_NOTES.md").write_text(
        "\n".join(release_notes) + "\n", encoding="utf-8"
    )
    print("Generated RELEASE_NOTES.md")
    print(Path("RELEASE_NOTES.md").read_text(encoding="utf-8"))


def release():
    _, tag, _ = version_info()
    target = apk_path()
    if not target.is_file():
        raise SystemExit(f"Release APK is missing: {target}")

    repo = os.environ["GITHUB_REPOSITORY"]
    result = subprocess.run(
        ["gh", "release", "view", tag, "--repo", repo],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    if result.returncode == 0:
        print(f"Release {tag} already exists. Skipping release creation.")
        return

    run(
        [
            "gh",
            "release",
            "create",
            tag,
            str(target),
            "--repo",
            repo,
            "--title",
            f"Speed Indicator v{version_info()[0]}",
            "--notes-file",
            "RELEASE_NOTES.md",
            "--latest",
        ]
    )
    print(f"GitHub release {tag} created.")


def multipart(fields, file_field, filename, content, content_type):
    boundary = b"----------------speedindicatorrelease"
    chunks = []
    for key, value in fields.items():
        chunks.extend(
            [
                b"--" + boundary + b"\r\n",
                f'Content-Disposition: form-data; name="{key}"\r\n\r\n'.encode(),
                str(value).encode(),
                b"\r\n",
            ]
        )
    chunks.extend(
        [
            b"--" + boundary + b"\r\n",
            (
                f'Content-Disposition: form-data; name="{file_field}"; '
                f'filename="{filename}"\r\n'
            ).encode(),
            f"Content-Type: {content_type}\r\n\r\n".encode(),
            content,
            b"\r\n--" + boundary + b"--\r\n",
        ]
    )
    return b"".join(chunks), f"multipart/form-data; boundary={boundary.decode()}"


def telegram():
    version, tag, _ = version_info()
    token = os.environ.get("TELEGRAM_BOT_TOKEN")
    chat_id = os.environ.get("RELEASE_CHAT_ID")
    if not token:
        raise SystemExit("TELEGRAM_BOT_TOKEN is missing.")
    if not chat_id:
        raise SystemExit("RELEASE_CHAT_ID is missing.")

    target = apk_path()
    if not target.is_file() or not os.access(target, os.R_OK):
        raise SystemExit(f"Release APK is unavailable or unreadable: {target}")

    tags = [item for item in previous_tag(tag) if item != tag]
    previous = tags[0] if tags else ""
    version_line = (
        f"🚀 Version: {previous[1:]} → {version}" if previous
        else f"🚀 Version: {version}"
    )
    release_url = f"https://github.com/{os.environ['GITHUB_REPOSITORY']}/releases/tag/{tag}"
    caption = (
        "📡 Real-time internet speed monitoring for Android\n"
        f"{version_line}\n\n"
        f'Changelog: <a href="{release_url}">Open</a>'
    )

    body, content_type = multipart(
        {
            "chat_id": chat_id,
            "caption": caption,
            "parse_mode": "HTML",
        },
        "document",
        target.name,
        target.read_bytes(),
        "application/vnd.android.package-archive",
    )

    print(f"Sending release APK through Telegram Bot API: {target}")
    print(f"Release APK size: {target.stat().st_size} bytes")
    req = request.Request(
        f"https://api.telegram.org/bot{token}/sendDocument",
        data=body,
        headers={"Content-Type": content_type},
        method="POST",
    )
    with request.urlopen(req, timeout=900) as response:
        result = response.read().decode("utf-8")
    if '"ok":true' not in result.replace(" ", "").lower():
        raise RuntimeError(f"Telegram API upload failed: {result}")
    print("Release APK sent successfully.")


def main():
    stage = sys.argv[1] if len(sys.argv) > 1 else ""
    if stage == "validate":
        validate(sys.argv[2] if len(sys.argv) > 2 else "")
    elif stage == "build":
        build()
    elif stage == "verify":
        verify()
    elif stage == "notes":
        notes()
    elif stage == "release":
        release()
    elif stage == "telegram":
        telegram()
    else:
        raise SystemExit(
            "Usage: release.py {validate|build|verify|notes|release|telegram} [version]"
        )


if __name__ == "__main__":
    main()
