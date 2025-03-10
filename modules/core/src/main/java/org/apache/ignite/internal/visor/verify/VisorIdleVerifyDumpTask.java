/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.visor.verify;

import org.apache.ignite.internal.management.cache.CacheIdleVerifyCommandArg;
import org.apache.ignite.internal.processors.cache.verify.VerifyBackupPartitionsDumpTask;
import org.apache.ignite.internal.processors.task.GridInternal;
import org.apache.ignite.internal.visor.VisorJob;
import org.apache.ignite.internal.visor.VisorOneNodeTask;

/**
 * Task to verify checksums of backup partitions and return all collected information.
 */
@GridInternal
public class VisorIdleVerifyDumpTask extends VisorOneNodeTask<CacheIdleVerifyCommandArg, String> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorJob<CacheIdleVerifyCommandArg, String> job(CacheIdleVerifyCommandArg arg) {
        return new VisorIdleVerifyJob<>(arg, debug, VerifyBackupPartitionsDumpTask.class);
    }
}
