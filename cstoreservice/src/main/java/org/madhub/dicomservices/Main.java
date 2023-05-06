package org.madhub.dicomservices;

import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.TransferCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Main {
    static  final Logger LOG = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {

        String serverPort = System.getenv("PORT");
        if ( serverPort == null || serverPort.isEmpty()) {
            serverPort = "11112";
        }
        // set STORAGE_PATH environment variable to store files in given directory
        String storagePath = System.getenv("STORAGE_PATH");
        if ( storagePath == null || storagePath.isEmpty()) {
            storagePath = "dicomfiles";
        }

        Device device;
        ApplicationEntity ae;
        Connection connection;
        device = new Device("store-scp");
        ae = new ApplicationEntity("*");
        connection = new Connection(null,"0.0.0.0",Integer.parseInt(serverPort));
        device.addApplicationEntity(ae);
        device.addConnection(connection);
        ae.addConnection(connection);
        ae.addTransferCapability( new TransferCapability(null, UID.Verification, TransferCapability.Role.SCP,
                UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian));
        ae.addTransferCapability( new TransferCapability(null, UID.Verification, TransferCapability.Role.SCP,
                UID.ImplicitVRLittleEndian,UID.ImplicitVRLittleEndian));
        ae.addTransferCapability( new TransferCapability(null, UID.CTImageStorage,
                TransferCapability.Role.SCP,UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian));
        ae.addTransferCapability( new TransferCapability(null, UID.MRImageStorage,
                TransferCapability.Role.SCP,UID.ImplicitVRLittleEndian,UID.ExplicitVRLittleEndian));

        StoreScp storeScp = new StoreScp(storagePath);
        device.setDimseRQHandler(storeScp);

        device.setAssociationMonitor(storeScp);
        ExecutorService executorService = Executors.newCachedThreadPool();
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        try {
            device.setExecutor(executorService);
            device.setScheduledExecutor(scheduledExecutorService);
            device.bindConnections();
            LOG.info("Application Started..."+connection.toString());
        }catch (Exception exception) {
            LOG.error("Application failed to start",exception);
        }

    }
}