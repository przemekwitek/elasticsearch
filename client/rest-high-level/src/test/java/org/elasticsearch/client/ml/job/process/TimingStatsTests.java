/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.client.ml.job.process;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.AbstractXContentTestCase;

import static org.hamcrest.Matchers.equalTo;

public class TimingStatsTests extends AbstractXContentTestCase<TimingStats> {

    private static final String JOB_ID = "my-job-id";

    public static TimingStats createTestInstance(String jobId) {
        return new TimingStats(jobId, randomDouble());
    }

    @Override
    public TimingStats createTestInstance() {
        return createTestInstance(randomAlphaOfLength(10));
    }

    @Override
    protected TimingStats doParseInstance(XContentParser parser) {
        return TimingStats.PARSER.apply(parser, null);
    }

    @Override
    protected boolean supportsUnknownFields() {
        return true;
    }

    public void testConstructor() {
        TimingStats stats = new TimingStats(JOB_ID, 1.23);

        assertThat(stats.getJobId(), equalTo(JOB_ID));
        assertThat(stats.getAvgBucketProcessingTimeMs(), equalTo(1.23));
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
}
