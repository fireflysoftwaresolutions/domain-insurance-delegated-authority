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

package io.fireflysoftware.domain.insurance.delegatedauthority.web.controllers;

import io.fireflysoftware.core.insurance.authority.sdk.model.BinderDTO;
import io.fireflysoftware.domain.insurance.delegatedauthority.core.services.DelegatedAuthorityService;
import io.fireflysoftware.domain.insurance.delegatedauthority.interfaces.dtos.StatusChangeRequest;
import org.fireflyframework.web.error.config.ErrorHandlingProperties;
import org.fireflyframework.web.error.converter.ExceptionConverterService;
import org.fireflyframework.web.error.service.ErrorResponseNegotiator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Web slice tests for the binder domain API.
 */
@WebFluxTest(BindersController.class)
class BindersControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private DelegatedAuthorityService service;

    @MockitoBean
    private ExceptionConverterService exceptionConverterService;

    @MockitoBean
    private ErrorHandlingProperties errorHandlingProperties;

    @MockitoBean
    private ErrorResponseNegotiator errorResponseNegotiator;

    @Test
    void create_returns201() {
        when(service.create(any())).thenReturn(Mono.just(new BinderDTO()));

        webTestClient.post()
                .uri("/api/v1/binders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new BinderDTO())
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BinderDTO.class);
    }

    @Test
    void get_returns200WhenFound() {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenReturn(Mono.just(new BinderDTO(id)));

        webTestClient.get()
                .uri("/api/v1/binders/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BinderDTO.class);
    }

    @Test
    void get_returns404WhenMissing() {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/v1/binders/{id}", id)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateStatus_returns200() {
        UUID id = UUID.randomUUID();
        when(service.updateStatus(any())).thenReturn(Mono.just(new BinderDTO(id)));
        StatusChangeRequest request = new StatusChangeRequest();
        request.setStatus("ACTIVE");
        request.setStatusReason("Caller request");

        webTestClient.put()
                .uri("/api/v1/binders/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BinderDTO.class);
    }
}
