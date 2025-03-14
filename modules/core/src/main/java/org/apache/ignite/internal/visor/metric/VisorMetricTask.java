/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.visor.metric;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.management.metric.MetricCommandArg;
import org.apache.ignite.internal.management.metric.MetricConfigureHistogramCommandArg;
import org.apache.ignite.internal.management.metric.MetricConfigureHitrateCommandArg;
import org.apache.ignite.internal.processors.metric.GridMetricManager;
import org.apache.ignite.internal.processors.task.GridInternal;
import org.apache.ignite.internal.processors.task.GridVisorManagementTask;
import org.apache.ignite.internal.visor.VisorJob;
import org.apache.ignite.internal.visor.VisorOneNodeTask;
import org.apache.ignite.spi.metric.BooleanMetric;
import org.apache.ignite.spi.metric.DoubleMetric;
import org.apache.ignite.spi.metric.IntMetric;
import org.apache.ignite.spi.metric.LongMetric;
import org.apache.ignite.spi.metric.Metric;
import org.apache.ignite.spi.metric.ObjectMetric;
import org.apache.ignite.spi.metric.ReadOnlyMetricRegistry;
import org.jetbrains.annotations.Nullable;
import static org.apache.ignite.internal.processors.metric.impl.MetricUtils.SEPARATOR;
import static org.apache.ignite.spi.metric.jmx.MetricRegistryMBean.searchHistogram;

/** Reperesents visor task for obtaining metric values. */
@GridInternal
@GridVisorManagementTask
public class VisorMetricTask extends VisorOneNodeTask<MetricCommandArg, Map<String, ?>> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorJob<MetricCommandArg, Map<String, ?>> job(MetricCommandArg arg) {
        return new VisorMetricJob(arg, false);
    }

    /** */
    private static class VisorMetricJob extends VisorJob<MetricCommandArg, Map<String, ?>> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * Create job with specified argument.
         *
         * @param arg   Job argument.
         * @param debug Flag indicating whether debug information should be printed into node log.
         */
        protected VisorMetricJob(@Nullable MetricCommandArg arg, boolean debug) {
            super(arg, debug);
        }

        /** {@inheritDoc} */
        @Override protected Map<String, ?> run(@Nullable MetricCommandArg arg) throws IgniteException {
            String name = arg.name();

            GridMetricManager mmgr = ignite.context().metric();

            try {
                if (arg instanceof MetricConfigureHistogramCommandArg) {
                    mmgr.configureHistogram(arg.name(), ((MetricConfigureHistogramCommandArg)arg).newBounds());

                    return null;
                }
                else if (arg instanceof MetricConfigureHitrateCommandArg) {
                    mmgr.configureHitRate(arg.name(), ((MetricConfigureHitrateCommandArg)arg).newRateTimeInterval());

                    return null;
                }
            }
            catch (IgniteCheckedException e) {
                throw new IgniteException(e);
            }

            for (ReadOnlyMetricRegistry mreg : mmgr) {
                String mregName = mreg.name();

                if (mregName.equals(name)) {
                    Map<String, Object> res = new HashMap<>();

                    mreg.forEach(metric -> res.put(metric.name(), valueOf(metric)));

                    return res;
                }

                String mregPrefix = mregName + SEPARATOR;

                if (!name.startsWith(mregPrefix))
                    continue;

                if (mregPrefix.length() == name.length())
                    return null;

                String metricName = name.substring(mregPrefix.length());

                Metric metric = mreg.findMetric(metricName);

                if (metric != null)
                    return Collections.singletonMap(name, valueOf(metric));

                Object val = searchHistogram(metricName, mreg);

                if (val != null)
                    return Collections.singletonMap(name, val);
            }

            return null;
        }

        /**
         * Obtains value of the metric.
         *
         * @param metric Metric which value should be obtained.
         * @return Value of the metric.
         */
        private Object valueOf(Metric metric) {
            if (metric instanceof BooleanMetric)
                return ((BooleanMetric)metric).value();
            else if (metric instanceof DoubleMetric)
                return ((DoubleMetric)metric).value();
            else if (metric instanceof IntMetric)
                return ((IntMetric)metric).value();
            else if (metric instanceof LongMetric)
                return ((LongMetric)metric).value();
            else if (metric instanceof ObjectMetric)
                return metric.getAsString();

            throw new IllegalArgumentException("Unknown metric class [class=" + metric.getClass() + ']');
        }
    }
}
