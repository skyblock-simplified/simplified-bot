package dev.sbs.simplifiedbot.util;

import dev.simplified.discordapi.DiscordBot;
import dev.simplified.discordapi.command.DiscordCommand;
import dev.simplified.discordapi.command.parameter.Parameter;
import dev.simplified.discordapi.component.interaction.SelectMenu;
import dev.simplified.discordapi.context.command.SlashCommandContext;
import dev.simplified.discordapi.exception.DiscordException;
import dev.simplified.discordapi.response.Emoji;
import dev.simplified.discordapi.response.embed.Author;
import dev.simplified.discordapi.response.embed.Embed;
import dev.simplified.discordapi.response.embed.Field;
import dev.simplified.discordapi.response.embed.Footer;
import dev.simplified.discordapi.response.handler.Search;
import dev.simplified.discordapi.response.handler.Sorter;
import dev.simplified.discordapi.response.handler.ItemHandler;
import dev.simplified.discordapi.component.TextDisplay;
import dev.simplified.discordapi.component.interaction.Button;
import dev.simplified.discordapi.component.layout.Section;
import dev.simplified.discordapi.response.page.Page;
import dev.simplified.discordapi.response.page.TreePage;
import dev.simplified.discordapi.util.DiscordDate;
import dev.sbs.skyblockdata.SkyBlockData;
import api.simplified.hypixel.response.hypixel.HypixelGuild;
import api.simplified.hypixel.response.skyblock.SkyBlockAuction;
import api.simplified.hypixel.response.skyblock.SkyBlockIsland;
import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import api.simplified.hypixel.response.skyblock.island.Banking;
import api.simplified.hypixel.response.skyblock.island.CommunityUpgrades;
import api.simplified.hypixel.response.skyblock.member.JacobsContest;
import api.simplified.hypixel.response.skyblock.member.dungeon.DungeonClass;
import api.simplified.hypixel.response.skyblock.member.dungeon.DungeonData;
import api.simplified.hypixel.response.skyblock.member.pet.OwnedPet;
import api.simplified.hypixel.response.skyblock.member.skill.SkillLevel;
import api.simplified.hypixel.response.skyblock.member.slayer.SlayerBoss;
import api.simplified.mojang.response.MojangProfile;
import dev.sbs.simplifiedapi.response.SkyBlockEmojis;
import lib.minecraft.nbt.tag.collection.CompoundTag;
import lib.minecraft.nbt.tag.primitive.StringTag;
import dev.sbs.skyblockdata.model.Collection;
import dev.sbs.skyblockdata.model.Item;
import dev.sbs.skyblockdata.model.Minion;
import dev.sbs.skyblockdata.model.Pet;
import dev.sbs.skyblockdata.model.Stat;
import api.simplified.hypixel.common.Experience;
import dev.sbs.skyblockdata.common.Profile;
import api.simplified.hypixel.common.Weight;
import dev.sbs.simplifiedbot.persistence.model.AppUser;
import api.simplified.hypixel.profile_stats.data.AccessoryData;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.StreamUtil;
import dev.simplified.collection.unmodifiable.ConcurrentUnmodifiableList;
import dev.simplified.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

public abstract class SkyBlockUserCommand extends DiscordCommand<SlashCommandContext> {

    public static final Pattern MOJANG_NAME = Pattern.compile("[\\w]{3,16}");

    protected SkyBlockUserCommand(@NotNull DiscordBot discordBot) {
        super(discordBot);
    }

    @Override
    protected final @NotNull Mono<Void> process(@NotNull SlashCommandContext commandContext) throws DiscordException {
        return this.subprocess(commandContext, new SkyBlockUser(commandContext));
    }

    protected abstract @NotNull Mono<Void> subprocess(@NotNull SlashCommandContext commandContext, @NotNull SkyBlockUser skyBlockUser);

    @Override
    public @NotNull ConcurrentUnmodifiableList<Parameter> getParameters() {
        return Concurrent.newUnmodifiableList(
            Parameter.builder()
                .withName("name")
                .withDescription("Minecraft Username or UUID")
                .withType(Parameter.Type.WORD)
                .withValidator((argument, commandContext) -> StringUtil.isUUID(argument) || MOJANG_NAME.matcher(argument).matches())
                .isRequired()
                .build(),
            Parameter.builder()
                .withName("profile")
                .withDescription("SkyBlock Profile Name")
                .withType(Parameter.Type.WORD)
                .withValidator((argument, commandContext) -> Profile.of(argument.toUpperCase()).isPresent())
                .withChoices(Profile.CHOICES)
                .build()
        );
    }

    protected static Embed.Builder getEmbedBuilder(MojangProfile mojangProfile, SkyBlockIsland skyBlockIsland, String identifier) {
        return Embed.builder()
            .withAuthor(
                Author.builder()
                    .withName(mojangProfile.getUsername())
                    .build()
            )
            .withColor(Color.DARK_GRAY)
            .withTitle(StringUtil.capitalizeFully(identifier.replace("_", " ")))
            .withFooter(
                Footer.builder()
                    .withText(
                        "%s %s",
                        skyBlockIsland.getProfile().getSymbol(),
                        skyBlockIsland.getProfile().getName()
                    )
                    .withTimestamp(Instant.now())
                    .build()
            )
            .withThumbnailUrl(
                "https://crafatar.com/avatars/%s?overlay",
                mojangProfile.getUniqueId()
            );
    }

    protected <T extends Experience> Embed getSkillEmbed(
        MojangProfile mojangProfile,
        SkyBlockIsland skyBlockIsland,
        String value,
        ConcurrentList<T> experienceObjects,
        double average,
        double experience,
        double totalProgress,
        Function<T, String> nameFunction,
        Function<T, Optional<Emoji>> emojiFunction,
        boolean details
    ) {
        String emojiReplyStem = this.getEmoji("REPLY_STEM").map(emoji -> String.format("%s ", emoji.asFormat())).orElse("");
        String emojiReplyLine = this.getEmoji("REPLY_LINE").map(Emoji::asPreSpacedFormat).orElse("");
        String emojiReplyEnd = this.getEmoji("REPLY_END").map(emoji -> String.format("%s ", emoji.asFormat())).orElse("");
        Embed.Builder startBuilder;

        if (details) {
            startBuilder = getEmbedBuilder(mojangProfile, skyBlockIsland, value)
                .withField(
                    "Details",
                    String.format(
                        """
                            %1$sAverage Level: **%3$.2f**
                            %1$sTotal Experience: **%4$,f**
                            %2$sTotal Progress: **%5$.2f%%**""",
                        emojiReplyStem,
                        emojiReplyEnd,
                        average,
                        experience,
                        totalProgress
                    )
                );
        } else
            startBuilder = getEmbedBuilder(mojangProfile, skyBlockIsland, value);

        return startBuilder.withFields(
                experienceObjects.stream()
                    .map(experienceObject -> Field.builder()
                        .withName(StringUtil.capitalizeFully(nameFunction.apply(experienceObject).replace("_", " ")))
                        .withValue(String.format(
                            """
                                %1$sLevel: **%4$s**
                                %1$sExperience:
                                %2$s**%5$.2f**
                                %3$sProgress: **%6$.2f%%**""",
                            emojiReplyStem,
                            emojiReplyLine,
                            emojiReplyEnd,
                            experienceObject.getLevel(),
                            experienceObject.getExperience(),
                            experienceObject.getTotalProgressPercentage()
                        ))
                        .withEmoji(emojiFunction.apply(experienceObject))
                        .isInline()
                        .build()
                    )
                    .collect(Concurrent.toList())
            )
            .withEmptyField(true)
            .build();
    }

    public @NotNull ConcurrentList<TreePage> buildPages(@NotNull SkyBlockUser skyBlockUser) {
        String emojiReplyStem = this.getEmoji("REPLY_STEM").map(Emoji::asPreSpacedFormat).orElse("");
        String emojiReplyLine = this.getEmoji("REPLY_LINE").map(Emoji::asPreSpacedFormat).orElse("");
        String emojiReplyEnd = this.getEmoji("REPLY_END").map(Emoji::asPreSpacedFormat).orElse("");
        MojangProfile mojangProfile = skyBlockUser.getMojangProfile();
        SkyBlockIsland skyBlockIsland = skyBlockUser.getSelectedIsland();
        SkyBlockMember member = skyBlockUser.getMember();
        int uniqueMinions = skyBlockIsland.getUniqueMinions();

        // Weights
        Weight totalWeight = member.getTotalWeight();
        ConcurrentMap<SkillLevel, Weight> skillWeight = member.getSkills().getWeight();
        ConcurrentMap<SlayerBoss, Weight> slayerWeight = member.getSlayers().getWeight();
        ConcurrentMap<DungeonData, Weight> dungeonWeight = member.getDungeons().getWeight();
        ConcurrentMap<DungeonClass, Weight> dungeonClassWeight = member.getDungeons().getClassWeight();

        return Concurrent.newList(
            Page.builder()
                .withOption(getOptionBuilder("stats").withEmoji(Emoji.of(skyBlockIsland.getProfile().getSymbol())).build())
                .withEmbeds(
                    getEmbedBuilder(mojangProfile, skyBlockIsland, "stats")
                        .withFields(
                            Field.builder()
                                .withEmoji(this.getEmoji("STATUS_INFO"))
                                .withName("Details")
                                .withValue(
                                    """
                                        %1$sStatus: %3$s
                                        %1$sDeaths: %4$s
                                        %2$sGuild: %5$s
                                        """,
                                    emojiReplyStem,
                                    emojiReplyEnd,
                                    skyBlockUser.getSession().isOnline() ? "Online" : "Offline",
                                    member.getPlayerData().getDeathCount(),
                                    skyBlockUser.getGuild().map(HypixelGuild::getName).orElse("None")
                                )
                                .isInline()
                                .build(),
                            Field.builder()
                                .withEmoji(this.getEmoji("TRADING_COIN_PIGGY"))
                                .withName("Coins")
                                .withValue(
                                    """
                                        %sBank: %3$,.2f
                                        %sPurse: %4$,.2f
                                        """, // {3,number,#,###}
                                    emojiReplyStem,
                                    emojiReplyEnd,
                                    skyBlockIsland.getBanking().map(Banking::getBalance).orElse(0.0),
                                    member.getCurrencies().getPurse()
                                )
                                .isInline()
                                .build(),
                            Field.empty(true)
                        )
                        .withFields(
                            Field.builder()
                                .withEmoji(this.getEmoji("GEM_EMERALD"))
                                .withName("Community Upgrades")
                                .withValue(
                                    StringUtil.join(
                                        StreamUtil.prependEach(
                                                Arrays.stream(CommunityUpgrades.Type.values())
                                                    .map(upgradeType -> String.format(
                                                        "%s: %s / %s",
                                                        upgradeType.getName(),
                                                        skyBlockIsland.getCommunityUpgrades()
                                                            .map(communityUpgrades -> communityUpgrades.getHighestTier(upgradeType))
                                                            .orElse(0),
                                                        upgradeType.getMaxLevel()
                                                    )),
                                                emojiReplyStem,
                                                emojiReplyEnd
                                            )
                                            .collect(Concurrent.toList()),
                                        "\n"
                                    )
                                )
                                .isInline()
                                .build(),
                            Field.builder()
                                .withEmoji(this.getEmoji("SKYBLOCK_LAPIS_MINION"))
                                .withName("Minions")
                                .withValue(
                                    """
                                        %1$sSlots: %3$s
                                        %2$sUniques: %4$s
                                        """,
                                    emojiReplyStem,
                                    emojiReplyEnd,
                                    Minion.UNIQUE_CRAFTS.stream()
                                        .filter(threshold -> uniqueMinions >= threshold)
                                        .count() +
                                        skyBlockIsland.getCommunityUpgrades()
                                            .map(communityUpgrades -> communityUpgrades.getHighestTier(CommunityUpgrades.Type.MINION_SLOTS))
                                            .orElse(0),
                                    uniqueMinions
                                )
                                .isInline()
                                .build(),
                            Field.empty(true)
                        )
                        .build()
                )
                .build(),
            Page.builder()
                .withOption(getOptionBuilder("skills").withEmoji(this.getEmoji("SKILLS")).build())
                .withEmbeds(
                    getSkillEmbed(
                        mojangProfile,
                        skyBlockIsland,
                        "skills",
                        member.getSkills()
                            .getSkillLevels()
                            .stream()
                            .collect(Concurrent.toList()),
                        member.getSkills().getAverage(),
                        member.getSkills().getExperience(),
                        member.getSkills().getProgressPercentage(),
                        skill -> skill.getSkill().getName(),
                        skill -> this.getEmoji("SKILL_" + skill.getId()),
                        true
                    )
                )
                .build(),
            Page.builder()
                .withOption(getOptionBuilder("slayers").withEmoji(this.getEmoji("SLAYER")).build())
                .withEmbeds(
                    getSkillEmbed(
                        mojangProfile,
                        skyBlockIsland,
                        "slayers",
                        member.getSlayers()
                            .getBosses()
                            .stream()
                            .collect(Concurrent.toList()),
                        member.getSlayers().getAverage(),
                        member.getSlayers().getExperience(),
                        member.getSlayers().getProgressPercentage(),
                        slayer -> slayer.getSlayer().getName(),
                        slayer -> this.getEmoji("SLAYER_" + slayer.getId()),
                        true
                    )
                )
                .build(),
            Page.builder()
                .withOption(getOptionBuilder("weight").withEmoji(this.getEmoji("WEIGHT")).build())
                .withEmbeds(
                    getEmbedBuilder(mojangProfile, skyBlockIsland, "weight")
                        .withDescription(String.format(
                            """
                            %1$sTotal Weight: **%3$s** (**%4$s** + **%5$s**)
                            %1$sTotal Skill Weight: **%6$s** (**%7$s** + **%8$s**)
                            %1$sTotal Slayer Weight: **%9$s** (**%10$s** + **%11$s**)
                            %1$sTotal Dungeon Weight: **%12$s** (**%13$s** + **%14$s**)
                            %2$sTotal Dungeon Class Weight: **%15$s** (**%16$s** + **%17$s**)
                            """,
                            emojiReplyStem,
                            emojiReplyEnd,
                            totalWeight.getTotal(),
                            totalWeight.getValue(),
                            totalWeight.getOverflow(),
                            skillWeight.stream().map(Map.Entry::getValue).mapToDouble(Weight::getTotal).sum(),
                            skillWeight.stream().map(Map.Entry::getValue).mapToDouble(Weight::getValue).sum(),
                            skillWeight.stream().map(Map.Entry::getValue).mapToDouble(Weight::getOverflow).sum(),
                            slayerWeight.stream().map(Map.Entry::getValue).mapToDouble(Weight::getTotal).sum(),
                            slayerWeight.stream().map(Map.Entry::getValue).mapToDouble(Weight::getValue).sum(),
                            slayerWeight.stream().map(Map.Entry::getValue).mapToDouble(Weight::getOverflow).sum(),
                            dungeonWeight.stream().map(Map.Entry::getValue).mapToDouble(Weight::getTotal).sum(),
                            dungeonWeight.stream().map(Map.Entry::getValue).mapToDouble(Weight::getValue).sum(),
                            dungeonWeight.stream().map(Map.Entry::getValue).mapToDouble(Weight::getValue).sum(),
                            dungeonClassWeight.stream().map(Map.Entry::getValue).mapToDouble(Weight::getTotal).sum(),
                            dungeonClassWeight.stream().map(Map.Entry::getValue).mapToDouble(Weight::getValue).sum(),
                            dungeonClassWeight.stream().map(Map.Entry::getValue).mapToDouble(Weight::getOverflow).sum()
                        ))
                        .withFields(
                            getWeightFields(
                                "Skills",
                                skillWeight,
                                skill -> skill.getSkill().getName()
                            )
                        )
                        .withFields(
                            getWeightFields(
                                "Slayers",
                                slayerWeight,
                                slayer -> slayer.getSlayer().getName()
                            )
                        )
                        .withFields(
                            getWeightFields(
                                "Dungeons",
                                dungeonWeight,
                                dungeon -> DungeonData.Type.CATACOMBS.getName()
                            )
                        )
                        .withFields(
                            getWeightFields(
                                "Dungeon Classes",
                                dungeonClassWeight,
                                dungeonClass -> member.getDungeons()
                                    .getClasses()
                                    .stream()
                                    .filter(entry -> entry.getValue() == dungeonClass)
                                    .map(entry -> entry.getKey().getName())
                                    .findFirst()
                                    .orElse("Unknown")
                            )
                        )
                        .build()
                )
                .build(),
            Page.builder()
                .withOption(getOptionBuilder("pets").withEmoji(this.getEmoji("PETS")).build())
                .withEmbeds(
                    getEmbedBuilder(mojangProfile, skyBlockIsland, "pets")
                        .withDescription(String.format(
                            """
                            Unique Pets: **%s** / **%s**
                            Pet Score: **%s** (+%s Magic Find)
                            """,
                            member.getPets()
                                .getPets()
                                .stream()
                                .filter(StreamUtil.distinctByKey(OwnedPet::getId))
                                .collect(Concurrent.toList())
                                .size(),
                            SkyBlockData.getRepository(Pet.class)
                                .findAll()
                                .size(),
                            member.getPets().getPetScore(),
                            member.getPets().getPetScoreMagicFind()
                        ))
                        .build()
                )
                .withItemHandler(
                    ItemHandler.<OwnedPet>builder()
                        .withItems(member.getPets().getPets())
                        .withTransformer((pet, index, size) -> Section.builder()
                            .withAccessory(Button.builder()
                                .withStyle(Button.Style.SECONDARY)
                                .withLabel(
                                    "%s%s",
                                    pet.getPrettyName(),
                                    this.getEmoji(String.format("RARITY_%s", pet.getRarity().name()))
                                        .map(Emoji::asPreSpacedFormat)
                                        .orElse("")
                                )
                                .withEmoji(
                                    skyBlockUser.getSkyBlockEmojis()
                                        .getPetEmoji(pet.getId())
                                        .map(SkyBlockUserCommand::getEmoji)
                                )
                                .setEnabled(false)
                                .build()
                            )
                            .withComponents(TextDisplay.of(
                                """
                                    %1$sLevel: **%4$d** / **%5$d**
                                    %1$sExperience:
                                    %2$s**%6$f**
                                    %3$sProgress: **%7$f%%**
                                    """,
                                emojiReplyStem,
                                emojiReplyLine,
                                emojiReplyEnd,
                                pet.getLevel(),
                                pet.getMaxLevel(),
                                pet.getExperience(),
                                pet.getProgressPercentage()
                            ))
                            .build())
                        .withSorters(
                            Sorter.<OwnedPet>builder()
                                .withComparators((o1, o2) -> Comparator.comparing(OwnedPet::getRarityOrdinal)
                                    .thenComparingInt(Experience::getLevel)
                                    .reversed()
                                    .thenComparing(OwnedPet::getId)
                                    .compare(o1, o2)
                                )
                                .withLabel("Default")
                                .build()
                        )
                        .withAmountPerPage(12)
                        .build()
                )
                .build(),
            Page.builder()
                .withOption(getOptionBuilder("accessories").withEmoji(this.getEmoji("ACCESSORIES")).build())
                .withEmbeds(
                    getEmbedBuilder(mojangProfile, skyBlockIsland, "accessories")
                        .withDescription("If you wish to see missing accessory information, use the /missing command.")
                        .build()
                )
                .withItemHandler(
                    ItemHandler.<AccessoryData>builder()
                        .withItems(skyBlockIsland.getProfileStats(member).getAccessoryBag().getAccessories())
                        .withTransformer((accessoryData, index, size) -> Section.builder()
                            .withAccessory(Button.builder()
                                .withStyle(Button.Style.SECONDARY)
                                .withLabel(
                                    "%s%s",
                                    accessoryData.getAccessory().getItem().getDisplayName(),
                                    this.getEmoji(String.format("RARITY_%s", accessoryData.getRarity().name()))
                                        .map(Emoji::asPreSpacedFormat)
                                        .orElse("")
                                )
                                .withEmoji(
                                    skyBlockUser.getSkyBlockEmojis()
                                        .getEmoji(accessoryData.getAccessory().getItem().getId())
                                        .map(SkyBlockUserCommand::getEmoji)
                                )
                                .setEnabled(false)
                                .build()
                            )
                            .withComponents(TextDisplay.of(
                                """
                                %1$sRecombobulator: **%3$s**
                                %2$sEnrichment: **%4$s**
                                """,
                                emojiReplyStem,
                                emojiReplyEnd,
                                this.getEmoji(accessoryData.isRecombobulated() ? "ACTION_ACCEPT" : "ACTION_DENY")
                                    .map(Emoji::asFormat)
                                    .orElse("?"),
                                (accessoryData.getRarity().isEnrichable() ? accessoryData.getEnrichmentStat()
                                                                            .map(Stat::getId)
                                                                            .map(statId -> String.format("TALISMAN_ENRICHMENT_%s", statId))
                                                                            .flatMap(this::getEmoji)
                                                                            .or(() -> this.getEmoji("TAG_NOT_APPLICABLE")) :
                                    this.getEmoji("TAG_NOT_APPLICABLE"))
                                    .map(Emoji::asFormat)
                                    .orElse("N/A")
                            ))
                            .build()
                        )
                        .withSorters(
                            Sorter.<AccessoryData>builder()
                                .withComparators((o1, o2) -> Comparator.comparing(AccessoryData::getRarity)
                                    .reversed()
                                    .thenComparing(accessoryData -> accessoryData.getAccessory().getItem().getDisplayName())
                                    .compare(o1, o2)
                                )
                                .withLabel("Default")
                                .build()
                        )
                        .withSearch(
                            Search.<AccessoryData>builder()
                                .withPredicates((accessoryData, value) -> accessoryData.getAccessory().getItem().getId().equalsIgnoreCase(value))
                                .withPredicates((accessoryData, value) -> accessoryData.getAccessory().getItem().getDisplayName().equalsIgnoreCase(value))
                                .withPlaceholder("Searches by ID/Name")
                                .build()
                        )
                        .withAmountPerPage(12)
                        .build()
                )
                .build(),
            Page.builder()
                .withOption(getOptionBuilder("auctions").withEmoji(this.getEmoji("AUCTIONS")).build())
                .withEmbeds(
                    getEmbedBuilder(mojangProfile, skyBlockIsland, "auctions")
                        .withDescription(String.format(
                            """
                            %1$sUnclaimed Auctions: **%3$s**
                            %1$sExpired Auctions: **%4$s**
                            %2$sTotal Coins: **%5$s**
                            """,
                            emojiReplyStem,
                            emojiReplyEnd,
                            skyBlockUser.getAuctions()
                                .matchAll(SkyBlockAuction::notClaimed)
                                .count(),
                            skyBlockUser.getAuctions()
                                .matchAll(skyBlockAuction -> skyBlockAuction.getEndsAt().getRealTime() > System.currentTimeMillis())
                                .count(),
                            skyBlockUser.getAuctions()
                                .stream()
                                .mapToDouble(SkyBlockAuction::getHighestBid)
                                .sum()
                        ))
                        .build()
                )
                .withItemHandler(
                    ItemHandler.<SkyBlockAuction>builder()
                        .withItems(skyBlockUser.getAuctions())
                        .withTransformer((skyBlockAuction, index, size) -> {
                            CompoundTag auctionNbt = skyBlockAuction.getItem().getNbtData();
                            String itemId = auctionNbt.getPathOrDefault("tag.ExtraAttributes.id", StringTag.EMPTY).getValue();

                            return Section.builder()
                                .withAccessory(Button.builder()
                                    .withStyle(Button.Style.SECONDARY)
                                    .withLabel(
                                        "%s%s",
                                        SkyBlockData.getRepository(Item.class)
                                            .findFirst(Item::getId, itemId)
                                            .map(Item::getDisplayName)
                                            .orElse("Unknown"),
                                        this.getEmoji(String.format("RARITY_%s", skyBlockAuction.getRarity().name()))
                                            .map(Emoji::asPreSpacedFormat)
                                            .orElse("")
                                    )
                                    .withEmoji(
                                        skyBlockUser.getSkyBlockEmojis()
                                            .getEmoji(itemId)
                                            .map(SkyBlockUserCommand::getEmoji)
                                    )
                                    .setEnabled(false)
                                    .build()
                                )
                                .withComponents(TextDisplay.of(
                                    """
                                    %1$sStarting Bid: **%3$s**
                                    %1$sHighest Bid: **%4$s**
                                    %1$sEnds: **%5$s**
                                    %2$sHighest BIN: **%6$s**
                                    """,
                                    emojiReplyStem,
                                    emojiReplyEnd,
                                    skyBlockAuction.getStartingBid(),
                                    skyBlockAuction.getHighestBid(),
                                    new DiscordDate(skyBlockAuction.getEndsAt().getRealTime()).as(DiscordDate.Type.RELATIVE),
                                    skyBlockUser.getAuctionHouse()
                                        .getItems()
                                        .stream()
                                        .filter(auctionHouseItem -> auctionHouseItem.getItem()
                                            .getNbtData()
                                            .getPathOrDefault("tag.ExtraAttributes.id", StringTag.EMPTY)
                                            .getValue().equals(itemId)
                                        )
                                        .map(SkyBlockAuction::getHighestBid)
                                        .collect(Concurrent.toList())
                                        .sorted()
                                        .getOrDefault(0, 0L)
                                ))
                                .build();
                        })
                        .withAmountPerPage(12)
                        .build()
                )
                .build(),
            Page.builder()
                .withOption(getOptionBuilder("jacobs_farming").withEmoji(this.getEmoji("SKILL_FARMING")).build())
                .withEmbeds(
                    getEmbedBuilder(mojangProfile, skyBlockIsland, "jacobs_farming")
                        .withFields(
                            Field.builder()
                                .withName("Medals")
                                .withValue(
                                    Arrays.stream(JacobsContest.Medal.values())
                                        .map(farmingMedal -> String.format(
                                            "%s%s: %s",
                                            "",
                                            StringUtil.capitalizeEnum(farmingMedal),
                                            member.getJacobsContest().getMedals().get(farmingMedal)
                                        ))
                                        .collect(StreamUtil.toStringBuilder(true))
                                        .toString()
                                )
                                .isInline()
                                .build(),
                            Field.empty(true),
                            Field.builder()
                                .withName("Upgrades")
                                .withValue(String.format(
                                    """
                                        Farming Level Cap: %s
                                        Double Drops: %s
                                        """,
                                    member.getJacobsContest().getFarmingLevelCap(),
                                    member.getJacobsContest().getDoubleDrops()
                                ))
                                .isInline()
                                .build()
                        )
                        .withFields(
                            Field.builder()
                                .withName("Collection")
                                .withValue(
                                    SkyBlockData.getRepository(Collection.class)
                                        .findFirst(Collection::getId, "FARMING")
                                        .map(Collection::getItems)
                                        .stream()
                                        .flatMap(items -> items.stream().map(entry -> entry.getValue().getName()))
                                        .collect(StreamUtil.toStringBuilder(true))
                                        .toString()
                                )
                                .isInline()
                                .build(),
                            Field.builder()
                                .withName("Highscores")
                                .withValue(
                                    SkyBlockData.getRepository(Collection.class)
                                        .findFirst(Collection::getId, "FARMING")
                                        .map(Collection::getItems)
                                        .stream()
                                        .flatMap(items -> items.keySet().stream())
                                        .map(itemId -> member.getJacobsContest()
                                            .getContests()
                                            .stream()
                                            .filter(farmingContest -> farmingContest.getCollectionName().equals(itemId))
                                            .sorted((o1, o2) -> Comparator.comparing(JacobsContest.Contest::getCollected).compare(o2, o1))
                                            .map(JacobsContest.Contest::getCollected)
                                            .findFirst()
                                            .orElse(0)
                                        )
                                        .collect(StreamUtil.toStringBuilder(true))
                                        .toString()
                                )
                                .isInline()
                                .build(),
                            Field.builder()
                                .withName("Unique Gold")
                                .withValue(
                                    SkyBlockData.getRepository(Collection.class)
                                        .findFirst(Collection::getId, "FARMING")
                                        .map(Collection::getItems)
                                        .stream()
                                        .flatMap(items -> items.keySet().stream())
                                        .map(itemId -> member.getJacobsContest()
                                            .getUniqueBrackets()
                                            .stream()
                                            .filter(entry -> entry.getValue().contains(itemId))
                                            .findFirst()
                                            .map(entry -> "Yes")
                                            .orElse("No")
                                        )
                                        .collect(StreamUtil.toStringBuilder(true))
                                        .toString()
                                )
                                .isInline()
                                .build()
                        )
                        .build()
                )
                .build()
        );
    }

    private static <T extends Experience> ConcurrentList<Field> getWeightFields(String title, ConcurrentMap<T, Weight> weightMap, Function<T, String> typeNameFunction) {
        return Concurrent.newList(
            Field.builder()
                .withName(title)
                .withValue(
                    weightMap.stream()
                        .map(Map.Entry::getKey)
                        .map(typeNameFunction)
                        .collect(StreamUtil.toStringBuilder(true))
                        .toString()
                )
                .isInline()
                .build(),
            Field.builder()
                .withName("Weight")
                .withValue(
                    weightMap.stream()
                        .map(Map.Entry::getValue)
                        .map(Weight::getValue)
                        .collect(StreamUtil.toStringBuilder(true))
                        .toString()
                )
                .isInline()
                .build(),
            Field.builder()
                .withName("Overflow")
                .withValue(
                    weightMap.stream()
                        .map(Map.Entry::getValue)
                        .map(Weight::getOverflow)
                        .collect(StreamUtil.toStringBuilder(true))
                        .toString()
                )
                .isInline()
                .build()
        );
    }

    private static SelectMenu.Option.Builder getOptionBuilder(String identifier) {
        return SelectMenu.Option.builder()
            .withValue(identifier)
            .withLabel(StringUtil.capitalizeFully(identifier.replace("_", " ")));
    }

    protected static @NotNull Emoji getEmoji(@NotNull SkyBlockEmojis.Emoji sbEmoji) {
        return Emoji.of(
            sbEmoji.getId(),
            sbEmoji.getName(),
            sbEmoji.isAnimated()
        );
    }

    public final boolean isUserVerified(@NotNull UUID uniqueId) {
        return SkyBlockData.getRepository(AppUser.class).matchFirst(userModel -> userModel.getMojangUniqueIds().contains(uniqueId)).isPresent();
    }

}
