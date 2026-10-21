/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.domain.insurance.delegatedauthority.core.lifecycle;

import io.fireflysoftware.core.insurance.authority.sdk.model.BinderDTO;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Moves copied from the binder lifecycle on {@code core-insurance-delegated-authority}.
 * A missing status is treated as DRAFT, which is the only status the core will delete.
 */
public final class BinderRecordLifecycle {

    private static final Map<BinderDTO.BinderStatusEnum, Set<BinderDTO.BinderStatusEnum>> ALLOWED = Map.of(
            BinderDTO.BinderStatusEnum.DRAFT, EnumSet.of(BinderDTO.BinderStatusEnum.ACTIVE),
            BinderDTO.BinderStatusEnum.ACTIVE, EnumSet.of(BinderDTO.BinderStatusEnum.SUSPENDED, BinderDTO.BinderStatusEnum.EXPIRED),
            BinderDTO.BinderStatusEnum.SUSPENDED, EnumSet.of(BinderDTO.BinderStatusEnum.ACTIVE, BinderDTO.BinderStatusEnum.EXPIRED)
    );

    private BinderRecordLifecycle() {
    }

    public static boolean canRemove(BinderDTO.BinderStatusEnum status) {
        return status == null || status == BinderDTO.BinderStatusEnum.DRAFT;
    }

    public static boolean canTransition(BinderDTO.BinderStatusEnum from, BinderDTO.BinderStatusEnum to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;
        }
        Set<BinderDTO.BinderStatusEnum> next = ALLOWED.get(from);
        return next != null && next.contains(to);
    }
}
