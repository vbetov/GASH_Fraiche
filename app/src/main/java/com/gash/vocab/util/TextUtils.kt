package com.gash.vocab.util

import java.text.Normalizer

/**
 * Normalises a French string for deduplication:
 * 1. Unicode NFC normalisation
 * 2. Replace curly apostrophes with straight
 * 3. Lowercase
 */
fun normaliseFrench(text: String): String {
    val nfc = Normalizer.normalize(text, Normalizer.Form.NFC)
    return nfc.replace('\u2019', '\'')  // right single quotation mark → apostrophe
        .replace('\u2018', '\'')         // left single quotation mark → apostrophe
        .lowercase()
}
