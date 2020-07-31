import jdk.swing.interop.SwingInterOpUtils;
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
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class Main {
    private static ArrayList<Double> arrayList= new ArrayList<>();

    public static void main(String[] args) throws ExecutionException, InterruptedException, UaException {
        //This will get all endpoints
        System.out.println("\n\nENDPOINTS:");
        List<EndpointDescription> endpoints = DiscoveryClient.getEndpoints("opc.tcp://localhost:50000").get();
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
        OpcUaClientConfig opcUaConfiguration2 = OpcUaClientConfig.builder()
                                                    .setEndpoint(oneOfEndpoints)
                                                    .setApplicationName(LocalizedText.english("Test2 of eclipse milo opc-ua client")) //shows in session
                                                    .setRequestTimeout(uint(5000))
                                                    .build();
        OpcUaClient client = OpcUaClient.create(opcUaConfiguration);
        OpcUaClient client2 = OpcUaClient.create(opcUaConfiguration2);
        //CONNECTING
        System.out.println("CONNECTING:");
        long startConnection;
        long endConnection;
        float timeDifference;
        OpcUaSession opcUaSession;

        startConnection = System.currentTimeMillis();
        client.connect().get(); // only one connection is possible !!!
        endConnection= System.currentTimeMillis();
        timeDifference = (float)(endConnection- startConnection)/1000;
        opcUaSession = client.getSession().get();
        System.out.println("First connection took: "+ timeDifference +" s. And SESSION ID is " + opcUaSession.getSessionId());

        startConnection = System.currentTimeMillis();
        client2.connect().get(); // only one connection is possible !!!
        endConnection= System.currentTimeMillis();
        timeDifference = (float)(endConnection- startConnection)/1000;
        opcUaSession = client.getSession().get();
        System.out.println("Another client connection took: "+ timeDifference +" s. And SESSION ID is " + opcUaSession.getSessionId());

        startConnection = System.currentTimeMillis();
        client.connect().get(); // only one connection is possible !!!
        endConnection= System.currentTimeMillis();
        timeDifference = (float)(endConnection- startConnection)/1000;
        opcUaSession = client.getSession().get();
        System.out.println("Second connection took: "+ timeDifference +" s. And SESSION ID is " + opcUaSession.getSessionId());

        //DISCONNECTING
        client.disconnect().get();

        startConnection = System.currentTimeMillis();
        client.connect().get(); // only one connection is possible !!!
        endConnection= System.currentTimeMillis();
        timeDifference = (float)(endConnection- startConnection)/1000;
        opcUaSession = client.getSession().get();
        System.out.println("Third connection took: "+ timeDifference +" s. And SESSION ID is " + opcUaSession.getSessionId());



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



        //NODES
        NodeId booleanNode = new NodeId(2,"AlternatingBoolean");
        NodeId arrayOfIntegerNode = new NodeId(2,"FastUIntArray1");
        NodeId subscriptionDoubleNode = new NodeId(2,"DipData");

        //Read boolean value
        System.out.println("READ BOOLEAN VALUE:");
        boolean booleanOpcUa = (boolean)client.getAddressSpace().getVariableNode(booleanNode).get().getValue().get();
        System.out.println(booleanOpcUa);


        //Read array values
        System.out.println("READ ARRAY VALUES:");
        UInteger[] arrayOfIntegerOpcUa = (UInteger[]) client.getAddressSpace().getVariableNode(arrayOfIntegerNode).get().getValue().get();
        List<UInteger> ListOfIntegerOpcUa = List.of(arrayOfIntegerOpcUa);
        System.out.println(ListOfIntegerOpcUa);

        System.out.println("READ BOOLEAN DATA VALUE:");
        //Data value
        System.out.println(client.readValue(0, TimestampsToReturn.Both,booleanNode).get()); //Read directly
        System.out.println();








        //WRITING VALUES
        StatusCode statusCode;

        //WRITING BOOLEAN VALUE
        System.out.println("WRITE BOOLEAN VALUE");
        statusCode = client.writeValue(booleanNode,DataValue.valueOnly(new Variant(true))).get();
        if(statusCode.isBad()){
            System.out.println("ERROR: "+statusCode);
        }else{
            System.out.println("OK: "+statusCode);
        }

        //WRITING ARRAY OF FLOAT VALUES
        System.out.println("WRITE ARRAY OF UINT VALUE");
        UInteger[] arrayOfFloatWrite = {UInteger.valueOf(1),UInteger.valueOf(2),UInteger.valueOf(3),UInteger.valueOf(4),UInteger.valueOf(5),UInteger.valueOf(6)};
        statusCode = client.writeValue(arrayOfIntegerNode,DataValue.valueOnly(new Variant(arrayOfFloatWrite))).get();
        if(statusCode.isBad()){
            System.out.println("ERROR: "+statusCode);
        }else{
            System.out.println("OK: "+statusCode);
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










        //Subscribe
        // what to read
        ReadValueId readValueId = new ReadValueId(subscriptionDoubleNode, AttributeId.Value.uid(), null, null);

        // monitoring parameters
        int clientHandle = 123456789; //random number
        MonitoringParameters parameters =
                new MonitoringParameters(uint(clientHandle), 10.0, null, uint(10), true);


        // creation request
        MonitoredItemCreateRequest request =
                new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters);

        BiConsumer<UaMonitoredItem, DataValue> consumer =
                (item, value) ->
                        onChange(item,value);

        // setting the consumer after the subscription creation
        BiConsumer<UaMonitoredItem, Integer> onItemCreated =
                (monitoredItem, id) ->
                        monitoredItem.setValueConsumer(consumer);

        // creating the subscription
        UaSubscription subscription =
                client.getSubscriptionManager().createSubscription(10.0).get();

        List<UaMonitoredItem> items = subscription.createMonitoredItems(
                TimestampsToReturn.Both,
                Arrays.asList(request),
                onItemCreated)
                .get();

        //Time to read values 1000s
        Thread.sleep(1000000);







    }
    private static void onChange(UaMonitoredItem item, DataValue value){
        String valueName = item.getReadValueId().getNodeId().getIdentifier().toString();
        Double doubleValue = (Double)value.getValue().getValue();
        if(arrayList.size()<=1000){
            arrayList.add(doubleValue);
        }else{
            arrayList.remove(0);
            arrayList.add(doubleValue);
        }

        Double sum = 0.0;
        for(Double number:arrayList){
            sum +=number;
        }
        Double average = sum/arrayList.size();

        System.out.format("Changed happened: %s -> %s, average is %s\n", valueName , doubleValue,average);

    }
}
