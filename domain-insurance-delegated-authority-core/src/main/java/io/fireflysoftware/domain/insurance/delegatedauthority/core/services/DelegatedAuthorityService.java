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

package io.fireflysoftware.domain.insurance.delegatedauthority.core.services;

import io.fireflysoftware.core.insurance.authority.sdk.model.BinderDTO;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.commands.CreateBinderCommand;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.commands.UpdateBinderStatusCommand;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.commands.UpdateBinderCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Application service for binder orchestration.
 */
public interface DelegatedAuthorityService {

    /**
     * Creates a binder through the register saga.
     *
     * @param command the create command
     * @return the record returned by the core
     */
    Mono<BinderDTO> create(CreateBinderCommand command);

    /**
     * Moves a binder to a new status through the core.
     *
     * @param command the status change
     * @return the updated record
     */
    Mono<BinderDTO> updateStatus(UpdateBinderStatusCommand command);

    /**
     * Reads one binder.
     *
     * @param binderId the core identifier
     * @return the record, or empty when the core has none
     */
    Mono<BinderDTO> get(UUID binderId);

    /**
     * Updates fields on a binder and leaves its status unchanged.
     *
     * @param command the update
     * @return the record returned by the core
     */
    Mono<BinderDTO> update(UpdateBinderCommand command);

    /**
     * Expires a binder when the delegated authority core allows EXPIRED.
     *
     * @param binderId the core identifier
     * @param statusReason optional reason stored by the status saga when the core keeps one
     * @return the updated record
     */
    Mono<BinderDTO> expire(UUID binderId, String statusReason);

    /**
     * Deletes a binder when the core lifecycle allows removal.
     *
     * @param binderId the core identifier
     * @return completion when the core has deleted the record
     */
    Mono<Void> remove(UUID binderId);
}
