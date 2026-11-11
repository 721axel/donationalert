package com.donationalerts;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import org.joml.Matrix3x2fStack;

import java.util.List;

/** The top-donors panel. PODIUM = top 3 on a podium with heads and a crown, LIST = plain top-N list. */
public final class LeaderboardHud {
    private static final int GREEN = 0xFF3DFF6E;
    private static final int GOLD = 0xFFD700;
    private static final int SILVER = 0xC8C8D0;
    private static final int BRONZE = 0xCD7F32;

    private LeaderboardHud() {}

    public static void render(DrawContext ctx) {
        Config c = Config.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!c.leaderboardVisible || mc.options.hudHidden) return;
        if ("LIST".equalsIgnoreCase(c.leaderboardStyle)) list(ctx, mc, c);
        else podium(ctx, mc, c);
    }

    // ------------------------------------------------------------------ podium

    private static void podium(DrawContext ctx, MinecraftClient mc, Config c) {
        TextRenderer tr = mc.textRenderer;
        List<DonationStore.Entry> top = DonationStore.top(3);
        boolean single = "SINGLE".equalsIgnoreCase(c.leaderboardMode);
        String header = single ? "BIGGEST DONATIONS" : "TOP DONATORS";

        final int colW = 46;
        final int gap = 3;
        final int pad = 6;
        final int titleH = 15;
        final int[] blockH = {30, 22, 15};
        final int[] headSz = {20, 16, 16};
        final int[] blockTop = {0xFFF7D36B, 0xFFE0E3EA, 0xFFE59C58};
        final int[] blockMid = {0xFFE0A93A, 0xFFB8BCC4, 0xFFC47A3B};
        final int[] amountRgb = {0xFFC83D, 0xFFFFFF, 0xE0903A};
        final int[] order = {1, 0, 2}; // left = 2nd, middle = 1st, right = 3rd

        int width = pad * 2 + colW * 3 + gap * 2;
        int contentH = blockH[0] + 2 + 9 + 1 + 9 + 2 + headSz[0] + 9;
        int height = titleH + 4 + contentH + 5;

        float sc = Math.max(0.5f, c.leaderboardScale);
        int sw = (int) (ctx.getScaledWindowWidth() / sc);
        int x = "TOP_RIGHT".equalsIgnoreCase(c.leaderboardAnchor) ? sw - width - 4 : 4;
        int y = (int) (c.leaderboardOffsetY / sc);

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(sc, sc);

        // panel + title
        ctx.fill(x, y, x + width, y + height, 0xC0050A06);
        Gfx.border(ctx, x, y, width, height, GREEN);
        Gfx.border(ctx, x + 1, y + 1, width - 2, height - 2, 0x663DFF6E);
        ctx.fill(x + 1, y + titleH, x + width - 1, y + titleH + 1, 0xFF1E7A38);
        m.pushMatrix();
        m.translate(x + width / 2f, y + 3f);
        m.scale(1.15f, 1.15f);
        ctx.drawText(tr, header, -tr.getWidth(header) / 2, 0, GREEN, true);
        m.popMatrix();

        long since = System.currentTimeMillis() - DonationStore.lastChangeMs;
        int baseY = y + titleH + 4 + contentH;

        for (int i = 0; i < 3; i++) {
            int r = order[i];
            int colX = x + pad + i * (colW + gap);
            int cx = colX + colW / 2;
            int top0 = baseY - blockH[r];
            boolean has = r < top.size();
            DonationStore.Entry e = has ? top.get(r) : null;

            // glow behind the donor who just donated
            if (has && since < 3000 && e.name().equalsIgnoreCase(DonationStore.lastChangedName)) {
                float pulse = (float) (0.5 + 0.5 * Math.sin(since / 110.0));
                int a = (int) ((1f - since / 3000f) * 80 * (0.4f + 0.6f * pulse));
                ctx.fill(colX, y + titleH + 2, colX + colW, baseY, (Math.max(a, 4) << 24) | 0x55FF55);
            }

            // podium block with the rank number
            ctx.fill(colX + 3, top0, colX + colW - 3, baseY, blockMid[r]);
            ctx.fill(colX + 3, top0, colX + colW - 3, top0 + 2, blockTop[r]);
            ctx.fill(colX + 3, baseY - 2, colX + colW - 3, baseY, 0x33000000);
            drawCentered(ctx, tr, String.valueOf(r + 1), cx, top0 + (blockH[r] - 8) / 2f - (r == 0 ? 1f : 0f),
                0xFFFFFFFF, r == 0 ? 1.5f : 1.2f, true);

            if (!has) {
                drawCentered(ctx, tr, "---", cx, top0 - 2 - 9 - 1 - 9, 0xFF6A6A6A, 1f, false);
                continue;
            }

            int amountY = top0 - 2 - 9;
            int nameY = amountY - 1 - 9;
            int hs = headSz[r];
            int headY = nameY - 2 - hs;

            String amt = Money.format(e.amount());
            float aSc = Math.min(1f, (colW - 2f) / Math.max(1, tr.getWidth(amt)));
            drawCentered(ctx, tr, amt, cx, amountY, 0xFF000000 | amountRgb[r], aSc, true);

            String name = fit(tr, e.name(), colW - 2);
            drawCentered(ctx, tr, name, cx, nameY, 0xFFFFFFFF, 1f, true);

            Gfx.border(ctx, cx - hs / 2 - 1, headY - 1, hs + 2, hs + 2, 0xFF000000 | (r == 0 ? GOLD : r == 1 ? SILVER : BRONZE));
            PlayerSkinDrawer.draw(ctx, Gfx.skinFor(e.name()), cx - hs / 2, headY, hs);

            if (r == 0) {
                float bob = (float) Math.sin(System.currentTimeMillis() / 350.0) * 0.6f;
                Gfx.crown(ctx, cx, headY + 3 + bob, 0.5f);
            }
        }
        m.popMatrix();
    }

    /** Trims a name and adds ".." if it doesn't fit. */
    private static String fit(TextRenderer tr, String s, int maxW) {
        if (tr.getWidth(s) <= maxW) return s;
        int dots = tr.getWidth("..");
        String t = s;
        while (t.length() > 1 && tr.getWidth(t) + dots > maxW) t = t.substring(0, t.length() - 1);
        return t + "..";
    }

    private static void drawCentered(DrawContext ctx, TextRenderer tr, String s, float cx, float y, int argb, float sc, boolean shadow) {
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, y);
        m.scale(sc, sc);
        ctx.drawText(tr, s, -tr.getWidth(s) / 2, 0, argb, shadow);
        m.popMatrix();
    }

    // ------------------------------------------------------------------ plain list

    private static void list(DrawContext ctx, MinecraftClient mc, Config c) {
        TextRenderer tr = mc.textRenderer;
        List<DonationStore.Entry> top = DonationStore.top(Math.max(1, c.leaderboardSize));
        boolean single = "SINGLE".equalsIgnoreCase(c.leaderboardMode);
        String header = single ? "BIGGEST DONATIONS" : "TOP DONATORS";

        int nameW = 0;
        int amountW = 0;
        for (DonationStore.Entry e : top) {
            nameW = Math.max(nameW, tr.getWidth(e.name()));
            amountW = Math.max(amountW, tr.getWidth(Money.format(e.amount())));
        }
        int rowW = 14 + nameW + 10 + amountW;
        int width = Math.max(tr.getWidth(header), top.isEmpty() ? tr.getWidth("No donations yet") : rowW) + 12;
        int rowH = 11;
        int height = 16 + Math.max(1, top.size()) * rowH + 3;

        float sc = Math.max(0.5f, c.leaderboardScale);
        int sw = (int) (ctx.getScaledWindowWidth() / sc);
        int x = "TOP_LEFT".equalsIgnoreCase(c.leaderboardAnchor) ? 4 : sw - width - 4;
        int y = (int) (c.leaderboardOffsetY / sc);

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(sc, sc);

        ctx.fill(x, y, x + width, y + height, 0xC0050A06);
        Gfx.border(ctx, x, y, width, height, GREEN);
        ctx.fill(x + 1, y + 14, x + width - 1, y + 15, 0xFF1E7A38);
        ctx.drawText(tr, header, x + (width - tr.getWidth(header)) / 2, y + 4, GREEN, true);

        long sinceChange = System.currentTimeMillis() - DonationStore.lastChangeMs;
        if (top.isEmpty()) {
            String s = "No donations yet";
            ctx.drawText(tr, s, x + (width - tr.getWidth(s)) / 2, y + 18, 0xFF9A9A9A, false);
        }
        for (int i = 0; i < top.size(); i++) {
            DonationStore.Entry e = top.get(i);
            int ry = y + 18 + i * rowH;
            int rgb = i == 0 ? GOLD : i == 1 ? SILVER : i == 2 ? BRONZE : 0xFFFFFF;

            if (sinceChange < 2500 && e.name().equalsIgnoreCase(DonationStore.lastChangedName)) {
                float pulse = (float) (0.5 + 0.5 * Math.sin(sinceChange / 120.0));
                int a = (int) ((1f - sinceChange / 2500f) * 90 * (0.4f + 0.6f * pulse));
                ctx.fill(x + 2, ry - 2, x + width - 2, ry + 9, (Math.max(a, 4) << 24) | 0x55FF55);
            }

            ctx.drawText(tr, (i + 1) + ".", x + 5, ry, 0xFF000000 | rgb, false);
            ctx.drawText(tr, e.name(), x + 14, ry, 0xFF000000 | rgb, true);
            String amt = Money.format(e.amount());
            ctx.drawText(tr, amt, x + width - 5 - tr.getWidth(amt), ry, 0xFF55FF55, true);
        }
        m.popMatrix();
    }
}
