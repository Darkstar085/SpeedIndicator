#!/usr/bin/env python3

import json
import os
import sys
import urllib.request
from pathlib import Path


def main() -> None:
    path = Path(sys.argv[1])
    token = os.environ.get("TELEGRAM_BOT_TOKEN")
    chat_id = os.environ.get("RELEASE_CHAT_ID")

    if not token or not chat_id:
        raise SystemExit("Telegram release credentials are missing.")

    if not path.is_file() or path.stat().st_size == 0:
        raise SystemExit("Release APK is missing or empty.")

    boundary = os.urandom(16).hex()
    body = (
        f"--{boundary}\r\n"
        'Content-Disposition: form-data; name="chat_id"\r\n'
        "\r\n"
        f"{chat_id}\r\n"
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="document"; filename="{path.name}"\r\n'
        "Content-Type: application/vnd.android.package-archive\r\n"
        "\r\n"
    ).encode() + path.read_bytes() + f"\r\n--{boundary}--\r\n".encode()

    request = urllib.request.Request(
        f"https://api.telegram.org/bot{token}/sendDocument",
        data=body,
        headers={
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        },
    )

    with urllib.request.urlopen(request, timeout=900) as response:
        result = json.loads(response.read())

    if not result.get("ok"):
        raise SystemExit(json.dumps(result))

    print(f"Sent {path.name} ({path.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
