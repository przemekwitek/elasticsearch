package org.elasticsearch.client.ml.job.process;

import org.elasticsearch.client.ml.job.config.Job;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

import static org.elasticsearch.common.xcontent.ConstructingObjectParser.constructorArg;

/**
 * Stats that give more insight into timing of various operations performed as part of anomaly detection job.
 */
public class TimingStats implements ToXContentObject {

    public static final ParseField AVG_BUCKET_PROCESSING_TIME_MS = new ParseField("average_bucket_processing_time_ms");

    public static final ConstructingObjectParser<TimingStats, Void> PARSER =
        new ConstructingObjectParser<>(
            "timing_stats",
            true,
            args -> new TimingStats((String) args[0], Duration.ofMillis((long) args[1])));

    static {
        PARSER.declareString(constructorArg(), Job.ID);
        PARSER.declareDouble(constructorArg(), AVG_BUCKET_PROCESSING_TIME_MS);
    }

    private final String jobId;
    private final Duration avgBucketProcessingTime;

    public TimingStats(String jobId, @Nullable Duration avgBucketProcessingTime) {
        this.jobId = jobId;
        this.avgBucketProcessingTime = avgBucketProcessingTime;
    }

    public String getJobId() {
        return jobId;
    }

    public Duration getAvgBucketProcessingTime() {
        return avgBucketProcessingTime;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        return builder
            .startObject()
            .field(Job.ID.getPreferredName(), jobId)
            .field(AVG_BUCKET_PROCESSING_TIME_MS.getPreferredName(), avgBucketProcessingTime)
            .endObject();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimingStats that = (TimingStats) o;
        return Objects.equals(this.jobId, that.jobId)
            && Objects.equals(this.avgBucketProcessingTime, that.avgBucketProcessingTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, avgBucketProcessingTime);
    }

    @Override
    public String toString() {
        return Strings.toString(this);
    }
}
