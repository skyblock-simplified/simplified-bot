/**
 * Phase 6c write-dispatch SDK for the bot producer side of the
 * Hazelcast {@code IQueue<WriteRequest>} write path.
 *
 * <p>This package holds the {@link dev.sbs.bot.write.WriteDispatcher}
 * utility that translates bot-side entity mutations into
 * {@link dev.simplified.persistence.source.WriteRequest} envelopes and puts
 * them onto the Hazelcast {@code skyblock.writes} IQueue. The consumer side
 * lives in the {@code data} service and drains the queue.
 *
 * <p>Phase 6c ships the dispatcher as an SDK only - it is NOT eagerly
 * instantiated from {@link dev.sbs.bot.SimplifiedBot} or from any
 * Discord command. Concrete write triggers land in later phases once specific
 * mutation flows are identified.
 *
 * <p>Callers that want to wire the dispatcher should use
 * {@link dev.sbs.bot.write.WriteDispatcher#createClient()} (or one
 * of its overloads), hold the returned instance for the lifetime of the bot
 * process, and call {@link dev.sbs.bot.write.WriteDispatcher#close()}
 * on shutdown. The dispatcher is a thin pass-through - it does not buffer,
 * retry, or acknowledge. Durability is provided by the Hazelcast IQueue's
 * sync-backup-1 configuration on the server side.
 *
 * @see dev.simplified.persistence.source.WriteRequest
 * @see dev.simplified.persistence.source.MutableSource
 */
package dev.sbs.bot.write;
