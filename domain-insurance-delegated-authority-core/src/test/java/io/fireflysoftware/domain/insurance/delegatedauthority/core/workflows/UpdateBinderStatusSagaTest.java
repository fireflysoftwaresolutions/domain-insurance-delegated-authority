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
import io.fireflysoftware.domain.insurance.delegatedauthority.core.commands.UpdateBinderStatusCommand;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.constants.BinderSagaConstants;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies a status change is written through the core and can be restored.
 */
@ExtendWith(MockitoExtension.class)
class UpdateBinderStatusSagaTest {

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    private UpdateBinderStatusSaga saga;

    @BeforeEach
    void setUp() {
        saga = new UpdateBinderStatusSaga(commandBus, queryBus);
    }

    @Test
    void updateStatus_sendsTheRequestedStatusToTheCore() {
        UUID id = UUID.randomUUID();
        BinderDTO current = new BinderDTO(id);
        current.setBinderStatus(BinderDTO.BinderStatusEnum.DRAFT);
        when(commandBus.send(any(ApplyBinderStatusCommand.class))).thenReturn(Mono.just(current));

        ExecutionContext ctx = ExecutionContext.forSaga(BinderSagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(BinderSagaConstants.CTX_RECORD, current);

        StepVerifier.create(saga.updateBinderStatus(new UpdateBinderStatusCommand(id, "ACTIVE", "Caller request"), ctx))
                .expectNext(current)
                .verifyComplete();

        ArgumentCaptor<ApplyBinderStatusCommand> captor = ArgumentCaptor.forClass(ApplyBinderStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().binderId()).isEqualTo(id);
        assertThat(captor.getValue().record().getBinderStatus()).isEqualTo(BinderDTO.BinderStatusEnum.ACTIVE);
        assertThat(ctx.getVariableAs(BinderSagaConstants.CTX_PREVIOUS_STATUS, String.class)).isEqualTo("DRAFT");
    }

    @Test
    void restore_writesThePreviousStatusBack() {
        UUID id = UUID.randomUUID();
        BinderDTO updated = new BinderDTO(id);
        updated.setBinderStatus(BinderDTO.BinderStatusEnum.ACTIVE);
        when(commandBus.send(any(ApplyBinderStatusCommand.class))).thenReturn(Mono.just(updated));

        ExecutionContext ctx = ExecutionContext.forSaga(BinderSagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(BinderSagaConstants.CTX_PREVIOUS_STATUS, "DRAFT");

        StepVerifier.create(saga.restoreBinderStatus(updated, ctx))
                .verifyComplete();

        ArgumentCaptor<ApplyBinderStatusCommand> captor = ArgumentCaptor.forClass(ApplyBinderStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().record().getBinderStatus()).isEqualTo(BinderDTO.BinderStatusEnum.DRAFT);
    }
}
