#!/usr/bin/env python3

import os
import shutil
import subprocess
import sys
from pathlib import Path


def run(command):
    print("$ " + " ".join(command), flush=True)
    subprocess.run(command, check=True)


def prepare_keystore():
    android_dir = Path.home() / ".android"
    android_dir.mkdir(parents=True, exist_ok=True)
    keystore = android_dir / "debug.keystore"

    if not keystore.is_file():
        run(
            [
                "keytool",
                "-genkeypair",
                "-v",
                "-keystore",
                str(keystore),
                "-storepass",
                "android",
                "-keypass",
                "android",
                "-alias",
                "androiddebugkey",
                "-keyalg",
                "RSA",
                "-keysize",
                "2048",
                "-validity",
                "10000",
                "-dname",
                "CN=Android Debug,O=Android,C=US",
            ]
        )

    print(f"Debug keystore ready: {keystore}")


def build():
    run(["chmod", "+x", "./gradlew"])
    run(["./gradlew", "assembleDebug", "--stacktrace"])


def package_apk():
    source = Path("app/build/outputs/apk/debug/app-debug.apk")
    if not source.is_file():
        raise SystemExit(f"Debug APK is missing: {source}")

    short_sha = os.environ.get("GITHUB_SHA", "local")[:7]
    target = Path(f"SpeedIndicator-debug-{short_sha}.apk")
    shutil.copy2(source, target)

    size = target.stat().st_size
    print(f"APK: {target}")
    print(f"APK size: {size} bytes")


def send():
    short_sha = os.environ.get("GITHUB_SHA", "local")[:7]
    target = Path(f"SpeedIndicator-debug-{short_sha}.apk")
    if not target.is_file():
        raise SystemExit(f"Packaged debug APK is missing: {target}")

    subprocess.run(
        [sys.executable, ".github/scripts/telegram.py", str(target)],
        check=True,
        env=os.environ.copy(),
    )


def main():
    stage = sys.argv[1] if len(sys.argv) > 1 else ""
    if stage == "prepare":
        prepare_keystore()
    elif stage == "build":
        build()
    elif stage == "package":
        package_apk()
    elif stage == "telegram":
        send()
    else:
        raise SystemExit(
            "Usage: debug.py {prepare|build|package|telegram}"
        )


if __name__ == "__main__":
    main()
