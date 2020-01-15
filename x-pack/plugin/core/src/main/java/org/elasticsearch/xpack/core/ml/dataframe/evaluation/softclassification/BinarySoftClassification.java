/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.ml.dataframe.evaluation.softclassification;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.AbstractEvaluation;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.EvaluationMetric;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Evaluation of binary soft classification methods, e.g. outlier detection.
 * This is useful to evaluate problems where a model outputs a probability of whether
 * a data frame row belongs to one of two groups.
 */
public class BinarySoftClassification extends AbstractEvaluation {

    private static final ParseField PREDICTED_FIELD = new ParseField("predicted_probability_field");
    private static final ConstructingObjectParser<AbstractEvaluation, Void> PARSER =
        createParser(PREDICTED_FIELD, BinarySoftClassification::defaultMetrics);

    public static AbstractEvaluation fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null);
    }

    static QueryBuilder actualIsTrueQuery(String actualField) {
        return QueryBuilders.queryStringQuery(actualField + ": (1 OR true)");
    }

    public BinarySoftClassification(String actualField, String predictedField, @Nullable List<EvaluationMetric> metrics) {
        super(actualField, predictedField, metrics, BinarySoftClassification::defaultMetrics);
    }

    public BinarySoftClassification(StreamInput in) throws IOException {
        super(in);
    }

    private static List<EvaluationMetric> defaultMetrics() {
        return Arrays.asList(
            new AucRoc(false),
            new Precision(Arrays.asList(0.25, 0.5, 0.75)),
            new Recall(Arrays.asList(0.25, 0.5, 0.75)),
            new ConfusionMatrix(Arrays.asList(0.25, 0.5, 0.75)));
    }
}
