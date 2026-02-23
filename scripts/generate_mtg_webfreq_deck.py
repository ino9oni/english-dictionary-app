#!/usr/bin/env python3
"""
Expand MTG story deck entries using high-frequency words from official web stories.

Usage:
  python3 scripts/generate_mtg_webfreq_deck.py \
    --deck app/src/main/assets/decks/mtg_story.v1.json \
    --max-pages 28 \
    --add-count 140
"""

from __future__ import annotations

import argparse
import collections
import datetime as dt
import html
import json
import re
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from typing import Dict, Iterable, List, Optional, Set, Tuple


BASE = "https://magic.wizards.com"
INDEX_URL = f"{BASE}/en/news/magic-story"
DICT_API = "https://api.dictionaryapi.dev/api/v2/entries/en/"
USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) Codex/MTGDeckBuilder"
UUID_NS = uuid.UUID("06d4d9a0-88a2-4e3f-ae8f-c3b4a7805e10")

TOKEN_RE = re.compile(r"[A-Za-z][A-Za-z'’-]{2,}")
ARTICLE_LINK_RE = re.compile(r'href="(/en/news/magic-story/[^"#?]+)"')

STOPWORDS = {
    "about", "above", "after", "again", "against", "almost", "along", "also",
    "although", "always", "among", "around", "because", "before", "being", "below",
    "between", "both", "could", "did", "does", "doing", "done", "during", "each",
    "either", "else", "even", "ever", "every", "from", "further", "have", "having",
    "hers", "herself", "him", "himself", "his", "into", "itself", "just", "like",
    "many", "might", "more", "most", "must", "neither", "never", "next", "once",
    "only", "other", "ours", "ourselves", "over", "same", "should", "since", "some",
    "such", "than", "that", "their", "theirs", "them", "themselves", "then", "there",
    "these", "they", "this", "those", "through", "under", "until", "very", "was",
    "were", "what", "when", "where", "which", "while", "who", "whom", "whose", "why",
    "will", "with", "would", "your", "yours", "yourself", "yourselves", "able",
    "across", "amid", "amidst", "cannot", "cant", "didnt", "doesnt", "dont", "had",
    "has", "its", "let", "lets", "may", "much", "our", "said", "says", "still",
    "thats", "the", "and", "are", "but", "for", "not", "you", "all", "can", "any",
    "too", "off", "out", "few", "get", "got", "hadnt", "havent", "isnt", "wasnt",
    "werent", "im", "ive", "ill", "we", "us", "me", "my", "mine", "i", "he", "she",
    "do", "if", "or", "as", "at", "by", "in", "on", "to", "of", "an", "a", "be",
}

BLOCKLIST = {
    "wizards", "coast", "hasbro", "facebook", "twitter", "instagram", "youtube",
    "cookie", "cookies", "privacy", "copyright", "rights", "reserved", "javascript",
    "browser", "newsletter", "episode", "chapters", "chapter", "magic", "story",
}

COMMON_GENERIC_WORDS = {
    "already", "another", "anything", "asked", "away", "back", "been", "began", "behind",
    "better", "came", "close", "come", "dead", "enough", "everything", "eyes", "face", "feet",
    "felt", "fight", "find", "first", "found", "going", "gone", "good", "great", "ground", "hand",
    "hands", "head", "heart", "help", "here", "home", "inside", "keep", "knew", "know", "last",
    "left", "life", "light", "little", "looked", "looking", "lost", "made", "make", "maybe",
    "mind", "moment", "need", "nothing", "open", "others", "people", "place", "power", "right",
    "room", "said", "saw", "seemed", "seems", "seen", "side", "something", "someone", "still",
    "stood", "take", "taken", "than", "then", "there", "thing", "things", "think", "thought",
    "through", "time", "times", "took", "turned", "turning", "under", "until", "upon", "want",
    "wanted", "watch", "watching", "went", "were", "what", "when", "where", "while", "with", "without",
    "word", "words", "work", "world", "would",
}

MTG_LORE_ALLOWLIST = {
    "chandra", "jace", "nissa", "kaya", "kellan", "huatli", "elspeth", "eldrazi",
    "phyrexia", "phyrexian", "planeswalker", "planeswalkers", "ravnica", "zendikar",
    "innistrad", "dominaria", "ikar", "aether", "mana", "multiverse",
}

UNWANTED_POS = {
    "pronoun", "preposition", "conjunction", "determiner", "article", "interjection", "prefix", "suffix"
}


def fetch_text(url: str, timeout: int = 15) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=timeout) as res:
        data = res.read()
    return data.decode("utf-8", "ignore")


def discover_story_urls(max_pages: int) -> List[str]:
    found: List[str] = []
    seen: Set[str] = set()
    empty_streak = 0
    for page in range(1, max_pages + 1):
        url = f"{INDEX_URL}?page={page}"
        try:
            html_text = fetch_text(url)
        except Exception:
            empty_streak += 1
            if empty_streak >= 3:
                break
            continue

        links = ARTICLE_LINK_RE.findall(html_text)
        if not links:
            empty_streak += 1
            if empty_streak >= 3:
                break
            continue

        empty_streak = 0
        for link in links:
            full = urllib.parse.urljoin(BASE, link)
            if full not in seen:
                seen.add(full)
                found.append(full)
    return found


def strip_html(text: str) -> str:
    text = re.sub(r"(?is)<script[^>]*>.*?</script>", " ", text)
    text = re.sub(r"(?is)<style[^>]*>.*?</style>", " ", text)
    text = re.sub(r"(?is)<svg[^>]*>.*?</svg>", " ", text)
    text = re.sub(r"(?is)<noscript[^>]*>.*?</noscript>", " ", text)
    text = re.sub(r"(?s)<[^>]+>", " ", text)
    text = html.unescape(text)
    text = re.sub(r"\s+", " ", text)
    return text


def extract_article_body(html_text: str) -> str:
    main = re.search(r'(?is)<div id="article-body"[^>]*>.*?</article>', html_text)
    if main:
        return strip_html(main.group(0))

    fallback = re.search(r'(?is)<main[^>]*>.*?</main>', html_text)
    if fallback:
        return strip_html(fallback.group(0))
    return strip_html(html_text)


def normalize_token(token: str) -> str:
    t = token.lower().replace("’", "'")
    t = re.sub(r"'s$", "", t)
    t = re.sub(r"^[^a-z]+|[^a-z]+$", "", t)
    return t


def tokenize_words(text: str) -> Iterable[str]:
    for raw in TOKEN_RE.findall(text):
        t = normalize_token(raw)
        if len(t) <= 3:
            continue
        if t in STOPWORDS or t in BLOCKLIST:
            continue
        if not re.fullmatch(r"[a-z]+", t):
            continue
        yield t


def existing_word_set(entries: List[dict]) -> Set[str]:
    words: Set[str] = set()
    for entry in entries:
        term = entry.get("term", "")
        for token in tokenize_words(term):
            words.add(token)
        normalized_term = normalize_token(term)
        if normalized_term:
            words.add(normalized_term)
    return words


def fetch_definition(
    word: str,
    cache: Dict[str, Optional[Tuple[str, str, Optional[str]]]],
    timeout: int,
) -> Optional[Tuple[str, str, Optional[str]]]:
    if word in cache:
        return cache[word]
    url = DICT_API + urllib.parse.quote(word)
    try:
        body = fetch_text(url, timeout=timeout)
        parsed = json.loads(body)
        if not isinstance(parsed, list) or not parsed:
            cache[word] = None
            return None
        meanings = parsed[0].get("meanings") or []
        for m in meanings:
            pos = (m.get("partOfSpeech") or "").strip() or "word"
            if pos.lower() in UNWANTED_POS:
                continue
            defs = m.get("definitions") or []
            for d in defs:
                meaning = (d.get("definition") or "").strip()
                if meaning:
                    example = (d.get("example") or "").strip() or None
                    cache[word] = (pos, meaning, example)
                    return cache[word]
    except Exception:
        pass
    cache[word] = None
    return None


def build_entry(word: str, definition: Optional[Tuple[str, str, Optional[str]]]) -> dict:
    if definition is None:
        pos = "word"
        meaning_en = "A frequent word in MTG story web articles (curation pending)."
        example_en = f"The term '{word}' appears frequently in MTG story contexts."
    else:
        pos, meaning_en, example = definition
        example_en = example or f"The term '{word}' appears in MTG story text."

    updated = dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%dT00:00:00Z")
    entry_id = str(uuid.uuid5(UUID_NS, f"mtg-webfreq::{word}"))
    return {
        "entry_id": entry_id,
        "term": word,
        "display_term": word,
        "pos": pos,
        "meaning_en": meaning_en,
        "meaning_ja": "（要確認）MTGストーリー頻出語",
        "lore_note": "Collected from MTG web story frequency extraction.",
        "canonical_translation": "",
        "tags": ["mtg", "story", "webfreq"],
        "synonyms": [],
        "confusables": [],
        "examples": [{"text_en": example_en, "text_ja": ""}],
        "source_quotes": [],
        "updated_at": updated,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--deck", default="app/src/main/assets/decks/mtg_story.v1.json")
    parser.add_argument("--max-pages", type=int, default=28)
    parser.add_argument("--add-count", type=int, default=140)
    parser.add_argument("--min-freq", type=int, default=4)
    parser.add_argument("--lookup-limit", type=int, default=90)
    parser.add_argument("--dict-timeout", type=int, default=6)
    parser.add_argument("--dict-sleep-ms", type=int, default=40)
    parser.add_argument("--replace-webfreq", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    with open(args.deck, "r", encoding="utf-8") as f:
        deck = json.load(f)
    entries: List[dict] = list(deck.get("entries", []))
    if args.replace_webfreq:
        before = len(entries)
        entries = [
            e for e in entries
            if "webfreq" not in [t.lower() for t in e.get("tags", [])]
        ]
        removed = before - len(entries)
        print(f"removed old webfreq entries: {removed}", flush=True)

    story_urls = discover_story_urls(args.max_pages)
    print(f"discovered story urls: {len(story_urls)}", flush=True)
    if not story_urls:
        raise SystemExit("No story URLs discovered.")

    counter: collections.Counter[str] = collections.Counter()
    for idx, url in enumerate(story_urls, start=1):
        try:
            html_text = fetch_text(url)
            body_text = extract_article_body(html_text)
        except Exception as exc:
            print(f"skip article ({idx}/{len(story_urls)}): {url} ({exc})", flush=True)
            continue
        counter.update(tokenize_words(body_text))
        if idx % 10 == 0:
            print(f"processed {idx}/{len(story_urls)} articles", flush=True)

    existing = existing_word_set(entries)
    candidates = [
        (word, freq)
        for word, freq in counter.most_common()
        if freq >= args.min_freq and word not in existing
        and word not in COMMON_GENERIC_WORDS
    ]
    print(f"candidate words: {len(candidates)}", flush=True)

    definition_cache: Dict[str, Optional[Tuple[str, str, Optional[str]]]] = {}
    added: List[dict] = []
    for word, freq in candidates:
        if len(added) >= args.add_count:
            break
        use_lookup = len(added) < max(0, args.lookup_limit)
        definition = None
        if use_lookup:
            definition = fetch_definition(
                word=word,
                cache=definition_cache,
                timeout=args.dict_timeout,
            )
        if definition is None and word not in MTG_LORE_ALLOWLIST:
            # Avoid noisy generic words when dictionary data is unavailable.
            continue
        added.append(build_entry(word, definition))
        existing.add(word)
        # Mild pacing for free API.
        if use_lookup and args.dict_sleep_ms > 0:
            time.sleep(args.dict_sleep_ms / 1000.0)
        if len(added) % 20 == 0:
            print(f"prepared {len(added)} / {args.add_count} entries", flush=True)

    print(f"new entries prepared: {len(added)}", flush=True)
    if args.dry_run:
        for e in added[:20]:
            print(f"- {e['term']} ({e['pos']})")
        return

    entries.extend(added)
    entries.sort(key=lambda e: (str(e.get("term", "")).lower(), str(e.get("entry_id", ""))))
    deck["entries"] = entries

    with open(args.deck, "w", encoding="utf-8") as f:
        json.dump(deck, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"deck updated: {args.deck}", flush=True)
    print(f"total entries: {len(entries)}", flush=True)


if __name__ == "__main__":
    main()
