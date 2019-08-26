package com.netflix.gradle.plugins.deb

import groovy.transform.Canonical
import org.vafer.jdeb.DataConsumer
import org.vafer.jdeb.DataProducer
import org.vafer.jdeb.shaded.commons.compress.archivers.tar.TarArchiveEntry

@Canonical
public class DataProducerFileSimple implements DataProducer {

    String filename
    File file
    String user
    int uid
    String group
    int gid
    int mode

    @Override
    void produce(DataConsumer receiver) throws IOException {
        file.withInputStream { InputStream is ->
            receiver.onEachFile(is, createEntry())
        }
    }

    private TarArchiveEntry createEntry() {
        TarArchiveEntry entry = new TarArchiveEntry(filename)
        entry.userName = user
        entry.userId = uid
        entry.groupName = group
        entry.groupId = gid
        entry.mode = mode
        entry.size = file.size()
        entry
    }
}
