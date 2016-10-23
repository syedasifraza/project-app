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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import sun.misc.IOUtils;

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
import javax.xml.soap.Text;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;
import static org.onlab.util.Tools.nullIsNotFound;
import static org.onosproject.net.intent.IntentState.FAILED;
import static org.onosproject.net.intent.IntentState.WITHDRAWN;
import static org.slf4j.LoggerFactory.getLogger;

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Sample web resource.
 */
@Path("path")
public class AppWebResource extends AbstractWebResource {

    private UriInfo uriInfo;
    /**
     * Get hello world greeting.
     *
     * @return 200 OK
     */
  /*  @GET
    @Path("hello")
    public Response getGreeting() {
        ObjectNode node = mapper().createObjectNode().put("hello", "world");
        return ok(node).build();
    }*/

    @GET
    @Path("hosts")
    public Response getProjectHosts() {

        final Iterable<Host> hosts = get(HostService.class).getHosts();
        final ObjectNode root = encodeArray(Host.class, "hosts", hosts);
        return ok(root).build();

    }

    @GET
    @Path("links")
    public Response getProjectIntents() {

        final Iterable<Link> links = get(LinkService.class).getLinks();
        final ObjectNode root = encodeArray(Link.class, "links", links);
        return ok(root).build();

    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createIntent(InputStream stream) {
        String flowOne=null;
        String flowTwo=null;
        String linkDstSwId=null;
        String ingressPort=null;
        Integer Err=0;

        InputStream flowStream;
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode DstHost = jsonTree.path("DstHost");
            JsonNode SrcHost = jsonTree.path("SrcHost");
            ArrayNode PathLinks = jsonTree.get("PathInfo") == null
                    ? mapper().createArrayNode() : (ArrayNode) jsonTree.get("PathInfo");

            //When both hosts on same switch
            if(SrcHost.get("SrcSwId").asText().equals(DstHost.get("DstSwId").asText())){
                flowOne = flowSetup(DstHost.get("DstMac").asText(),
                        SrcHost.get("SrcMac").asText(),
                        SrcHost.get("SrcPort").asText(),
                        SrcHost.get("SrcSwId").asText(),
                        DstHost.get("DstPort").asText(),
                        SrcHost.get("SrcSwId").asText());

                flowTwo = flowSetup(SrcHost.get("SrcMac").asText(),
                        DstHost.get("DstMac").asText(),
                        DstHost.get("DstPort").asText(),
                        SrcHost.get("SrcSwId").asText(),
                        SrcHost.get("SrcPort").asText(),
                        SrcHost.get("SrcSwId").asText());

                flowStream = new ByteArrayInputStream(flowOne.getBytes(StandardCharsets.UTF_8));
                pathSetup(flowStream);
                flowStream = new ByteArrayInputStream(flowTwo.getBytes(StandardCharsets.UTF_8));
                pathSetup(flowStream);
            }

            for (JsonNode node : PathLinks) {
                //When Source host's Switch ID and Link's Source Switch ID same
                if(SrcHost.get("SrcSwId").asText().equals(node.get("SrcSwId").asText())){
                    flowOne = flowSetup(DstHost.get("DstMac").asText(),
                            SrcHost.get("SrcMac").asText(),
                            SrcHost.get("SrcPort").asText(),
                            node.get("SrcSwId").asText(),
                            node.get("SrcPort").asText(),
                            node.get("SrcSwId").asText());

                    flowTwo = flowSetup(SrcHost.get("SrcMac").asText(),
                            DstHost.get("DstMac").asText(),
                            node.get("SrcPort").asText(),
                            node.get("SrcSwId").asText(),
                            SrcHost.get("SrcPort").asText(),
                            node.get("SrcSwId").asText());

                    flowStream = new ByteArrayInputStream(flowOne.getBytes(StandardCharsets.UTF_8));
                    pathSetup(flowStream);
                    flowStream = new ByteArrayInputStream(flowTwo.getBytes(StandardCharsets.UTF_8));
                    pathSetup(flowStream);
                }

                //When Destination host's Switch ID and Link's Destination Switch ID same
                if(DstHost.get("DstSwId").asText().equals(node.get("DstSwId").asText())){
                    flowOne = flowSetup(DstHost.get("DstMac").asText(),
                            SrcHost.get("SrcMac").asText(),
                            node.get("DstPort").asText(),
                            node.get("DstSwId").asText(),
                            DstHost.get("DstPort").asText(),
                            node.get("DstSwId").asText());

                    flowTwo = flowSetup(SrcHost.get("SrcMac").asText(),
                            DstHost.get("DstMac").asText(),
                            DstHost.get("DstPort").asText(),
                            node.get("DstSwId").asText(),
                            node.get("DstPort").asText(),
                            node.get("DstSwId").asText());

                    flowStream = new ByteArrayInputStream(flowOne.getBytes(StandardCharsets.UTF_8));
                    pathSetup(flowStream);
                    flowStream = new ByteArrayInputStream(flowTwo.getBytes(StandardCharsets.UTF_8));
                    pathSetup(flowStream);
                }
                                
                // when Source Switch Id of link and Dst of link are same then create link path between two switches
                if(node.get("SrcSwId").asText().equals(linkDstSwId)){
                    flowOne = flowSetup(DstHost.get("DstMac").asText(),
                            SrcHost.get("SrcMac").asText(),
                            ingressPort,
                            node.get("SrcSwId").asText(),
                            node.get("SrcPort").asText(),
                            node.get("SrcSwId").asText());

                    flowTwo = flowSetup(SrcHost.get("SrcMac").asText(),
                            DstHost.get("DstMac").asText(),
                            node.get("SrcPort").asText(),
                            node.get("SrcSwId").asText(),
                            ingressPort,
                            node.get("SrcSwId").asText());

                    flowStream = new ByteArrayInputStream(flowOne.getBytes(StandardCharsets.UTF_8));
                    pathSetup(flowStream);
                    flowStream = new ByteArrayInputStream(flowTwo.getBytes(StandardCharsets.UTF_8));
                    pathSetup(flowStream);
                }
                linkDstSwId = node.get("DstSwId").asText();
                ingressPort = node.get("DstPort").asText();

            }

            return Response.status(Err).entity(stream).build();

        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);

        }


    }

    public void pathSetup(InputStream stream) {
        try {
            IntentService service = get(IntentService.class);
            ObjectNode root = (ObjectNode) mapper().readTree(stream);
            Intent intent = codec(Intent.class).decode(root, this);
            service.submit(intent);
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }

    public String flowSetup(String Arg1, String Arg2, String Arg3, String Arg4, String Arg5, String Arg6){
        String test1 = "{ \"type\": \"PointToPointIntent\", \"appId\": \"org.project.app\", \"selector\":"+
                "{ \"criteria\": [ { \"type\": \"ETH_DST\", \"mac\": \"";
        String flow=null;
        flow = test1+
                Arg1+"\"}, { \"type\": \"ETH_SRC\", \"mac\": \""+
                Arg2+"\"} ] }, \"priority\": 100, \"ingressPoint\": { \"port\": \""+
                Arg3+"\", \"device\": \""+
                Arg4+"\" }, \"egressPoint\": { \"port\": \""+
                Arg5+"\", \"device\": \""+
                Arg6+"\" } }\"";
        return flow;
    }

    /*@POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createIntent(InputStream stream) {
        try {
            IntentService service = get(IntentService.class);
            ObjectNode root = (ObjectNode) mapper().readTree(stream);
            Intent intent = codec(Intent.class).decode(root, this);
            service.submit(intent);
            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path("intents")
                    .path(intent.appId().name())
                    .path(Long.toString(intent.id().fingerprint()));
            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }*/


}
/*
 public Response createIntent(InputStream stream) {
        String test=null;
        Integer Err=0;
        String test1 = "{ \"type\": \"PointToPointIntent\", \"appId\": \"org.project.app\", \"selector\": { \"criteria\": [ { \"type\": \"ETH_DST\", \"mac\": \"12:EC:CE:07:11:44\" }, { \"type\": \"ETH_SRC\", \"mac\": \"06:44:CA:7F:29:A3\"} ] }, \"priority\": 55, \"ingressPoint\": { \"port\": \"4\", \"device\": \"of:0000000000000103\" }, \"egressPoint\": { \"port\": \"1\", \"device\": \"of:0000000000000103\" } }";
        InputStream Stest;
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode DstHost = jsonTree.path("DstHost");
            JsonNode SrcHost = jsonTree.path("SrcHost");
            ArrayNode PathLinks = jsonTree.get("PathInfo") == null
                    ? mapper().createArrayNode() : (ArrayNode) jsonTree.get("PathInfo");
            for (JsonNode node : PathLinks) {
                JsonNode PathSrcSwId = node.get("SrcSwID");
                JsonNode PathSrcPort = node.get("SrcPort");
                if(PathSrcPort.asText().equals("6")){
                    Err = 200;
                    test = PathSrcPort.toString();
                }
                else{
                    Err = 201;
                    test = PathSrcPort.toString();
                }

            }

            Stest = new ByteArrayInputStream(test1.getBytes(StandardCharsets.UTF_8));
            pathIntent(Stest);
            return Response.status(Err).entity(stream).build();

        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);

        }


    }

    public void pathIntent(InputStream stream) {
        try {
            IntentService service = get(IntentService.class);
            ObjectNode root = (ObjectNode) mapper().readTree(stream);
            Intent intent = codec(Intent.class).decode(root, this);
            service.submit(intent);
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }
 */