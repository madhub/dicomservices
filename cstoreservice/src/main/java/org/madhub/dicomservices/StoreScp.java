package org.madhub.dicomservices;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.AAssociateRJ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.net.service.DicomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
/*
    Implements DICOMStore receiver
 */
public class StoreScp extends BasicCStoreSCP implements  AssociationMonitor{
    static  final Logger LOG = LoggerFactory.getLogger(StoreScp.class);
    private String storageDir;
    private int status = 0;
    private static final String DCM_EXT = ".dcm";
    private String hostName = "";
    public StoreScp(String storageDir) {
        this.storageDir = storageDir;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.error("["+hostName+"] : Getting local hostname failed",e);
        }

    }

    private Path getStorageFilePath(String aeTitle, String assoicationUniqueId, String sopInstanceUId) {

        return  Paths.get(storageDir,aeTitle,assoicationUniqueId,sopInstanceUId+".dcm");


    }
    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse,Attributes rq, PDVInputStream data) throws IOException {
        if ( dimse == Dimse.C_ECHO_RQ) {
            as.tryWriteDimseRSP(pc,Commands.mkEchoRSP(rq,Status.Success));
            return;
        }

        if (dimse != Dimse.C_STORE_RQ)
            throw new DicomServiceException(Status.UnrecognizedOperation);

        Attributes rsp = Commands.mkCStoreRSP(rq, Status.Success);
        storeDicomData(as, pc, rq, data, rsp);
        as.tryWriteDimseRSP(pc, rsp);
        return;

    }
    @Override
    public void onClose(Association as) {
        LOG.info("["+hostName+"] : Closing the assoication  "+as.getRemoteAET());
        super.onClose(as);
    }

    private void storeDicomData(Association as, PresentationContext pc, Attributes rq, PDVInputStream data, Attributes rsp) throws IOException {
        LOG.info("["+hostName+"] : Store Request Received ...AssociationGuid-"+as.getProperty("AssociationGuid")) ;

        rsp.setInt(Tag.Status, VR.US, status);
        if (storageDir == null)
            return;


        String cuid = rq.getString(Tag.AffectedSOPClassUID);
        String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
        String tsuid = pc.getTransferSyntax();
        String assoicationUniqueId = as.getProperty("AssociationGuid").toString();

        Path filePath = getStorageFilePath(as.getRemoteAET(),assoicationUniqueId,iuid);
        try {
            // create directory if not exist
            Files.createDirectories(filePath.getParent());
            Path file = Files.createFile(filePath);
            try(DicomOutputStream out = new DicomOutputStream(file.toFile()))
            {
                Attributes fileMetaInformation = as.createFileMetaInformation(iuid, cuid, tsuid);
                out.writeFileMetaInformation(fileMetaInformation);
                data.copyTo(out);
            }

        }catch (Exception exp) {
            LOG.error("["+hostName+"] : Store DICOM data failed ...", exp);
            throw new DicomServiceException(Status.ProcessingFailure, exp);
        }

    }

    @Override
    public void onAssociationEstablished(Association as) {
    }

    @Override
    public void onAssociationFailed(Association as, Throwable e) {

    }

    @Override
    public void onAssociationRejected(Association as, AAssociateRJ aarj) {

    }

    @Override
    public void onAssociationAccepted(Association as) {
        // store unique assoication id
        as.setProperty("AssociationGuid",UUID.randomUUID());
        LOG.info("["+hostName+"] : Association Accepted.. "+as.getConnection().getHostname()+" of AE Title "+as.getRemoteAET());
    }
}
