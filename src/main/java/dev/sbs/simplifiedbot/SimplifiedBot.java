package dev.sbs.simplifiedbot;

import dev.sbs.discordapi.DiscordBot;
import dev.sbs.discordapi.command.DiscordCommand;
import dev.sbs.discordapi.handler.DiscordConfig;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelContract;
import dev.sbs.minecraftapi.client.sbs.request.SbsContract;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockEmojiData;
import dev.sbs.simplifiedbot.processor.resource.ResourceCollectionsProcessor;
import dev.sbs.simplifiedbot.processor.resource.ResourceItemsProcessor;
import dev.sbs.simplifiedbot.processor.resource.ResourceSkillsProcessor;
import dev.sbs.simplifiedbot.util.ItemCache;
import dev.simplified.persistence.JpaConfig;
import dev.simplified.reflection.Reflection;
import dev.simplified.util.NumberUtil;
import dev.simplified.util.SystemUtil;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.AllowedMentions;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@Getter
@Log4j2
public final class SimplifiedBot extends DiscordBot {

    private static final HypixelContract HYPIXEL_RESOURCE_REQUEST = MinecraftApi.getClient(HypixelContract.class).getContract();
    private ItemCache itemCache;
    private SkyBlockEmojiData skyBlockEmojis;

    private SimplifiedBot(@NotNull DiscordConfig discordConfig) {
        super(discordConfig);
    }

    public static void main(final String[] args) {
        // Connect the SkyBlock H2 session before commands fire - the static initializer
        // no longer auto-connects (see MinecraftApi.connectSkyBlockSession Javadoc).
        MinecraftApi.connectSkyBlockSession();

        DiscordConfig discordConfig = DiscordConfig.builder()
            .withToken(SystemUtil.getEnv("DISCORD_TOKEN"))
            .withMainGuildId(652148034448261150L)
            .withLogChannelId(SystemUtil.getEnv("DEVELOPER_ERROR_LOG_CHANNEL_ID").map(NumberUtil::tryParseLong))
            .withJpaConfig(JpaConfig.commonSql())
            .withCommands(
                Reflection.getResources()
                    .filterPackage("dev.sbs.simplifiedbot.command")
                    .getTypesOf(DiscordCommand.class)
            )
            .withBotEventListeners(GatewayConnectListener.class)
            .withEmojis(Reflection.getResources(SimplifiedBot.class.getClassLoader()).getResources("emojis/"))
            .withAllowedMentions(AllowedMentions.suppressEveryone())
            .withDisabledIntents(IntentSet.of(Intent.GUILD_PRESENCES))
            .withClientPresence(ClientPresence.online(ClientActivity.watching("commands")))
            .withMemberRequestFilter(MemberRequestFilter.withLargeGuilds())
            .build();

        SimplifiedBot simplifiedBot = new SimplifiedBot(discordConfig);
        simplifiedBot.start();
    }

    /**
     * Bootstraps Hypixel API access, builds the SkyBlock emoji and item caches,
     * and schedules the periodic resource processors. Invoked from
     * {@link GatewayConnectListener} once the gateway is online.
     */
    void bootstrap() {
        MinecraftApi.getKeyManager().add(SystemUtil.getEnvPair("HYPIXEL_API_KEY"));

        // Update Caches
        log.info("Building Caches");
        this.skyBlockEmojis = MinecraftApi.getClient(SbsContract.class).getContract().getItemEmojis();
        this.itemCache = new ItemCache();
        this.getItemCache().getAuctionHouse().update();
        this.getItemCache().getBazaar().update();
        this.getItemCache().getEndedAuctions().update();

        // Schedule SkyBlock Emoji Cache Updates
        this.getScheduler().scheduleAsync(
            () -> this.skyBlockEmojis = MinecraftApi.getClient(SbsContract.class).getContract().getItemEmojis(),
            10,
            10,
            TimeUnit.MINUTES
        );

        // Schedule Item Cache Updates
        this.getScheduler().scheduleAsync(() -> {
            this.getItemCache().getAuctionHouse().update();
            this.getItemCache().getBazaar().update();
            this.getItemCache().getEndedAuctions().update();
        }, 1, 1, TimeUnit.SECONDS);


        // Schedule Item Resource Updates
        this.getScheduler().scheduleAsync(() -> {
            try {
                log.info("Processing Items");
                new ResourceItemsProcessor(HYPIXEL_RESOURCE_REQUEST.getItems()).process();
            } catch (Exception exception) {
                log.atError()
                    .withThrowable(exception)
                    .log("An error occurred while processing the resource API.");
            }
        }, 0, 1, TimeUnit.MINUTES);

        // Schedule Skill Resource Updates
        this.getScheduler().scheduleAsync(() -> {
            try {
                log.info("Processing Skills");
                new ResourceSkillsProcessor(HYPIXEL_RESOURCE_REQUEST.getSkills()).process();
            } catch (Exception exception) {
                log.atError()
                    .withThrowable(exception)
                    .log("An error occurred while processing the resource API.");
            }
        }, 0, 1, TimeUnit.MINUTES);

        // Schedule Collection Resource Updates
        this.getScheduler().scheduleAsync(() -> {
            try {
                log.info("Processing Collections");
                new ResourceCollectionsProcessor(HYPIXEL_RESOURCE_REQUEST.getCollections()).process();
            } catch (Exception exception) {
                log.atError()
                    .withThrowable(exception)
                    .log("An error occurred while processing the resource API.");
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

}
