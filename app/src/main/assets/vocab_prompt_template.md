# GASH Fraîche — French Vocabulary Generation Prompt

You are helping me prepare new French vocabulary entries for the **GASH Fraîche** flashcard app. The output is a single JSON file that I will import back into the app via Settings → Vocabulary Import.

---

## 1. Your inputs from me

I will give you:

1. A list of French words or phrases (which may or may not include English translations).
2. A `Source` label that identifies where these words came from (e.g., `Lingoda B1.1 L3`, `29 Apr`, `Le Monde — Aug 2026`).

If I forget to give you a `Source`, **ask for one before generating any entries.** Do not guess or invent it.

If a word in my list has no English translation, you supply an accurate one.

---

## 2. Existing vocabulary — do NOT create duplicates

The app already contains the entries below. Each line shows the French headword followed by its normalised form (lowercase, NFC, straight apostrophes) — that is the form used by the app for deduplication.

For each word I give you, check it against this list using normalised matching. If there is already an entry with the same normalised form, **skip it** and tell me at the end which word(s) you skipped.

{{EXISTING_VOCAB_LIST}}

---

## 3. JSON schema

Produce a single JSON file containing an array of objects, one per **new** entry.

| Field | Type | Description |
|-------|------|-------------|
| `id` | int | Sequential integer ID. Continue from the highest existing ID in the list above. |
| `french` | string | The French word or phrase exactly as it would appear in running text (with appropriate articles where relevant). |
| `english` | string | A concise, natural English translation. Multiple distinct senses separated by ` / ` (e.g., `a chip / a flea`). |
| `example` | string | One complete French sentence that uses the word naturally, **at B1–B2 level**. |
| `exampleA2` | string | The same sentence rephrased at **A2 level**: simpler grammar (present tense where possible), shorter, more common surrounding vocabulary. |
| `pos` | string | Part of speech — see Section 4. |
| `source` | string | The `Source` label I gave you. |
| `weeks` | string | **Always leave this blank: `""`.** Do not ask me about Weeks. Do not invent a value. |
| `notes` | string | Optional usage notes (irregular forms, disambiguation, register). Empty string if nothing noteworthy. |
| `cloze` | array of 3 strings | Three **B1–B2** cloze sentences (target word replaced with `[...]`). |
| `clozeA2` | array of 3 strings | Three **A2** cloze sentences for the same target word. |
| `related` | array of 3 strings | Three thematically related French words, formatted `"french term (English translation)"`. See Section 5. |
| `relatedEN` | array of 3 strings | The English portion of each `related` entry, in the same order. |
| `etymology` | array of 3 strings | Three French words sharing the same root as the target. Each entry follows the same `"french term (English translation)"` format used for `related`. |

Both arrays of cloze sentences (`cloze` and `clozeA2`) are required — every entry must have all three at each level.

---

## 4. Valid Part of Speech values

Use exactly one of these. Pick the most specific applicable.

- `noun (f.)`
- `noun (m.)`
- `noun (m. pl.)`
- `noun phrase (f.)`
- `noun phrase (m.)`
- `verb`
- `verb phrase`
- `adjective`
- `adverb`
- `adverbial phrase`
- `pronoun`
- `phrase`
- `preposition`
- `prepositional phrase`
- `conjunction`

If a word genuinely doesn't fit any of the above, use the closest match and add a clarifying line in `notes`.

---

## 5. Related words — rules

Related words must **NOT** be direct synonyms. They should be words a learner naturally encounters in the same context — a semantic neighbourhood, not a list of substitutes.

**Good related words:**

- *Same semantic field, different role.* For `la tante` (the aunt) → `le neveu (the nephew)`, `la cousine (the (female) cousin)`, `la marraine (the godmother)`.
- *Antonyms or contrasts.* For `baisser` (to decrease) → `augmenter (to increase)`.
- *Commonly co-occurring terms.* For `le pétrole` (oil) → `le gaz naturel (natural gas)`, `le carburant (fuel)`, `le gazole (diesel)`.
- *Same register or domain.* For `un otage` (a hostage) → `un prisonnier (a prisoner)`, `un captif (a captive)`, `un détenu (a detainee)`.

**Bad related words (do NOT use):**

- Direct synonyms that could substitute in most contexts. For `rappeler` (to remind), do NOT use `faire penser à` or `remémorer` — too close in meaning. Use `l'appel (the call)`, `un rappel (a reminder)`, `la mémoire (memory)` instead.
- Words from completely unrelated domains.

**Format:** Each entry in the `related` array is `"french term (English translation)"` — French first, English in parentheses. The `relatedEN` array contains only the English portions, **in the same positional order**.

---

## 6. Example sentence guidelines (both B1–B2 and A2)

Every entry needs **both** a B1–B2 example and an A2 example for the same target word.

- **B1–B2** (`example`): natural to a near-fluent speaker — varied tenses, idiomatic phrasing, multi-clause constructions are fine.
- **A2** (`exampleA2`): the same target word, but with simpler grammar — present tense where possible, fewer clauses, more common surrounding vocabulary, shorter overall.

Both versions must illustrate the target word in a clearly natural usage context. The A2 version should feel meaningfully simpler — not just a copy of the B1–B2 sentence.

Example for `surveiller` (to supervise / to monitor):

- B1–B2: `Le directeur surveille de près l'évolution des résultats trimestriels.`
- A2: `La maman surveille ses enfants au parc.`

---

## 7. Cloze sentence guidelines

Each entry needs **three B1–B2 cloze sentences** *and* **three A2 cloze sentences** for the same target word.

For both levels:

- Replace **only the target word** with `[...]`. Do not blank out articles or prepositions that are part of the target phrase.
- The sentence must make the answer unambiguous — a native speaker should be able to fill the blank with only one reasonable word given the context.
- Use varied contexts across the three sentences within a level.
- Avoid repeating sentence structures across the three sentences for a given level.

For B1–B2 cloze: full B1–B2 sentence complexity is expected.
For A2 cloze: simpler grammar, mostly present tense, shorter sentences, common surrounding vocabulary.

Example B1–B2 cloze for `rappeler` (to remind):

- `Peux-tu me [...] d'acheter du pain en rentrant ce soir ?`
- `Cette chanson me [...] mon enfance dans le sud de la France.`
- `Le médecin m'a [...] de prendre mes médicaments chaque matin.`

Example A2 cloze for `rappeler`:

- `Tu peux me [...] son numéro de téléphone ?`
- `Mon ami me [...] toujours mes rendez-vous.`
- `Cette photo me [...] mes vacances en Espagne.`

---

## 8. Etymological relatives

Three French words sharing the same root as the target. Prefer a mix: the root noun/verb, a derived adjective or adverb, and a related abstract noun.

**Format:** Each etymology entry is `"french term (English translation)"` — French first, English in parentheses, the same format used for `related`. The English translation is mandatory; never leave it out.

Examples:

- `rappeler` (to remind) → `l'appel (the call)`, `un rappel (a reminder)`, `appeler (to call)`
- `le plafond` (the ceiling) → `plafonner (to cap / to peak)`, `le plafonnement (the capping)`, `le plancher (the floor)`

These help the learner recognise word families and build vocabulary through morphological awareness.

---

## 9. Output format

Produce a single JSON file (suitable to download or for me to copy-paste into a file). Suggested filename: `vocab_import_YYYY-MM-DD.json` using today's date.

Example shape (one entry — your real output will contain one object per new word):

```json
[
  {
    "id": 611,
    "french": "le mot",
    "english": "the word",
    "example": "Ce mot est difficile à prononcer correctement dans un contexte formel.",
    "exampleA2": "Je ne comprends pas ce mot.",
    "pos": "noun (m.)",
    "source": "29 Apr",
    "weeks": "",
    "notes": "",
    "cloze": [
      "Je ne connais pas ce [...] en français.",
      "Quel est le [...] juste pour décrire cette situation ?",
      "Il a trouvé le [...] parfait pour conclure son discours."
    ],
    "clozeA2": [
      "C'est un [...] difficile.",
      "Comment dit-on ce [...] en français ?",
      "Je cherche le bon [...]."
    ],
    "related": [
      "le terme (the term)",
      "la phrase (the sentence)",
      "le vocabulaire (the vocabulary)"
    ],
    "relatedEN": [
      "the term",
      "the sentence",
      "the vocabulary"
    ],
    "etymology": [
      "le langage (the language)",
      "le vocabulaire (the vocabulary)",
      "verbal (verbal)"
    ]
  }
]
```

---

## 10. Quality checklist (verify before delivering)

For each entry:

- [ ] French spelling correct (accents, hyphens, apostrophes)
- [ ] English translation accurate and natural
- [ ] B1–B2 example sentence uses the word correctly and is at level
- [ ] A2 example sentence is meaningfully simpler than the B1–B2 version
- [ ] POS matches one of the values in Section 4
- [ ] All three B1–B2 cloze sentences are unambiguous and use different contexts
- [ ] All three A2 cloze sentences are unambiguous and use different contexts
- [ ] Related words are thematically connected but NOT synonyms
- [ ] `related` entries include English translations in parentheses
- [ ] `relatedEN` corresponds positionally to `related`
- [ ] Etymology entries share a genuine morphological root with the target
- [ ] Each etymology entry includes its English translation in parentheses
- [ ] `weeks` is `""` (blank)
- [ ] No duplicate against the existing vocabulary list in Section 2

---

## 11. Workflow summary

1. I give you a list of French words/phrases plus a `Source` label.
2. If I haven't given you a `Source`, ask for one before going any further.
3. Translate any missing English meanings.
4. Check each word against the existing vocabulary list (Section 2) using normalised matching.
5. For each new (non-duplicate) word, generate:
   - English translation if I didn't supply one
   - B1–B2 example sentence + A2 simplified example
   - 3 B1–B2 cloze sentences + 3 A2 cloze sentences
   - 3 thematically related French words (with English translations) + matching `relatedEN`
   - 3 etymological relatives
   - POS classification from the list in Section 4
   - Sequential ID continuing from the highest ID in the existing list
6. Run the quality checklist (Section 10) on every entry.
7. Produce the JSON file and tell me explicitly which (if any) words you skipped as duplicates.
