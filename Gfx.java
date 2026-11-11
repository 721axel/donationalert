package com.donationalerts;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Uuids;
import org.joml.Matrix3x2fStack;

import java.util.HashMap;
import java.util.Map;

/** Small drawing helpers shared by the alerts and the leaderboard. */
final class Gfx {
    private Gfx() {}

    /** 1px rectangle outline drawn with fills. */
    static void border(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x, y, x + w, y + 1, color);
        c.fill(x, y + h - 1, x + w, y + h, color);
        c.fill(x, y + 1, x + 1, y + h - 1, color);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Pixel-art crown. (cx, yBottom) is the middle of its bottom edge; s scales it (1 = 40px wide). */
    static void crown(DrawContext ctx, float cx, float yBottom, float s) {
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, yBottom);
        m.scale(s, s);
        int gold = 0xFFFFD23F;
        int dark = 0xFFC48A00;
        ctx.fill(-20, -7, 20, 0, gold);
        ctx.fill(-20, -2, 20, 0, dark);
        ctx.fill(-20, -17, -13, -7, gold);
        ctx.fill(-4, -22, 4, -7, gold);
        ctx.fill(13, -17, 20, -7, gold);
        ctx.fill(-12, -11, -4, -7, gold);
        ctx.fill(4, -11, 12, -7, gold);
        ctx.fill(-18, -20, -15, -17, 0xFFFFFFFF);
        ctx.fill(-2, -25, 2, -22, 0xFFFFFFFF);
        ctx.fill(15, -20, 18, -17, 0xFFFFFFFF);
        ctx.fill(-2, -5, 2, -2, 0xFFFF4D6D);
        m.popMatrix();
    }

    // ---- skins: real skin if the player is in the tab list (remembered for the session), otherwise a default skin
    private record Cached(SkinTextures skin, long ts, boolean real) {}

    private static final Map<String, Cached> CACHE = new HashMap<>();

    static SkinTextures skinFor(String name) {
        long now = System.currentTimeMillis();
        Cached c = CACHE.get(name);
        if (c != null && now - c.ts() < 5000) return c.skin();

        ClientPlayNetworkHandler nh = MinecraftClient.getInstance().getNetworkHandler();
        PlayerListEntry e = nh == null ? null : nh.getPlayerListEntry(name);
        if (e != null) {
            c = new Cached(e.getSkinTextures(), now, true);
        } else if (c != null && c.real()) {
            c = new Cached(c.skin(), now, true);
        } else {
            c = new Cached(DefaultSkinHelper.getSkinTextures(Uuids.getOfflinePlayerUuid(name)), now, false);
        }
        CACHE.put(name, c);
        return c.skin();
    }
}
