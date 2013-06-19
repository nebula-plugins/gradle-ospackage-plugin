package com.trigonic.gradle.plugins.deb

import groovy.transform.Canonical
import org.vafer.jdeb.DataConsumer
import org.vafer.jdeb.DataProducer

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
            receiver.onEachFile( is, filename, null, user, uid, group, gid, mode, file.size() )
        }
    }
}
