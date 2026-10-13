package com.cashcompass.app;

/**
 * Pure helpers for the in-app update check. JVM-testable — no Android APIs.
 */
final class UpdateCheck {
    private UpdateCheck() {}

    /**
     * First non-draft release in a {@code GET /repos/{owner}/{repo}/releases}
     * list payload, which GitHub returns newest-first.
     *
     * <p>Prereleases count: OddDough ships every version as a prerelease, so
     * {@code /releases/latest} (which excludes prereleases and drafts) always
     * 404s for this repo — the list endpoint is the correct source.
     *
     * <p>Returns {@code null} for an empty list, an all-draft list, or
     * unparseable input.
     */
    static org.json.JSONObject newestRelease(String listBody) {
        if (listBody == null) return null;
        try {
            org.json.JSONArray arr = new org.json.JSONArray(listBody);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject r = arr.optJSONObject(i);
                if (r != null && !r.optBoolean("draft", false)) return r;
            }
        } catch (org.json.JSONException e) {
            // fall through to null
        }
        return null;
    }
}
