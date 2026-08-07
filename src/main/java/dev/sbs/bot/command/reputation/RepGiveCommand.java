package dev.sbs.bot.command.reputation;

import dev.sbs.discordapi.DiscordBot;
import dev.sbs.discordapi.command.DiscordCommand;
import dev.sbs.discordapi.command.Structure;
import dev.sbs.discordapi.command.parameter.Argument;
import dev.sbs.discordapi.command.parameter.Parameter;
import dev.sbs.discordapi.context.command.SlashCommandContext;
import dev.sbs.discordapi.exception.DiscordException;
import dev.sbs.discordapi.response.Response;
import dev.sbs.discordapi.response.embed.Embed;
import dev.sbs.discordapi.response.page.Page;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.bot.persistence.model.AppGuildReputation;
import dev.sbs.bot.persistence.model.AppGuildReputationType;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.tuple.pair.Pair;
import dev.simplified.collection.unmodifiable.ConcurrentUnmodifiableList;
import dev.simplified.persistence.JpaRepository;
import dev.simplified.util.StringUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.util.Optional;

@Structure(
    parent = @Structure.Parent(
        name = "rep",
        description = "Reputation commands"
    ),
    name = "give",
    description = "Give reputation to another user"
)
public class RepGiveCommand extends DiscordCommand<SlashCommandContext> {

    protected RepGiveCommand(@NotNull DiscordBot discordBot) {
        super(discordBot);
    }

    @Override
    protected @NotNull Mono<Void> process(@NotNull SlashCommandContext commandContext) throws DiscordException {
        final long submitterDiscordId = commandContext.getInteractUserId().asLong();
        final long receiverDiscordId = commandContext.getArgument("user").map(Argument::asLong).orElseThrow();
        String reputationType = commandContext.getArgument("type").map(Argument::asString).orElseThrow();
        String reason = commandContext.getArgument("reason").map(Argument::asString).orElseThrow();
        Optional<Member> receivingMember = commandContext.getGuild()
            .flatMap(guild -> guild.getMemberById(Snowflake.of(receiverDiscordId)))
            .blockOptional();

        // Check if member is in current guild
        if (receivingMember.isEmpty())
            return commandContext.reply(genericResponse("That user is no longer in the server!", Color.RED));

        // Prevent Self Reputation
        if (submitterDiscordId == receiverDiscordId)
            return commandContext.reply(genericResponse("You cannot give yourself reputation!", Color.RED));

        Optional<AppGuildReputationType> reputationTypeModel = MinecraftApi.getRepository(AppGuildReputationType.class)
            .findFirst(AppGuildReputationType::getKey, reputationType.toUpperCase());

        // Check Reputation Type
        if (reputationTypeModel.isEmpty())
            return commandContext.reply(genericResponse(String.format("The provided reputation type '%s' is invalid!", reputationType), Color.RED));

        long lastReceivedId = MinecraftApi.getRepository(AppGuildReputation.class)
            .findLast(AppGuildReputation::getSubmitterDiscordId, submitterDiscordId)
            .map(AppGuildReputation::getReceiverDiscordId)
            .orElse(0L);

        // Prevent Continuous Reputation
        if (lastReceivedId == receiverDiscordId)
            return commandContext.reply(genericResponse("You have recently given rep to this user!", Color.RED));

        // Create New Reputation
        AppGuildReputation entry = new AppGuildReputation();
        entry.setSubmitterDiscordId(submitterDiscordId);
        entry.setReceiverDiscordId(receiverDiscordId);
        entry.setType(reputationTypeModel.get());
        entry.setReason(reason);
        ((JpaRepository<AppGuildReputation>) MinecraftApi.getRepository(AppGuildReputation.class)).save(entry);

        return commandContext.reply(
            genericResponse(String.format(
                """
                    You have given +1 %s reputation to %s
                    Reason: %s""",
                StringUtil.capitalizeFully(reputationType.replace("_", " ")),
                receivingMember.get().getMention(),
                reason
            ), Color.YELLOW)
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
                .withDescription("SkyBlock Profile Name")
                .withType(Parameter.Type.USER)
                .isRequired()
                .build(),
            Parameter.builder()
                .withName("type")
                .withDescription("Type of the given Rep")
                .withType(Parameter.Type.WORD)
                .isRequired()
                .withChoices(
                    MinecraftApi.getRepository(AppGuildReputationType.class)
                        .stream()
                        .map(reputationType -> Pair.of(reputationType.getName(), reputationType.getKey().toLowerCase()))
                        .collect(Concurrent.toWeakLinkedMap())
                )
                .build(),
            Parameter.builder()
                .withName("reason")
                .withDescription("Reason for the Rep")
                .withType(Parameter.Type.TEXT)
                .isRequired()
                .build()
        );
    }

}
