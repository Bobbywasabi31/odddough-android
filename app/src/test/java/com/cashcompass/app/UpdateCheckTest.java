package com.cashcompass.app;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for UpdateCheck.newestRelease — the /releases list picking logic
 * behind "Check for updates". Pure JSON, no Android framework.
 */
public class UpdateCheckTest {

    private static String release(String tag, boolean draft, boolean prerelease) {
        return "{\"tag_name\":\"" + tag + "\",\"draft\":" + draft
            + ",\"prerelease\":" + prerelease + ",\"name\":\"" + tag + "\"}";
    }

    @Test public void picksFirstEntryWhenItIsAPrerelease() {
        // OddDough's normal case: every published version is a prerelease.
        String body = "[" + release("v1.64.0-preview.101", false, true) + ","
            + release("v1.64.0-preview.100", false, true) + "]";
        JSONObject r = UpdateCheck.newestRelease(body);
        assertEquals("v1.64.0-preview.101", r.optString("tag_name"));
    }

    @Test public void skipsDraftsAtTheTop() {
        String body = "[" + release("v9.9.9-draft", true, false) + ","
            + release("v1.64.0-preview.101", false, true) + "]";
        JSONObject r = UpdateCheck.newestRelease(body);
        assertEquals("v1.64.0-preview.101", r.optString("tag_name"));
    }

    @Test public void treatsMissingDraftFlagAsPublished() {
        JSONObject r = UpdateCheck.newestRelease("[{\"tag_name\":\"v1.0.0\"}]");
        assertEquals("v1.0.0", r.optString("tag_name"));
    }

    @Test public void returnsNullForEmptyList() {
        assertNull(UpdateCheck.newestRelease("[]"));
    }

    @Test public void returnsNullWhenEverythingIsADraft() {
        String body = "[" + release("v9.9.9-draft", true, false) + "]";
        assertNull(UpdateCheck.newestRelease(body));
    }

    @Test public void returnsNullForGarbageInput() {
        assertNull(UpdateCheck.newestRelease("not json"));
        assertNull(UpdateCheck.newestRelease(""));
        assertNull(UpdateCheck.newestRelease(null));
    }

    @Test public void returnsNullForSingleObjectInsteadOfList() {
        // A /releases/latest-shaped payload is not a list; never misread it.
        assertNull(UpdateCheck.newestRelease(release("v1.64.0-preview.101", false, true)));
    }
}
