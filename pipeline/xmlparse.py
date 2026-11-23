"""Tolerant parsers for price-transparency XML files.

Chains publish the same logical schema with different casing and wrappers
(e.g. <Item>/<Product>, <StoreId>/<STOREID>, <SubChains><Stores><Store>).
Everything is matched case-insensitively on the local tag name.
"""
from __future__ import annotations

import gzip
import io
import zipfile
from typing import Iterator

from lxml import etree


def _open(path: str) -> io.BufferedIOBase:
    with open(path, "rb") as fh:
        head = fh.read(4)
    if head[:2] == b"\x1f\x8b":
        return gzip.open(path, "rb")
    if head[:2] == b"PK":
        z = zipfile.ZipFile(path)
        return z.open(z.namelist()[0])
    return open(path, "rb")


def _tag(el) -> str:
    t = el.tag
    if not isinstance(t, str):
        return ""
    return t.rsplit("}", 1)[-1].lower()


def _fields(el) -> dict[str, str]:
    out = {}
    for ch in el:
        k = _tag(ch)
        if k and len(ch) == 0:
            out[k] = (ch.text or "").strip()
    return out


def _iter(path: str, wanted: set[str] | None):
    """Yield (element, ancestors_fields) for every element whose tag is in `wanted`."""
    parser = etree.iterparse(_open(path), events=("start", "end"), recover=True, huge_tree=True)
    stack: list[dict] = []
    for event, el in parser:
        tag = _tag(el)
        if event == "start":
            stack.append({})
            continue
        stack.pop()
        if (tag in wanted) if wanted is not None else (len(el) > 0 and any(_tag(c) in ("storeid", "storeno") for c in el)):
            ctx = {}
            for frame in stack:
                ctx.update(frame)
            yield el, ctx
            el.clear()
        elif len(el) == 0 and stack:
            # leaf next to the list (e.g. <ChainId> before <Items>) -> context for later items
            stack[-1][tag] = (el.text or "").strip()


def _first(d: dict, *keys: str) -> str:
    for k in keys:
        v = d.get(k)
        if v:
            return v
    return ""


def parse_prices(path: str) -> tuple[dict, Iterator[dict]]:
    """Returns (header, items). Header has chain_id/store_id when present."""
    header: dict = {}

    def gen():
        for el, ctx in _iter(path, {"item", "product"}):
            f = _fields(el)
            if not f:
                continue
            if not header:
                header.update(
                    chain_id=_first(ctx, "chainid"),
                    subchain_id=_first(ctx, "subchainid"),
                    store_id=_first(ctx, "storeid"),
                )
            code = _first(f, "itemcode", "itemid")
            name = _first(f, "itemname", "itemnm", "manufactureritemdescription")
            price = _first(f, "itemprice")
            if not code or not name or not price:
                continue
            yield {
                "code": code,
                "item_type": _first(f, "itemtype"),
                "name": name,
                "manufacturer": _first(f, "manufacturername", "manufacturename"),
                "quantity": _first(f, "quantity"),
                "unit": _first(f, "unitofmeasure", "unitqty"),
                "unit_qty": _first(f, "unitqty"),
                "is_weighted": _first(f, "bisweighted", "blnisweighted"),
                "price": price,
                "unit_price": _first(f, "unitofmeasureprice"),
                "updated": _first(f, "priceupdatedate"),
            }

    return header, gen()


def parse_stores(path: str) -> Iterator[dict]:
    # A store record is any element with a direct <StoreId> leaf plus a name/address/city leaf.
    # (City Market nests them as <Store><SubChainStoreXMLObject>..., Shufersal uses <STORE>, etc.)
    for el, ctx in _iter(path, None):
        f = _fields(el)
        sid = _first(f, "storeid", "storeno")
        if not sid or not (f.keys() & {"storename", "storenm", "address", "city"}):
            continue
        yield {
            "chain_id": _first(f, "chainid") or _first(ctx, "chainid"),
            "chain_name": _first(f, "chainname") or _first(ctx, "chainname"),
            "subchain_id": _first(f, "subchainid") or _first(ctx, "subchainid"),
            "subchain_name": _first(f, "subchainname") or _first(ctx, "subchainname"),
            "store_id": sid,
            "name": _first(f, "storename", "storenm"),
            "address": _first(f, "address", "storeaddress"),
            "city": _first(f, "city", "storecity"),
            "zip": _first(f, "zipcode"),
        }


def parse_promos(path: str) -> tuple[dict, Iterator[dict]]:
    header: dict = {}

    def gen():
        for el, ctx in _iter(path, {"promotion", "sale"}):
            if not header:
                header.update(chain_id=_first(ctx, "chainid"), store_id=_first(ctx, "storeid"))
            f = {}
            codes = []
            for node in el.iter():
                t = _tag(node)
                if t == "itemcode" and node.text and node.text.strip().strip("0"):
                    codes.append(node.text.strip())
                elif len(node) == 0 and t:
                    f.setdefault(t, (node.text or "").strip())
            yield {
                "promo_id": _first(f, "promotionid"),
                "description": _first(f, "promotiondescription"),
                "end": _first(f, "promotionenddatetime", "promotionenddate"),
                "club": _first(f, "clubid", "clubs"),
                "min_qty": _first(f, "minqty"),
                "discounted_price": _first(f, "discountedprice"),
                "discount_rate": _first(f, "discountrate"),
                "codes": codes,
            }

    return header, gen()
