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
import io.fireflysoftware.domain.insurance.delegatedauthority.core.commands.CreateBinderCommand;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.commands.RemoveBinderCommand;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.constants.BinderSagaConstants;

import org.fireflyframework.cqrs.command.CommandBus;
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
 * Verifies the register saga stores the core result and can compensate by deleting it.
 */
@ExtendWith(MockitoExtension.class)
class RegisterBinderSagaTest {

    @Mock
    private CommandBus commandBus;

    private RegisterBinderSaga saga;

    @BeforeEach
    void setUp() {
        saga = new RegisterBinderSaga(commandBus);
    }

    @Test
    void register_storesTheRecordReturnedByTheCore() {
        UUID id = UUID.randomUUID();
        BinderDTO created = new BinderDTO(id);
        created.setBinderStatus(BinderDTO.BinderStatusEnum.DRAFT);
        when(commandBus.send(any(CreateBinderCommand.class))).thenReturn(Mono.just(created));

        ExecutionContext ctx = ExecutionContext.forSaga(BinderSagaConstants.SAGA_REGISTER, "test");
        BinderDTO request = new BinderDTO();
        request.setBinderStatus(BinderDTO.BinderStatusEnum.DRAFT);

        StepVerifier.create(saga.registerBinder(new CreateBinderCommand(request), ctx))
                .expectNext(created)
                .verifyComplete();

        assertThat(ctx.getVariableAs(BinderSagaConstants.CTX_RECORD, BinderDTO.class).getBinderId()).isEqualTo(id);
    }

    @Test
    void remove_sendsDeleteForTheCreatedRecord() {
        UUID id = UUID.randomUUID();
        when(commandBus.send(any(RemoveBinderCommand.class))).thenReturn(Mono.empty());

        StepVerifier.create(saga.removeBinder(new BinderDTO(id)))
                .verifyComplete();

        ArgumentCaptor<RemoveBinderCommand> captor = ArgumentCaptor.forClass(RemoveBinderCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().binderId()).isEqualTo(id);
    }

}
