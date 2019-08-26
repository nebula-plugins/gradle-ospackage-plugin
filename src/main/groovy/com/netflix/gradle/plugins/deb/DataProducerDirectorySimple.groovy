package com.netflix.gradle.plugins.deb

import groovy.transform.Canonical
import org.vafer.jdeb.DataConsumer
import org.vafer.jdeb.DataProducer
import org.vafer.jdeb.shaded.commons.compress.archivers.tar.TarArchiveEntry

@Canonical
public class DataProducerDirectorySimple implements DataProducer {

    String dirname
    String user
    int uid = 0
    String group
    int gid = 0
    int mode

    @Override
    void produce(DataConsumer receiver) throws IOException {
        // TODO Investigate what happens if we don't have a uid/gid
        receiver.onEachDir(createEntry())
    }

    private TarArchiveEntry createEntry() {
        TarArchiveEntry entry = new TarArchiveEntry(dirname)
        entry.userName = user
        entry.userId = uid
        entry.groupName = group
        entry.groupId = gid
        entry.mode = mode
        entry
    }
}
