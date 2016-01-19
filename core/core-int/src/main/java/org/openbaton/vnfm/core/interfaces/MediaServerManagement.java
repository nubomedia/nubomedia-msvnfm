package org.openbaton.vnfm.core.interfaces;

import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.MediaServer;

import java.util.Set;


/**
 * Created by mpa on 01.10.15.
 */
public interface MediaServerManagement {
    MediaServer add(MediaServer mediaServer) throws Exception;

    MediaServer add(String vnfrId, VNFCInstance vnfcInstance) throws Exception;

    void delete(String mediaServerId) throws NotFoundException;

    void delete(String vnfrId, String hostname) throws NotFoundException;

    void deleteByVnfrId(String vnfrId) throws NotFoundException;

    Iterable<MediaServer> query();

    MediaServer query(String id);

    Set<MediaServer> queryByVnrfId(String vnfr_id);

    MediaServer queryByHostName(String hostName);

    MediaServer queryBestMediaServerByVnfrId(String vnfr_id) throws NotFoundException;

    MediaServer update(MediaServer mediaServer, String id);
}
