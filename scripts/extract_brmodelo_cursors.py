#!/usr/bin/env python3
"""
Extract RT_CURSOR resources from MSVC/Delphi cursor.RES, write valid .cur files
and PNGs for Compose Multiplatform resources.
"""
from __future__ import annotations

import json
import struct
import sys
from pathlib import Path


def align_dword(n: int) -> int:
    return (n + 3) & ~3


def read_word_ordinal_or_utf16_name(data: bytes, off: int) -> tuple[int | str, int]:
    """
    Win32 .res: if first WORD is 0xFFFF, next WORD is numeric ordinal (type or name id).
    Otherwise the field is a UTF-16LE null-terminated string.
    """
    w0 = struct.unpack_from("<H", data, off)[0]
    if w0 == 0xFFFF:
        w1 = struct.unpack_from("<H", data, off + 2)[0]
        return int(w1), off + 4
    chars: list[str] = []
    p = off
    while p + 1 < len(data):
        ch = struct.unpack_from("<H", data, p)[0]
        p += 2
        if ch == 0:
            break
        chars.append(chr(ch))
    return "".join(chars), align_dword(p)


def parse_res(data: bytes) -> list[dict]:
    """
    Parse Win32 .res (MSVC / Delphi resource compiler output).

    HeaderSize includes the leading DataSize and HeaderSize DWORDs; body starts at
    pos + header_size through pos + header_size + data_size - 1.
    """
    resources: list[dict] = []
    pos = 0
    while pos + 8 <= len(data):
        data_size, header_size = struct.unpack_from("<II", data, pos)
        if header_size == 0 and data_size == 0:
            break
        if pos + header_size + data_size > len(data):
            break
        # Type/name/version block begins after the two size DWORDs
        h_start = pos + 8
        h_end = pos + header_size
        if h_end < h_start or h_end > len(data):
            break
        rtype, p = read_word_ordinal_or_utf16_name(data, h_start)
        rname, p = read_word_ordinal_or_utf16_name(data, p)
        if p + 16 > h_end:
            raise ValueError(f"Header truncated at {pos}: p={p} h_end={h_end}")
        _data_version, _mem_flags, lang_id, _version, _characteristics = struct.unpack_from(
            "<IHHII", data, p
        )
        body_start = pos + header_size
        body = data[body_start : body_start + data_size]
        pos = align_dword(body_start + data_size)
        resources.append(
            {
                "type": rtype,
                "name": rname,
                "lang_id": lang_id,
                "data": body,
            }
        )
    return resources


def wrap_dib_as_ico(dib: bytes) -> bytes:
    """
    Wrap a DIB blob (BITMAPINFO + XOR + AND) as a single-entry ICO in memory.

    Pillow's CurImageFile does not apply the AND mask as alpha; IcoFile.frame()
    does when the directory entry's bpp matches the DIB (see IcoImagePlugin).
    """
    bi_bitcount = struct.unpack_from("<H", dib, 14)[0]
    bi_width = struct.unpack_from("<i", dib, 4)[0]
    bi_height = struct.unpack_from("<i", dib, 8)[0]
    visible_h = abs(bi_height) // 2
    w = min(max(bi_width, 1), 255)
    h = min(max(visible_h, 1), 255)
    out = bytearray()
    out += struct.pack("<HHH", 0, 1, 1)  # reserved, type=icon, count=1
    image_offset = 22
    out += struct.pack(
        "<BBBBHHII",
        w if w < 256 else 0,
        h if h < 256 else 0,
        0,
        0,
        1,
        bi_bitcount,
        len(dib),
        image_offset,
    )
    out += dib
    return bytes(out)


def wrap_rt_cursor_to_cur_file(rt_cursor_payload: bytes) -> bytes:
    """
    RT_CURSOR resource = WORD xHotspot, WORD yHotspot, then DIB (BITMAPINFO + XOR + AND).
    Build a single-entry .cur file for Pillow / viewers.
    """
    if len(rt_cursor_payload) < 8:
        raise ValueError("cursor payload too small")
    x_hot, y_hot = struct.unpack_from("<HH", rt_cursor_payload, 0)
    dib = rt_cursor_payload[4:]
    # ICONDIR + ICONDIRENTRY (cursor: planes=hotspot x, bitcount=hotspot y per MS)
    image_offset = 6 + 16
    out = bytearray()
    out += struct.pack("<HHH", 0, 2, 1)  # reserved, type=cursor, count=1
    # ICONDIRENTRY — width/height from DIB header if possible
    bi_size = struct.unpack_from("<I", dib, 0)[0]
    if bi_size < 40:
        raise ValueError("invalid BITMAPINFOHEADER")
    bi_width = struct.unpack_from("<i", dib, 4)[0]
    bi_height = struct.unpack_from("<i", dib, 8)[0]  # XOR+AND combined height
    height_icons = max(1, (bi_height // 2) & 0xFF)
    width_icons = max(1, min(bi_width & 0xFF, 255))
    out += struct.pack(
        "<BBBBHHII",
        width_icons,
        height_icons,
        0,
        0,
        x_hot,
        y_hot,
        len(dib),
        image_offset,
    )
    out += dib
    return bytes(out)


def cursor_payload_to_png(cur_payload: bytes, png_path: Path) -> tuple[int, int]:
    """Decode XOR+AND to RGBA PNG. Returns (hotspot_x, hotspot_y)."""
    from io import BytesIO

    from PIL.IcoImagePlugin import IcoFile  # type: ignore[import-untyped]

    x_hot, y_hot = struct.unpack_from("<HH", cur_payload, 0)
    dib = cur_payload[4:]
    ico_bytes = wrap_dib_as_ico(dib)
    im = IcoFile(BytesIO(ico_bytes)).frame(0)
    im.save(png_path, "PNG")
    return int(x_hot), int(y_hot)


def resource_base_name(name: int | str) -> str:
    if isinstance(name, int):
        return f"id_{name}"
    s = str(name)
    # Compose resource file names: lowercase letters, digits, underscore
    out = []
    for ch in s:
        if ch.isalnum():
            out.append(ch.lower())
        elif ch in ("_", "-"):
            out.append("_")
        else:
            out.append("_")
    base = "".join(out).strip("_")
    return base or "unnamed"


RT_CURSOR = 1
RT_GROUP_CURSOR = 12

# RT_GROUP_CURSOR name (uppercase in .res) -> string passed to LoadCursor(HInstance, ...) in mer.pas CarregueCursor
GROUP_NAME_TO_DELPHI_LOAD: dict[str, str] = {
    "ENTIDADE": "Entidade",
    "RELACAO": "Relacao",
    "ENTASSOSS": "EntAssoss",
    "ESPECIALIZACAO": "Especializacao",
    "TEXTO": "Texto",
    "TEXTOII": "TextoII",
    "ATRIBUTO": "Atributo",
    "AUTOREL": "AutoRel",
    "LIGACAO": "Ligacao",
    "LIGACAO2": "Ligacao2",
    "APAGAR": "APAGAR",
    "ESPECIALIZACAOA": "EspecializacaoA",
    "ESPECIALIZACAOB": "EspecializacaoB",
    "LRELACAO": "LRELACAO",
    "LRELACAO2": "LRELACAO2",
    "LCAMPO": "LCAMPO",
    "LTABELA": "LTABELA",
    "SEPARADOR": "SEPARADOR",
    "TRABALHO_TABELA": "TRABALHO_TABELA",
}


def pair_cursor_with_group(entries: list[dict]) -> list[tuple[bytes, str]]:
    """Each RT_CURSOR is immediately followed by an RT_GROUP_CURSOR with the string name."""
    out: list[tuple[bytes, str]] = []
    i = 0
    while i < len(entries):
        e = entries[i]
        if e["type"] == RT_CURSOR and i + 1 < len(entries):
            g = entries[i + 1]
            if g["type"] == RT_GROUP_CURSOR and isinstance(g["name"], str):
                out.append((e["data"], g["name"]))
                i += 2
                continue
        i += 1
    return out


def main() -> int:
    repo = Path(__file__).resolve().parents[1]
    res_path = repo.parent / "Fontes-Originais" / "cursor.RES"
    if not res_path.is_file():
        print(f"Missing {res_path}", file=sys.stderr)
        return 1

    out_cur = repo / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "brmodelo_cursors" / "cur"
    out_png = repo / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "brmodelo_cursors" / "png"
    out_cur.mkdir(parents=True, exist_ok=True)
    out_png.mkdir(parents=True, exist_ok=True)

    data = res_path.read_bytes()
    entries = parse_res(data)
    pairs = pair_cursor_with_group(entries)
    hotspots: dict[str, dict[str, int | str]] = {}

    for cur_payload, group_name in pairs:
        base = resource_base_name(group_name)
        cur_name = f"cursor_{base}.cur"
        png_name = f"cursor_{base}.png"
        cur_path = out_cur / cur_name
        png_path = out_png / png_name
        cur_path.write_bytes(wrap_rt_cursor_to_cur_file(cur_payload))
        try:
            hx, hy = cursor_payload_to_png(cur_payload, png_path)
            key = f"files/brmodelo_cursors/png/{png_name}"
            hotspots[key] = {
                "hotspotX": hx,
                "hotspotY": hy,
                "delphiGroupName": group_name,
                "delphiLoadCursorResource": GROUP_NAME_TO_DELPHI_LOAD.get(
                    group_name, group_name
                ),
                "curRelativePath": f"files/brmodelo_cursors/cur/{cur_name}",
            }
        except Exception as ex:  # noqa: BLE001
            print(f"PNG failed for {group_name}: {ex}", file=sys.stderr)
            hotspots[f"files/brmodelo_cursors/cur/{cur_name}"] = {
                "error": str(ex),
                "delphiGroupName": group_name,
                "delphiLoadCursorResource": GROUP_NAME_TO_DELPHI_LOAD.get(
                    group_name, group_name
                ),
                "curRelativePath": f"files/brmodelo_cursors/cur/{cur_name}",
            }

    meta_path = out_png.parent / "hotspots.json"
    meta_path.write_text(json.dumps({"cursors": hotspots}, indent=2), encoding="utf-8")
    print(f"Wrote {len(pairs)} .cur and PNGs under composeResources/files/brmodelo_cursors/")
    print(f"Hotspot metadata: {meta_path.relative_to(repo)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
