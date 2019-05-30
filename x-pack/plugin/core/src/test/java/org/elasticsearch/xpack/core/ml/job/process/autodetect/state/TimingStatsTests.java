/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.ml.job.process.autodetect.state;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.AbstractSerializingTestCase;

import static org.hamcrest.Matchers.equalTo;

public class TimingStatsTests extends AbstractSerializingTestCase<TimingStats> {

    private static final String JOB_ID = "my-job-id";

    public static TimingStats createTestInstance(String jobId) {
        return new TimingStats(jobId, randomDouble());
    }

    @Override
    public TimingStats createTestInstance() {
        return createTestInstance(randomAlphaOfLength(10));
    }

    @Override
    protected Writeable.Reader<TimingStats> instanceReader() {
        return TimingStats::new;
    }

    @Override
    protected TimingStats doParseInstance(XContentParser parser) {
        return TimingStats.PARSER.apply(parser, null);
    }

    public void testEquals() {
        TimingStats stats1 = new TimingStats(JOB_ID, 1.23);
        TimingStats stats2 = new TimingStats(JOB_ID, 1.23);
        TimingStats stats3 = new TimingStats(JOB_ID, 3.21);

        assertTrue(stats1.equals(stats1));
        assertTrue(stats1.equals(stats2));
        assertFalse(stats2.equals(stats3));
    }

    public void testHashCode() {
        TimingStats stats1 = new TimingStats(JOB_ID, 1.23);
        TimingStats stats2 = new TimingStats(JOB_ID, 1.23);
        TimingStats stats3 = new TimingStats(JOB_ID, 3.21);

        assertEquals(stats1.hashCode(), stats1.hashCode());
        assertEquals(stats1.hashCode(), stats2.hashCode());
        assertNotEquals(stats2.hashCode(), stats3.hashCode());
    }

    public void testDefaultConstructor() {
        TimingStats stats = new TimingStats(JOB_ID);

        assertThat(stats.getJobId(), equalTo(JOB_ID));
        assertThat(stats.getAvgBucketProcessingTimeMs(), equalTo(0.0));
    }

    public void testConstructor() {
        TimingStats stats = new TimingStats(JOB_ID, 1.23);

        assertThat(stats.getJobId(), equalTo(JOB_ID));
        assertThat(stats.getAvgBucketProcessingTimeMs(), equalTo(1.23));
    }

    public void testCopyConstructor() {
        TimingStats stats1 = new TimingStats(JOB_ID, 1.23);
        TimingStats stats2 = new TimingStats(stats1);

        assertThat(stats2.getJobId(), equalTo(JOB_ID));
        assertThat(stats2.getAvgBucketProcessingTimeMs(), equalTo(1.23));
        assertEquals(stats1, stats2);
        assertEquals(stats1.hashCode(), stats2.hashCode());
    }

    public void testIncrementTotalBucketProcessingTimeMs() {
        TimingStats stats = new TimingStats(JOB_ID);

        stats.incrementTotalBucketProcessingTimeMs(0, 1);
        assertThat(stats.getAvgBucketProcessingTimeMs(), equalTo(1.0));

        stats.incrementTotalBucketProcessingTimeMs(1, 2);
        assertThat(stats.getAvgBucketProcessingTimeMs(), equalTo(1.5));

        stats.incrementTotalBucketProcessingTimeMs(2, 3);
        assertThat(stats.getAvgBucketProcessingTimeMs(), equalTo(2.0));

        stats.incrementTotalBucketProcessingTimeMs(3, 4);
        assertThat(stats.getAvgBucketProcessingTimeMs(), equalTo(2.5));

        stats.incrementTotalBucketProcessingTimeMs(4, 5);
        assertThat(stats.getAvgBucketProcessingTimeMs(), equalTo(3.0));

        stats.incrementTotalBucketProcessingTimeMs(1, 7);
        assertThat(stats.getAvgBucketProcessingTimeMs(), equalTo(5.0));
    }

    public void testDocumentId() {
        assertThat(TimingStats.documentId("my-job-id"), equalTo("my-job-id_timing_stats"));
    }
}
