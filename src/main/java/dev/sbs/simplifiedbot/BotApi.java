package dev.sbs.simplifiedbot;

import com.google.gson.Gson;
import dev.sbs.hypixelapi.HypixelContract;
import dev.sbs.hypixelapi.exception.HypixelApiException;
import dev.sbs.sbsapi.SbsContract;
import dev.sbs.sbsapi.exception.SbsApiException;
import dev.sbs.simplifiedbot.write.WriteDispatcher;
import dev.simplified.client.Client;
import dev.simplified.client.ClientConfig;
import dev.simplified.gson.GsonSettings;
import dev.simplified.manager.KeyManager;
import dev.simplified.manager.Manager;
import dev.simplified.scheduler.Scheduler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Bot-local service locator that replaces the former {@code MinecraftApi} static holder.
 * <p>
 * Owns the {@link Gson} and {@link GsonSettings} used by the bot for contract I/O and
 * the {@link WriteDispatcher}, a {@link KeyManager} that supplies the Hypixel API key
 * header on demand, a {@link Scheduler} for async/recurring work, and the {@link Client}
 * instances for the Hypixel and SBS contracts. Persistence access flows through
 * {@code dev.sbs.skyblockdata.SkyBlockData} directly - this locator does not own it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotApi {

    @Getter private static final @NotNull GsonSettings gsonSettings = GsonSettings.defaults();
    @Getter private static final @NotNull Gson gson = gsonSettings.create();

    @Getter private static final @NotNull KeyManager keyManager = new KeyManager(
        (entry, key) -> key.equalsIgnoreCase(entry.getKey()),
        Manager.Mode.UPDATE
    );

    @Getter private static final @NotNull Scheduler scheduler = new Scheduler();

    @Getter private static final @NotNull Client<HypixelContract> hypixelClient = Client.create(
        ClientConfig.builder(HypixelContract.class, gson)
            .withErrorDecoder(HypixelApiException::new)
            .withDynamicHeader("API-Key", keyManager.getSupplier("HYPIXEL_API_KEY"))
            .build()
    );

    @Getter private static final @NotNull Client<SbsContract> sbsClient = Client.create(
        ClientConfig.builder(SbsContract.class, gson)
            .withErrorDecoder(SbsApiException::new)
            .build()
    );

}
