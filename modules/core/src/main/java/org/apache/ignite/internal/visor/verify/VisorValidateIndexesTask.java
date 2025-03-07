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

package org.apache.ignite.internal.visor.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.internal.management.cache.CacheValidateIndexesCommandArg;
import org.apache.ignite.internal.processors.task.GridInternal;
import org.apache.ignite.internal.util.typedef.internal.A;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.visor.VisorJob;
import org.apache.ignite.internal.visor.VisorMultiNodeTask;
import org.apache.ignite.internal.visor.VisorTaskArgument;
import org.apache.ignite.internal.visor.annotation.InterruptibleVisorTask;
import org.apache.ignite.resources.LoggerResource;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
@GridInternal
@InterruptibleVisorTask
public class VisorValidateIndexesTask extends VisorMultiNodeTask<CacheValidateIndexesCommandArg,
    VisorValidateIndexesTaskResult, VisorValidateIndexesJobResult> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Nullable @Override protected VisorValidateIndexesTaskResult reduce0(List<ComputeJobResult> list) throws IgniteException {
        Map<UUID, Exception> exceptions = new HashMap<>();
        Map<UUID, VisorValidateIndexesJobResult> jobResults = new HashMap<>();

        for (ComputeJobResult res : list) {
            if (res.getException() != null)
                exceptions.put(res.getNode().id(), res.getException());
            else
                jobResults.put(res.getNode().id(), res.getData());
        }

        return new VisorValidateIndexesTaskResult(jobResults, exceptions);
    }

    /** {@inheritDoc} */
    @Override protected VisorJob<CacheValidateIndexesCommandArg, VisorValidateIndexesJobResult> job(CacheValidateIndexesCommandArg arg) {
        return new VisorValidateIndexesJob(arg, debug);
    }

    /** {@inheritDoc} */
    @Override protected Collection<UUID> jobNodes(VisorTaskArgument<CacheValidateIndexesCommandArg> arg) {
        Collection<ClusterNode> srvNodes = ignite.cluster().forServers().nodes();
        Collection<UUID> ret = new ArrayList<>(srvNodes.size());

        CacheValidateIndexesCommandArg taskArg = arg.getArgument();

        Set<UUID> nodeIds = taskArg.nodeIds() != null ? new HashSet<>(Arrays.asList(taskArg.nodeIds())) : null;

        if (nodeIds == null) {
            for (ClusterNode node : srvNodes)
                ret.add(node.id());
        }
        else {
            for (ClusterNode node : srvNodes) {
                if (nodeIds.contains(node.id()))
                    ret.add(node.id());
            }
        }

        return ret;
    }

    /**
     *
     */
    private static class VisorValidateIndexesJob extends VisorJob<CacheValidateIndexesCommandArg, VisorValidateIndexesJobResult> {
        /** */
        private static final long serialVersionUID = 0L;

        /** Injected logger. */
        @LoggerResource
        private IgniteLogger log;

        /**
         * @param arg Argument.
         * @param debug Debug.
         */
        protected VisorValidateIndexesJob(@Nullable CacheValidateIndexesCommandArg arg, boolean debug) {
            super(arg, debug);
        }

        /** {@inheritDoc} */
        @Override protected VisorValidateIndexesJobResult run(CacheValidateIndexesCommandArg arg) throws IgniteException {
            A.notNull(arg, "arg");

            try {
                ValidateIndexesClosure clo = new ValidateIndexesClosure(
                    this::isCancelled,
                    arg.caches() == null ? null : new HashSet<>(Arrays.asList(arg.caches())),
                    arg.checkFirst(),
                    arg.checkThrough(),
                    arg.checkCrc(),
                    arg.checkSizes()
                );

                ignite.context().resource().injectGeneric(clo);

                return clo.call();
            }
            catch (Exception e) {
                cancel();

                throw new IgniteException(e);
            }
        }

        /** {@inheritDoc} */
        @Override public void cancel() {
            log.warning("Index validation was cancelled.");

            super.cancel();
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorValidateIndexesJob.class, this);
        }
    }
}
