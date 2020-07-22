/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.ml.action;

import org.elasticsearch.Version;
import org.elasticsearch.common.io.stream.ByteBufferStreamInput;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.NamedWriteableAwareStreamInput;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.test.AbstractSerializingTestCase;
import org.elasticsearch.xpack.core.ml.action.EvaluateDataFrameAction.Request;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.Evaluation;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.MlEvaluationNamedXContentProvider;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.classification.ClassificationTests;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.outlierdetection.OutlierDetection;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.outlierdetection.Precision;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.regression.RegressionTests;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.outlierdetection.OutlierDetectionTests;
import org.elasticsearch.xpack.core.ml.utils.QueryProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class EvaluateDataFrameActionRequestTests extends AbstractSerializingTestCase<Request> {

    @Override
    protected NamedWriteableRegistry getNamedWriteableRegistry() {
        List<NamedWriteableRegistry.Entry> namedWriteables = new ArrayList<>();
        namedWriteables.addAll(MlEvaluationNamedXContentProvider.getNamedWriteables());
        namedWriteables.addAll(new SearchModule(Settings.EMPTY, Collections.emptyList()).getNamedWriteables());
        return new NamedWriteableRegistry(namedWriteables);
    }

    @Override
    protected NamedXContentRegistry xContentRegistry() {
        List<NamedXContentRegistry.Entry> namedXContent = new ArrayList<>();
        namedXContent.addAll(new MlEvaluationNamedXContentProvider().getNamedXContentParsers());
        namedXContent.addAll(new SearchModule(Settings.EMPTY, Collections.emptyList()).getNamedXContents());
        return new NamedXContentRegistry(namedXContent);
    }

    @Override
    protected Request createTestInstance() {
        int indicesCount = randomIntBetween(1, 5);
        List<String> indices = new ArrayList<>(indicesCount);
        for (int i = 0; i < indicesCount; i++) {
            indices.add(randomAlphaOfLength(10));
        }
        QueryProvider queryProvider = null;
        if (randomBoolean()) {
            try {
                queryProvider = QueryProvider.fromParsedQuery(QueryBuilders.termQuery(randomAlphaOfLength(10), randomAlphaOfLength(10)));
            } catch (IOException e) {
                // Should never happen
                throw new UncheckedIOException(e);
            }
        }
        Evaluation evaluation =
            randomFrom(OutlierDetectionTests.createRandom(), ClassificationTests.createRandom(), RegressionTests.createRandom());
        return new Request()
            .setIndices(indices)
            .setQueryProvider(queryProvider)
            .setEvaluation(evaluation);
    }

    public void testDeprecatedName_OutlierDetection() throws IOException {
        OutlierDetection before = new OutlierDetection("a", "b", Collections.singletonList(new Precision(Collections.singletonList(0.5))));
        OutlierDetection after = copyInstance(before, getNamedWriteableRegistry(), (out, value) -> {
            out.writeString(value.getActualField());
            out.writeString(value.getPredictedField());
            out.writeVInt(value.getMetrics().size());
            out.writeString(OutlierDetection.DEPRECATED_NAME + "." + Precision.NAME.getPreferredName());
            value.getMetrics().get(0).writeTo(out);
        }, OutlierDetection::new, Version.V_8_0_0);

        assertThat(after, is(equalTo(before)));
    }

    public void testDeprecatedName_Request() throws IOException {
        OutlierDetection beforeOD = new OutlierDetection("a", "b", Collections.singletonList(new Precision(Collections.singletonList(0.5))));

        Request before = new Request().setIndices(Collections.singletonList("i")).setEvaluation(beforeOD);
        Request after = copyInstance(before, getNamedWriteableRegistry(), (out, valu) -> {
            OutlierDetection value = (OutlierDetection) valu.getEvaluation();
            out.writeString(value.getActualField());
            out.writeString(value.getPredictedField());
            out.writeVInt(value.getMetrics().size());
            out.writeString(OutlierDetection.DEPRECATED_NAME + "." + Precision.NAME.getPreferredName());
            value.getMetrics().get(0).writeTo(out);
        }, Request::new, Version.V_8_0_0);

        assertThat(after, is(equalTo(before)));
    }

    @Override
    protected Writeable.Reader<Request> instanceReader() {
        return Request::new;
    }

    @Override
    protected boolean supportsUnknownFields() {
        return false;
    }

    @Override
    protected Request doParseInstance(XContentParser parser) {
        return Request.parseRequest(parser);
    }
}
