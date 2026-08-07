package dev.sbs.bot.write;

import com.google.gson.Gson;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import dev.simplified.persistence.JpaModel;
import dev.simplified.persistence.source.WriteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link WriteDispatcher}.
 *
 * <p>Uses {@link Proxy}-based stubs for {@link HazelcastInstance} and
 * {@link IQueue} instead of an embedded Hazelcast member so the suite stays
 * fast and hermetic. The dispatcher only touches
 * {@link HazelcastInstance#getQueue(String)},
 * {@link HazelcastInstance#getName()},
 * {@link HazelcastInstance#shutdown()}, and
 * {@link IQueue#put(Object)}, so the proxy handlers cover the full contract
 * surface without pulling in Hazelcast network bootstrap.
 */
class WriteDispatcherTest {

    private static final String SOURCE_ID = "skyblock-data";
    private static final String QUEUE_NAME = "skyblock.writes";

    private Gson gson;
    private List<WriteRequest> putLog;
    private AtomicBoolean shutdownCalled;
    private HazelcastInstance fakeHazelcast;
    /**
     * If non-null, the next {@code IQueue.put(...)} invocation throws this
     * exception. Lets the interrupt-handling test inject an
     * {@link InterruptedException} via the proxy.
     */
    private InterruptedException pendingPutInterrupt;

    @BeforeEach
    void setUp() {
        this.gson = new Gson();
        this.putLog = new ArrayList<>();
        this.shutdownCalled = new AtomicBoolean(false);
        this.pendingPutInterrupt = null;
        this.fakeHazelcast = this.newFakeHazelcast();
    }

    @Test
    @DisplayName("dispatchUpsert produces a WriteRequest with Operation=UPSERT and the expected fields")
    void dispatchUpsert_enqueuesCorrectEnvelope() {
        WriteDispatcher dispatcher = new WriteDispatcher(this.fakeHazelcast, this.gson, SOURCE_ID, QUEUE_NAME);
        TestEntity entity = new TestEntity("z-1", "Year of the Seal");

        dispatcher.dispatchUpsert(TestEntity.class, entity);

        assertThat(this.putLog, hasSize(1));
        WriteRequest enqueued = this.putLog.get(0);
        assertThat(enqueued.getOperation(), is(WriteRequest.Operation.UPSERT));
        assertThat(enqueued.getEntityClassName(), is(equalTo(TestEntity.class.getName())));
        assertThat(enqueued.getSourceId(), is(equalTo(SOURCE_ID)));
        assertThat(enqueued.getRequestId(), is(notNullValue()));
        assertThat(enqueued.getTimestamp(), is(notNullValue()));
        TestEntity roundTripped = this.gson.fromJson(enqueued.getEntityJson(), TestEntity.class);
        assertThat(roundTripped.getId(), is(equalTo("z-1")));
        assertThat(roundTripped.getName(), is(equalTo("Year of the Seal")));
    }

    @Test
    @DisplayName("dispatchDelete produces a WriteRequest with Operation=DELETE and the expected fields")
    void dispatchDelete_enqueuesCorrectEnvelope() {
        WriteDispatcher dispatcher = new WriteDispatcher(this.fakeHazelcast, this.gson, SOURCE_ID, QUEUE_NAME);
        TestEntity entity = new TestEntity("z-2", "Year of the Whale");

        dispatcher.dispatchDelete(TestEntity.class, entity);

        assertThat(this.putLog, hasSize(1));
        WriteRequest enqueued = this.putLog.get(0);
        assertThat(enqueued.getOperation(), is(WriteRequest.Operation.DELETE));
        assertThat(enqueued.getEntityClassName(), is(equalTo(TestEntity.class.getName())));
        assertThat(enqueued.getSourceId(), is(equalTo(SOURCE_ID)));
        TestEntity roundTripped = this.gson.fromJson(enqueued.getEntityJson(), TestEntity.class);
        assertThat(roundTripped.getId(), is(equalTo("z-2")));
    }

    @Test
    @DisplayName("dispatch(WriteRequest) passes a pre-built envelope through to the queue unchanged")
    void dispatchRawRequest_passthrough() {
        WriteDispatcher dispatcher = new WriteDispatcher(this.fakeHazelcast, this.gson, SOURCE_ID, QUEUE_NAME);
        TestEntity entity = new TestEntity("z-3", "Year of the Koi");
        WriteRequest prebuilt = WriteRequest.upsert(TestEntity.class, entity, this.gson, "some-other-source");

        dispatcher.dispatch(prebuilt);

        assertThat(this.putLog, hasSize(1));
        WriteRequest enqueued = this.putLog.get(0);
        assertThat(enqueued, is(sameInstance(prebuilt)));
        assertThat(enqueued.getSourceId(), is(equalTo("some-other-source")));
    }

    @Test
    @DisplayName("close() on a dispatcher constructed with an external HazelcastInstance does NOT shut it down")
    void close_externalHazelcast_doesNotShutDown() {
        WriteDispatcher dispatcher = new WriteDispatcher(this.fakeHazelcast, this.gson, SOURCE_ID, QUEUE_NAME);

        dispatcher.close();

        assertThat(this.shutdownCalled.get(), is(false));
    }

    @Test
    @DisplayName("close() on a dispatcher that owns its HazelcastInstance DOES shut it down")
    void close_ownedHazelcast_shutsDown() {
        WriteDispatcher dispatcher = new WriteDispatcher(this.fakeHazelcast, this.gson, SOURCE_ID, QUEUE_NAME, true);

        dispatcher.close();

        assertThat(this.shutdownCalled.get(), is(true));
    }

    @Test
    @DisplayName("dispatch() preserves the interrupt flag and wraps InterruptedException in IllegalStateException")
    void dispatchInterrupted_preservesInterruptFlag() {
        this.pendingPutInterrupt = new InterruptedException("simulated interrupt from IQueue.put");
        WriteDispatcher dispatcher = new WriteDispatcher(this.fakeHazelcast, this.gson, SOURCE_ID, QUEUE_NAME);
        TestEntity entity = new TestEntity("z-4", "Year of the Horse");

        try {
            IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> dispatcher.dispatchUpsert(TestEntity.class, entity)
            );
            assertThat(thrown.getCause(), is(sameInstance(this.pendingPutInterrupt)));
            assertThat(Thread.currentThread().isInterrupted(), is(true));
        } finally {
            // Clear interrupt flag so subsequent tests in the class are not affected
            // if JUnit reuses the worker thread.
            Thread.interrupted();
        }
        assertThat(this.putLog, hasSize(0));
    }

    @Test
    @DisplayName("queue name and source id exposed via getters match constructor inputs")
    void accessors_returnConstructorValues() {
        WriteDispatcher dispatcher = new WriteDispatcher(this.fakeHazelcast, this.gson, "other-source", "other.queue");

        assertThat(dispatcher.getSourceId(), is(equalTo("other-source")));
        assertThat(dispatcher.getQueueName(), is(equalTo("other.queue")));
    }

    @Test
    @DisplayName("DEFAULT_SOURCE_ID and DEFAULT_QUEUE_NAME constants match the locked Phase 6 wire format")
    void constants_matchPhase6WireFormat() {
        assertThat(WriteDispatcher.DEFAULT_SOURCE_ID, is(equalTo("skyblock-data")));
        assertThat(WriteDispatcher.DEFAULT_QUEUE_NAME, is(equalTo("skyblock.writes")));
    }

    @Test
    @DisplayName("each dispatchUpsert call mints a distinct request id even for the same entity instance")
    void dispatchUpsert_mintsDistinctRequestIdsPerCall() {
        WriteDispatcher dispatcher = new WriteDispatcher(this.fakeHazelcast, this.gson, SOURCE_ID, QUEUE_NAME);
        TestEntity entity = new TestEntity("z-5", "Year of the Wolf");

        dispatcher.dispatchUpsert(TestEntity.class, entity);
        dispatcher.dispatchUpsert(TestEntity.class, entity);

        assertThat(this.putLog, hasSize(2));
        assertThat(this.putLog.get(0).getRequestId(), is(not(equalTo(this.putLog.get(1).getRequestId()))));
    }

    /**
     * Builds a {@link Proxy}-based fake {@link HazelcastInstance} that routes
     * {@code getQueue(String)} to a fake {@link IQueue} whose {@code put}
     * records invocations into {@link #putLog} (or throws
     * {@link #pendingPutInterrupt} if set), and whose {@code getName()} +
     * {@code shutdown()} hit {@link #shutdownCalled}. Every other method
     * throws {@link UnsupportedOperationException} so any unintended
     * interaction surfaces loudly.
     */
    private HazelcastInstance newFakeHazelcast() {
        @SuppressWarnings("unchecked")
        IQueue<WriteRequest> fakeQueue = (IQueue<WriteRequest>) Proxy.newProxyInstance(
            IQueue.class.getClassLoader(),
            new Class<?>[]{IQueue.class},
            (proxy, method, args) -> {
                if ("put".equals(method.getName()) && method.getParameterCount() == 1) {
                    if (this.pendingPutInterrupt != null)
                        throw this.pendingPutInterrupt;
                    this.putLog.add((WriteRequest) args[0]);
                    return null;
                }
                if ("getName".equals(method.getName()))
                    return QUEUE_NAME;
                throw new UnsupportedOperationException("FakeIQueue: " + method.getName());
            }
        );

        return (HazelcastInstance) Proxy.newProxyInstance(
            HazelcastInstance.class.getClassLoader(),
            new Class<?>[]{HazelcastInstance.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getQueue":
                        return fakeQueue;
                    case "getName":
                        return "fake-hz";
                    case "shutdown":
                        this.shutdownCalled.set(true);
                        return null;
                    default:
                        throw new UnsupportedOperationException("FakeHazelcastInstance: " + method.getName());
                }
            }
        );
    }

    /**
     * Minimal {@link JpaModel} used by these tests. Has two {@link String}
     * fields so Gson round-tripping verifies the envelope preserves entity
     * state end-to-end.
     */
    @SuppressWarnings("ClassCanBeRecord")
    private static final class TestEntity implements JpaModel {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String id;
        private final String name;

        TestEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

    }

}
