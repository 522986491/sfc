/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.SfpServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.SfpServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class has the APIs to operate on the ServiceFunctionPath
 * datastore.
 *
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 * @see org.opendaylight.sfc.provider.SfcProviderSfpEntryDataListener
 *
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderServicePathAPI implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServicePathAPI.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private String methodName = null;
    private Object[] parameters;
    private Class[] parameterTypes;
    private static AtomicInteger numCreatedPath = new AtomicInteger(0);


    SfcProviderServicePathAPI (Object[] params, String m) {
        int i = 0;
        this.methodName = m;
        this.parameters = new Object[params.length];
        this.parameterTypes = new Class[params.length];
        this.parameters = Arrays.copyOf(params, params.length);
        for (Object obj : parameters) {
            this.parameterTypes[i] = obj.getClass();
            i++;
        }

    }

    SfcProviderServicePathAPI (Object[] params, Class[] paramsTypes, String m) {
        this.methodName = m;
        this.parameters = new Object[params.length];
        this.parameterTypes = new Class[params.length];
        this.parameters = Arrays.copyOf(params, params.length);
        this.parameterTypes = Arrays.copyOf(paramsTypes, paramsTypes.length);
    }

    public static  SfcProviderServicePathAPI getDeleteServicePathContainingFunction (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServicePathContainingFunction");
    }

    public static  SfcProviderServicePathAPI getDeleteServicePathInstantiatedFromChain (Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "deleteServicePathInstantiatedFromChain");
    }

    public static  SfcProviderServicePathAPI getCreateServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "createServiceFunctionPathEntry");
    }

    public static  SfcProviderServicePathAPI getUpdateServicePathAPI(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateServiceFunctionPathEntry");
    }

    public static  SfcProviderServicePathAPI getUpdateServicePathInstantiatedFromChain(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateServicePathInstantiatedFromChain");
    }

    public static  SfcProviderServicePathAPI getUpdateServicePathContainingFunction(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServicePathAPI(params, paramsTypes, "updateServicePathContainingFunction");
    }


    public int numCreatedPathIncrementGet() {
        return numCreatedPath.incrementAndGet();
    }

    public int numCreatedPathDecrementGet() {
        return numCreatedPath.decrementAndGet();
    }

    public static int numCreatedPathGetValue() {
        return numCreatedPath.get();
    }

   /* Today A Service Function Chain modification is catastrophic. We delete all Paths
    * and recreate them. Maybe a real patch is possible but given the complexities of the possible
    * modifications, this is the safest approach.
    */
   @SuppressWarnings("unused")
    private void updateServicePathInstantiatedFromChain (ServiceFunctionPath serviceFunctionPath) {
        deleteServicePathInstantiatedFromChain(serviceFunctionPath);
        createServiceFunctionPathEntry(serviceFunctionPath);
    }

    // TODO:Needs change
    private void deleteServicePathInstantiatedFromChain (ServiceFunctionPath serviceFunctionPath) {

        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionChain serviceFunctionChain;
        String serviceChainName = serviceFunctionPath.getServiceChainName();
        if ((serviceChainName == null) || ((serviceFunctionChain = SfcProviderServiceChainAPI
                .readServiceFunctionChain(serviceChainName)) == null)) {
            LOG.error("\n########## ServiceFunctionChain name for Path {} not provided",
                    serviceFunctionPath.getName());
            return;
        }


        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        ServiceFunctionChainState serviceFunctionChainState;
        ServiceFunctionChainStateKey serviceFunctionChainStateKey =
                new ServiceFunctionChainStateKey(serviceFunctionChain.getName());
        InstanceIdentifier<ServiceFunctionChainState> sfcStateIID =
                InstanceIdentifier.builder(ServiceFunctionChainsState.class)
                        .child(ServiceFunctionChainState.class, serviceFunctionChainStateKey)
                        .build();

        ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
        Optional<ServiceFunctionChainState> serviceFunctionChainStateObject = null;
        try {
            serviceFunctionChainStateObject = readTx.read(LogicalDatastoreType.OPERATIONAL, sfcStateIID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        // TODO: Remove path name from Service Function path list
        if (serviceFunctionChainStateObject instanceof ServiceFunctionChainState) {
            serviceFunctionChainState = (ServiceFunctionChainState) serviceFunctionChainStateObject;
            List<String> sfcServiceFunctionPathList =
                    serviceFunctionChainState.getSfcServiceFunctionPath();
            List<String> removedPaths = new ArrayList<>();
            for (String pathName : sfcServiceFunctionPathList) {

                ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(pathName);
                sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                        .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                        .build();

                WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
                writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                        sfpIID);
                writeTx.commit();

            }

            sfcServiceFunctionPathList.removeAll(removedPaths);

            /* After we are done removing all paths from the datastore we commit the updated the path list
             * under the Service Chain operational tree
             */
            ServiceFunctionChainStateBuilder serviceFunctionChainStateBuilder  = new ServiceFunctionChainStateBuilder();
            serviceFunctionChainStateBuilder.setName(serviceFunctionChain.getName());
            serviceFunctionChainStateBuilder.setSfcServiceFunctionPath(sfcServiceFunctionPathList);
            WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.OPERATIONAL,
                    sfcStateIID, serviceFunctionChainStateBuilder.build(), true);
            writeTx.commit();

        } else {
            LOG.error("Failed to get reference to Service Function Chain State {} ", serviceFunctionChain.getName());
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

    @SuppressWarnings("unused")
    private void updateServiceFunctionPathEntry (ServiceFunctionPath serviceFunctionPath) {
        this.createServiceFunctionPathEntry(serviceFunctionPath);
    }

    /*
     * This function is actually an updated to a previously created SFP where only
     * the service chain name was given. In this function we patch the SFP with the
     * names of the chosen SFs
     */
    private void createServiceFunctionPathEntry (ServiceFunctionPath serviceFunctionPath) {

        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        long pathId;
        int pos_index = 0;
        int service_index;
        ServiceFunctionChain serviceFunctionChain;
        String serviceFunctionChainName = serviceFunctionPath.getServiceChainName();
        if ((serviceFunctionChainName == null) || ((serviceFunctionChain = SfcProviderServiceChainAPI
                    .readServiceFunctionChain(serviceFunctionChainName)) == null)) {
            LOG.error("\n########## ServiceFunctionChain name for Path {} not provided",
                    serviceFunctionPath.getName());
            return;
        }


        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        ArrayList<SfpServiceFunction> sfpServiceFunctionArrayList= new ArrayList<>();
        SfpServiceFunctionBuilder sfpServiceFunctionBuilder = new SfpServiceFunctionBuilder();

        /*
         * For each ServiceFunction type in the list of ServiceFunctions we select a specific
         * service function from the list of service functions by type.
         */
        List<SfcServiceFunction> SfcServiceFunctionList = serviceFunctionChain.getSfcServiceFunction();
        service_index = SfcServiceFunctionList.size();
        for (SfcServiceFunction sfcServiceFunction : SfcServiceFunctionList) {
            LOG.debug("\n########## ServiceFunction name: {}", sfcServiceFunction.getName());

            /*
             * We iterate thorough the list of service function types and for each one we try to get
             * get a suitable Service Function. WE need to perform lots of checking to make sure
             * we do not hit NULL Pointer exceptions
             */

            ServiceFunctionType serviceFunctionType = SfcProviderServiceTypeAPI.getServiceFunctionTypeList(sfcServiceFunction.getType());
            if (serviceFunctionType != null) {
                List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
                if (!sftServiceFunctionNameList.isEmpty()) {
                    for (SftServiceFunctionName sftServiceFunctionName : sftServiceFunctionNameList) {
                        // TODO: API to select suitable Service Function
                        String serviceFunctionName = sftServiceFunctionName.getName();
                        ServiceFunction serviceFunction;
                        if ((serviceFunctionName != null) && ((
                             serviceFunction = SfcProviderServiceFunctionAPI
                                    .readServiceFunction(serviceFunctionName)) != null)) {

                            sfpServiceFunctionBuilder.setName(serviceFunctionName)
                                    .setServiceIndex((short)service_index)
                                        .setServiceFunctionForwarder(serviceFunction.getSfDataPlaneLocator()
                                                .getServiceFunctionForwarder());
                            sfpServiceFunctionArrayList.add(pos_index,sfpServiceFunctionBuilder.build());
                            service_index--;
                            pos_index++;
                            break;
                        } else {
                            LOG.error("\n####### Could not find suitable SF of type in data store: {}",
                                    sfcServiceFunction.getType());
                            return;
                        }
                    }
                } else {
                    LOG.error("\n########## No configured SFs of type: {}", sfcServiceFunction.getType());
                    return;
                }
            } else {
                LOG.error("\n########## No configured SFs of type: {}", sfcServiceFunction.getType());
                return;
            }

        }

        //Build the service function path so it can be committed to datastore


        pathId = (serviceFunctionPath.getPathId() != null)  ?  serviceFunctionPath.getPathId()
                : numCreatedPathIncrementGet();
        serviceFunctionPathBuilder.setSfpServiceFunction(sfpServiceFunctionArrayList);
        if (serviceFunctionPath.getName().isEmpty())  {
            serviceFunctionPathBuilder.setName(serviceFunctionChainName + "-Path-" + pathId);
        } else {
            serviceFunctionPathBuilder.setName(serviceFunctionPath.getName());

        }

        serviceFunctionPathBuilder.setPathId(pathId);
        // TODO: Find out the exact rules for service index generation
        serviceFunctionPathBuilder.setStartingIndex((short) (sfpServiceFunctionArrayList.size() + 1));
        serviceFunctionPathBuilder.setServiceChainName(serviceFunctionChainName);

        ServiceFunctionPathKey serviceFunctionPathKey = new
                ServiceFunctionPathKey(serviceFunctionPathBuilder.getName());
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                .build();

        ServiceFunctionPath newServiceFunctionPath = serviceFunctionPathBuilder.build();
        WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION,
                sfpIID, newServiceFunctionPath, true);
        writeTx.commit();
        //SfcProviderServiceForwarderAPI.addPathIdtoServiceFunctionForwarder(newServiceFunctionPath);
        SfcProviderServiceFunctionAPI.addPathToServiceFunctionState(newServiceFunctionPath);

        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);

    }

    private void deleteServiceFunctionPathEntry (ServiceFunctionChain serviceFunctionChain) {

        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        String serviceFunctionChainName = serviceFunctionChain.getName();
        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(serviceFunctionChainName + "-Path");
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                .build();

        WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                sfpIID);
        writeTx.commit();
        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);

    }

    public static ServiceFunctionPath readServiceFunctionPath (String path) {
        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionPathKey serviceFuntionPathKey = new ServiceFunctionPathKey(path);
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFuntionPathKey)
                .build();

        ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
        Optional<ServiceFunctionPath> serviceFunctionPathObject = null;
        try {
            serviceFunctionPathObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfpIID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (serviceFunctionPathObject != null  &&
                (serviceFunctionPathObject.get() instanceof ServiceFunctionPath)) {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return serviceFunctionPathObject.get();
        } else {
            LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return null;
        }
    }

    /*
     * We iterate through all service paths that use this service function and remove them.
     * In the end since there is no more operational state, we remove the state tree.
     */

    @SuppressWarnings("unused")
    private void deleteServicePathContainingFunction (ServiceFunction serviceFunction) {

        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        ServiceFunctionState serviceFunctionState;
        ServiceFunctionStateKey serviceFunctionStateKey =
                new ServiceFunctionStateKey(serviceFunction.getName());
        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();

        ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
        Optional<ServiceFunctionState> serviceFunctionStateObject = null;
        try {
            serviceFunctionStateObject = readTx.read(LogicalDatastoreType.OPERATIONAL, sfStateIID).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (serviceFunctionStateObject != null &&
                (serviceFunctionStateObject.get() instanceof ServiceFunctionState)) {
            serviceFunctionState = serviceFunctionStateObject.get();
            List<String> sfServiceFunctionPathList =
                    serviceFunctionState.getSfServiceFunctionPath();
            List<String> removedPaths = new ArrayList<>();
            for (String pathName : sfServiceFunctionPathList) {

                ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(pathName);
                sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                        .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                        .build();

                WriteTransaction writeTx = odlSfc.dataProvider.newWriteOnlyTransaction();
                writeTx.delete(LogicalDatastoreType.CONFIGURATION,
                        sfpIID);
                writeTx.commit();
                // TODO: Need to consider failure of transaction
                removedPaths.add(pathName);
            }

            // If no more SFP associated with this SF, remove the state.
            if (removedPaths.containsAll(sfServiceFunctionPathList)) {
                SfcProviderServiceFunctionAPI.deleteServiceFunctionState(serviceFunction.getName());
            } else {
                LOG.error("Could not remove all paths containing function: {} ", serviceFunction.getName());
            }
        } else {
            LOG.warn("Failed to get reference to Service Function State {} ", serviceFunction.getName());
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }


    /*
     * When a SF is updated, meaning key remains the same, but other fields change we need to
     * update all affected SFPs. We need to do that because admin can update critical fields
     * as SFC type, rendering the path unfeasible. The update reads the current path from
     * data store, keeps pathID intact and rebuild the SF list.
     *
     * The update can or not work.
     */
    private void updateServicePathContainingFunction (ServiceFunction serviceFunction) {

        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);

        InstanceIdentifier<ServiceFunctionPath> sfpIID;

        ServiceFunctionState serviceFunctionState = SfcProviderServiceFunctionAPI.readServiceFunctionState(serviceFunction.getName());
        if (serviceFunctionState != null) {
            List<String> sfServiceFunctionPathList =
                    serviceFunctionState.getSfServiceFunctionPath();
            for (String pathName : sfServiceFunctionPathList) {

                ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(pathName);
                sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                        .child(ServiceFunctionPath.class, serviceFunctionPathKey)
                        .build();

                ReadOnlyTransaction readTx = odlSfc.dataProvider.newReadOnlyTransaction();
                Optional<ServiceFunctionPath> serviceFunctionPathObject = null;
                try {
                    serviceFunctionPathObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfpIID).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                if (serviceFunctionPathObject != null &&
                        (serviceFunctionPathObject.get() instanceof  ServiceFunctionPath)) {
                    ServiceFunctionPath servicefunctionPath = serviceFunctionPathObject.get();
                    createServiceFunctionPathEntry(servicefunctionPath);
                }
            }
        } else {
            LOG.error("Failed to get reference to Service Function State {} ", serviceFunction.getName());
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
    }

    @Override
    public void run() {
        if (methodName != null) {
            Class<?> c = this.getClass();
            Method method;
            try {
                method = c.getDeclaredMethod(methodName, parameterTypes);
                method.invoke(this, parameters);
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

    }
}