package dev.sbs.bot.command.developer;

import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.unmodifiable.ConcurrentUnmodifiableList;
import dev.simplified.util.StringUtil;
import dev.sbs.discordapi.DiscordBot;
import dev.sbs.discordapi.command.DiscordCommand;
import dev.sbs.discordapi.command.Structure;
import dev.sbs.discordapi.command.parameter.Argument;
import dev.sbs.discordapi.command.parameter.Parameter;
import dev.sbs.discordapi.context.command.SlashCommandContext;
import dev.sbs.discordapi.exception.DiscordException;
import dev.sbs.discordapi.response.Emoji;
import dev.sbs.discordapi.response.Response;
import dev.sbs.discordapi.response.embed.Author;
import dev.sbs.discordapi.response.embed.Embed;
import dev.sbs.discordapi.response.embed.Footer;
import dev.sbs.discordapi.response.page.Page;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.Channel;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.time.Instant;
import java.util.Optional;

@Structure(
    parent = @Structure.Parent(
        name = "dev",
        description = "Developer Commands"
    ),
    name = "stats",
    description = "Get stats about a guild"
)
public class DevStatsCommand extends DiscordCommand<SlashCommandContext> {

    protected DevStatsCommand(@NotNull DiscordBot discordBot) {
        super(discordBot);
    }

    @Override
    public @NotNull ConcurrentUnmodifiableList<Parameter> getParameters() {
        return Concurrent.newUnmodifiableList(
            Parameter.builder()
                .withName("guild")
                .withDescription("Discord Guild Name")
                .withType(Parameter.Type.TEXT)
                .build()
        );
    }

    @Override
    protected @NotNull Mono<Void> process(@NotNull SlashCommandContext commandContext) throws DiscordException {
        Optional<Snowflake> optionalGuildId = Optional.empty();

        // Handle DMs
        if (commandContext.getArguments().isEmpty()) {
            if (!commandContext.isPrivateChannel())
                optionalGuildId = commandContext.getGuild().map(Guild::getId).blockOptional();
        } else
            optionalGuildId = commandContext.getArgument("guild").map(Argument::asSnowflake);

        Embed.Builder builder = Embed.builder()
            .withColor(Color.DARK_GRAY)
            .withFooter(
                Footer.builder()
                    .withTimestamp(Instant.now())
                    .build()
            );

        optionalGuildId.flatMap(guildId -> this.getDiscordBot()
            .getGateway()
            .getGuildById(guildId)
            .blockOptional()
        ).ifPresentOrElse(guild -> {
            String emojiReplyStem = this.getEmoji("REPLY_STEM").map(Emoji::asFormat).orElse("");
            String emojiReplyEnd = this.getEmoji("REPLY_END").map(Emoji::asFormat).orElse("");
            ConcurrentList<Channel> channels = guild.getChannels().toStream().collect(Concurrent.toList());
            boolean animatedIcon = guild.getData().icon().map(value -> value.startsWith("a_")).orElse(false);

            builder.withAuthor(
                    Author.builder()
                        .withName("Server Information")
                        .withIconUrl(this.getEmoji("STATUS_INFO").map(Emoji::getUrl))
                        .build()
                )
                .withFooter(
                    Footer.builder()
                        .withText(guild.getVanityUrlCode().map(vanityCode -> String.format("https://discord.gg/%s", vanityCode)).orElse(""))
                        .withIconUrl(this.getDiscordBot().getMainGuild().getIconUrl(discord4j.rest.util.Image.Format.GIF))
                        .build()
                )
                .withTitle("Server :: %s", guild.getName())
                .withDescription(guild.getDescription())
                .withThumbnailUrl(guild.getIconUrl(animatedIcon ? discord4j.rest.util.Image.Format.GIF : discord4j.rest.util.Image.Format.PNG))
                .withField(
                    "About",
                    String.format(
                        """
                        %1$sOwner: %3$s
                        %1$sCreated: <t:%4$s:D>
                        %1$sMembers: %5$s / %6$s
                        %1$sRoles: %7$s
                        %2$sChannels: %8$s
                        """,
                        emojiReplyStem,
                        emojiReplyEnd,
                        guild.getOwner().map(Member::getMention).blockOptional().orElse("Unknown"),
                        guild.getId().getTimestamp().getEpochSecond(),
                        this.getDiscordBot()
                            .getGateway()
                            .getRestClient()
                            .restGuild(guild.getData())
                            .getData()
                            .blockOptional()
                            .flatMap(guildUpdateData -> guildUpdateData.approximatePresenceCount().toOptional())
                            .orElse(0),
                        guild.getMemberCount(),
                        guild.getRoles().toStream().collect(Concurrent.toList()).size(),
                        channels.size()
                    )
                )
                .withField(
                    "Security",
                    String.format(
                        """
                        %1$sVerification Level: %3$s
                        %1$sContent Filter: %4$s
                        %1$sDefault Notifications: %5$s
                        %2$sTwo-Factor Authentication: %6$s
                        """,
                        emojiReplyStem,
                        emojiReplyEnd,
                        StringUtil.capitalizeEnum(guild.getVerificationLevel()),
                        StringUtil.capitalizeEnum(guild.getContentFilterLevel()),
                        StringUtil.capitalizeEnum(guild.getNotificationLevel()),
                        StringUtil.capitalizeEnum(guild.getMfaLevel())
                    )
                );

            if (!guild.getGuildFeatures().isEmpty()) {
                builder.withField(
                    "Features",
                    StringUtil.join(
                        guild.getGuildFeatures()
                            .stream()
                            .map(Guild.GuildFeature::getValue)
                            .map(value -> value.replace("_", " "))
                            .map(StringUtil::capitalizeFully)
                            .collect(Concurrent.toList()),
                        ", "
                    )
                );
            }
        }, () -> builder.withDescription("This isn't a Guild."));

        return commandContext.reply(
            Response.builder()
                .withPages(
                    Page.builder()
                        .withEmbeds(builder.build())
                        .build()
                )
                .build()
        );
    }

}
