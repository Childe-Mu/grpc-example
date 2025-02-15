/*
 * Copyright 2023 The gRPC Authors
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

package io.grpc.examples.keepalive;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Server that manages startup/shutdown of a keep alive server.
 */
public class KeepAliveServer {
    private static final Logger logger = Logger.getLogger(KeepAliveServer.class.getName());

    private Server server;

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final KeepAliveServer server = new KeepAliveServer();
        server.start();
        server.blockUntilShutdown();
    }

    private void start() throws IOException {
        /* The port on which the server should run */
        int port = 50051;

        // Start a server with the following configurations (demo only, you should set more appropriate
        // values based on your real environment):
        // keepAliveTime: Ping the client if it is idle for 5 seconds to ensure the connection is
        // still active. Set to an appropriate value in reality, e.g. in minutes.
        // keepAliveTimeout: Wait 1 second for the ping ack before assuming the connection is dead.
        // Set to an appropriate value in reality, e.g. (10, TimeUnit.SECONDS).
        // permitKeepAliveTime: If a client pings more than once every 5 seconds, terminate the
        // connection.
        // permitKeepAliveWithoutCalls: Allow pings even when there are no active streams.
        // maxConnectionIdle: If a client is idle for 15 seconds, send a GOAWAY.
        // maxConnectionAge: If any connection is alive for more than 30 seconds, send a GOAWAY.
        // maxConnectionAgeGrace: Allow 5 seconds for pending RPCs to complete before forcibly closing
        // connections.
        // Use JAVA_OPTS=-Djava.util.logging.config.file=logging.properties to see keep alive ping
        // frames.
        // More details see: https://github.com/grpc/proposal/blob/master/A9-server-side-conn-mgt.md
        server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                .addService(new GreeterImpl())
                .keepAliveTime(5, TimeUnit.SECONDS)
                .keepAliveTimeout(1, TimeUnit.SECONDS)
                .permitKeepAliveTime(5, TimeUnit.SECONDS)
                .permitKeepAliveWithoutCalls(true)
                .maxConnectionIdle(15, TimeUnit.SECONDS)
                .maxConnectionAge(30, TimeUnit.SECONDS)
                .maxConnectionAgeGrace(5, TimeUnit.SECONDS)
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    KeepAliveServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

        @Override
        public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
            HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}
