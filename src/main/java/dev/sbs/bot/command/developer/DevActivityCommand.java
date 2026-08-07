package dev.sbs.bot.command.developer;

import dev.sbs.discordapi.DiscordBot;
import dev.sbs.discordapi.command.DiscordCommand;
import dev.sbs.discordapi.command.Structure;
import dev.sbs.discordapi.context.command.SlashCommandContext;
import dev.sbs.discordapi.exception.DiscordException;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

@Structure(
    parent = @Structure.Parent(
        name = "dev",
        description = "Developer Commands"
    ),
    name = "activity",
    description = "Get the bot's activity"
)
public class DevActivityCommand extends DiscordCommand<SlashCommandContext> {

    protected DevActivityCommand(@NotNull DiscordBot discordBot) {
        super(discordBot);
    }

    @Override
    protected @NotNull Mono<Void> process(@NotNull SlashCommandContext commandContext) throws DiscordException {
        return Mono.empty();
    }

}
