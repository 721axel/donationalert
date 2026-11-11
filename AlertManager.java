package com.donationalerts;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayDeque;

/** Plays alerts one at a time. */
public final class AlertManager {
    private static final ArrayDeque<Donation> QUEUE = new ArrayDeque<>();
    private static ActiveAlert current;

    private AlertManager() {}

    public static void enqueue(Donation d) {
        QUEUE.add(d);
    }

    /** Skip the alert that is playing right now. */
    public static void skip() {
        current = null;
    }

    /** Skip the current alert and drop everything waiting. */
    public static void clearAll() {
        current = null;
        QUEUE.clear();
    }

    public static void render(DrawContext ctx) {
        if (current == null) {
            Donation next = QUEUE.poll();
            if (next == null) return;
            current = new ActiveAlert(next);
        }
        try {
            if (!current.render(ctx, MinecraftClient.getInstance())) current = null;
        } catch (Exception e) {
            DonationAlertsClient.LOG.error("Alert crashed, dropping it", e);
            current = null;
        }
    }
}
