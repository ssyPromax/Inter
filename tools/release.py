#!/usr/bin/env python3
"""把 Inter APK 发布到 GitHub Release，并生成应用内更新使用的 update.json。

用法:
    python tools/release.py --version 1.2.1 --version-code 4 \
        --apk dist/Inter-v1.2.1.apk --notes-file RELEASE_NOTES.md

    python tools/release.py --version 1.2.1 --version-code 4 \
        --apk dist/Inter-v1.2.1.apk --dry-run

Token 来源（按顺序）:
    1. 环境变量 GITHUB_TOKEN / GH_TOKEN
    2. Windows 用户环境变量（注册表）
    3. 交互输入（不回显）

只上传 Release 资产，不提交代码；代码请先用 suite/tools/sync-all.py 推送。
Token 不会写入任何文件，也不会打印。
"""
from __future__ import annotations

import argparse
import getpass
import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

API = "https://api.github.com"
UPLOAD_API = "https://uploads.github.com"
DEFAULT_OWNER = "ssyPromax"
DEFAULT_REPO = "Inter"


def _utf8_console() -> None:
    if sys.platform == "win32":
        try:
            os.system("chcp 65001 >nul")
        except Exception:
            pass
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass


def get_token() -> str:
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if token:
        return token.strip()
    if sys.platform == "win32":
        try:
            import winreg

            with winreg.OpenKey(winreg.HKEY_CURRENT_USER, "Environment") as key:
                value, _ = winreg.QueryValueEx(key, "GITHUB_TOKEN")
                if value:
                    return str(value).strip()
        except Exception:
            pass
    return getpass.getpass("GitHub Personal Access Token（输入不回显）: ").strip()


def headers(token: str, content_type: str | None = None) -> dict:
    result = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github+json",
        "User-Agent": "inter-release-publisher",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    if content_type:
        result["Content-Type"] = content_type
    return result


def call(token, method, url, body=None, data=None, content_type=None, timeout=300):
    payload = data
    if body is not None:
        payload = json.dumps(body).encode("utf-8")
        content_type = "application/json"
    last_error = None
    for attempt in range(3):
        request = urllib.request.Request(
            url,
            data=payload,
            method=method,
            headers=headers(token, content_type),
        )
        try:
            with urllib.request.urlopen(request, timeout=timeout) as response:
                raw = response.read()
                return response.status, json.loads(raw) if raw else {}
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", "replace")
            if error.code in (500, 502, 503, 504) and attempt < 2:
                last_error = f"{error.code} {detail[:120]}"
                time.sleep(1.5 * (attempt + 1))
                continue
            return error.code, {"error": detail[:400]}
        except (urllib.error.URLError, TimeoutError, OSError) as error:
            last_error = str(error)
            if attempt < 2:
                print(f"  [重试 {attempt + 1}/2] {type(error).__name__}: {last_error[:120]}")
                time.sleep(1.5 * (attempt + 1))
                continue
            raise
    return 599, {"error": last_error}


def sha256_of(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> int:
    _utf8_console()
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", required=True, help="例如 1.2.1")
    parser.add_argument("--version-code", required=True, type=int, help="BuildConfig.VERSION_CODE")
    parser.add_argument("--apk", required=True, help="本地 APK 路径")
    parser.add_argument("--asset-name", default="", help="Release 里的 APK 文件名，默认 Inter-v<version>.apk")
    parser.add_argument("--notes", default="", help="Release 正文（Markdown）")
    parser.add_argument("--notes-file", default="", help="从文件读取 Release 正文")
    parser.add_argument("--owner", default=DEFAULT_OWNER)
    parser.add_argument("--repo", default=DEFAULT_REPO)
    parser.add_argument("--tag", default="", help="默认 v<version>")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    apk = Path(args.apk).resolve()
    if not apk.is_file():
        print(f"APK 不存在: {apk}")
        return 1
    version = args.version.strip().lstrip("v")
    tag = args.tag.strip() or f"v{version}"
    asset_name = args.asset_name.strip() or f"Inter-v{version}.apk"
    if not asset_name.lower().endswith(".apk"):
        asset_name += ".apk"

    if args.notes_file:
        notes = Path(args.notes_file).read_text(encoding="utf-8")
    elif args.notes:
        notes = args.notes
    else:
        notes = f"Inter v{version}"

    size = apk.stat().st_size
    digest = sha256_of(apk)
    published_at = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    page_url = f"https://github.com/{args.owner}/{args.repo}/releases/tag/{tag}"
    apk_url = f"https://github.com/{args.owner}/{args.repo}/releases/download/{tag}/{asset_name}"
    update = {
        "versionCode": args.version_code,
        "versionName": version,
        "tag": tag,
        "apkAsset": asset_name,
        "apkUrl": apk_url,
        "sha256": digest,
        "sizeBytes": size,
        "publishedAt": published_at,
        "pageUrl": page_url,
        "notes": notes,
    }
    update_json = json.dumps(update, ensure_ascii=False, indent=2) + "\n"
    update_path = apk.parent / "update.json"
    update_path.write_text(update_json, encoding="utf-8")

    print(f"仓库:     {args.owner}/{args.repo}")
    print(f"版本:     v{version} (versionCode {args.version_code})")
    print(f"APK:      {apk.name}  {size / 1024 / 1024:.2f} MB")
    print(f"SHA-256:  {digest}")
    print(f"update:   {update_path}")
    print(f"Tag:      {tag}")
    if args.dry_run:
        print("\n[DRY RUN] 未联网，update.json 已生成。")
        return 0

    token = get_token()
    if not token:
        print("没有拿到 token，退出。")
        return 1

    repo_url = f"{API}/repos/{args.owner}/{args.repo}"
    status, repo = call(token, "GET", repo_url)
    if status != 200:
        print(f"仓库不可用（HTTP {status}），请先用 sync-all.py 推送代码。")
        return 1
    print(f"仓库:     {repo['full_name']} ({repo.get('visibility', 'public')})")

    status, release = call(token, "GET", f"{repo_url}/releases/tags/{tag}")
    if status == 404:
        status, release = call(
            token,
            "POST",
            f"{repo_url}/releases",
            body={
                "tag_name": tag,
                "target_commitish": repo.get("default_branch", "main"),
                "name": f"Inter v{version}",
                "body": notes,
                "draft": False,
                "prerelease": False,
            },
        )
        if status not in (200, 201):
            print(f"创建 Release 失败（HTTP {status}）: {release}")
            return 1
        print(f"已创建 Release: {release['html_url']}")
    elif status == 200:
        print(f"Release 已存在: {release['html_url']}")
        call(
            token,
            "PATCH",
            f"{repo_url}/releases/{release['id']}",
            body={"body": notes, "draft": False, "prerelease": False},
        )
    else:
        print(f"读取 Release 失败（HTTP {status}）: {release}")
        return 1

    release_id = release["id"]
    status, assets = call(token, "GET", f"{repo_url}/releases/{release_id}/assets?per_page=100")
    existing = {}
    if status == 200 and isinstance(assets, list):
        existing = {asset["name"]: asset["id"] for asset in assets}

    for name, payload, content_type in (
        (asset_name, apk.read_bytes(), "application/vnd.android.package-archive"),
        ("update.json", update_json.encode("utf-8"), "application/json"),
    ):
        if name in existing:
            print(f"  删除旧资产: {name}")
            call(token, "DELETE", f"{repo_url}/releases/assets/{existing[name]}")
        url = (
            f"{UPLOAD_API}/repos/{args.owner}/{args.repo}/releases/{release_id}/assets"
            f"?name={urllib.parse.quote(name)}"
        )
        print(f"  上传: {name} ({len(payload) / 1024 / 1024:.2f} MB)")
        status, result = call(token, "POST", url, data=payload, content_type=content_type)
        if status not in (200, 201):
            print(f"  上传失败（HTTP {status}）: {result}")
            return 1
        print(f"    -> {result.get('browser_download_url', '')}")

    print(f"\n完成: {page_url}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
