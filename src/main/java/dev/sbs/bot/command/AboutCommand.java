package dev.sbs.bot.command;

import dev.sbs.discordapi.DiscordBot;
import dev.sbs.discordapi.command.DiscordCommand;
import dev.sbs.discordapi.command.Structure;
import dev.sbs.discordapi.component.interaction.SelectMenu;
import dev.sbs.discordapi.context.command.SlashCommandContext;
import dev.sbs.discordapi.response.Emoji;
import dev.sbs.discordapi.response.Response;
import dev.sbs.discordapi.response.embed.Author;
import dev.sbs.discordapi.response.embed.Embed;
import dev.sbs.discordapi.response.embed.Field;
import dev.sbs.discordapi.response.embed.Footer;
import dev.sbs.discordapi.response.handler.item.ItemHandler;
import dev.sbs.discordapi.response.page.Page;
import dev.sbs.discordapi.response.page.item.field.StringItem;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.bot.persistence.model.AppUser;
import dev.sbs.bot.persistence.model.SbsLegacyDonor;
import dev.simplified.collection.Concurrent;
import dev.simplified.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.time.Instant;

@Structure(
    name = "about",
    description = "Information about the bot"
)
public class AboutCommand extends DiscordCommand<SlashCommandContext> {

    protected AboutCommand(@NotNull DiscordBot discordBot) {
        super(discordBot);
    }

    @Override
    protected @NotNull Mono<Void> process(@NotNull SlashCommandContext commandContext) {
        return commandContext.reply(
            Response.builder()
                .withTimeToLive(60)
                .withPages(
                    Page.builder()
                        .withOption(
                            SelectMenu.Option.builder()
                                .withValue("about")
                                .withLabel("About")
                                .withDescription("General information about the bot.")
                                .build()
                        )
                        .withEmbeds(
                            Embed.builder()
                                .withAuthor(
                                    Author.builder()
                                        .withName("Bot Information")
                                        .withIconUrl(this.getEmoji("STATUS_INFO").map(Emoji::getUrl))
                                        .build()
                                )
                                .withTitle("About :: General")
                                .withColor(Color.DARK_GRAY)
                                .withDescription(
                                    "The official SkyBlock Simplified bot. Optimize your gear with `/optimizer`, " +
                                        "lookup guilds with `/guild`, lookup data on players such as general details, " +
                                        "net worth, skills, dungeons, slayers and more with `/player`. Look for Hypixel " +
                                        "reports on players like scamlist, macros, and irl trading using `/report`. Add and " +
                                        "receive reputation with `/rep`, host giveaways, and much more."
                                )
                                .withFields(
                                    Field.builder()
                                        .withName("Owner")
                                        .withValue("<@154743493464555521>")
                                        .isInline()
                                        .build(),
                                    Field.builder()
                                        .withName("Developers")
                                        .withValue(
                                            StringUtil.join(
                                                MinecraftApi.getRepository(AppUser.class)
                                                    .matchAll(AppUser::isDeveloper)
                                                    .map(userModel -> String.format("<@%s>", userModel.getDiscordIds().getFirst()))
                                                    .collect(Concurrent.toList()),
                                                "\n"
                                            )
                                        )
                                        .build(),
                                    Field.builder()
                                        .withName("Version")
                                        .withValue("1.5 Beta")
                                        .isInline()
                                        .build()
                                )
                                .withFields(
                                    Field.builder()
                                        .withName("Support Server")
                                        .withValue("https://discord.gg/sbs")
                                        .isInline()
                                        .build(),
                                    Field.empty(),
                                    Field.builder()
                                        .withName("Bot Invite Link")
                                        .withValue("https://sbs.dev/bot/invite")
                                        .isInline()
                                        .build()
                                )
                                .withField(
                                    "Premium",
                                    """
                                        Paying for Patreon comes with perks available on the SkyBlock Simplified Discord, Bot, Mod, Website and API.
                                        The full details of available perks can be seen on the Patreon page in the menu below.
                                        Sign up here: N/A"""
                                )
                                .withFooter(
                                    Footer.builder()
                                        .withTimestamp(Instant.now())
                                        .build()
                                )
                                .build()
                        )
                        .withPages(
                            Page.builder()
                                .withOption(
                                    SelectMenu.Option.builder()
                                        .withValue("donors")
                                        .withLabel("Donors")
                                        .withDescription("All legacy donors of the SkyBlock Simplified discord.")
                                        .build()
                                )
                                .withEmbeds(
                                    Embed.builder()
                                        .withAuthor(
                                            Author.builder()
                                                .withName("Bot Information")
                                                .withIconUrl(this.getEmoji("STATUS_INFO").map(Emoji::getUrl))
                                                .build()
                                        )
                                        .withTitle("About :: Donors")
                                        .withColor(Color.DARK_GRAY)
                                        .withDescription("A big thank you goes out to those who donated early on in the development of the SkyBlock Simplified discord and bot!")
                                        .withFooter(
                                            Footer.builder()
                                                .withTimestamp(Instant.now())
                                                .build()
                                        )
                                        .build()
                                )
                                .withItemHandler(
                                    ItemHandler.<SbsLegacyDonor>embed()
                                        .withItems(MinecraftApi.getRepository(SbsLegacyDonor.class).findAll())
                                        .withTransformer((legacyDonorModel, index, size) -> StringItem.builder()
                                            .withLabel("<@%s>", legacyDonorModel.getDiscordId())
                                            .withValue("$%.2f", legacyDonorModel.getAmount())
                                            .build()
                                        )
                                        .build()
                                )
                                .build(),
                            Page.builder()
                                .withOption(
                                    SelectMenu.Option.builder()
                                        .withValue("premium")
                                        .withLabel("Premium")
                                        .withDescription("Details and list of perks available for Patreon users.")
                                        .build()
                                )
                                .withEmbeds(
                                    Embed.builder()
                                        .withAuthor(
                                            Author.builder()
                                                .withName("Bot Information")
                                                .withIconUrl(this.getEmoji("STATUS_INFO").map(Emoji::getUrl))
                                                .build()
                                        )
                                        .withTitle("About :: Patreon")
                                        .withColor(Color.DARK_GRAY)
                                        .withDescription("Perks will be listed here as they become available. The Patreon system is not live yet.")
                                        .withFooter(
                                            Footer.builder()
                                                .withTimestamp(Instant.now())
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()
        );
    }

}
