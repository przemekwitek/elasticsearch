/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.ml.dataframe.evaluation.classification;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.AbstractEvaluation;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.EvaluationMetric;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Evaluation of classification results.
 */
public class Classification extends AbstractEvaluation {

    private static final ParseField PREDICTED_FIELD = new ParseField("predicted_field");
    private static final ConstructingObjectParser<AbstractEvaluation, Void> PARSER =
        createParser(PREDICTED_FIELD, Classification::defaultMetrics);

    public static AbstractEvaluation fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null);
    }

    public Classification(String actualField, String predictedField, @Nullable List<EvaluationMetric> metrics) {
        super(actualField, predictedField, metrics, Classification::defaultMetrics);
    }

    public Classification(StreamInput in) throws IOException {
        super(in);
    }

    private static List<EvaluationMetric> defaultMetrics() {
        return Arrays.asList(new MulticlassConfusionMatrix());
    }
}
