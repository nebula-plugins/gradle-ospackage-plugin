package com.trigonic.gradle.plugins.deb

import com.google.common.base.Preconditions
import com.google.common.io.Files
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.ar.ArArchiveEntry
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

public class Scanner {
    static final String CONTROL_GZ_FILE = 'control.tar.gz'
    static final String CONTROL_FILE = './control'
    static final String DATA_FILE = 'data.tar.gz'

    File debFile
    Map<String, String> controlContents
    Map<TarArchiveEntry, File> dataContents
    Map<String, String> headerFields

    /**
     *
     * @param debFile
     * @param tmpOutput If not null, files will get extracted
     */
    Scanner(File debFile, File tmpOutput = null) {
        this.debFile
        unpack(debFile, tmpOutput)
        headerFields = parseControl(getControl())
    }

    public TarArchiveEntry getEntry(String filename) {
        def entry = dataContents.keySet().find { TarArchiveEntry entry ->
            println "Comparing ${entry.name} and $filename"
            entry.name == filename
        }
        if(entry==null) {
            throw new FileNotFoundException(filename);
        }
        return entry
    }

    public String getControl(String path = CONTROL_FILE) {
        return controlContents[path]
    }

    public String getHeaderEntry(String key) {
        return headerFields[key]
    }

    public File getEntryFile(String path) {
        TarArchiveEntry entry = getEntry(path)
        return dataContents[entry]
    }
    /**
     * From http://stackoverflow.com/questions/7432223/open-debian-package-with-java
     */
    /**
     * Unpack a deb archive provided as an input file, to an output directory.
     * <p>
     *
     * @param deb      the input deb file.
     * @param outputDir     the output directory.
     * @throws IOException
     * @throws org.apache.commons.compress.archivers.ArchiveException
     *
     * @returns A {@link List} of all the unpacked files.
     *
     */
    private String unpack(final File deb, final File outputDir) {

        println "Unzipping deb file ${deb.getAbsoluteFile()}"

        final InputStream is = new FileInputStream(deb);
        final ArArchiveInputStream debInputStream = (ArArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("ar", is);
        ArArchiveEntry entry = null;

        while ((entry = (ArArchiveEntry)debInputStream.getNextEntry()) != null) {
            println "Read entry ${entry}"

            if(entry.name == CONTROL_GZ_FILE) {
                controlContents = extractContents(debInputStream)
            } else if(entry.name == DATA_FILE) {
                dataContents = extractFiles(debInputStream, outputDir)
            }
        }
        debInputStream.close();

        Preconditions.checkState(controlContents != null)
        Preconditions.checkState(dataContents != null)
    }

    public static Map<String, String> extractContents(InputStream is) {
        GzipCompressorInputStream gzIn = new GzipCompressorInputStream(is);

        TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn);

        Map<String,String> contents = [:]

        TarArchiveEntry entry
        while ((entry = tarIn.getNextTarEntry()) != null) {
            println "Tar Entry: ${entry.name}"
            StringWriter writer = new StringWriter();
            IOUtils.copy(tarIn, writer);
            contents.put(entry.name, writer.toString())
        }

        return contents
    }

    public static Map<TarArchiveEntry, File> extractFiles(InputStream is, File outputDir) {
        GzipCompressorInputStream gzIn = new GzipCompressorInputStream(is);

        TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn);

        Map<TarArchiveEntry, File> contents = [:]

        TarArchiveEntry entry
        while ((entry = tarIn.getNextTarEntry()) != null) {
            println "Data Tar Entry: ${entry.name}"

            File outputFile = null
            if(outputDir != null) {
                outputFile = new File(outputDir, entry.getName()).getCanonicalFile()
                Files.createParentDirs(outputFile);

                if(entry.isFile()) {
                    final OutputStream outputFileStream = new FileOutputStream(outputFile);
                    IOUtils.copy(tarIn, outputFileStream);
                    outputFileStream.close();
                } else if(entry.isDirectory()) {
                    outputFile.mkdir();
                } else {
                    throw new RuntimeException("Unknown type of tar entry " + entry.get)
                    // outputFile might not exist, e.g. a symlink. The TarArchiveEntry can be used to learn more about the file.
                }
            }
            contents[entry] = outputFile
        }

        return contents
    }

    /**
     Package: bleah
     Source: bleah
     Version: 1370980162032:1.0-1
     Section: null
     Priority: optional
     Architecture: all
     Depends: blech
     Provides: bleah
     Installed-Size: 0
     Maintainer: jryan
     Description: Bleah blarg
     Not a very interesting library.
     Homepage: http://www.example.com/
     * @param contents
     * @return
     */
    Map<String, String> parseControl(String contents) {
        Map<String, String> fields = [:]
        String lastKey = null
        contents.eachLine { String line ->
            def m = line =~ /\s*([\w-]+)\s*:(.*)/
            String key
            String value
            if(m) {
                key = m[0][1]
                value = m[0][2].toString().trim() // TODO we might be trimming to much off the end
            } else if(lastKey!=null) {
                key = lastKey
                value = fields[key] + "\n" + line
            } else {
                throw new RuntimeException("Unmatchable line in header")
            }
            fields[key] = value
            lastKey = key
        }
        return fields
    }
}
