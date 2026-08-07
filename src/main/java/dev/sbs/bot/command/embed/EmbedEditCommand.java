package dev.sbs.bot.command.embed;

import dev.sbs.discordapi.DiscordBot;
import dev.sbs.discordapi.command.DiscordCommand;
import dev.sbs.discordapi.command.Structure;
import dev.sbs.discordapi.context.command.SlashCommandContext;
import dev.sbs.discordapi.exception.DiscordException;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;


@Structure(
    parent = @Structure.Parent(
        name = "embed",
        description = "Embed commands"
    ),
    name = "edit",
    description = "Edit an embed"
)
public class EmbedEditCommand extends DiscordCommand<SlashCommandContext> {

    protected EmbedEditCommand(@NotNull DiscordBot discordBot) {
        super(discordBot);
    }

    @NotNull
    @Override
    protected Mono<Void> process(@NotNull SlashCommandContext commandContext) throws DiscordException {
        return Mono.empty();
    }

}
