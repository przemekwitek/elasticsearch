/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.ml.dataframe.evaluation;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.core.ml.utils.ExceptionsHelper;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static org.elasticsearch.xpack.core.ml.dataframe.evaluation.MlEvaluationNamedXContentProvider.registeredMetricName;

public class AbstractEvaluation implements Evaluation {

    public static final ParseField NAME = new ParseField("abstract_evaluation");

    private static final ParseField ACTUAL_FIELD = new ParseField("actual_field");
    private static final ParseField PREDICTED_FIELD = new ParseField("predicted_field");
    private static final ParseField METRICS = new ParseField("metrics");

    protected static final ConstructingObjectParser<AbstractEvaluation, Void> createParser(
            Supplier<List<EvaluationMetric>> defaultMetricsSupplier) {

        ConstructingObjectParser<AbstractEvaluation, Void> PARSER = new ConstructingObjectParser<>(
            NAME.getPreferredName(),
            a -> new AbstractEvaluation((String) a[0], (String) a[1], (List<EvaluationMetric>) a[2], defaultMetricsSupplier));

        PARSER.declareString(ConstructingObjectParser.constructorArg(), ACTUAL_FIELD);
        PARSER.declareString(ConstructingObjectParser.constructorArg(), PREDICTED_FIELD);
        PARSER.declareNamedObjects(ConstructingObjectParser.optionalConstructorArg(),
            (p, c, n) -> p.namedObject(EvaluationMetric.class, registeredMetricName(NAME.getPreferredName(), n), c), METRICS);

        return PARSER;
    }

    /**
     * The field containing the actual value
     * The value of this field is assumed to be numeric
     */
    private final String actualField;

    /**
     * The field containing the predicted value
     * The value of this field is assumed to be numeric
     */
    private final String predictedField;

    /**
     * The list of metrics to calculate
     */
    private final List<EvaluationMetric> metrics;

    protected AbstractEvaluation(String actualField,
                                 String predictedField,
                                 @Nullable List<EvaluationMetric> metrics,
                                 Supplier<List<EvaluationMetric>> defaultMetricsSupplier) {
        this.actualField = ExceptionsHelper.requireNonNull(actualField, ACTUAL_FIELD);
        this.predictedField = ExceptionsHelper.requireNonNull(predictedField, PREDICTED_FIELD);
        this.metrics = initMetrics(metrics, defaultMetricsSupplier);
    }

    protected AbstractEvaluation(StreamInput in) throws IOException {
        this.actualField = in.readString();
        this.predictedField = in.readString();
        this.metrics = in.readNamedWriteableList(EvaluationMetric.class);
    }

    @Override
    public String getName() {
        return NAME.getPreferredName();
    }

    @Override
    public String getActualField() {
        return actualField;
    }

    @Override
    public String getPredictedField() {
        return predictedField;
    }

    @Override
    public List<EvaluationMetric> getMetrics() {
        return metrics;
    }

    @Override
    public String getWriteableName() {
        return NAME.getPreferredName();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(actualField);
        out.writeString(predictedField);
        out.writeNamedWriteableList(metrics);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(ACTUAL_FIELD.getPreferredName(), actualField);
        builder.field(PREDICTED_FIELD.getPreferredName(), predictedField);

        builder.startObject(METRICS.getPreferredName());
        for (EvaluationMetric metric : metrics) {
            builder.field(metric.getName(), metric);
        }
        builder.endObject();

        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractEvaluation that = (AbstractEvaluation) o;
        return Objects.equals(that.actualField, this.actualField)
            && Objects.equals(that.predictedField, this.predictedField)
            && Objects.equals(that.metrics, this.metrics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actualField, predictedField, metrics);
    }
}
