#!/usr/bin/env python3

import asyncio
import hashlib
import os
import sys
import time
from pathlib import Path

from telethon import TelegramClient, custom, helpers, utils
from telethon.tl import functions, types


UPLOAD_TIMEOUT = 600
MAX_ATTEMPTS = 2
UPLOAD_CONCURRENCY = 4


class FastTelegramClient(TelegramClient):
    async def upload_file(
        self,
        file,
        *,
        part_size_kb=None,
        file_size=None,
        file_name=None,
        use_cache=None,
        key=None,
        iv=None,
        progress_callback=None,
    ):
        if isinstance(file, (types.InputFile, types.InputFileBig)):
            return file

        async with helpers._FileStream(file, file_size=file_size) as stream:
            file_size = stream.file_size
            if not part_size_kb:
                part_size_kb = utils.get_appropriated_part_size(file_size)

            if part_size_kb > 512:
                raise ValueError("The part size must be less or equal to 512KB")

            part_size = int(part_size_kb * 1024)
            if part_size % 1024 != 0:
                raise ValueError(
                    "The part size must be evenly divisible by 1024"
                )

            file_id = helpers.generate_random_long()
            if not file_name:
                file_name = stream.name or str(file_id)

            if not os.path.splitext(file_name)[-1]:
                file_name += utils._get_extension(stream)

            is_big = file_size > 10 * 1024 * 1024
            part_count = (file_size + part_size - 1) // part_size
            hash_md5 = hashlib.md5()
            parts = []

            for part_index in range(part_count):
                part = await helpers._maybe_await(stream.read(part_size))
                if not isinstance(part, bytes):
                    raise TypeError(
                        f"file descriptor returned {type(part)}, not bytes"
                    )

                if len(part) != part_size and part_index < part_count - 1:
                    raise ValueError(
                        f"read less than {part_size} before reaching the end"
                    )

                if key and iv:
                    from telethon.crypto import AES

                    part = AES.encrypt_ige(part, key, iv)

                if not is_big:
                    hash_md5.update(part)

                parts.append(part)

            uploaded = 0
            progress_lock = asyncio.Lock()
            semaphore = asyncio.Semaphore(UPLOAD_CONCURRENCY)

            async def send_part(part_index, part):
                nonlocal uploaded

                async with semaphore:
                    if is_big:
                        request = functions.upload.SaveBigFilePartRequest(
                            file_id,
                            part_index,
                            part_count,
                            part,
                        )
                    else:
                        request = functions.upload.SaveFilePartRequest(
                            file_id,
                            part_index,
                            part,
                        )

                    result = await self(request)
                    if not result:
                        raise RuntimeError(
                            f"Failed to upload file part {part_index}"
                        )

                    async with progress_lock:
                        uploaded += len(part)
                        if progress_callback:
                            await helpers._maybe_await(
                                progress_callback(uploaded, file_size)
                            )

            tasks = [
                asyncio.create_task(send_part(index, part))
                for index, part in enumerate(parts)
            ]

            try:
                await asyncio.gather(*tasks)
            except BaseException:
                for task in tasks:
                    if not task.done():
                        task.cancel()

                await asyncio.gather(*tasks, return_exceptions=True)
                raise

            if is_big:
                return types.InputFileBig(file_id, part_count, file_name)

            return custom.InputSizedFile(
                file_id,
                part_count,
                file_name,
                md5=hash_md5,
                size=file_size,
            )


async def send_debug_apk(path: Path) -> None:
    api_id = os.environ.get("TELEGRAM_API_ID")
    api_hash = os.environ.get("TELEGRAM_API_HASH")
    token = os.environ.get("TELEGRAM_BOT_TOKEN")
    chat_id = os.environ.get("TELEGRAM_CHAT_ID")

    if not api_id:
        raise SystemExit("TELEGRAM_API_ID is missing.")

    if not api_hash:
        raise SystemExit("TELEGRAM_API_HASH is missing.")

    if not token:
        raise SystemExit("TELEGRAM_BOT_TOKEN is missing.")

    if not chat_id:
        raise SystemExit("TELEGRAM_CHAT_ID is missing.")

    if not path.is_file() or path.stat().st_size == 0:
        raise SystemExit(f"APK is missing or empty: {path}")

    try:
        api_id_value = int(api_id)
    except ValueError as exc:
        raise SystemExit("TELEGRAM_API_ID must be an integer.") from exc

    file_size = path.stat().st_size

    for attempt in range(1, MAX_ATTEMPTS + 1):
        client = FastTelegramClient(
            f"speedindicator-ci-{attempt}",
            api_id_value,
            api_hash,
            timeout=30,
            connection_retries=3,
            request_retries=3,
            retry_delay=5,
        )

        started = time.monotonic()
        last_reported = -1

        async def progress(current, total):
            nonlocal last_reported
            percent = int(current * 100 / total) if total else 0

            if percent >= last_reported + 5 or percent == 100:
                last_reported = percent
                elapsed = max(time.monotonic() - started, 0.001)
                rate = current / elapsed / (1024 * 1024)
                print(
                    f"Telegram upload: {percent}% "
                    f"({current}/{total} bytes, {rate:.1f} MiB/s)",
                    flush=True,
                )

        try:
            await client.start(bot_token=token)
            await asyncio.wait_for(
                client.send_file(
                    int(chat_id),
                    path,
                    force_document=True,
                    part_size_kb=512,
                    progress_callback=progress,
                ),
                timeout=UPLOAD_TIMEOUT,
            )

            elapsed = max(time.monotonic() - started, 0.001)
            print(
                "Telegram upload completed successfully in "
                f"{elapsed:.1f}s "
                f"({file_size / elapsed / (1024 * 1024):.2f} MiB/s average).",
                flush=True,
            )
            break
        except asyncio.TimeoutError:
            print(
                f"Telegram upload timed out after {UPLOAD_TIMEOUT}s.",
                flush=True,
            )
        except Exception as exc:
            print(
                "Telegram upload attempt "
                f"{attempt} failed: {type(exc).__name__}: {exc}",
                flush=True,
            )
        finally:
            await client.disconnect()
    else:
        raise RuntimeError(
            f"Telegram upload failed after {MAX_ATTEMPTS} attempts."
        )


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: telegram.py <apk-path>")

    asyncio.run(send_debug_apk(Path(sys.argv[1])))


if __name__ == "__main__":
    main()
