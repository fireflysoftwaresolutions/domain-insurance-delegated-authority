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

package io.fireflysoftware.domain.insurance.delegatedauthority.core.workflows;

import io.fireflysoftware.core.insurance.authority.sdk.model.BinderDTO;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.commands.ApplyBinderStatusCommand;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.commands.GetBinderQuery;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.commands.UpdateBinderStatusCommand;

import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.insurance.delegatedauthority.core.constants.BinderSagaConstants.COMPENSATE_REMOVE_STATUS_HISTORY;
import static io.fireflysoftware.domain.insurance.delegatedauthority.core.constants.BinderSagaConstants.COMPENSATE_RESTORE;
import static io.fireflysoftware.domain.insurance.delegatedauthority.core.constants.BinderSagaConstants.CTX_PREVIOUS_STATUS;
import static io.fireflysoftware.domain.insurance.delegatedauthority.core.constants.BinderSagaConstants.CTX_RECORD;
import static io.fireflysoftware.domain.insurance.delegatedauthority.core.constants.BinderSagaConstants.EVENT_RETRIEVED;
import static io.fireflysoftware.domain.insurance.delegatedauthority.core.constants.BinderSagaConstants.EVENT_STATUS_HISTORY_RECORDED;
import static io.fireflysoftware.domain.insurance.delegatedauthority.core.constants.BinderSagaConstants.EVENT_STATUS_UPDATED;
import static io.fireflysoftware.domain.insurance.delegatedauthority.core.constants.BinderSagaConstants.SAGA_UPDATE_STATUS;
import static io.fireflysoftware.domain.insurance.delegatedauthority.core.constants.BinderSagaConstants.STEP_RECORD_STATUS_HISTORY;
import static io.fireflysoftware.domain.insurance.delegatedauthority.core.constants.BinderSagaConstants.STEP_RETRIEVE;
import static io.fireflysoftware.domain.insurance.delegatedauthority.core.constants.BinderSagaConstants.STEP_UPDATE_STATUS;

/**
 * Changes a binder status by reading it and writing the new status through the core.
 */
@Saga(name = SAGA_UPDATE_STATUS)
@Service
public class UpdateBinderStatusSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Autowired
    public UpdateBinderStatusSaga(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @SagaStep(id = STEP_RETRIEVE)
    @StepEvent(type = EVENT_RETRIEVED)
    public Mono<BinderDTO> retrieveBinder(GetBinderQuery query, ExecutionContext ctx) {
        return queryBus.query(query)
                .doOnNext(record -> ctx.putVariable(CTX_RECORD, record));
    }

    @SagaStep(id = STEP_UPDATE_STATUS, dependsOn = STEP_RETRIEVE, compensate = COMPENSATE_RESTORE)
    @StepEvent(type = EVENT_STATUS_UPDATED)
    public Mono<BinderDTO> updateBinderStatus(UpdateBinderStatusCommand command, ExecutionContext ctx) {
        BinderDTO current = ctx.getVariableAs(CTX_RECORD, BinderDTO.class);
        if (current == null || current.getBinderId() == null) {
            return Mono.error(new IllegalStateException("Binder was not found"));
        }
        if (current.getBinderStatus() == null) {
            return Mono.error(new IllegalStateException("Binder has no current status"));
        }
        if (command.status() == null || command.status().isBlank()) {
            return Mono.error(new IllegalArgumentException("Status is required"));
        }
        ctx.putVariable(CTX_PREVIOUS_STATUS, current.getBinderStatus().getValue());
        current.setBinderStatus(BinderDTO.BinderStatusEnum.fromValue(command.status()));
        return commandBus.send(new ApplyBinderStatusCommand(current.getBinderId(), current))
                .doOnNext(updated -> ctx.putVariable(CTX_RECORD, updated));
    }

    public Mono<Void> restoreBinderStatus(BinderDTO updated, ExecutionContext ctx) {
        if (updated == null || updated.getBinderId() == null) {
            return Mono.empty();
        }
        String previous = ctx.getVariableAs(CTX_PREVIOUS_STATUS, String.class);
        if (previous == null || previous.isBlank()) {
            return Mono.empty();
        }
        updated.setBinderStatus(BinderDTO.BinderStatusEnum.fromValue(previous));
        return commandBus.send(new ApplyBinderStatusCommand(updated.getBinderId(), updated)).then();
    }

}
