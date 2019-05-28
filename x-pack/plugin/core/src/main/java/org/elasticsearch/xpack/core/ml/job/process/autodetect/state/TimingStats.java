/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.ml.job.process.autodetect.state;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.core.ml.job.config.Job;

import java.io.IOException;
import java.util.Objects;

import static org.elasticsearch.common.xcontent.ConstructingObjectParser.constructorArg;

/**
 * Stats that give more insight into timing of various operations performed as part of anomaly detection job.
 */
public class TimingStats implements ToXContentObject, Writeable {

    public static final ParseField AVG_BUCKET_PROCESSING_TIME_MS = new ParseField("average_bucket_processing_time_ms");

    public static final ParseField TYPE = new ParseField("timing_stats");

    public static final ConstructingObjectParser<TimingStats, Void> PARSER =
        new ConstructingObjectParser<>(
            TYPE.getPreferredName(),
            true,
            args -> new TimingStats((String) args[0], (double) args[1]));

    static {
        PARSER.declareString(constructorArg(), Job.ID);
        PARSER.declareDouble(constructorArg(), AVG_BUCKET_PROCESSING_TIME_MS);
    }

    public static String documentId(String jobId) {
        return jobId + "_timing_stats";
    }

    private final String jobId;
    private double avgBucketProcessingTimeMs;

    public TimingStats(String jobId, double avgBucketProcessingTimeMs) {
        this.jobId = jobId;
        this.avgBucketProcessingTimeMs = avgBucketProcessingTimeMs;
    }

    public TimingStats(String jobId) {
        this.jobId = jobId;
    }

    public TimingStats(TimingStats lhs) {
        jobId = lhs.jobId;
        avgBucketProcessingTimeMs = lhs.avgBucketProcessingTimeMs;
    }

    public TimingStats(StreamInput in) throws IOException {
        jobId = in.readString();
        avgBucketProcessingTimeMs = in.readDouble();
    }

    public String getJobId() {
        return jobId;
    }

    public double getAvgBucketProcessingTimeMs() {
        return avgBucketProcessingTimeMs;
    }

    public void incrementTotalBucketProcessingTimeMs(int oldBucketCount, long bucketProcessingTimeMs) {
        avgBucketProcessingTimeMs = (avgBucketProcessingTimeMs * oldBucketCount + bucketProcessingTimeMs) / (oldBucketCount + 1);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(jobId);
        out.writeOptionalDouble(avgBucketProcessingTimeMs);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder
            .startObject()
            .field(Job.ID.getPreferredName(), jobId)
            .field(AVG_BUCKET_PROCESSING_TIME_MS.getPreferredName(), avgBucketProcessingTimeMs)
            .endObject();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimingStats that = (TimingStats) o;
        return Objects.equals(this.jobId, that.jobId)
            && this.avgBucketProcessingTimeMs == that.avgBucketProcessingTimeMs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, avgBucketProcessingTimeMs);
    }

    @Override
    public String toString() {
        return Strings.toString(this);
    }
}
