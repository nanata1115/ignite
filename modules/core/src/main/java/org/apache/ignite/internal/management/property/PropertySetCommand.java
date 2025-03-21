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

package org.apache.ignite.internal.management.property;

import org.apache.ignite.internal.commandline.property.tasks.PropertyOperationResult;
import org.apache.ignite.internal.commandline.property.tasks.PropertyTask;
import org.apache.ignite.internal.management.api.ComputeCommand;

/** */
public class PropertySetCommand implements ComputeCommand<PropertyGetCommandArg, PropertyOperationResult> {
    /** {@inheritDoc} */
    @Override public String description() {
        return "Set the property value";
    }

    /** {@inheritDoc} */
    @Override public Class<PropertySetCommandArg> argClass() {
        return PropertySetCommandArg.class;
    }

    /** {@inheritDoc} */
    @Override public Class<PropertyTask> taskClass() {
        return PropertyTask.class;
    }
}
