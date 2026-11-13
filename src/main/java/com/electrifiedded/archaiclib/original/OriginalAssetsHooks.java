package com.electrifiedded.archaiclib.original;

import java.util.function.Consumer;

/** Side bridge: the client proxy installs a listener that turns resolved requirements into resource packs. */
public final class OriginalAssetsHooks {
    private static Consumer<OriginalModAssets.Requirement> listener = r -> {};

    private OriginalAssetsHooks() {}

    public static void setListener(Consumer<OriginalModAssets.Requirement> l) {
        listener = l;
        for (OriginalModAssets.Requirement r : OriginalModAssets.all()) if (r.isPresent()) l.accept(r);
    }

    static void onRequirementResolved(OriginalModAssets.Requirement r) {
        listener.accept(r);
    }
}
