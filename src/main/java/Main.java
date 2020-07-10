import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException, UaException {
        //This will get all endpoints
        System.out.println("\n\nENDPOINTS:");
        List<EndpointDescription> endpoints = DiscoveryClient.getEndpoints("opc.tcp://opcua.123mc.com:4840/").get();
        System.out.println("Number of endpoints = " + endpoints.size());
        endpoints.stream().forEach(System.out::println);

        //Endpoints parameters
        EndpointDescription oneOfEndpoints = endpoints.get(6);
        System.out.println("EndpoitUrl: "+oneOfEndpoints.getEndpointUrl());
        System.out.println("ServerCertificate: "+oneOfEndpoints.getServerCertificate()); //For security
        System.out.println("SecurityPolicyUri: "+oneOfEndpoints.getSecurityPolicyUri()); //For security
        System.out.println("SecurityMode: "+oneOfEndpoints.getSecurityMode()); //For security
        System.out.println("UserIdentityTokens: "+oneOfEndpoints.getUserIdentityTokens());
        System.out.println();

        //select endpoint and configure connection
        OpcUaClientConfigBuilder cfg = new OpcUaClientConfigBuilder();
        cfg.setEndpoint(oneOfEndpoints);
        OpcUaClient client = OpcUaClient.create(cfg.build());
        client.connect().get();

        //Browse nodes
        System.out.println("BROWSE NODES:");
        client.getAddressSpace().browse(Identifiers.RootFolder).get() //known NodeIds
                .stream()
                .map(Node::getBrowseName)
                .forEach(name -> {
                    try {
                        System.out.println(name.get());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                });

        //Prints serial number
        System.out.println("SERIAL NUMBER:");
        System.out.println(client.getAddressSpace().getVariableNode(new NodeId(3,"SerialNumber")).get().getValue().get());

        //Data value
        System.out.println(client.readValue(0, TimestampsToReturn.Both,new NodeId(3,"SerialNumber")).get()); //Read directly
        System.out.println();
        //Program is running and this is done when the value is ready
        client.readValue(0, TimestampsToReturn.Both,new NodeId(3,"SerialNumber")).thenAccept(System.out::println);


    }
}
