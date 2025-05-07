/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.esql.plan.logical;

import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.xpack.esql.core.capabilities.Resolvables;
import org.elasticsearch.xpack.esql.core.expression.Expression;
import org.elasticsearch.xpack.esql.core.tree.NodeInfo;
import org.elasticsearch.xpack.esql.core.tree.Source;
import org.elasticsearch.xpack.esql.expression.Order;
import org.elasticsearch.xpack.esql.io.stream.PlanStreamInput;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class TopN extends UnaryPlan implements PipelineBreaker, ExecutesOn {
    public static final NamedWriteableRegistry.Entry ENTRY = new NamedWriteableRegistry.Entry(LogicalPlan.class, "TopN", TopN::new);

    private final Expression partitionField;
    private final List<Order> order;
    private final Expression limit;
    /**
     * Local topn is not a pipeline breaker, and is applied only to the local node's data.
     * It should always end up inside a fragment.
     */
    private final transient boolean local;

    public TopN(Source source, LogicalPlan child, Expression partitionField, List<Order> order, Expression limit, boolean local) {
        super(source, child);
        this.partitionField = partitionField;
        this.order = order;
        this.limit = limit;
        this.local = local;
    }

    private TopN(StreamInput in) throws IOException {
        this(
            Source.readFrom((PlanStreamInput) in),
            in.readNamedWriteable(LogicalPlan.class),
            in.readOptionalNamedWriteable(Expression.class),
            in.readCollectionAsList(Order::new),
            in.readNamedWriteable(Expression.class),
            false
        );
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        Source.EMPTY.writeTo(out);
        out.writeNamedWriteable(child());
        out.writeOptionalNamedWriteable(partitionField());
        out.writeCollection(order);
        out.writeNamedWriteable(limit);
    }

    @Override
    public String getWriteableName() {
        return ENTRY.name;
    }

    @Override
    public boolean expressionsResolved() {
        return limit.resolved() && Resolvables.resolved(order);
    }

    @Override
    protected NodeInfo<TopN> info() {
        return NodeInfo.create(this, TopN::new, child(), partitionField, order, limit, local);
    }

    @Override
    public TopN replaceChild(LogicalPlan newChild) {
        return new TopN(source(), newChild, partitionField, order, limit, local);
    }

    public TopN withLocal(boolean local) {
        return new TopN(source(), child(), order, limit, local);
    }

    public boolean local() {
        return local;
    }

    public Expression partitionField() {
        return partitionField;
    }

    public Expression limit() {
        return limit;
    }

    public List<Order> order() {
        return order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), partitionField, order, limit, local);
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            var other = (TopN) obj;
            return Objects.equals(partitionField, other.partitionField) && Objects.equals(order, other.order) && Objects.equals(limit, other.limit) && local == other.local;
        }
        return false;
    }

    @Override
    public ExecuteLocation executesOn() {
        return local ? ExecuteLocation.ANY : ExecuteLocation.COORDINATOR;
    }
}
