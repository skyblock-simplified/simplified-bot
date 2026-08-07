package dev.sbs.bot;

import dev.sbs.discordapi.DiscordBot;
import dev.sbs.discordapi.event.lifecycle.GatewayConnectBotEvent;
import dev.sbs.discordapi.listener.BotEventListener;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Listens for the {@link GatewayConnectBotEvent} and bootstraps the
 * {@link SimplifiedBot} caches and scheduled resource processors once the
 * gateway is online.
 */
public final class GatewayConnectListener extends BotEventListener<GatewayConnectBotEvent> {

    /**
     * Constructs a new {@code GatewayConnectListener} for the given bot.
     *
     * @param discordBot the bot instance
     */
    public GatewayConnectListener(@NotNull DiscordBot discordBot) {
        super(discordBot);
    }

    @Override
    public Publisher<Void> apply(@NotNull GatewayConnectBotEvent event) {
        return Mono.fromRunnable(() -> this.getDiscordBot(SimplifiedBot.class).bootstrap());
    }

}
