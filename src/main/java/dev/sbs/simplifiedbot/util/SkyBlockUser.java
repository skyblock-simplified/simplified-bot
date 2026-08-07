package dev.sbs.simplifiedbot.util;

import dev.sbs.discordapi.command.exception.InputException;
import dev.sbs.discordapi.command.parameter.Argument;
import dev.sbs.discordapi.context.command.SlashCommandContext;
import dev.sbs.discordapi.exception.DiscordUserException;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelContract;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelGuild;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelStatus;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockAuction;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockIsland;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockMember;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockProfiles;
import dev.sbs.minecraftapi.client.mojang.response.MojangProfile;
import dev.sbs.minecraftapi.client.sbs.request.SbsContract;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockEmojiData;
import dev.sbs.minecraftapi.skyblock.common.Profile;
import dev.sbs.simplifiedbot.SimplifiedBot;
import dev.sbs.simplifiedbot.command.exception.UnlinkedAccountException;
import dev.sbs.simplifiedbot.persistence.model.AppUser;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.util.StringUtil;
import discord4j.common.util.Snowflake;
import lombok.Getter;
import org.intellij.lang.annotations.PrintFormat;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public final class SkyBlockUser {

    private final SkyBlockProfiles profiles;
    @Getter private final MojangProfile mojangProfile;
    @Getter private SkyBlockIsland selectedIsland;
    @Getter private final SkyBlockEmojiData skyBlockEmojis;
    @Getter private final ConcurrentList<SkyBlockAuction> auctions;
    @Getter private final ItemCache.AuctionHouse auctionHouse;
    @Getter private final Optional<HypixelGuild> guild;
    @Getter private final HypixelStatus.Session session;

    @PrintFormat
    public SkyBlockUser(@NotNull SlashCommandContext commandContext) {
        SimplifiedBot bot = commandContext.getDiscordBot(SimplifiedBot.class);
        this.auctionHouse = bot.getItemCache().getAuctionHouse();
        this.skyBlockEmojis = bot.getSkyBlockEmojis();
        Optional<String> optionalPlayerID = commandContext.getArgument("name").map(Argument::asString);

        if (optionalPlayerID.isEmpty()) {
            if (!this.isVerified(commandContext.getInteractUserId()))
                throw new UnlinkedAccountException();

            optionalPlayerID = MinecraftApi.getRepository(AppUser.class)
                .matchFirst(userModel -> userModel.getDiscordIds().contains(commandContext.getInteractUserId().asLong()))
                .map(userModel -> userModel.getMojangUniqueIds().getLast())
                .map(UUID::toString);
        }

        String playerID = optionalPlayerID.orElseThrow(); // Will never reach here
        SbsContract sbs = MinecraftApi.getClient(SbsContract.class).getContract();
        HypixelContract hypixel = MinecraftApi.getClient(HypixelContract.class).getContract();
        this.mojangProfile = StringUtil.isUUID(playerID) ? sbs.getProfileFromUniqueId(StringUtil.toUUID(playerID)) : sbs.getProfileFromUsername(playerID);
        this.profiles = hypixel.getProfiles(this.getMojangProfile().getUniqueId());
        this.guild = hypixel.getGuildByPlayer(this.getMojangProfile().getUniqueId()).getGuild();
        this.session = hypixel.getStatus(this.getMojangProfile().getUniqueId()).getSession();
        this.auctions = hypixel.getAuctionByPlayer(this.getMojangProfile().getUniqueId()).getAuctions();

        // Empty Profile
        if (this.profiles.getIslands().isEmpty())
            throw new DiscordUserException("The Hypixel account `%s` has either never played SkyBlock or has been profile wiped.", this.getMojangProfile().getUsername());

        Optional<String> optionalProfileName = commandContext.getArgument("profile").map(Argument::asString);
        Optional<SkyBlockIsland> optionalSkyBlockIsland = Optional.empty();

        if (optionalProfileName.isPresent()) {
            String profileName = optionalProfileName.get();
            Profile profile = Profile.of(profileName.toUpperCase())
                .orElseThrow(() -> new InputException(StringUtil.capitalizeFully(profileName)));

            optionalSkyBlockIsland = this.profiles.getIsland(profile);
        }

        this.selectedIsland = optionalSkyBlockIsland.orElse(this.profiles.getSelected());
    }

    public @NotNull ConcurrentList<SkyBlockIsland> getIslands() {
        return this.profiles.getIslands();
    }

    public Optional<SkyBlockIsland> getIsland(@NotNull Profile profile) {
        return this.profiles.getIsland(profile);
    }

    public @NotNull SkyBlockIsland getSelected() {
        return this.profiles.getSelected();
    }

    public @NotNull SkyBlockMember getMember() {
        return this.getMember(this.getMojangProfile().getUniqueId());
    }

    public SkyBlockMember getMember(@NotNull UUID uniqueId) {
        return this.selectedIsland.getMembers().get(uniqueId);
    }

    public boolean isVerified(@NotNull Snowflake userId) {
        return MinecraftApi.getRepository(AppUser.class).matchFirst(userModel -> userModel.getDiscordIds().contains(userId.asLong())).isPresent();
    }

    public void setSelectedIsland(@NotNull Profile profile) {
        this.selectedIsland = this.getIsland(profile).orElse(this.selectedIsland);
    }

}
