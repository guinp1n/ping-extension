/*
 * Copyright 2018-present HiveMQ GmbH
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
package com.hivemq.extensions.ping;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pingreq.PingReqInboundInterceptor;
import com.hivemq.extension.sdk.api.parameter.*;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import com.hivemq.extension.sdk.api.services.session.SessionInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * This is the main class of the extension,
 * which is instantiated either during the HiveMQ start up process (if extension is enabled)
 * or when HiveMQ is already started by enabling the extension.
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class HelloWorldMain implements ExtensionMain {

    private static final @NotNull Logger log = LoggerFactory.getLogger(HelloWorldMain.class);

    @Override
    public void extensionStart(
            final @NotNull ExtensionStartInput extensionStartInput,
            final @NotNull ExtensionStartOutput extensionStartOutput) {

        try {
            final PingReqInboundInterceptor interceptor = (pingReqInboundInput, pingReqInboundOutput) -> {

                final String clientId = pingReqInboundInput.getClientInformation().getClientId();
                log.info("PingReqInboundInterceptor intercepted a PINGREQ packet of client '{}'.", clientId);
                final ClientService clientService = Services.clientService();
                CompletableFuture<Optional<SessionInformation>> sessionFuture = clientService.getSession(clientId);

                sessionFuture.whenComplete(new BiConsumer<Optional<SessionInformation>, Throwable>() {
                    @Override
                    public void accept(Optional<SessionInformation> sessionInformationOptional, Throwable throwable) {
                        if (throwable == null) {

                            if (sessionInformationOptional.isPresent()) {
                                SessionInformation information = sessionInformationOptional.get();
                                log.info("PingReqInboundInterceptor says Session Found");
                                log.info("PingReqInboundInterceptor says ID: " + information.getClientIdentifier());
                                log.info("PingReqInboundInterceptor says Connected: " + information.isConnected());
                                log.info("PingReqInboundInterceptor says Session Expiry Interval " + information.getSessionExpiryInterval());
                            } else {
                                log.info("PingReqInboundInterceptor says No session found for client id: " + clientId);
                            }

                        } else {
                            //please use more sophisticated logging
                            throwable.printStackTrace();
                        }
                    }
                });

            };

            Services.initializerRegistry().setClientInitializer((initializerInput, clientContext) -> {
                clientContext.addPingReqInboundInterceptor(interceptor);
            });

            final ExtensionInformation extensionInformation = extensionStartInput.getExtensionInformation();
            log.info("Started " + extensionInformation.getName() + ":" + extensionInformation.getVersion());

        } catch (final Exception e) {
            log.error("Exception thrown at extension start: ", e);
        }
    }

    @Override
    public void extensionStop(
            final @NotNull ExtensionStopInput extensionStopInput,
            final @NotNull ExtensionStopOutput extensionStopOutput) {

        final ExtensionInformation extensionInformation = extensionStopInput.getExtensionInformation();
        log.info("Stopped " + extensionInformation.getName() + ":" + extensionInformation.getVersion());
    }
}