/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.client.ml;

import org.elasticsearch.client.ml.dataframe.evaluation.EvaluationMetric;
import org.elasticsearch.client.ml.dataframe.evaluation.softclassification.BinarySoftClassification;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.NamedObjectNotFoundException;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static org.elasticsearch.common.xcontent.ConstructingObjectParser.constructorArg;

public class EvaluateDataFrameResponse implements ToXContentObject {

    public static EvaluateDataFrameResponse fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null);
    }

    private static final ParseField SOFT_CLASSIFICATION_METRICS = new ParseField(BinarySoftClassification.NAME);

    @SuppressWarnings("unchecked")
    private static ConstructingObjectParser<EvaluateDataFrameResponse, Void> PARSER =
        new ConstructingObjectParser<>(
            "evaluate_data_frame_response", true, args -> new EvaluateDataFrameResponse((Map<String, EvaluationMetric.Result>) args[0]));

    static {
        PARSER.declareObject(constructorArg(), (p, c) -> parseSoftClassificationMetrics(p), SOFT_CLASSIFICATION_METRICS);
    }

    private static Map<String, EvaluationMetric.Result> parseSoftClassificationMetrics(XContentParser parser) throws IOException {
        Map<String, EvaluationMetric.Result> metrics = parser.genericMapOrdered((p, n) -> {
            try {
                return p.namedObject(EvaluationMetric.Result.class, n, null);
            } catch (NamedObjectNotFoundException e) {
                parser.skipChildren();
                return null;
            }
        });
        // Remove null values as those come from {@link NamedObjectNotFoundException} being thrown and we do want to ignore unknown metrics.
        metrics.values().removeIf(Objects::isNull);
        return metrics;
    }

    private final Map<String, EvaluationMetric.Result> metrics;

    public EvaluateDataFrameResponse(Map<String, EvaluationMetric.Result> metrics) {
        this.metrics = Objects.requireNonNull(metrics);
    }

    public String getEvaluationName() {
        return BinarySoftClassification.NAME;
    }

    public Map<String, EvaluationMetric.Result> getMetrics() {
        return Collections.unmodifiableMap(metrics);
    }

    @SuppressWarnings("unchecked")
    public <T extends EvaluationMetric.Result> T getMetricByName(String metricName) {
        return (T) metrics.get(metricName);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        return builder
            .startObject()
            .field(BinarySoftClassification.NAME, metrics)
            .endObject();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvaluateDataFrameResponse other = (EvaluateDataFrameResponse) o;
        return Objects.equals(metrics, other.metrics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metrics);
    }

    @Override
    public final String toString() {
        return Strings.toString(this);
    }
}
