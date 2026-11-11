package com.donationalerts;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DonationAlertsClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("donationalerts");

    @Override
    public void onInitializeClient() {
        Config.load();
        DonationStore.load();

        // System/game messages only, so other players can't fake a donation by typing it in normal chat.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) ChatWatcher.handle(message.getString());
        });

        HudElementRegistry.addLast(Identifier.of("donationalerts", "overlay"), (ctx, tickCounter) -> {
            LeaderboardHud.render(ctx);
            AlertManager.render(ctx);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("donationalerts")
                .then(ClientCommandManager.literal("test")
                    .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                        .executes(c -> test(c, StringArgumentType.getString(c, "amount"), null))
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                            .executes(c -> test(c, StringArgumentType.getString(c, "amount"),
                                StringArgumentType.getString(c, "name"))))))
                .then(ClientCommandManager.literal("leaderboard").executes(c -> {
                    Config cfg = Config.get();
                    cfg.leaderboardVisible = !cfg.leaderboardVisible;
                    Config.save();
                    c.getSource().sendFeedback(Text.literal("Leaderboard " + (cfg.leaderboardVisible ? "shown" : "hidden")));
                    return 1;
                }))
                .then(ClientCommandManager.literal("style").executes(c -> {
                    Config cfg = Config.get();
                    cfg.leaderboardStyle = "LIST".equalsIgnoreCase(cfg.leaderboardStyle) ? "PODIUM" : "LIST";
                    Config.save();
                    c.getSource().sendFeedback(Text.literal("Leaderboard style: " + cfg.leaderboardStyle));
                    return 1;
                }))
                .then(ClientCommandManager.literal("top").executes(c -> {
                    List<DonationStore.Entry> top = DonationStore.top(10);
                    if (top.isEmpty()) {
                        c.getSource().sendFeedback(Text.literal("No donations yet."));
                    }
                    for (int i = 0; i < top.size(); i++) {
                        c.getSource().sendFeedback(Text.literal((i + 1) + ". " + top.get(i).name()
                            + " - " + Money.format(top.get(i).amount())));
                    }
                    return 1;
                }))
                .then(ClientCommandManager.literal("skip").executes(c -> {
                    AlertManager.skip();
                    return 1;
                }))
                .then(ClientCommandManager.literal("clear").executes(c -> {
                    AlertManager.clearAll();
                    DonationStore.clearTests();
                    c.getSource().sendFeedback(Text.literal("Cleared the alert queue and test donations."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("reset").executes(c -> {
                    AlertManager.clearAll();
                    DonationStore.clear();
                    c.getSource().sendFeedback(Text.literal("Leaderboard wiped (including saved data)."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("reload").executes(c -> {
                    Config.load();
                    c.getSource().sendFeedback(Text.literal("Config reloaded."));
                    return 1;
                }))));
    }

    private static int test(CommandContext<FabricClientCommandSource> c, String amountArg, String name) {
        long amount = Money.parseArg(amountArg);
        if (amount <= 0) {
            c.getSource().sendFeedback(Text.literal("Couldn't read that amount. Try 5m, 100b or 27.06b."));
            return 0;
        }
        String who = name != null ? name : MinecraftClient.getInstance().getSession().getUsername();
        Donation d = new Donation(who, amount, true);
        AlertManager.enqueue(d);
        c.getSource().sendFeedback(Text.literal("Queued test donation: " + who + " " + Money.format(amount)
            + " (" + Tier.forAmount(amount).name() + ")"));
        return 1;
    }
}
