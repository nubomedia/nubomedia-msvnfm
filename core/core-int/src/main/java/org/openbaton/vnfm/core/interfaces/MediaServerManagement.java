package org.openbaton.vnfm.core.interfaces;

import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.MediaServer;

import java.util.Set;


/**
 * Created by mpa on 01.10.15.
 */
public interface MediaServerManagement {
    MediaServer add(MediaServer mediaServer) throws Exception;

    void delete(String mediaServerId) throws NotFoundException;

    void deleteByVnfrId(String vnfrId) throws NotFoundException;

    Iterable<MediaServer> query();

    MediaServer query(String id);

    Set<MediaServer> queryByVnrfId(String vnfr_id);

    MediaServer queryBestMediaServerByVnfrId(String vnfr_id) throws NotFoundException;

    MediaServer update(MediaServer mediaServer, String id);
}
