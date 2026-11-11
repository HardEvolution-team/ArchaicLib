# ArchaicLib

Shared compatibility library for ported old-version Minecraft mods. It brings legacy Minecraft APIs (1.7.10 and 1.8.9 style) to Minecraft 1.12.2 running on Cleanroom Loader.

Version: 1.3.0 · Minecraft: 1.12.2 · Loader: Cleanroom only

## What it is

ArchaicLib is a library, not a standalone mod. Ported mods depend on it so they can keep using the APIs they were written against, instead of being rewritten for 1.12.2 each time.

It is also a coremod: it ships an access transformer and a loading plugin, and it applies a Mixin to `Minecraft`.

## What it provides

**1.7.10 compatibility (`v1710/api`, `v1710/client`)**
- `ArchaicBlock`, `ArchaicItem`: base classes with the legacy block/item behaviour.
- `IArchaicRenderBlock`, `ArchaicRenderPart`: custom block rendering with multiple render parts.
- `IItemRenderer`, `ArchaicMinecraftForgeClient`: legacy item renderer hooks.
- `IIcon`, `IIconRegister`, `ArchaicIcon`: legacy icon registration.
- `ArchaicChannel`, `AbstractPacket`: legacy network packet channel.
- `LegacyEntityMover`: legacy entity movement helper.
- `ArchaicTEISR`, `ArchaicBakedModel`, `ArchaicModelLoader`: item/tile-entity rendering and model loading for legacy models.

**1.8.9 compatibility (`v189`)**
- `LegacyModelConverter189`: converts 1.8.9-format JSON models into the 1.12.2 format.
- `LegacyModelResourcePack189`: resource pack wrapper that applies the conversion.
- `LegacyModelHooks189`: hooks that connect the converter to model loading.

**Legacy runtime helpers (`legacy/`)**
- `entity/`: extended entity properties, a legacy `DataWatcher`, legacy entity list, inventory, and throwable base class.
- `registry/`: `LegacyGameRegistry` for registering blocks, items, item models and entities with the old-style calls, plus `LegacyRecipes`.
- `sound/`: `LegacySounds` for legacy sound registration.
- `client/`: `LegacyTessellator`, `LegacySmartItemModel`, `LegacyModelRegistrar`.

**Core**
- `ModMixinConfigPlugin`: controls which Mixins are applied.
- `mixin/MinecraftMixin`: client-side Mixin into `Minecraft`.
- `proxy/`: client and common proxies.
- `ExampleLoadingPlugin`: coremod entry point, referenced from `gradle.properties`.

## Requirements

- Minecraft 1.12.2
- Cleanroom Loader
- Java 25 (the Mixin config uses `JAVA_25` compatibility)

## Using it from another mod

Add ArchaicLib as a dependency in your mod's build, and list the ArchaicLib version your mod was built against. Mods that depend on ArchaicLib need it installed at runtime.

## Building

```
./gradlew build
```

Run the client or server through the Gradle run tasks. Use `genSources` if you need vanilla sources.

## Status

- Builds and runs as a library. Used by ported mods in this workspace.
- `ExampleMod` and the `java-templates` `Reference` class are leftovers from the Cleanroom template and are not part of the library API.

## License

All rights reserved. The source is publicly visible, but it may not be copied, modified, redistributed or reused without the author's written permission. See [LICENSE](LICENSE).

## Links

- Discord: https://discord.gg/h8Se4W3BFa
- Author: electrifiedded
