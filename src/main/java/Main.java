import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaSession;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException, UaException {
        //This will get all endpoints
        System.out.println("\n\nENDPOINTS:");
        List<EndpointDescription> endpoints = DiscoveryClient.getEndpoints("opc.tcp://opcuademo.sterfive.com:26543").get();
        System.out.println("Number of endpoints = " + endpoints.size());
        endpoints.stream().forEach(System.out::println);
        System.out.println();

        //Select endpoint
        System.out.println("SELECT ENDPOINT WITHOUT ENCRYPTION:");
        EndpointDescription oneOfEndpoints = endpoints.stream().filter(endpointDescription -> endpointDescription.getSecurityMode().equals(MessageSecurityMode.None)).findFirst().get();
        System.out.println();

        //Endpoint parameters
        System.out.println("ENDPOINT PARAMETERS:");
        System.out.println("EndpoitUrl: "+oneOfEndpoints.getEndpointUrl());
        System.out.println("SecurityPolicyUri: "+oneOfEndpoints.getSecurityPolicyUri()); //For security
        System.out.println("SecurityMode: "+oneOfEndpoints.getSecurityMode()); //For security
        System.out.println("ServerCertificate: "+oneOfEndpoints.getServerCertificate()); //For security
        System.out.println("UserIdentityTokens: "+oneOfEndpoints.getUserIdentityTokens()[0]);
        System.out.println();

        //select endpoint and configure connection
        OpcUaClientConfig opcUaConfiguration = OpcUaClientConfig.builder()
                                                    .setEndpoint(oneOfEndpoints)
                                                    .setApplicationName(LocalizedText.english("Test of eclipse milo opc-ua client")) //shows in session
                                                    .setRequestTimeout(uint(5000))
                                                    .build();
        OpcUaClient client = OpcUaClient.create(opcUaConfiguration);
        client.connect().get();
        System.out.println();

        //Session
        System.out.println("SESSION:");
        OpcUaSession opcUaSession = client.getSession().get();
        System.out.println(opcUaSession);


        System.out.println();

        //Browse nodes ASYNCHRONOUS
        System.out.println("BROWSE NODES:");
        System.out.println("First level:");
        List<CompletableFuture<Void>> sequence = new ArrayList<>();

        List<Node> nodesToPrint = client.getAddressSpace().browse(Identifiers.RootFolder).get();;
        for(Node node: nodesToPrint){
            sequence.add(node.getBrowseName().thenAccept(System.out::println));
        }
        CompletableFuture<Void> printAll = CompletableFuture.allOf(sequence.toArray(new CompletableFuture[0]));
        printAll.get();

        System.out.println("Second level:");
        sequence.clear();
        NodeId nodeId = nodesToPrint.get(0).getNodeId().get();
        nodesToPrint = client.getAddressSpace().browse(nodeId).get();
        for(Node node: nodesToPrint){
            sequence.add(node.getBrowseName().thenAccept(System.out::println));
        }
        printAll = CompletableFuture.allOf(sequence.toArray(new CompletableFuture[0]));
        printAll.get();

        System.out.println("Third level:");
        sequence.clear();
        nodeId = nodesToPrint.get(0).getNodeId().get();
        nodesToPrint = client.getAddressSpace().browse(nodeId).get();
        for(Node node: nodesToPrint){
            sequence.add(node.getBrowseName().thenAccept(System.out::println));
        }
        printAll = CompletableFuture.allOf(sequence.toArray(new CompletableFuture[0]));
        printAll.get();


        System.out.println();



        //Read/write values
        nodeId = new NodeId(3,"Scalar_Static_Boolean");

        //Read value
        System.out.println("READ VALUE:");
        System.out.println(client.getAddressSpace().getVariableNode(nodeId).get().getValue().get());

        //Data value
        System.out.println(client.readValue(0, TimestampsToReturn.Both,new NodeId(3,"Scalar_Static_Boolean")).get()); //Read directly
        System.out.println();





//        //Subscribe
//        // what to read
//        ReadValueId readValueId = new ReadValueId(nodeId, AttributeId.Value.uid(), null, null);
//
//        // monitoring parameters
//        int clientHandle = 123456789;
//        MonitoringParameters parameters =
//                new MonitoringParameters(uint(clientHandle), 1000.0, null, uint(10), true);
//
//
//        // creation request
//        MonitoredItemCreateRequest request =
//                new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters);
//
//        BiConsumer<UaMonitoredItem, DataValue> consumer =
//                (item, value) ->
//                        System.out.format("Changed happened: %s -> %s%n", item, value);
//
//        // setting the consumer after the subscription creation
//        BiConsumer<UaMonitoredItem, Integer> onItemCreated =
//                (monitoredItem, id) ->
//                        monitoredItem.setValueConsumer(consumer);
//
//        // creating the subscription
//        UaSubscription subscription =
//                client.getSubscriptionManager().createSubscription(100.0).get();
//
//        List<UaMonitoredItem> items = subscription.createMonitoredItems(
//                TimestampsToReturn.Both,
//                Arrays.asList(request),
//                onItemCreated)
//                .get();


        float floatValue = 1f;
        StatusCode statusCode = client.writeValue(new NodeId(3,"Scalar_Static_Float"),DataValue.valueOnly(new Variant(true))).get();
        if(statusCode.isBad()){
            System.out.println("ERROR: "+statusCode);
        }

//        //Write value
//        System.out.println("WRITE VALUE:");
//        boolean toggle = false;
//        for(int i= 0;i<100;i++){
//            Thread.sleep(500);
//            toggle = !toggle;
//            client.writeValue(nodeId, DataValue.valueOnly(new Variant(toggle))).get();
//            System.out.println("Value is: "+toggle);
//        }





    }
}
