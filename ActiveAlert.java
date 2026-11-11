package com.donationalerts;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;

import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * One donation alert being played. The timeline is: [intro, tiers GODLIKE and up only] then [main banner].
 * Everything is drawn with DrawContext primitives, so there are no texture or sound files to ship.
 */
final class ActiveAlert {
    private static final String CHARS = "0123456789ABCDEF#$%&@";
    private static final String[] GLYPH = new String[CHARS.length()];
    private static final int[] CONFETTI = {0xFFD700, 0xFF4DFF, 0x4DFFFF, 0x7CFF4D, 0xFFFFFF, 0xFF7A4D};

    static {
        for (int i = 0; i < CHARS.length(); i++) GLYPH[i] = String.valueOf(CHARS.charAt(i));
    }

    private static final class P {
        float x, y, vx, vy, life;
        int rgb, size;
    }

    private final Donation donation;
    private final Tier tier;
    private final SkinTextures skin;
    private final Random rnd = new Random();
    private final List<P> particles = new ArrayList<>();
    private final long startNs = System.nanoTime();
    private long lastNs = startNs;
    private boolean revealed;
    private int stage;
    private float confettiAcc;

    ActiveAlert(Donation donation) {
        this.donation = donation;
        this.tier = Tier.forAmount(donation.amount);
        this.skin = Gfx.skinFor(donation.name);
    }

    // ------------------------------------------------------------------ helpers

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    private static float easeOutCubic(float x) {
        float f = 1f - x;
        return 1f - f * f * f;
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float f = x - 1f;
        return 1f + c3 * f * f * f + c1 * f * f;
    }

    private static int argb(int rgb, float a) {
        int al = Math.round(clamp01(a) * 255f);
        return (Math.max(al, 8) << 24) | (rgb & 0xFFFFFF);
    }

    private static void play(SoundEvent s, float pitch, float volume) {
        if (!Config.get().playSounds) return;
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p != null) p.playSound(s, volume, pitch);
    }

    /** Some vanilla sounds are registry entries in newer versions; this overload handles those. */
    private static void play(RegistryEntry<SoundEvent> s, float pitch, float volume) {
        play(s.value(), pitch, volume);
    }

    private void centered(DrawContext ctx, TextRenderer tr, String s, float cx, float y, int rgb, float a, float sc, boolean shadow) {
        if (a < 0.03f) return;
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, y);
        m.scale(sc, sc);
        ctx.drawText(tr, s, -tr.getWidth(s) / 2, 0, argb(rgb, a), shadow);
        m.popMatrix();
    }

    // ------------------------------------------------------------------ main entry

    /** Returns false when the alert is finished. */
    boolean render(DrawContext ctx, MinecraftClient mc) {
        long now = System.nanoTime();
        float t = (now - startNs) / 1_000_000f;
        float dt = Math.min((now - lastNs) / 1_000_000f, 50f);
        lastNs = now;

        float intro = tier.introMs;
        if (t >= intro + tier.mainMs) return false;

        int W = ctx.getScaledWindowWidth();
        int H = ctx.getScaledWindowHeight();
        TextRenderer tr = mc.textRenderer;
        Matrix3x2fStack m = ctx.getMatrices();

        m.pushMatrix();
        if (t < intro) {
            if (tier == Tier.GODLIKE) hackIntro(ctx, tr, W, H, t);
            else universeIntro(ctx, tr, W, H, t);
        } else {
            float mt = t - intro;
            if (!revealed) reveal(W, H);
            if (Config.get().screenShake && tier.atLeast(Tier.HUGE) && mt < 700f) {
                float k = 1f - mt / 700f;
                m.translate((rnd.nextFloat() - 0.5f) * 8f * k, (rnd.nextFloat() - 0.5f) * 8f * k);
            }
            main(ctx, tr, W, H, mt, dt);
        }
        m.popMatrix();
        return true;
    }

    private void reveal(int W, int H) {
        revealed = true;
        DonationStore.add(donation);
        switch (tier) {
            case COMMON -> play(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.8f);
            case NICE -> play(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 0.7f);
            case BIG -> {
                play(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.3f, 0.8f);
                play(SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.6f);
            }
            case HUGE -> {
                play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
                play(SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 0.9f, 0.8f);
            }
            case LEGENDARY -> {
                play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.0f);
                play(SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.0f);
                play(SoundEvents.BLOCK_BEACON_ACTIVATE, 1.2f, 0.8f);
            }
            case GODLIKE -> {
                play(SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
                play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
                play(SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 0.7f, 1.0f);
            }
            case UNIVERSE -> {
                play(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.0f);
                play(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.6f);
                play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
                play(SoundEvents.BLOCK_BEACON_ACTIVATE, 0.8f, 1.0f);
            }
        }
        int burst = switch (tier) {
            case COMMON -> 0;
            case NICE -> 20;
            case BIG -> 50;
            case HUGE -> 90;
            case LEGENDARY -> 140;
            case GODLIKE -> 200;
            case UNIVERSE -> 300;
        };
        burst(W / 2f, H * 0.28f, burst);
    }

    // ------------------------------------------------------------------ intros

    /** GODLIKE: fake "hacking the mainframe" terminal over falling code. */
    private void hackIntro(DrawContext ctx, TextRenderer tr, int W, int H, float t) {
        float bg = clamp01(t / 300f);
        ctx.fill(0, 0, W, H, argb(0x000000, 0.88f * bg));
        matrixRain(ctx, tr, W, H, t, 0.85f * bg);

        int tw = 270;
        int th = 74;
        int x0 = W / 2 - tw / 2;
        int y0 = H / 2 - th / 2 - 20;

        if (t < 3500f) {
            ctx.fill(x0, y0, x0 + tw, y0 + th, 0xE6021402);
            Gfx.border(ctx, x0, y0, tw, th, 0xFF00FF41);
            ctx.drawText(tr, "donut@mainframe:~", x0 + 6, y0 + 4, 0xFF00B02F, false);
            ctx.fill(x0 + 1, y0 + 14, x0 + tw - 1, y0 + 15, 0x6600FF41);

            String l1 = "> connecting to DonutSMP mainframe...";
            int n1 = (int) (l1.length() * clamp01(t / 1200f));
            ctx.drawText(tr, l1.substring(0, n1), x0 + 8, y0 + 20, 0xFF7CFF9A, false);

            if (t > 1300f) {
                String l2 = "> bypassing firewall...";
                int n2 = (int) (l2.length() * clamp01((t - 1300f) / 1000f));
                boolean cursor = ((int) (t / 250f) & 1) == 0 && t < 2400f;
                ctx.drawText(tr, l2.substring(0, n2) + (cursor ? "_" : ""), x0 + 8, y0 + 32, 0xFF7CFF9A, false);
            }
            if (t > 2400f) {
                float p = clamp01((t - 2400f) / 1100f);
                int bx = x0 + 8;
                int by = y0 + 46;
                int bw = tw - 16;
                Gfx.border(ctx, bx, by, bw, 10, 0xFF00FF41);
                ctx.fill(bx + 2, by + 2, bx + 2 + (int) ((bw - 4) * p), by + 8, 0xFF00FF41);
                String pct = (int) (p * 100) + "%";
                ctx.drawText(tr, pct, bx + bw - tr.getWidth(pct), by + 13, 0xFF7CFF9A, false);
            }
        } else {
            if ((stage & 1) == 0) {
                stage |= 1;
                play(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.0f);
            }
            float gt = t - 3500f;
            float jitter = gt < 450f ? 3f : 0.8f;
            float cx = W / 2f;
            float cy = H / 2f - 30f;
            // glitchy RGB split
            centered(ctx, tr, "ACCESS GRANTED", cx - jitter + (rnd.nextFloat() - 0.5f) * jitter, cy, 0xFF2040, 0.8f, 3f, false);
            centered(ctx, tr, "ACCESS GRANTED", cx + jitter + (rnd.nextFloat() - 0.5f) * jitter, cy, 0x20FFFF, 0.8f, 3f, false);
            centered(ctx, tr, "ACCESS GRANTED", cx, cy, 0x3DFF6E, 1f, 3f, false);
            // scanline glitch bars
            for (int i = 0; i < 4; i++) {
                if (rnd.nextFloat() < 0.5f) {
                    int gy = rnd.nextInt(H);
                    ctx.fill(0, gy, W, gy + 1 + rnd.nextInt(3), argb(0x00FF66, 0.18f));
                }
            }
            int bw = 270;
            int bx = W / 2 - bw / 2;
            int by = (int) cy + 36;
            Gfx.border(ctx, bx, by, bw, 10, 0xFF00FF41);
            ctx.fill(bx + 2, by + 2, bx + bw - 2, by + 8, 0xFF00FF41);
        }
    }

    /** UNIVERSE: "In the beginning, there was nothing... then NAME donated." */
    private void universeIntro(DrawContext ctx, TextRenderer tr, int W, int H, float t) {
        ctx.fill(0, 0, W, H, argb(0x000000, clamp01(t / 800f)));
        float cy = H * 0.42f;

        float a1 = clamp01((t - 900f) / 1300f) * (1f - clamp01((t - 3800f) / 800f));
        centered(ctx, tr, "In the beginning, there was nothing...", W / 2f, cy, 0xB8B8B8, a1, 1f, false);

        if (t > 2200f && t < 7600f) {
            float pulse = 0.6f + 0.4f * (float) Math.sin(t / 180f);
            float a = clamp01((t - 2200f) / 600f) * pulse * (1f - clamp01((t - 7000f) / 500f));
            ctx.fill(W / 2 - 3, H / 2 - 3, W / 2 + 3, H / 2 + 3, argb(0xFFFFFF, a * 0.15f));
            ctx.fill(W / 2 - 1, H / 2 - 1, W / 2 + 1, H / 2 + 1, argb(0xFFFFFF, a));
        }

        if (t > 4800f) {
            if ((stage & 2) == 0) {
                stage |= 2;
                play(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.5f);
            }
            float a2 = clamp01((t - 4800f) / 900f) * (1f - clamp01((t - 7300f) / 600f));
            String partA = "...then ";
            String partB = " donated.";
            float total = tr.getWidth(partA) + tr.getWidth(donation.name) + tr.getWidth(partB);
            float sc = 1.4f;
            if (a2 > 0.03f) {
                Matrix3x2fStack m = ctx.getMatrices();
                m.pushMatrix();
                m.translate(W / 2f - total * sc / 2f, cy);
                m.scale(sc, sc);
                int x = 0;
                ctx.drawText(tr, partA, x, 0, argb(0xFFFFFF, a2), false);
                x += tr.getWidth(partA);
                ctx.drawText(tr, donation.name, x, 0, argb(0xFFAA00, a2), false);
                x += tr.getWidth(donation.name);
                ctx.drawText(tr, partB, x, 0, argb(0xFFFFFF, a2), false);
                m.popMatrix();
            }
            pinwheel(ctx, W / 2f, H / 2f + 38f, t, a2);
        }
    }

    private void pinwheel(DrawContext ctx, float cx, float cy, float t, float a) {
        if (a < 0.03f) return;
        Matrix3x2fStack m = ctx.getMatrices();
        for (int i = 0; i < 4; i++) {
            m.pushMatrix();
            m.translate(cx, cy);
            m.rotate((float) Math.toRadians(t * (0.03f + 0.02f * i) * (i % 2 == 0 ? 1f : -1f)));
            int s = 8 + i * 6;
            Gfx.border(ctx, -s, -s, s * 2, s * 2, argb(i % 2 == 0 ? 0xFF8FB5 : 0xC78BFF, a * 0.8f));
            m.popMatrix();
        }
    }

    // ------------------------------------------------------------------ main banner

    private void main(DrawContext ctx, TextRenderer tr, int W, int H, float mt, float dt) {
        float fadeOut = clamp01((tier.mainMs - mt) / 500f);
        float pop = easeOutBack(clamp01(mt / 350f)) * fadeOut;
        float cx = W / 2f;
        float cy = H * 0.28f;

        // dim the world behind the big tiers
        if (tier.atLeast(Tier.LEGENDARY)) {
            int tint = tier == Tier.GODLIKE ? 0x001A08 : tier == Tier.UNIVERSE ? 0x1A0F00 : 0x000000;
            ctx.fill(0, 0, W, H, argb(tint, 0.38f * clamp01(mt / 400f) * fadeOut));
        }
        if (tier == Tier.GODLIKE) matrixRain(ctx, tr, W, H, mt, 0.28f * fadeOut);
        if (tier == Tier.UNIVERSE) ring(ctx, cx, cy, mt, fadeOut);
        if (tier.atLeast(Tier.BIG)) {
            int rayRgb = tier == Tier.GODLIKE ? 0x3DFF6E : tier == Tier.UNIVERSE ? 0xFFD060 : tier.color;
            int rays = tier.atLeast(Tier.GODLIKE) ? 24 : 14;
            rays(ctx, cx, cy, mt, rayRgb, 0.16f * clamp01(mt / 500f) * fadeOut, rays, Math.max(W, H));
        }

        // banner
        float bw = 240f;
        float bh = 76f;
        float base = switch (tier) {
            case COMMON -> 0.8f;
            case NICE -> 0.9f;
            case BIG -> 1.0f;
            case HUGE -> 1.1f;
            case LEGENDARY -> 1.2f;
            case GODLIKE -> 1.3f;
            case UNIVERSE -> 1.4f;
        };
        base = Math.min(base, (W * 0.92f) / bw);

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, cy);
        m.scale(base * pop, base * pop);
        if (pop > 0.02f) banner(ctx, tr, mt, bw, bh);
        m.popMatrix();

        // particles
        float rate = switch (tier) {
            case GODLIKE -> 0.05f;
            case UNIVERSE -> 0.07f;
            case LEGENDARY -> 0.03f;
            default -> 0f;
        };
        if (rate > 0f && mt < tier.mainMs - 800f) {
            confettiAcc += dt * rate;
            while (confettiAcc >= 1f) {
                confettiAcc -= 1f;
                P p = newParticle();
                p.x = rnd.nextFloat() * W;
                p.y = -4f;
                p.vx = (rnd.nextFloat() - 0.5f) * 0.05f;
                p.vy = 0.05f + rnd.nextFloat() * 0.1f;
                p.life = 4000f;
            }
        }
        particles(ctx, dt, H);

        // white flash after the universe intro
        if (tier == Tier.UNIVERSE && mt < 900f) {
            ctx.fill(0, 0, W, H, argb(0xFFFFFF, 1f - mt / 900f));
        }
    }

    private void banner(DrawContext ctx, TextRenderer tr, float mt, float bw, float bh) {
        int hx = (int) (bw / 2f);
        int hy = (int) (bh / 2f);
        int bg = tier == Tier.GODLIKE ? 0xE0021A08 : tier == Tier.UNIVERSE ? 0xE0100818 : 0xD0080810;
        ctx.fill(-hx, -hy, hx, hy, bg);

        float pulse = 0.6f + 0.4f * (float) Math.sin(mt / 160f);
        Gfx.border(ctx, -hx, -hy, (int) bw, (int) bh, 0xFF000000 | tier.color);
        Gfx.border(ctx, -hx + 3, -hy + 3, (int) bw - 6, (int) bh - 6, argb(tier.color, 0.5f + 0.5f * pulse));

        if (tier == Tier.UNIVERSE) crown(ctx, -hy - 4 + (float) Math.sin(mt / 300f) * 1.5f);

        // title, one letter at a time so it can wave and change colour
        String title = tier.title;
        float titleW = tr.getWidth(title);
        float ts = Math.min(2f, (bw - 20f) / titleW);
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(0f, -hy + 8f);
        m.scale(ts, ts);
        float x = -titleW / 2f;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            float wave = tier.atLeast(Tier.LEGENDARY) ? (float) Math.sin(mt / 140f + i * 0.5f) * 1.5f : 0f;
            ctx.drawText(tr, ch, Math.round(x), Math.round(wave), 0xFF000000 | titleColor(i, mt), true);
            x += tr.getWidth(ch);
        }
        m.popMatrix();

        // head + name
        String name = donation.name;
        float nameSc = 1.25f;
        float rowW = 16f + 4f + tr.getWidth(name) * nameSc;
        float rx = -rowW / 2f;
        PlayerSkinDrawer.draw(ctx, skin, Math.round(rx), -9, 16);
        m.pushMatrix();
        m.translate(rx + 20f, -9f + 3f);
        m.scale(nameSc, nameSc);
        ctx.drawText(tr, name, 0, 0, 0xFF000000 | nameColor(), true);
        m.popMatrix();

        // amount, counting up
        float p = easeOutCubic(clamp01(mt / 1400f));
        String amount = p >= 1f ? Money.format(donation.amount) : Money.formatIn(donation.amount * p, donation.amount);
        int amountRgb = tier == Tier.UNIVERSE ? 0xFFD060 : tier.atLeast(Tier.LEGENDARY) ? 0xFFF0A0 : 0xFFFFFF;
        m.pushMatrix();
        m.translate(0f, 12f);
        m.scale(2.2f, 2.2f);
        ctx.drawText(tr, amount, -tr.getWidth(amount) / 2, 0, 0xFF000000 | amountRgb, true);
        m.popMatrix();
    }

    private int titleColor(int i, float mt) {
        if (tier == Tier.GODLIKE || tier == Tier.UNIVERSE) {
            return MathHelper.hsvToRgb(((mt / 1200f) + i * 0.045f) % 1f, 0.55f, 1f);
        }
        if (tier == Tier.LEGENDARY) {
            float k = 0.5f + 0.5f * (float) Math.sin(mt / 200f - i * 0.4f);
            return lerpRgb(0xFFD23F, 0xFFFFFF, k);
        }
        return tier.color;
    }

    private int nameColor() {
        return switch (tier) {
            case COMMON -> 0xFFFFFF;
            case UNIVERSE -> 0xFF9AFF;
            default -> lerpRgb(tier.color, 0xFFFFFF, 0.5f);
        };
    }

    private static int lerpRgb(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private void crown(DrawContext ctx, float yBottom) {
        int yb = Math.round(yBottom);
        int gold = 0xFFFFD23F;
        int dark = 0xFFC48A00;
        ctx.fill(-20, yb - 7, 20, yb, gold);          // band
        ctx.fill(-20, yb - 2, 20, yb, dark);
        ctx.fill(-20, yb - 17, -13, yb - 7, gold);    // left prong
        ctx.fill(-4, yb - 22, 4, yb - 7, gold);       // middle prong
        ctx.fill(13, yb - 17, 20, yb - 7, gold);      // right prong
        ctx.fill(-12, yb - 11, -4, yb - 7, gold);     // fill between prongs
        ctx.fill(4, yb - 11, 12, yb - 7, gold);
        ctx.fill(-18, yb - 20, -15, yb - 17, 0xFFFFFFFF);  // tip jewels
        ctx.fill(-2, yb - 25, 2, yb - 22, 0xFFFFFFFF);
        ctx.fill(15, yb - 20, 18, yb - 17, 0xFFFFFFFF);
        ctx.fill(-2, yb - 5, 2, yb - 2, 0xFFFF4D6D);       // front gem
    }

    // ------------------------------------------------------------------ effects

    private void rays(DrawContext ctx, float cx, float cy, float t, int rgb, float alpha, int n, float len) {
        if (alpha < 0.02f) return;
        Matrix3x2fStack m = ctx.getMatrices();
        for (int i = 0; i < n; i++) {
            m.pushMatrix();
            m.translate(cx, cy);
            m.rotate((float) Math.toRadians(i * 360f / n + t * 0.012f));
            int w = i % 2 == 0 ? 9 : 4;
            ctx.fill(14, -w, (int) len, w, argb(rgb, i % 2 == 0 ? alpha : alpha * 0.7f));
            m.popMatrix();
        }
    }

    private void ring(DrawContext ctx, float cx, float cy, float t, float fade) {
        for (int k = 0; k < 2; k++) {
            float phase = ((t * 0.09f) + k * 110f) % 220f;
            float r = 40f + phase;
            float a = (1f - phase / 220f) * 0.9f * fade;
            if (a < 0.03f) continue;
            int n = 64;
            for (int i = 0; i < n; i++) {
                double ang = i * Math.PI * 2 / n + t * 0.0004;
                int x = Math.round(cx + (float) Math.cos(ang) * r * 1.7f);
                int y = Math.round(cy + (float) Math.sin(ang) * r * 0.95f);
                ctx.fill(x, y, x + 2, y + 2, argb(0xFFD060, a));
            }
        }
    }

    private void matrixRain(DrawContext ctx, TextRenderer tr, int W, int H, float t, float alpha) {
        if (alpha < 0.03f) return;
        int colW = 14;
        int cols = W / colW + 1;
        int trail = 9;
        int cellH = 9;
        long tick = (long) (t / 110f);
        for (int c = 0; c < cols; c++) {
            float speed = 0.05f + ((c * 37) % 11) * 0.011f;
            float off = (c * 53) % H;
            float headY = (off + t * speed) % (H + trail * cellH);
            for (int k = 0; k < trail; k++) {
                float y = headY - k * cellH;
                if (y < -cellH || y > H) continue;
                int idx = Math.floorMod((int) (c * 31L + k * 17L + tick * (k + 3L)), GLYPH.length);
                int rgb = k == 0 ? 0xCCFFCC : 0x00FF41;
                float a = (k == 0 ? 1f : 1f - k / (float) trail) * alpha;
                ctx.drawText(tr, GLYPH[idx], c * colW, (int) y, argb(rgb, a), false);
            }
        }
    }

    // ------------------------------------------------------------------ particles

    private P newParticle() {
        P p = new P();
        p.rgb = CONFETTI[rnd.nextInt(CONFETTI.length)];
        p.size = 1 + rnd.nextInt(3);
        if (particles.size() < 700) particles.add(p);
        return p;
    }

    private void burst(float cx, float cy, int n) {
        for (int i = 0; i < n; i++) {
            P p = newParticle();
            p.x = cx + (rnd.nextFloat() - 0.5f) * 120f;
            p.y = cy;
            p.vx = (rnd.nextFloat() - 0.5f) * 0.55f;
            p.vy = -(0.08f + rnd.nextFloat() * 0.4f);
            p.life = 1200f + rnd.nextFloat() * 1200f;
        }
    }

    private void particles(DrawContext ctx, float dt, int H) {
        for (Iterator<P> it = particles.iterator(); it.hasNext(); ) {
            P p = it.next();
            p.life -= dt;
            if (p.life <= 0f || p.y > H + 5) {
                it.remove();
                continue;
            }
            p.vy += 0.00035f * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            int x = (int) p.x;
            int y = (int) p.y;
            ctx.fill(x, y, x + p.size, y + p.size, argb(p.rgb, clamp01(p.life / 400f)));
        }
    }
}
