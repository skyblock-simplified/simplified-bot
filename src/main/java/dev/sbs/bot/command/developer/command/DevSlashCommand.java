package dev.sbs.bot.command.developer.command;

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
    group = @Structure.Group(
        name = "command",
        description = "Developer command management"
    ),
    name = "update",
    description = "Update the slash commands"
)
public class DevSlashCommand extends DiscordCommand<SlashCommandContext> {

    protected DevSlashCommand(@NotNull DiscordBot discordBot) {
        super(discordBot);
    }

    @Override
    protected @NotNull Mono<Void> process(@NotNull SlashCommandContext commandContext) throws DiscordException {
        return this.getDiscordBot().getCommandHandler().updateApplicationCommands();
    }

}
