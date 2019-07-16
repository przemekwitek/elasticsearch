/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.ml.utils;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.core.common.time.TimeUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Objects;

import static org.elasticsearch.common.xcontent.ConstructingObjectParser.optionalConstructorArg;

/**
 * Utility for calculating current value of exponentially-weighted moving average per fixed-sized time window.
 */
public class ExponentialAverageCalculationContext implements Writeable, ToXContentObject {

    public static final ParseField INCREMENTAL_TIME_METRIC_MS = new ParseField("incremental_time_metric_ms");
    public static final ParseField LATEST_TIMESTAMP = new ParseField("latest_timestamp");
    public static final ParseField PREVIOUS_EXPONENTIAL_AVERAGE_MS = new ParseField("previous_exponential_average_ms");

    public static final ConstructingObjectParser<ExponentialAverageCalculationContext, Void> PARSER =
        new ConstructingObjectParser<>(
            "exponential_average_calculation_context",
            true,
            args -> {
                Double incrementalTimeMetricValueMs = (Double) args[0];
                Instant latestTimestamp = (Instant) args[1];
                Double previousExponentialAverageMs = (Double) args[2];
                return new ExponentialAverageCalculationContext(
                    getOrDefault(incrementalTimeMetricValueMs, 0.0),
                    latestTimestamp,
                    previousExponentialAverageMs);
            });

    static {
        PARSER.declareDouble(optionalConstructorArg(), INCREMENTAL_TIME_METRIC_MS);
        PARSER.declareField(
            optionalConstructorArg(),
            p -> TimeUtils.parseTimeFieldToInstant(p, LATEST_TIMESTAMP.getPreferredName()),
            LATEST_TIMESTAMP,
            ObjectParser.ValueType.VALUE);
        PARSER.declareDouble(optionalConstructorArg(), PREVIOUS_EXPONENTIAL_AVERAGE_MS);
    }

    private static final TemporalUnit WINDOW_UNIT = ChronoUnit.HOURS;
    private static final Duration WINDOW_SIZE = WINDOW_UNIT.getDuration();

    private double incrementalTimeMetricValueMs;
    private Instant latestTimestamp;
    private Double previousExponentialAverageMs;

    public ExponentialAverageCalculationContext() {
        this(0.0, null, null);
    }

    public ExponentialAverageCalculationContext(
            double incrementalTimeMetricValueMs,
            @Nullable Instant latestTimestamp,
            @Nullable Double previousExponentialAverageMs) {
        this.incrementalTimeMetricValueMs = incrementalTimeMetricValueMs;
        this.latestTimestamp = latestTimestamp != null ? Instant.ofEpochMilli(latestTimestamp.toEpochMilli()) : null;
        this.previousExponentialAverageMs = previousExponentialAverageMs;
    }

    public ExponentialAverageCalculationContext(ExponentialAverageCalculationContext lhs) {
        this(lhs.incrementalTimeMetricValueMs, lhs.latestTimestamp, lhs.previousExponentialAverageMs);
    }

    public ExponentialAverageCalculationContext(StreamInput in) throws IOException {
        this.incrementalTimeMetricValueMs = in.readDouble();
        this.latestTimestamp = in.readOptionalInstant();
        this.previousExponentialAverageMs = in.readOptionalDouble();
    }

    double getIncrementalTimeMetricValueMs() {
        return incrementalTimeMetricValueMs;
    }

    Instant getLatestTimestamp() {
        return latestTimestamp;
    }

    Double getPreviousExponentialAverageMs() {
        return previousExponentialAverageMs;
    }

    public Double getCurrentExponentialAverageMs() {
        if (previousExponentialAverageMs == null || latestTimestamp == null) return incrementalTimeMetricValueMs;
        Instant currentWindowStartTimestamp = latestTimestamp.truncatedTo(WINDOW_UNIT);
        double alpha = Math.exp(
            - (double) Duration.between(currentWindowStartTimestamp, latestTimestamp).toMillis() / WINDOW_SIZE.toMillis());
        return alpha * previousExponentialAverageMs + (1 - alpha) * incrementalTimeMetricValueMs;
    }

    public void increment(double metricValueDeltaMs) {
        incrementalTimeMetricValueMs += metricValueDeltaMs;
    }

    public void setLatestTimestamp(Instant newLatestTimestamp) {
        Objects.requireNonNull(newLatestTimestamp);
        if (this.latestTimestamp != null) {
            Instant nextWindowStartTimestamp = this.latestTimestamp.truncatedTo(WINDOW_UNIT).plus(WINDOW_SIZE);
            if (newLatestTimestamp.compareTo(nextWindowStartTimestamp) >= 0) {
                // When we cross the boundary between windows, we update the exponential average with metric values accumulated so far in
                // incrementalTimeMetricValueMs variable.
                this.previousExponentialAverageMs = getCurrentExponentialAverageMs();
                this.incrementalTimeMetricValueMs = 0.0;
            }
        } else {
            // This is the first time {@link #setLatestRecordTimestamp} is called on this object.
        }
        if (this.latestTimestamp == null || newLatestTimestamp.isAfter(this.latestTimestamp)) {
            this.latestTimestamp = newLatestTimestamp;
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeDouble(incrementalTimeMetricValueMs);
        out.writeOptionalInstant(latestTimestamp);
        out.writeOptionalDouble(previousExponentialAverageMs);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(INCREMENTAL_TIME_METRIC_MS.getPreferredName(), incrementalTimeMetricValueMs);
        if (latestTimestamp != null) {
            builder.timeField(
                LATEST_TIMESTAMP.getPreferredName(),
                LATEST_TIMESTAMP.getPreferredName() + "_string",
                latestTimestamp.toEpochMilli());
        }
        if (previousExponentialAverageMs != null) {
            builder.field(PREVIOUS_EXPONENTIAL_AVERAGE_MS.getPreferredName(), previousExponentialAverageMs);
        }
        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExponentialAverageCalculationContext that = (ExponentialAverageCalculationContext) o;
        return this.incrementalTimeMetricValueMs == that.incrementalTimeMetricValueMs
            && Objects.equals(this.latestTimestamp, that.latestTimestamp)
            && Objects.equals(this.previousExponentialAverageMs, that.previousExponentialAverageMs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(incrementalTimeMetricValueMs, latestTimestamp, previousExponentialAverageMs);
    }

    @Override
    public String toString() {
        return Strings.toString(this);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getOrDefault(@Nullable T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
