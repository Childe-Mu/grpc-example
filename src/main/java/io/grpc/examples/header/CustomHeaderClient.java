/*
 * Copyright 2015 The gRPC Authors
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

package io.grpc.examples.header;

import io.grpc.*;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple client that like {@link io.grpc.examples.helloworld.HelloWorldClient}.
 * This client can help you create custom headers.
 */
public class CustomHeaderClient {
    private static final Logger logger = Logger.getLogger(CustomHeaderClient.class.getName());

    private final ManagedChannel originChannel;
    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    /**
     * A custom client.
     */
    private CustomHeaderClient(String host, int port) {
        originChannel = Grpc
                .newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create())
                .build();
        ClientInterceptor interceptor = new HeaderClientInterceptor();
        Channel channel = ClientInterceptors.intercept(originChannel, interceptor);
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    /**
     * Main start the client from the command line.
     */
    public static void main(String[] args) throws Exception {
        // Access a service running on the local machine on port 50051
        CustomHeaderClient client = new CustomHeaderClient("localhost", 50051);
        try {
            String user = "world";
            // Use the arg as the name to greet if provided
            if (args.length > 0) {
                user = args[0];
            }
            client.greet(user);
        } finally {
            client.shutdown();
        }
    }

    private void shutdown() throws InterruptedException {
        originChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * A simple client method that like {@link io.grpc.examples.helloworld.HelloWorldClient}.
     */
    private void greet(String name) {
        logger.info("Will try to greet " + name + " ...");
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        try {
            response = blockingStub.sayHello(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        logger.info("Greeting: " + response.getMessage());
    }
}
