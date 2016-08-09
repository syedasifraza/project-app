/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.project.app;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.link.LinkService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.nullIsNotFound;
import static org.onosproject.net.intent.IntentState.FAILED;
import static org.onosproject.net.intent.IntentState.WITHDRAWN;
import static org.slf4j.LoggerFactory.getLogger;

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Sample web resource.
 */
@Path("sample")
public class AppWebResource extends AbstractWebResource {

    /**
     * Get hello world greeting.
     *
     * @return 200 OK
     */
    @GET
    @Path("hello")
    public Response getGreeting() {
        ObjectNode node = mapper().createObjectNode().put("hello", "world");
        return ok(node).build();
    }

    @GET
    @Path("hosts")
    public Response getProjectHosts() {

        final Iterable<Host> hosts = get(HostService.class).getHosts();
        final ObjectNode root = encodeArray(Host.class, "hosts", hosts);
        return ok(root).build();

    }

    @GET
    @Path("intents")
    public Response getProjectIntents() {

        final Iterable<Link> links = get(LinkService.class).getLinks();
        final ObjectNode root = encodeArray(Link.class, "links", links);
        return ok(root).build();

    }

    @POST
    @Path("test")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createIntent(InputStream stream) {
        try {

            ObjectNode root = (ObjectNode) mapper().readTree(stream);

            return Response.ok().build();
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }


}
