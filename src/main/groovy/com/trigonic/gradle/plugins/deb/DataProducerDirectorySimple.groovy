package com.trigonic.gradle.plugins.deb

import groovy.transform.Canonical
import org.vafer.jdeb.DataConsumer
import org.vafer.jdeb.DataProducer

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
        receiver.onEachDir(dirname, null, user, uid, group, gid, mode, 0)
    }
}
