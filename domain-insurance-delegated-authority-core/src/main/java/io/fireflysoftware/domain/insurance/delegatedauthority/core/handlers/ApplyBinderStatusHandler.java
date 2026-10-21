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

package io.fireflysoftware.domain.insurance.delegatedauthority.core.handlers;

import io.fireflysoftware.core.insurance.authority.sdk.api.BindersApi;
import io.fireflysoftware.core.insurance.authority.sdk.model.BinderDTO;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.commands.ApplyBinderStatusCommand;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Updates a binder, including its status, through the core.
 */
@Slf4j
@CommandHandlerComponent
public class ApplyBinderStatusHandler extends CommandHandler<ApplyBinderStatusCommand, BinderDTO> {

    private final BindersApi bindersApi;

    public ApplyBinderStatusHandler(BindersApi bindersApi) {
        this.bindersApi = bindersApi;
    }

    @Override
    protected Mono<BinderDTO> doHandle(ApplyBinderStatusCommand command) {
        log.debug("Updating binder status: binderId={}", command.binderId());
        return bindersApi.updateBinder(command.binderId(), command.record(), UUID.randomUUID().toString());
    }
}
