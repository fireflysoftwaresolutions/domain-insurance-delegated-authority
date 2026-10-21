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
import io.fireflysoftware.domain.insurance.delegatedauthority.core.commands.GetBinderQuery;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.commands.UpdateBinderStatusCommand;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.commands.UpdateBinderCommand;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.commands.RemoveBinderCommand;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.lifecycle.BinderRecordLifecycle;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.constants.BinderSagaConstants;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Runs the binder sagas and reads through the query bus.
 */
@Service
public class DelegatedAuthorityServiceImpl implements DelegatedAuthorityService {

    private final SagaEngine sagaEngine;
    private final QueryBus queryBus;
    private final CommandBus commandBus;

    public DelegatedAuthorityServiceImpl(SagaEngine sagaEngine, QueryBus queryBus, CommandBus commandBus) {
        this.sagaEngine = sagaEngine;
        this.queryBus = queryBus;
        this.commandBus = commandBus;
    }

    @Override
    public Mono<BinderDTO> create(CreateBinderCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(BinderSagaConstants.STEP_REGISTER, command)
                .build();
        return sagaEngine.execute(BinderSagaConstants.SAGA_REGISTER, inputs)
                .flatMap(result -> outcome(result, BinderSagaConstants.STEP_REGISTER, BinderDTO.class));
    }

    @Override
    public Mono<BinderDTO> updateStatus(UpdateBinderStatusCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(BinderSagaConstants.STEP_RETRIEVE, new GetBinderQuery(command.binderId()))
                .forStepId(BinderSagaConstants.STEP_UPDATE_STATUS, command)
                .build();
        return sagaEngine.execute(BinderSagaConstants.SAGA_UPDATE_STATUS, inputs)
                .flatMap(result -> outcome(result, BinderSagaConstants.STEP_UPDATE_STATUS, BinderDTO.class));
    }

    @Override
    public Mono<BinderDTO> get(UUID binderId) {
        return queryBus.query(new GetBinderQuery(binderId));
    }


    @Override
    public Mono<BinderDTO> update(UpdateBinderCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(BinderSagaConstants.STEP_RETRIEVE_FOR_UPDATE, new GetBinderQuery(command.binderId()))
                .forStepId(BinderSagaConstants.STEP_UPDATE_RECORD, command)
                .build();
        return sagaEngine.execute(BinderSagaConstants.SAGA_UPDATE_RECORD, inputs)
                .flatMap(result -> outcome(result, BinderSagaConstants.STEP_UPDATE_RECORD, BinderDTO.class));
    }

    @Override
    public Mono<BinderDTO> expire(UUID binderId, String statusReason) {
        return transition(binderId, BinderDTO.BinderStatusEnum.EXPIRED, statusReason);
    }

    private Mono<BinderDTO> transition(UUID binderId, BinderDTO.BinderStatusEnum target, String statusReason) {
        return get(binderId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Binder was not found")))
                .flatMap(current -> {
                    if (!BinderRecordLifecycle.canTransition(current.getBinderStatus(), target)) {
                        String from = current.getBinderStatus() == null
                                ? "unset"
                                : current.getBinderStatus().getValue();
                        return Mono.<BinderDTO>error(new IllegalStateException(
                                "Binder " + binderId + " cannot move from " + from + " to " + target.getValue()));
                    }
                    return updateStatus(new UpdateBinderStatusCommand(binderId, target.getValue(), statusReason));
                });
    }

    @Override
    public Mono<Void> remove(UUID binderId) {
        return get(binderId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Binder was not found")))
                .flatMap(current -> {
                    if (!BinderRecordLifecycle.canRemove(current.getBinderStatus())) {
                        return Mono.error(new IllegalStateException(
                                "Binder " + binderId + " is " + current.getBinderStatus().getValue()
                                        + " and cannot be deleted; expire it so the binder history is kept"));
                    }
                    return commandBus.send(new RemoveBinderCommand(binderId));
                });
    }

    private static <T> Mono<T> outcome(SagaResult result, String stepId, Class<T> type) {
        if (result.isFailed()) {
            Throwable error = result.error().orElseGet(() ->
                    new IllegalStateException("Saga " + result.sagaName() + " failed"));
            return Mono.error(error);
        }
        return Mono.justOrEmpty(result.resultOf(stepId, type))
                .switchIfEmpty(Mono.error(new IllegalStateException("Saga step " + stepId + " produced no result")));
    }
}
