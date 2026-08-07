package dev.sbs.bot.command.reputation;

import dev.sbs.discordapi.DiscordBot;
import dev.sbs.discordapi.command.DiscordCommand;
import dev.sbs.discordapi.command.Structure;
import dev.sbs.discordapi.command.parameter.Argument;
import dev.sbs.discordapi.command.parameter.Parameter;
import dev.sbs.discordapi.context.command.SlashCommandContext;
import dev.sbs.discordapi.exception.DiscordException;
import dev.sbs.discordapi.response.Response;
import dev.sbs.discordapi.response.embed.Author;
import dev.sbs.discordapi.response.embed.Embed;
import dev.sbs.discordapi.response.embed.Field;
import dev.sbs.discordapi.response.page.Page;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.bot.persistence.model.AppGuild;
import dev.sbs.bot.persistence.model.AppGuildReputation;
import dev.sbs.bot.persistence.model.AppGuildReputationType;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.query.SearchFunction;
import dev.simplified.collection.tuple.pair.Pair;
import dev.simplified.collection.unmodifiable.ConcurrentUnmodifiableList;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

@Structure(
    parent = @Structure.Parent(
        name = "rep",
        description = "Reputation commands"
    ),
    name = "check",
    description = "Check a users reputation"
)
public class RepCheckCommand extends DiscordCommand<SlashCommandContext> {

    protected RepCheckCommand(@NotNull DiscordBot discordBot) {
        super(discordBot);
    }

    @Override
    protected @NotNull Mono<Void> process(@NotNull SlashCommandContext commandContext) throws DiscordException {
        final long receiverDiscordId = commandContext.getArgument("user").map(Argument::asLong).orElseThrow();
        Optional<Member> receivingMember = commandContext.getGuild()
            .flatMap(guild -> guild.getMemberById(Snowflake.of(receiverDiscordId)))
            .blockOptional();

        // Check if member is in current guild
        if (receivingMember.isEmpty())
            return commandContext.reply(genericResponse("That user is no longer in the server!", Color.RED));

        // Load Nickname (Tag) or Tag
        String userName = receivingMember.get()
            .getNickname()
            .map(nick -> String.format(
                "%s (%s)",
                nick,
                receivingMember.get().getTag()
            ))
            .orElse(receivingMember.get().getTag());

        // Get reputation types for current Guild
        ConcurrentList<AppGuildReputationType> reputationTypes = MinecraftApi.getRepository(AppGuildReputationType.class)
            .findAll(SearchFunction.combine(AppGuildReputationType::getGuild, AppGuild::getGuildId), commandContext.getGuildId().orElseThrow())
            .collect(Concurrent.toList());

        // Check if reputation types have been created
        if (reputationTypes.isEmpty())
            return commandContext.reply(genericResponse("No reputation types have been setup!", Color.RED));

        ConcurrentMap<AppGuildReputationType, ConcurrentList<AppGuildReputation>> receiverReputation = reputationTypes.stream()
            .map(reputationType -> Pair.of(
                reputationType,
                MinecraftApi.getRepository(AppGuildReputation.class)
                    .findAll(
                        Pair.of(AppGuildReputation::getType, reputationType),
                        Pair.of(AppGuildReputation::getReceiverDiscordId, receiverDiscordId)
                    )
                    .collect(Concurrent.toList())
            ))
            .collect(Concurrent.toMap());

        ConcurrentList<Field> typeFields = reputationTypes.stream()
            .map(reputationType -> Field.builder()
                .withName(reputationType.getName())
                .withValue(String.valueOf(receiverReputation.get(reputationType).size()))
                .isInline()
                .build()
            )
            .collect(Concurrent.toList());

        while (typeFields.size() % 3 != 0)
            typeFields.add(Field.empty(true));

        return commandContext.reply(
            Response.builder()
                .withPages(
                    Page.builder()
                        .withEmbeds(
                            Embed.builder()
                                .withAuthor(
                                    Author.builder()
                                        .withName(userName)
                                        .withIconUrl(receivingMember.get().getAvatarUrl())
                                        .build()
                                )
                                .withColor(Color.YELLOW)
                                .withTitle("Reputation")
                                .withDescription(
                                        """
                                        Total: All verified and unverified reputation a user has been given.
                                        Verified: The total reputation confirmed by the staff team.
                                        Unverified: The total reputation not yet confirmed by the staff team.
                                        """
                                )
                                .withField("Total", String.valueOf(receiverReputation.values().stream().mapToInt(ConcurrentList::size).sum()), true)
                                .withField("Verified", String.valueOf(
                                    receiverReputation.values()
                                        .stream()
                                        .flatMap(list -> list.stream().filter(guildReputation -> Objects.nonNull(guildReputation.getAssigneeDiscordId())))
                                        .count()
                                ), true)
                                .withField("Unverified", String.valueOf(
                                    receiverReputation.values()
                                        .stream()
                                        .flatMap(list -> list.stream().filter(guildReputation -> Objects.isNull(guildReputation.getAssigneeDiscordId())))
                                        .count()
                                ), true)
                                .withEmptyField(true)
                                .withFields(typeFields)
                                .build()
                        )
                        .build()
                )
                .build()
        );
    }

    private static Response genericResponse(String description, Color color) {
        return Response.builder()
            .withPages(
                Page.builder()
                    .withEmbeds(
                        Embed.builder()
                            .withTitle("Reputation")
                            .withDescription(description)
                            .withColor(color)
                            .build()
                    )
                    .build()
            )
            .build();
    }

    @Override
    public @NotNull ConcurrentUnmodifiableList<Parameter> getParameters() {
        return Concurrent.newUnmodifiableList(
            Parameter.builder()
                .withName("user")
                .withDescription("Discord User")
                .withType(Parameter.Type.USER)
                .isRequired()
                .build()
        );
    }

}
