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

package org.elasticsearch.client.ml.dataframe;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

public class OutlierDetection implements DataFrameAnalysis {

    public static OutlierDetection fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null).build();
    }

    public static OutlierDetection createDefault() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final ParseField NAME = new ParseField("outlier_detection");
    static final ParseField N_NEIGHBORS = new ParseField("n_neighbors");
    static final ParseField METHOD = new ParseField("method");

    private static ObjectParser<Builder, Void> PARSER = new ObjectParser<>(NAME.getPreferredName(), true, Builder::new);

    static {
        PARSER.declareInt(Builder::setNNeighbors, N_NEIGHBORS);
        PARSER.declareEnumValue(Builder::setMethod, Method::fromString, METHOD);
    }

    private final Integer nNeighbors;
    private final Method method;

    /**
     * Constructs the outlier detection configuration
     * @param nNeighbors The number of neighbors. Leave unspecified for dynamic detection.
     * @param method The method. Leave unspecified for a dynamic mixture of methods.
     */
    private OutlierDetection(@Nullable Integer nNeighbors, @Nullable Method method) {
        if (nNeighbors != null && nNeighbors <= 0) {
            throw new IllegalArgumentException("[" + N_NEIGHBORS.getPreferredName() + "] must be a positive integer");
        }

        this.nNeighbors = nNeighbors;
        this.method = method;
    }

    @Override
    public String getName() {
        return NAME.getPreferredName();
    }

    public Integer getNNeighbors() {
        return nNeighbors;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (nNeighbors != null) {
            builder.field(N_NEIGHBORS.getPreferredName(), nNeighbors);
        }
        if (method != null) {
            builder.field(METHOD.getPreferredName(), method);
        }
        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OutlierDetection other = (OutlierDetection) o;
        return Objects.equals(nNeighbors, other.nNeighbors)
            && Objects.equals(method, other.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nNeighbors, method);
    }

    @Override
    public String toString() {
        return Strings.toString(this);
    }

    public enum Method {
        LOF, LDOF, DISTANCE_KTH_NN, DISTANCE_KNN;

        public static Method fromString(String value) {
            return Method.valueOf(value.toUpperCase(Locale.ROOT));
        }

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static class Builder {

        private Integer nNeighbors;
        private Method method;

        private Builder() {}

        public Builder setNNeighbors(Integer nNeighbors) {
            this.nNeighbors = nNeighbors;
            return this;
        }

        public Builder setMethod(Method method) {
            this.method = method;
            return this;
        }

        public OutlierDetection build() {
            return new OutlierDetection(nNeighbors, method);
        }
    }
}
