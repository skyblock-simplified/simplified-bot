package dev.sbs.bot.write;

import com.google.gson.Gson;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.simplified.persistence.JpaModel;
import dev.simplified.persistence.source.WriteRequest;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

/**
 * Producer-side SDK for the Phase 6c Hazelcast {@code IQueue<WriteRequest>}
 * write path.
 *
 * <p>Translates bot-side entity mutations into
 * {@link WriteRequest} envelopes and puts them onto the Hazelcast
 * {@link IQueue} named {@link #DEFAULT_QUEUE_NAME} (or a caller-supplied
 * override). The consumer side lives in {@code data} and drains
 * the queue via {@code WriteQueueConsumer}.
 *
 * <p>This class is deliberately thin - it does not buffer, retry, or
 * acknowledge. Durability is provided by the server-side IQueue's sync-backup
 * configuration (see {@code infra/hazelcast/hazelcast.xml}). Producers call
 * {@link #dispatchUpsert(Class, JpaModel)} or
 * {@link #dispatchDelete(Class, JpaModel)} and return immediately.
 *
 * <p>Two construction modes are supported:
 * <ul>
 *   <li>{@link #WriteDispatcher(HazelcastInstance, Gson, String, String)} -
 *       caller-owned {@code HazelcastInstance}. The dispatcher does not
 *       manage the instance lifecycle, and {@link #close()} is a no-op for
 *       the Hazelcast connection. Useful when a single {@code HazelcastInstance}
 *       is shared across multiple concerns (write dispatch, read caches,
 *       distributed locks, etc.) and the caller tears it down explicitly.</li>
 *   <li>{@link #createClient()} (and overloads) - the dispatcher owns a
 *       freshly-created {@link HazelcastClient#newHazelcastClient()} and
 *       shuts it down on {@link #close()}. Useful when the dispatcher is
 *       the only Hazelcast consumer in the process.</li>
 * </ul>
 *
 * <p>Phase 6c is SDK only - no eager instantiation happens in
 * {@link dev.sbs.bot.SimplifiedBot}. Callers that want to activate
 * the dispatcher must wire it manually in their own startup path.
 *
 * @see WriteRequest
 * @see dev.simplified.persistence.source.MutableSource
 */
@Log4j2
public final class WriteDispatcher implements AutoCloseable {

    /**
     * Default Hazelcast {@link IQueue} name for the SkyBlock write path.
     * Matches the server-side queue declared in {@code infra/hazelcast/hazelcast.xml}
     * and consumed by {@code data}'s {@code WriteQueueConsumer}.
     */
    public static final @NotNull String DEFAULT_QUEUE_NAME = "skyblock.writes";

    /**
     * Default {@code sourceId} carried on every {@link WriteRequest}. Matches
     * the {@code skyblock-data} source id used by the Phase 4a
     * {@code RemoteJsonSource} chain and the Phase 6b
     * {@code WritableRemoteJsonSource} consumer mapping.
     */
    public static final @NotNull String DEFAULT_SOURCE_ID = "skyblock-data";

    private final @NotNull HazelcastInstance hazelcast;
    private final @NotNull Gson gson;
    private final @NotNull String sourceId;
    private final @NotNull String queueName;
    private final boolean ownsHazelcast;

    /**
     * Constructs a dispatcher that uses a caller-owned
     * {@link HazelcastInstance}. {@link #close()} does NOT shut down the
     * instance - the caller retains full lifecycle ownership.
     *
     * @param hazelcast the Hazelcast client or member to publish through
     * @param gson the Gson instance used to serialize entities into the
     *             {@link WriteRequest#getEntityJson()} payload
     * @param sourceId the {@code sourceId} carried on every request
     * @param queueName the name of the target {@link IQueue}
     */
    public WriteDispatcher(
        @NotNull HazelcastInstance hazelcast,
        @NotNull Gson gson,
        @NotNull String sourceId,
        @NotNull String queueName
    ) {
        this(hazelcast, gson, sourceId, queueName, false);
    }

    /**
     * Package-private canonical constructor used by {@link #createClient()}
     * and the test harness. The {@code ownsHazelcast} flag controls whether
     * {@link #close()} shuts down {@code hazelcast}.
     */
    WriteDispatcher(
        @NotNull HazelcastInstance hazelcast,
        @NotNull Gson gson,
        @NotNull String sourceId,
        @NotNull String queueName,
        boolean ownsHazelcast
    ) {
        this.hazelcast = hazelcast;
        this.gson = gson;
        this.sourceId = sourceId;
        this.queueName = queueName;
        this.ownsHazelcast = ownsHazelcast;
    }

    /**
     * Creates a dispatcher that owns a fresh {@link HazelcastClient} loaded
     * from the classpath {@code hazelcast-client.xml}. The Gson instance is
     * pulled from {@link MinecraftApi#getGson()}, and the default source id
     * and queue name are used.
     *
     * <p>{@link #close()} on the returned dispatcher shuts down the underlying
     * Hazelcast client.
     *
     * @return a new dispatcher wired against a freshly-created Hazelcast
     *         client
     */
    public static @NotNull WriteDispatcher createClient() {
        return createClient(DEFAULT_SOURCE_ID, DEFAULT_QUEUE_NAME);
    }

    /**
     * Creates a dispatcher that owns a fresh {@link HazelcastClient} loaded
     * from the classpath {@code hazelcast-client.xml}. The Gson instance is
     * pulled from {@link MinecraftApi#getGson()}.
     *
     * <p>{@link #close()} on the returned dispatcher shuts down the underlying
     * Hazelcast client.
     *
     * @param sourceId the {@code sourceId} carried on every request
     * @param queueName the name of the target {@link IQueue}
     * @return a new dispatcher wired against a freshly-created Hazelcast
     *         client
     */
    public static @NotNull WriteDispatcher createClient(@NotNull String sourceId, @NotNull String queueName) {
        HazelcastInstance instance = HazelcastClient.newHazelcastClient();
        log.info(
            "bot write dispatch: Hazelcast client '{}' connected to cluster '{}', queue '{}', source '{}'",
            instance.getName(), instance.getConfig().getClusterName(), queueName, sourceId
        );
        return new WriteDispatcher(instance, MinecraftApi.getGson(), sourceId, queueName, true);
    }

    /**
     * Builds an {@link WriteRequest.Operation#UPSERT} envelope for the given
     * entity and enqueues it on the configured Hazelcast {@link IQueue}.
     *
     * <p>Blocks until the put completes; on interrupt the thread's interrupt
     * flag is re-asserted and an {@link IllegalStateException} is thrown
     * wrapping the {@link InterruptedException}.
     *
     * @param entityType the entity class, used to capture the FQCN on the
     *                   envelope
     * @param entity the entity to persist
     * @param <T> the entity type
     */
    public <T extends JpaModel> void dispatchUpsert(@NotNull Class<T> entityType, @NotNull T entity) {
        this.dispatch(WriteRequest.upsert(entityType, entity, this.gson, this.sourceId));
    }

    /**
     * Builds a {@link WriteRequest.Operation#DELETE} envelope for the given
     * entity and enqueues it on the configured Hazelcast {@link IQueue}.
     *
     * <p>Blocks until the put completes; on interrupt the thread's interrupt
     * flag is re-asserted and an {@link IllegalStateException} is thrown
     * wrapping the {@link InterruptedException}.
     *
     * @param entityType the entity class, used to capture the FQCN on the
     *                   envelope
     * @param entity the entity to remove
     * @param <T> the entity type
     */
    public <T extends JpaModel> void dispatchDelete(@NotNull Class<T> entityType, @NotNull T entity) {
        this.dispatch(WriteRequest.delete(entityType, entity, this.gson, this.sourceId));
    }

    /**
     * Enqueues a pre-built {@link WriteRequest} on the configured Hazelcast
     * {@link IQueue}. Useful when a caller has already constructed the
     * envelope via {@link WriteRequest#upsert(Class, JpaModel, Gson, String)}
     * or {@link WriteRequest#delete(Class, JpaModel, Gson, String)} and wants
     * the dispatcher to handle only the queue interaction.
     *
     * <p>Blocks until the put completes; on interrupt the thread's interrupt
     * flag is re-asserted and an {@link IllegalStateException} is thrown
     * wrapping the {@link InterruptedException}.
     *
     * @param request the envelope to enqueue
     */
    public void dispatch(@NotNull WriteRequest request) {
        IQueue<WriteRequest> queue = this.hazelcast.getQueue(this.queueName);

        try {
            queue.put(request);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                String.format("Interrupted while enqueuing WriteRequest '%s'", request.getRequestId()),
                ex
            );
        }

        log.debug(
            "dispatched {} request '{}' for '{}' on queue '{}' (source '{}')",
            request.getOperation(), request.getRequestId(), request.getEntityClassName(), this.queueName, this.sourceId
        );
    }

    /**
     * Releases resources held by this dispatcher.
     *
     * <p>If this dispatcher created its own {@link HazelcastInstance} via
     * {@link #createClient()}, the instance is shut down here. If the caller
     * supplied the instance to the constructor directly, this method is a
     * no-op for the Hazelcast connection and lifecycle remains entirely the
     * caller's responsibility.
     */
    @Override
    public void close() {
        if (this.ownsHazelcast) {
            log.info("Shutting down owned Hazelcast client '{}'", this.hazelcast.getName());
            this.hazelcast.shutdown();
        }
    }

    /**
     * The source id this dispatcher stamps on every outgoing
     * {@link WriteRequest}.
     */
    public @NotNull String getSourceId() {
        return this.sourceId;
    }

    /**
     * The name of the Hazelcast {@link IQueue} this dispatcher publishes to.
     */
    public @NotNull String getQueueName() {
        return this.queueName;
    }

}
