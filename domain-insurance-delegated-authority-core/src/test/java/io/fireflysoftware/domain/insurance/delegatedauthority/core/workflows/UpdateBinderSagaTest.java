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
import io.fireflysoftware.domain.insurance.delegatedauthority.core.commands.UpdateBinderCommand;
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
 * Verifies a field update keeps the current status and can be restored.
 */
@ExtendWith(MockitoExtension.class)
class UpdateBinderSagaTest {

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    private UpdateBinderSaga saga;

    @BeforeEach
    void setUp() {
        saga = new UpdateBinderSaga(commandBus, queryBus);
    }

    @Test
    void update_keepsTheCurrentStatus() {
        UUID id = UUID.randomUUID();
        BinderDTO current = new BinderDTO(id);
        current.setBinderStatus(BinderDTO.BinderStatusEnum.DRAFT);
        BinderDTO incoming = new BinderDTO();
        incoming.setBinderStatus(BinderDTO.BinderStatusEnum.ACTIVE);
        when(commandBus.send(any(ApplyBinderStatusCommand.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0, ApplyBinderStatusCommand.class).record()));

        ExecutionContext ctx = ExecutionContext.forSaga(BinderSagaConstants.SAGA_UPDATE_RECORD, "test");
        ctx.putVariable(BinderSagaConstants.CTX_RECORD, current);

        StepVerifier.create(saga.updateBinder(new UpdateBinderCommand(id, incoming), ctx))
                .assertNext(saved -> assertThat(saved.getBinderStatus()).isEqualTo(BinderDTO.BinderStatusEnum.DRAFT))
                .verifyComplete();

        ArgumentCaptor<ApplyBinderStatusCommand> captor = ArgumentCaptor.forClass(ApplyBinderStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().binderId()).isEqualTo(id);
        assertThat(captor.getValue().record().getBinderStatus()).isEqualTo(BinderDTO.BinderStatusEnum.DRAFT);
        assertThat(ctx.getVariableAs(BinderSagaConstants.CTX_PREVIOUS_RECORD, BinderDTO.class)).isSameAs(current);
    }

    @Test
    void restore_writesThePreviousRecordBack() {
        UUID id = UUID.randomUUID();
        BinderDTO previous = new BinderDTO(id);
        previous.setBinderStatus(BinderDTO.BinderStatusEnum.DRAFT);
        BinderDTO updated = new BinderDTO(id);
        updated.setBinderStatus(BinderDTO.BinderStatusEnum.ACTIVE);
        when(commandBus.send(any(ApplyBinderStatusCommand.class))).thenReturn(Mono.just(previous));

        ExecutionContext ctx = ExecutionContext.forSaga(BinderSagaConstants.SAGA_UPDATE_RECORD, "test");
        ctx.putVariable(BinderSagaConstants.CTX_PREVIOUS_RECORD, previous);

        StepVerifier.create(saga.restoreBinder(updated, ctx))
                .verifyComplete();

        ArgumentCaptor<ApplyBinderStatusCommand> captor = ArgumentCaptor.forClass(ApplyBinderStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().record()).isSameAs(previous);
    }
}
