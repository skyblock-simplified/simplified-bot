package dev.sbs.bot.util;

import dev.sbs.discordapi.exception.DiscordException;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.hypixel.exception.HypixelApiException;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelContract;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockAuction;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockAuctions;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockAuctionsEnded;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockBazaar;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockProduct;
import dev.sbs.minecraftapi.nbt.tags.primitive.StringTag;
import dev.sbs.minecraftapi.persistence.model.Item;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.scheduler.Scheduler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

@Getter
public class ItemCache {

    private final AuctionHouse auctionHouse = new AuctionHouse();
    private final Bazaar bazaar = new Bazaar();
    private final EndedAuctions endedAuctions = new EndedAuctions();

    public final double getPrice(Item item) {
        return this.getPrice(item, 3);
    }

    public final double getPrice(Item item, int averageWith) {
        return this.getPrice(item, averageWith, false);
    }

    public final double getPrice(Item item, int averageWith, boolean recentHistory) {
        Optional<SkyBlockProduct> bazaarItem = this.getBazaar()
            .getItems()
            .stream()
            .filter(product -> product.getItemId().equals(item.getId()))
            .findFirst();

        double price;

        if (bazaarItem.isPresent())
            price = bazaarItem.get().getQuickStatus().getBuyPrice();
        else {
            if (recentHistory) {
                ConcurrentList<SkyBlockAuction.Ended> endedAuctions = this.getEndedAuctions()
                    .getItems()
                    .stream()
                    .filter(endedAuction -> item.getId().equals(
                            endedAuction.getItem()
                                .getNbtData()
                                .getPathOrDefault("tag.ExtraAttributes.id", StringTag.EMPTY)
                                .getValue()
                    ))
                    .collect(Concurrent.toList())
                    .sorted(endedAuction -> endedAuction.getTimestamp().getRealTime());

                // Calculate Recent Price
                double totalPrice = IntStream.range(0, averageWith)
                    .mapToDouble(index -> endedAuctions.get(index).getPrice())
                    .sum();
                price = totalPrice / averageWith;
            } else {
                ConcurrentList<SkyBlockAuction> activeAuctions = this.getAuctionHouse()
                    .getItems()
                    .stream()
                    .filter(activeAuction -> item.getId().equals(
                        activeAuction.getItem()
                            .getNbtData()
                            .getPathOrDefault("tag.ExtraAttributes.id", StringTag.EMPTY)
                            .getValue()
                    ))
                    .collect(Concurrent.toList())
                    .sorted(activeAuction -> activeAuction.getEndsAt().getRealTime());

                // Calculate Current Price
                double totalPrice = IntStream.range(0, averageWith)
                    .mapToDouble(index -> activeAuctions.get(index).getHighestBid())
                    .sum();
                price = totalPrice / averageWith;
            }
        }

        return price;
    }

    public static class AuctionHouse extends Cache<SkyBlockAuction> {

        @Override
        public long process() {
            try {
                HypixelContract hypixel = MinecraftApi.getClient(HypixelContract.class).getContract();
                ConcurrentList<SkyBlockAuction> auctions = Concurrent.newList();
                SkyBlockAuctions skyBlockAuctionsResponse = hypixel.getAuctions();
                auctions.addAll(skyBlockAuctionsResponse.getAuctions());
                long lastUpdated = skyBlockAuctionsResponse.getLastUpdated().getRealTime();

                // Handle Auction Pages
                for (int i = 1; i < skyBlockAuctionsResponse.getTotalPages(); i++) {
                    try {
                        SkyBlockAuctions skyBlockAuctionsPageResponse = hypixel.getAuctions(i);
                        auctions.addAll(skyBlockAuctionsPageResponse.getAuctions());

                        if (skyBlockAuctionsPageResponse.getLastUpdated().getRealTime() > lastUpdated)
                            lastUpdated = skyBlockAuctionsPageResponse.getLastUpdated().getRealTime();
                    } catch (HypixelApiException hypixelApiException) {
                        if (hypixelApiException.getStatus().getCode() == 404)
                            break;
                        else
                            throw hypixelApiException;
                    }
                }

                this.replaceItems(auctions);
                return lastUpdated;
            } catch (Exception exception) {
                throw new DiscordException("Unable to update entire Auction House cache!", exception);
            }
        }

    }

    public static class Bazaar extends Cache<SkyBlockProduct> {

        @Override
        public long process() {
            SkyBlockBazaar skyBlockBazaarResponse = MinecraftApi.getClient(HypixelContract.class).getContract().getBazaar();
            this.replaceItems(skyBlockBazaarResponse.getProducts().values());
            return skyBlockBazaarResponse.getLastUpdated().getRealTime();
        }

    }

    public static class EndedAuctions extends Cache<SkyBlockAuction.Ended> {

        @Override
        protected long process() {
            SkyBlockAuctionsEnded skyBlockAuctionsEndedResponse = MinecraftApi.getClient(HypixelContract.class).getContract().getEndedAuctions();
            this.replaceItems(skyBlockAuctionsEndedResponse.getAuctions());
            return skyBlockAuctionsEndedResponse.getLastUpdated().getRealTime();
        }

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static abstract class Cache<T> {

        @Getter private long lastUpdated;
        @Getter private long nextUpdate;
        private final ConcurrentList<T> items = Concurrent.newList();
        private final AtomicBoolean replacing = new AtomicBoolean();
        private final AtomicBoolean updating = new AtomicBoolean();

        public final ConcurrentList<T> getItems() {
            while (this.replacing.get())
                Scheduler.sleep(1);

            return Concurrent.newUnmodifiableList(this.items);
        }

        public final boolean isExpired() {
            return System.currentTimeMillis() >= this.getNextUpdate();
        }

        public final boolean isUpdating() {
            return this.updating.get();
        }

        protected void replaceItems(Collection<T> items) {
            this.replacing.set(true);
            this.items.clear();
            this.items.addAll(items);
            this.replacing.set(false);
        }

        protected abstract long process();

        public final void update() {
            if (this.isExpired() && !this.isUpdating()) {
                this.updating.set(true);
                long nextUpdate = this.process();
                this.updating.set(false);
                this.lastUpdated = System.currentTimeMillis();
                this.nextUpdate = nextUpdate + (2 * 60 * 1000);
            }
        }

    }

}
