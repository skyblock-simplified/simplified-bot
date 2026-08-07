package dev.sbs.bot.command.developer.command;

import dev.simplified.collection.Concurrent;
import dev.simplified.collection.unmodifiable.ConcurrentUnmodifiableList;
import dev.sbs.discordapi.DiscordBot;
import dev.sbs.discordapi.command.DiscordCommand;
import dev.sbs.discordapi.command.Structure;
import dev.sbs.discordapi.command.parameter.Parameter;
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
    name = "disable",
    description = "Disable a command"
)
public class DevDisableCommand extends DiscordCommand<SlashCommandContext> {

    protected DevDisableCommand(@NotNull DiscordBot discordBot) {
        super(discordBot);
    }

    @Override
    protected @NotNull Mono<Void> process(@NotNull SlashCommandContext commandContext) throws DiscordException {
        return Mono.empty();
    }

    @Override
    public @NotNull ConcurrentUnmodifiableList<Parameter> getParameters() {
        return Concurrent.newUnmodifiableList(
            Parameter.builder()
                .withName("command")
                .withDescription("The command to disable.")
                .withType(Parameter.Type.WORD)
                .isRequired()
                .build(),
            Parameter.builder()
                .withName("reason")
                .withDescription("The reason the command is being disabled.")
                .withType(Parameter.Type.TEXT)
                .build()
        );
    }

}
