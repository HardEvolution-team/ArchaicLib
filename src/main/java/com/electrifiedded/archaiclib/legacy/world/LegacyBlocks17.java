package com.electrifiedded.archaiclib.legacy.world;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.7.10 {@code net.minecraft.init.Blocks} SRG field name -> 1.12 block (SRG ids are stable, the registry names are
 * taken from the 1.12 Blocks class; renamed 1.7 entries such as fence/wooden_door are mapped by hand). Used to resolve
 * {@code GETSTATIC Blocks.field_xxx} in original (reobfuscated) 1.7 class files.
 */
public final class LegacyBlocks17 {
    private static final String TABLE =
            "field_150318_D=golden_rail field_150319_E=detector_rail field_150320_F=sticky_piston field_150321_G=web field_" +
            "150322_A=sandstone field_150323_B=noteblock field_150324_C=bed field_150325_L=wool field_150327_N=yellow_flowe" +
            "r field_150328_O=red_flower field_150329_H=tallgrass field_150330_I=deadbush field_150331_J=piston field_15033" +
            "2_K=piston_head field_150333_U=stone_slab field_150334_T=double_stone_slab field_150335_W=tnt field_150336_V=b" +
            "rick_block field_150337_Q=red_mushroom field_150338_P=brown_mushroom field_150339_S=iron_block field_150340_R=" +
            "gold_block field_150341_Y=mossy_cobblestone field_150342_X=bookshelf field_150343_Z=obsidian field_150344_f=pl" +
            "anks field_150345_g=sapling field_150346_d=dirt field_150347_e=cobblestone field_150348_b=stone field_150349_c" +
            "=grass field_150350_a=air field_150351_n=gravel field_150352_o=gold_ore field_150353_l=lava field_150354_m=san" +
            "d field_150355_j=water field_150356_k=flowing_lava field_150357_h=bedrock field_150358_i=flowing_water field_1" +
            "50359_w=glass field_150360_v=sponge field_150362_t=leaves field_150364_r=log field_150365_q=coal_ore field_150" +
            "366_p=iron_ore field_150367_z=dispenser field_150368_y=lapis_block field_150369_x=lapis_ore field_150370_cb=qu" +
            "artz_stairs field_150371_ca=quartz_block field_150372_bz=sandstone_stairs field_150373_bw=double_wooden_slab f" +
            "ield_150374_bv=lit_redstone_lamp field_150375_by=cocoa field_150376_bx=wooden_slab field_150377_bs=end_stone f" +
            "ield_150378_br=end_portal_frame field_150379_bu=redstone_lamp field_150380_bt=dragon_egg field_150381_bn=encha" +
            "nting_table field_150382_bo=brewing_stand field_150383_bp=cauldron field_150384_bq=end_portal field_150385_bj=" +
            "nether_brick field_150386_bk=nether_brick_fence field_150387_bl=nether_brick_stairs field_150388_bm=nether_war" +
            "t field_150389_bf=brick_stairs field_150390_bg=stone_brick_stairs field_150391_bh=mycelium field_150392_bi=wat" +
            "erlily field_150393_bb=pumpkin_stem field_150394_bc=melon_stem field_150395_bd=vine field_150397_co=stained_gl" +
            "ass_pane field_150398_cm=double_plant field_150399_cn=stained_glass field_150400_ck=acacia_stairs field_150401" +
            "_cl=dark_oak_stairs field_150402_ci=coal_block field_150403_cj=packed_ice field_150404_cg=carpet field_150405_" +
            "ch=hardened_clay field_150406_ce=stained_hardened_clay field_150407_cf=hay_block field_150408_cc=activator_rai" +
            "l field_150409_cd=dropper field_150410_aZ=glass_pane field_150411_aY=iron_bars field_150412_bA=emerald_ore fie" +
            "ld_150413_aR=unpowered_repeater field_150414_aQ=cake field_150415_aT=trapdoor field_150416_aS=powered_repeater" +
            " field_150417_aV=stonebrick field_150418_aU=monster_egg field_150419_aX=red_mushroom_block field_150420_aW=bro" +
            "wn_mushroom_block field_150421_aI=jukebox field_150423_aK=pumpkin field_150424_aL=netherrack field_150425_aM=s" +
            "oul_sand field_150426_aN=glowstone field_150427_aO=portal field_150428_aP=lit_pumpkin field_150429_aA=redstone" +
            "_torch field_150430_aB=stone_button field_150431_aC=snow_layer field_150432_aD=ice field_150433_aE=snow field_" +
            "150434_aF=cactus field_150435_aG=clay field_150436_aH=reeds field_150437_az=unlit_redstone_torch field_150438_" +
            "bZ=hopper field_150439_ay=lit_redstone_ore field_150440_ba=melon_block field_150441_bU=unpowered_comparator fi" +
            "eld_150442_at=lever field_150443_bT=heavy_weighted_pressure_plate field_150444_as=wall_sign field_150445_bS=li" +
            "ght_weighted_pressure_plate field_150446_ar=stone_stairs field_150447_bR=trapped_chest field_150448_aq=rail fi" +
            "eld_150449_bY=quartz_ore field_150450_ax=redstone_ore field_150451_bX=redstone_block field_150452_aw=wooden_pr" +
            "essure_plate field_150453_bW=daylight_detector field_150454_av=iron_door field_150455_bV=powered_comparator fi" +
            "eld_150456_au=stone_pressure_plate field_150457_bL=flower_pot field_150458_ak=farmland field_150459_bM=carrots" +
            " field_150460_al=furnace field_150461_bJ=beacon field_150462_ai=crafting_table field_150463_bK=cobblestone_wal" +
            "l field_150464_aj=wheat field_150465_bP=skull field_150467_bQ=anvil field_150468_ap=ladder field_150469_bN=pot" +
            "atoes field_150470_am=lit_furnace field_150471_bO=wooden_button field_150472_an=standing_sign field_150473_bD=" +
            "tripwire field_150474_ac=mob_spawner field_150475_bE=emerald_block field_150476_ad=oak_stairs field_150477_bB=" +
            "ender_chest field_150478_aa=torch field_150479_bC=tripwire_hook field_150480_ab=fire field_150481_bH=jungle_st" +
            "airs field_150482_ag=diamond_ore field_150483_bI=command_block field_150484_ah=diamond_block field_150485_bF=s" +
            "pruce_stairs field_150486_ae=chest field_150487_bG=birch_stairs field_150488_af=redstone_wire field_151015_O=w" +
            "heat field_151066_bu=cauldron field_151067_bt=brewing_stand field_151075_bm=nether_wart field_151104_aV=bed fi" +
            "eld_151105_aU=cake field_151120_aE=reeds field_151139_aw=iron_door field_151144_bL=skull field_151162_bE=flowe" +
            "r_pot field_151566_D=dragon_egg field_151567_E=portal field_151568_F=cake field_151569_G=web field_151570_A=ca" +
            "ctus field_151571_B=clay field_151574_g=anvil field_151577_b=grass field_151579_a=air field_151581_o=fire fiel" +
            "d_151582_l=vine field_151583_m=sponge field_151584_j=leaves field_151586_h=water field_151587_i=lava field_151" +
            "588_w=ice field_151590_u=tnt field_151592_s=glass field_151593_r=carpet field_151595_p=sand field_151597_y=sno" +
            "w field_151598_x=packed_ice field_151654_J=obsidian field_151655_K=netherrack field_151656_f=tnt field_151657_" +
            "g=ice field_151658_d=sand field_151660_b=air field_151661_c=grass field_151662_n=water field_151664_l=dirt fie" +
            "ld_151665_m=stone field_151666_j=snow field_151667_k=clay field_154349_a=log field_150422_aJ=fence field_15046" +
            "6_ao=wooden_door field_150396_be=fence_gate field_150363_s=log2";
    private static Map<String, String> map;

    private LegacyBlocks17() {}

    public static synchronized Block bySrg(String srg) {
        if (map == null) {
            map = new HashMap<>();
            for (String e : TABLE.trim().split(" ")) {
                int i = e.indexOf('=');
                if (i > 0) map.put(e.substring(0, i), e.substring(i + 1));
            }
        }
        String name = map.get(srg);
        if (name == null) return null;
        Block b = Block.REGISTRY.getObject(new ResourceLocation(name));
        return b == Blocks.AIR && !name.equals("air") ? null : b;
    }
}
