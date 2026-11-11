package com.donationalerts;

import net.minecraft.client.MinecraftClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Looks at server messages for "X paid Y $1M" and turns matches into alerts. */
public final class ChatWatcher {
    private static Pattern compiled;
    private static String compiledFrom;

    private ChatWatcher() {}

    private static Pattern pattern(Config c) {
        if (compiled == null || !c.chatRegex.equals(compiledFrom)) {
            try {
                compiled = Pattern.compile(c.chatRegex);
            } catch (PatternSyntaxException e) {
                DonationAlertsClient.LOG.warn("Bad chatRegex in config, using default", e);
                compiled = Pattern.compile(new Config().chatRegex);
            }
            compiledFrom = c.chatRegex;
        }
        return compiled;
    }

    public static void handle(String raw) {
        Config c = Config.get();
        if (!c.enabled || raw == null || raw.length() > 300) return;

        Matcher m = pattern(c).matcher(raw);
        if (!m.find()) return;

        String payer = m.group(1);
        String recipient = m.group(2);
        long amount = Money.parse(m.group(3), m.groupCount() >= 4 ? m.group(4) : "");
        if (amount < Math.max(1, c.minAmount)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        String me = c.trackedName == null || c.trackedName.isBlank() ? mc.getSession().getUsername() : c.trackedName;
        boolean toMe = recipient.equalsIgnoreCase("you") || recipient.equalsIgnoreCase(me);
        if (!c.trackAll && !toMe) return;

        AlertManager.enqueue(new Donation(payer, amount, false));
    }
}
