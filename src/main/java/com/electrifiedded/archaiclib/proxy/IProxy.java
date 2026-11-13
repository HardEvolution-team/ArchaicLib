package com.electrifiedded.archaiclib.proxy;

public interface IProxy {
    void initLegacyRendering();

    /** FMLConstructionEvent: runs before the client's first (and in 1.12 only) resource reload. */
    default void construct() {}
}
