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

package org.apache.ignite.internal.visor.query;

import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.internal.QueryMXBeanImpl;
import org.apache.ignite.internal.management.kill.KillContinuousCommandArg;
import org.apache.ignite.internal.processors.task.GridInternal;
import org.apache.ignite.internal.processors.task.GridVisorManagementTask;
import org.apache.ignite.internal.visor.VisorJob;
import org.apache.ignite.internal.visor.VisorOneNodeTask;
import org.jetbrains.annotations.Nullable;

/**
 * Task to cancel continuous query.
 */
@GridInternal
@GridVisorManagementTask
public class VisorContinuousQueryCancelTask extends VisorOneNodeTask<KillContinuousCommandArg, Void> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorContinuousQueryCancelJob job(KillContinuousCommandArg arg) {
        return new VisorContinuousQueryCancelJob(arg, debug);
    }

    /**
     * Job to cancel scan queries on node.
     */
    private static class VisorContinuousQueryCancelJob extends VisorJob<KillContinuousCommandArg, Void> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * Create job with specified argument.
         *
         * @param arg Job argument.
         * @param debug Flag indicating whether debug information should be printed into node log.
         */
        protected VisorContinuousQueryCancelJob(@Nullable KillContinuousCommandArg arg, boolean debug) {
            super(arg, debug);
        }

        /** {@inheritDoc} */
        @Override protected Void run(@Nullable KillContinuousCommandArg arg) throws IgniteException {
            if (arg == null)
                return null;

            IgniteLogger log = ignite.log().getLogger(VisorContinuousQueryCancelJob.class);

            if (log.isInfoEnabled())
                log.info("Cancelling continuous query[routineId=" + arg.routineId() + ']');

            new QueryMXBeanImpl(ignite.context()).cancelContinuous(arg.originNodeId(), arg.routineId());

            return null;
        }
    }
}
