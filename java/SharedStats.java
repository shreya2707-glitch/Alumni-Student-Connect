import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SharedStats - Thread-safe cross-component metrics holder.
 * Allows background threads to communicate runtime statistics
 * to UI monitor components without lock contention.
 */
public class SharedStats {

    // RUBRIC: [Concept: Atomic Variable for Lock-Free Counter]
    // Tracks pending unwritten chat messages waiting in the queue.
    public static final AtomicInteger activeChatQueueSize = new AtomicInteger(0);

    // RUBRIC: [Concept: Volatile Variable for Memory Visibility]
    // Guarantees immediate cross-thread visibility of timestamp without needing synchronized blocks.
    public static volatile long lastDbPollTimeMillis = System.currentTimeMillis();

    // Telemetry: Reader Thread Latency (ms)
    public static volatile long lastReaderLatencyMs = 0;

    // Telemetry: Total messages processed by writer (for Throughput computation)
    public static final AtomicLong totalMessagesWritten = new AtomicLong(0);

    // Telemetry: Live DB Heartbeat indicator signal
    public static final AtomicLong lastSuccessfulDbSyncMillis = new AtomicLong(0);

    private SharedStats() {}
}
