package com.electrifiedded.archaiclib.proxy;

import com.electrifiedded.archaiclib.ExampleMod;
import com.electrifiedded.archaiclib.v189.LegacyModelHooks189;

public class ClientProxy implements IProxy {
    @Override
    public void construct() {
        com.electrifiedded.archaiclib.client.resources.ArchaicPackInstaller.fixModPackFormats();
        com.electrifiedded.archaiclib.original.client.OriginalAssetsClient.install();
        com.electrifiedded.archaiclib.compat.VintageFixCompat.install();
    }

    @Override
    public void initLegacyRendering() {
        int wrapped = LegacyModelHooks189.install();
        ExampleMod.LOGGER.info("v189 legacy model conversion installed on {} resource pack(s)", wrapped);
    }
}
