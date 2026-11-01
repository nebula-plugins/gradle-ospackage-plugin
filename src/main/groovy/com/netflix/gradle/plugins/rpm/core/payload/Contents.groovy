/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.payload

import com.netflix.gradle.plugins.rpm.core.ChannelWrapper.Key
import com.netflix.gradle.plugins.rpm.core.ReadableChannelWrapper
import com.netflix.gradle.plugins.rpm.core.Util

import groovy.transform.CompileStatic

import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.security.NoSuchAlgorithmException
import java.util.logging.Level
import java.util.logging.Logger

import static com.netflix.gradle.plugins.rpm.core.Util.normalizePath
import static com.netflix.gradle.plugins.rpm.core.payload.CpioHeader.DEFAULT_DIRECTORY_PERMISSION
import static com.netflix.gradle.plugins.rpm.core.payload.CpioHeader.DEFAULT_FILE_PERMISSION
import static com.netflix.gradle.plugins.rpm.core.payload.CpioHeader.DEFAULT_GROUP
import static com.netflix.gradle.plugins.rpm.core.payload.CpioHeader.DEFAULT_USERNAME
import static com.netflix.gradle.plugins.rpm.core.payload.CpioHeader.DIR
import static com.netflix.gradle.plugins.rpm.core.payload.CpioHeader.FILE
import static com.netflix.gradle.plugins.rpm.core.payload.CpioHeader.SYMLINK

/**
 * The contents of an RPM archive. These entries define the files and links that the RPM
 * archive contains as well as headers those files require.
 *
 * Note that the RPM format requires that files in the archive be naturally ordered.
 */
@CompileStatic
class Contents {

    private static final Set<String> BUILTIN = new HashSet<String>([
            '/',
            '/bin',
            '/dev',
            '/etc',
            '/etc/bash_completion.d',
            '/etc/cron.d',
            '/etc/cron.daily',
            '/etc/cron.hourly',
            '/etc/cron.monthly',
            '/etc/cron.weekly',
            '/etc/default',
            '/etc/init.d',
            '/etc/logrotate.d',
            '/lib',
            '/usr',
            '/usr/bin',
            '/usr/lib',
            '/usr/lib64',
            '/usr/local',
            '/usr/local/bin',
            '/usr/local/lib',
            '/usr/sbin',
            '/usr/share',
            '/usr/share/applications',
            '/root',
            '/sbin',
            '/opt',
            '/run',
            '/srv',
            '/tmp',
            '/var',
            '/var/cache',
            '/var/lib',
            '/var/log',
            '/var/run',
            '/var/spool',
    ])

    private final Logger logger = Logger.getLogger(Contents.name)
    private int inode = 1

    protected final Set<CpioHeader> headers = new TreeSet<CpioHeader>(new HeaderComparator())
    protected final Set<String> files = new HashSet<String>()
    protected final Map<CpioHeader, Object> sources = new HashMap<CpioHeader, Object>()
    protected final Set<String> builtins = new HashSet<String>()

    Contents() {
        builtins.addAll(BUILTIN)
    }

    /**
     * Adds a directory entry to the archive with the default permissions of 644.
     *
     * @param path the destination path for the installed file.
     * @param target the target string
     */
    synchronized void addLink(final String path, final String target) {
        addLink(path, target, -1)
    }

    /**
     * Adds a directory entry to the archive with the specified permissions.
     *
     * @param path the destination path for the installed file.
     * @param target the target string
     * @param permissions the permissions flags.
     */
    synchronized void addLink(String path, final String target, int permissions) {
        addLink(path, target, permissions, null, null)
    }

    /**
     * Adds a directory entry to the archive with the specified permissions.
     *
     * @param path the destination path for the installed file.
     * @param target the target string
     * @param permissions the permissions flags.
     * @param uname user owner for the given file
     * @param gname group owner for the given file
     */
    synchronized void addLink(String path, final String target, int permissions, final String uname, final String gname) {
        if (files.contains(path)) return
        files.add(path)
        logger.log(Level.FINE, "Adding link ''{0}''.", path)
        CpioHeader header = new CpioHeader(path)
        header.setType(SYMLINK)
        header.setFileSize(target.length())
        header.setMtime(System.currentTimeMillis())
        header.setUname(getDefaultIfMissing(uname, DEFAULT_USERNAME))
        header.setGname(getDefaultIfMissing(gname, DEFAULT_GROUP))
        if (permissions != -1) header.setPermissions(permissions)
        headers.add(header)
        sources.put(header, target)
    }

    private static String getDefaultIfMissing(String value, String defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : value
    }

    /**
     * Adds a directory entry to the archive with the default permissions of 644.
     *
     * @param path the destination path for the installed file.
     */
    synchronized void addDirectory(final String path) {
        addDirectory(path, -1)
    }

    /**
     * Adds a directory entry to the archive with the default permissions of 644.
     *
     * @param path the destination path for the installed file.
     * @param directive directive indicating special handling for this directory.
     */
    synchronized void addDirectory(final String path, final Directive directive) {
        addDirectory(path, -1, directive, null, null)
    }

    /**
     * Adds a directory entry to the archive with the specified permissions.
     *
     * @param path the destination path for the installed file.
     * @param permissions the permissions flags.
     */
    synchronized void addDirectory(final String path, final int permissions) {
        addDirectory(path, permissions, null, null, null)
    }

    /**
     * Adds a directory entry to the archive with the specified permissions.
     *
     * @param path the destination path for the installed file.
     * @param permissions the permissions flags.
     * @param directive directive indicating special handling for this file.
     * @param uname user owner of the directory
     * @param gname group owner of the directory
     */
    synchronized void addDirectory(final String path, final int permissions, final Directive directive, final String uname, final String gname) {
        addDirectory(path, permissions, directive, uname, gname, true)
    }

    /**
     * Adds a directory entry to the archive with the specified permissions.
     *
     * @param path the destination path for the installed file.
     * @param permissions the permissions flags.
     * @param directive directive indicating special handling for this file.
     * @param uname user owner of the directory
     * @param gname group owner of the directory
     * @param addParents whether to add parent directories to the rpm
     */
    synchronized void addDirectory(final String path, final int permissions, final Directive directive, final String uname, final String gname, boolean addParents) {
        if (files.contains(path)) return

        if (addParents) this.addParents(new File(path), permissions, uname, gname)
        files.add(path)
        logger.log(Level.FINE, "Adding directory ''{0}''.", path)
        CpioHeader header = new CpioHeader(path)
        header.setType(DIR)
        header.setInode(inode++)
        header.setUname(getDefaultIfMissing(uname, DEFAULT_USERNAME))
        header.setGname(getDefaultIfMissing(gname, DEFAULT_GROUP))
        header.setMtime(System.currentTimeMillis())
        header.setPermissions(-1 == permissions ? DEFAULT_DIRECTORY_PERMISSION : permissions)
        headers.add(header)
        sources.put(header, '')
        if (directive != null) header.setFlags(directive.flag())
    }

    /**
     * Adds a file entry to the archive with the default permissions of 644.
     *
     * @param path the destination path for the installed file.
     * @param source the local file to be included in the package.
     * @throws FileNotFoundException file wasn't found
     */
    void addFile(final String path, final File source) throws FileNotFoundException {
        addFile(path, source, -1)
    }

    /**
     * Adds a file entry to the archive with the specified permissions.
     *
     * @param path the destination path for the installed file.
     * @param source the local file to be included in the package.
     * @param permissions the permissions flags.
     * @throws FileNotFoundException file wasn't found
     */
    void addFile(final String path, final File source, int permissions) throws FileNotFoundException {
        addFile(path, source, permissions, null, null, null)
    }

    /**
     * Adds a file entry to the archive with the specified permissions.
     *
     * @param path the destination path for the installed file.
     * @param source the local file to be included in the package.
     * @param permissions the permissions flags.
     * @param dirmode permission flags for parent directories, use -1 to leave as default.
     * @throws FileNotFoundException file wasn't found
     */
    void addFile(final String path, final File source, int permissions, int dirmode) throws FileNotFoundException {
        addFile(path, source, permissions, null, null, null, dirmode)
    }

    /**
     * Adds a file entry to the archive with the specified permissions.
     *
     * @param path the destination path for the installed file.
     * @param source the local file to be included in the package.
     * @param permissions the permissions flags.
     * @param directive directive indicating special handling for this file.
     * @throws FileNotFoundException file wasn't found
     */
    void addFile(final String path, final File source, int permissions, final Directive directive) throws FileNotFoundException {
        addFile(path, source, permissions, directive, null, null)
    }

    /**
     * Adds a file entry to the archive with the specified permissions.
     *
     * @param path the destination path for the installed file.
     * @param source the local file to be included in the package.
     * @param permissions the permissions flags, use -1 to leave as default.
     * @param directive directive indicating special handling for this file, use null to ignore.
     * @param uname user owner for the given file, use null for default user.
     * @param gname group owner for the given file, use null for default group.
     * @throws FileNotFoundException file wasn't found
     */
    void addFile(final String path, final File source, final int permissions, final Directive directive, final String uname, final String gname) throws FileNotFoundException {
        addFile(path, source, permissions, directive, uname, gname, -1)
    }

    /**
     * Adds a file entry to the archive with the specified permissions.
     *
     * @param path the destination path for the installed file.
     * @param source the local file to be included in the package.
     * @param permissions the permissions flags, use -1 to leave as default.
     * @param directive directive indicating special handling for this file, use null to ignore.
     * @param uname user owner for the given file, use null for default user.
     * @param gname group owner for the given file, use null for default group.
     * @param dirmode permission flags for parent directories, use -1 to leave as default.
     * @throws FileNotFoundException file wasn't found
     */
    synchronized void addFile(final String path, final File source, final int permissions, final Directive directive, final String uname, final String gname, final int dirmode) throws FileNotFoundException {
        addFile(path, source, permissions, directive, uname, gname, dirmode, true)
    }

    /**
     * Adds a file entry to the archive with the specified permissions.
     *
     * @param path the destination path for the installed file.
     * @param source the local file to be included in the package.
     * @param permissions the permissions flags, use -1 to leave as default.
     * @param directive directive indicating special handling for this file, use null to ignore.
     * @param uname user owner for the given file, use null for default user.
     * @param gname group owner for the given file, use null for default group.
     * @param dirmode permission flags for parent directories, use -1 to leave as default.
     * @param addParents whether to create parent directories for the file, defaults to true for other methods.
     * @throws FileNotFoundException file wasn't found
     */
    synchronized void addFile(final String path, final File source, final int permissions, final Directive directive, final String uname, final String gname, final int dirmode, final boolean addParents) throws FileNotFoundException {
        addFile(path, source, permissions, directive, uname, gname, dirmode, addParents, -1)
    }

    /**
     * Adds a file entry to the archive with the specified permissions.
     *
     * @param path the destination path for the installed file.
     * @param source the local file to be included in the package.
     * @param permissions the permissions flags, use -1 to leave as default.
     * @param directive directive indicating special handling for this file, use null to ignore.
     * @param uname user owner for the given file, use null for default user.
     * @param gname group owner for the given file, use null for default group.
     * @param dirmode permission flags for parent directories, use -1 to leave as default.
     * @param addParents whether to create parent directories for the file, defaults to true for other methods.
     * @param verifyFlags verify flags
     * @throws FileNotFoundException file wasn't found
     */
    synchronized void addFile(final String path, final File source, final int permissions, final Directive directive, final String uname, final String gname, final int dirmode, final boolean addParents, final int verifyFlags) throws FileNotFoundException {
        if (files.contains(path)) return

        if (addParents) this.addParents(new File(path), dirmode, uname, gname)
        files.add(path)
        logger.log(Level.FINE, "Adding file ''{0}''.", path)
        CpioHeader header
        if (directive != null && ((directive.flag() & Directive.RPMFILE_GHOST) == Directive.RPMFILE_GHOST)) {
            header = new CpioHeader(path)
        } else {
            header = new CpioHeader(path, source)
        }
        header.setType(FILE)
        header.setInode(inode++)
        header.setUname(getDefaultIfMissing(uname, DEFAULT_USERNAME))
        header.setGname(getDefaultIfMissing(gname, DEFAULT_GROUP))
        header.setPermissions(-1 == permissions ? DEFAULT_FILE_PERMISSION : permissions)
        header.setVerifyFlags(verifyFlags)
        headers.add(header)
        sources.put(header, source)
        if (directive != null) header.setFlags(directive.flag())
    }

    /**
     * Adds a URL entry to the archive with the specified permissions.
     *
     * @param path the destination path for the installed file.
     * @param source the URL with the data to be added
     * @param permissions the permissions flags.
     * @param directive directive indicating special handling for this file.
     * @param uname user owner for the given file
     * @param gname group owner for the given file
     * @param dirmode permission flags for parent directories, use -1 to leave as default.
     * @throws FileNotFoundException file wasn't found
     */
    synchronized void addURL(final String path, final URL source, final int permissions, final Directive directive, final String uname, final String gname, final int dirmode) throws FileNotFoundException {
        if (files.contains(path)) return

        addParents(new File(path), dirmode, uname, gname)
        files.add(path)
        logger.log(Level.FINE, "Adding file ''{0}''.", path)
        CpioHeader header = new CpioHeader(path, source)
        header.setType(FILE)
        header.setInode(inode++)
        if (uname != null) header.setUname(uname)
        if (gname != null) header.setGname(gname)
        if (permissions != -1) header.setPermissions(permissions)
        headers.add(header)
        sources.put(header, source)
        if (directive != null) header.setFlags(directive.flag())
    }

    /**
     * Adds entries for parent directories of this file, so that they may be cleaned up when
     * removing the package.
     *
     * @param file the file to add parent directories of
     * @param permissions the permissions flags
     * @param uname user owner for the given file
     * @param gname group owner for the given file
     */
    protected synchronized void addParents(final File file, final int permissions, final String uname, final String gname) {
        final List<String> parents = new ArrayList<String>()
        listParents(parents, file)
        for (String parent : parents) addDirectory(parent, permissions, null, uname, gname)
    }

    /**
     * Add additional directory that is assumed to already exist on system where the RPM will be
     * installed (e.g. /etc) and should not have an entry in the RPM.
     *
     * The builtin will be added to all instances of Contents created afterwards; prefer
     * {@link #addLocalBuiltinDirectory}.
     *
     * @param directory the directory to add
     */
    static synchronized void addBuiltinDirectory(final String directory) {
        BUILTIN.add(directory)
    }

    /**
     * Add additional directory that is assumed to already exist on system where the RPM will be
     * installed (e.g. /etc) and should not have an entry in the RPM.
     *
     * The builtin will only be added to this instance of Contents.
     *
     * @param directory the directory to add
     */
    synchronized void addLocalBuiltinDirectory(final String directory) {
        builtins.add(directory)
    }

    /**
     * Retrieve the size of this archive in number of files. This count includes both directory
     * entries and soft links.
     * @return the number of files in this archive
     */
    int size() { return headers.size() }

    /**
     * Retrieve the archive headers. The returned {@link Iterable} will iterate in the correct
     * order for the final archive.
     * @return the headers
     */
    Iterable<CpioHeader> headers() { return headers }

    /**
     * Retrieves the content for this archive entry, which may be a {@link File} if the entry is a
     * regular file or a {@link CharSequence} containing the name of the target path if the entry
     * is a link. This is the value to be written to the archive as the body of the entry.
     * @param header the header to get the content from
     * @return the content
     */
    Object getSource(CpioHeader header) { return sources.get(header) }

    /**
     * Accumulated size of all files included in the archive.
     * @return the size of all files included in the archive
     */
    int getTotalSize() {
        int total = 0
        try {
            for (Object object : sources.values()) {
                // int arithmetic with an explicit narrowing cast, as Java's compound assignment did
                if (object instanceof File) total = (int) (total + ((File) object).length())
                else if (object instanceof URL) total += ((URL) object).openConnection().getContentLength()
            }
        } catch (IOException e) {
            throw new RuntimeException(e)
        }
        return total
    }

    /**
     * Array of all parent directories in this archive.
     * @return the directory names
     */
    String[] getDirNames() {
        final Set<String> set = new LinkedHashSet<String>()
        for (CpioHeader header : headers) {
            String parent = parentDirOf(header)
            if (parent != null) set.add(parent)
        }
        return set.toArray(new String[set.size()])
    }

    /**
     * Caches the index of every directory in {@link #getDirNames} and returns an array
     * of indexes, one per entry, pointing at its parent directory.
     * @return the directory indexes
     */
    int[] getDirIndexes() {
        final List<String> dirs = Arrays.asList(getDirNames())
        int[] array = new int[headers.size()]
        int x = 0
        for (CpioHeader header : headers) {
            String parent = parentDirOf(header)
            if (parent == null) continue
            array[x++] = dirs.indexOf(parent)
        }
        return array
    }

    private static String parentDirOf(CpioHeader header) {
        String path = new File(header.getName()).getParent()
        if (path == null) return null
        String parent = normalizePath(path)
        return parent.endsWith('/') ? parent : parent + '/'
    }

    /**
     * Returns the file name (without the parent directory) of every entry.
     * @return the base names
     */
    String[] getBaseNames() {
        String[] array = new String[headers.size()]
        int x = 0
        for (CpioHeader header : headers) array[x++] = normalizePath(new File(header.getName()).getName())
        return array
    }

    /**
     * Returns the file sizes of every entry.
     * @return the sizes
     */
    int[] getSizes() {
        int[] array = new int[headers.size()]
        int x = 0
        try {
            for (CpioHeader header : headers) {
                Object object = sources.get(header)
                if (object instanceof File) array[x] = (int) ((File) object).length()
                else if (object instanceof URL) array[x] = ((URL) object).openConnection().getContentLength()
                else if (header.getType() == DIR) array[x] = 4096
                else if (header.getType() == SYMLINK) array[x] = ((String) object).length()
                ++x
            }
        } catch (IOException e) {
            throw new RuntimeException(e)
        }
        return array
    }

    /**
     * Returns the mode (type and permissions) of every entry.
     * @return the modes
     */
    short[] getModes() {
        short[] array = new short[headers.size()]
        int x = 0
        for (CpioHeader header : headers) array[x++] = (short) header.getMode()
        return array
    }

    /**
     * Returns the device number of every entry.
     * @return the rdevs
     */
    short[] getRdevs() {
        short[] array = new short[headers.size()]
        int x = 0
        for (CpioHeader header : headers) array[x++] = (short) ((header.getRdevMajor() << 8) + header.getRdevMinor())
        return array
    }

    /**
     * Returns the modification time of every entry, in seconds since the epoch.
     * @return the mtimes
     */
    int[] getMtimes() {
        int[] array = new int[headers.size()]
        int x = 0
        for (CpioHeader header : headers) array[x++] = header.getMtime()
        return array
    }

    /**
     * Calculates the SHA-256 digest of every file entry. Entries that are not files (or URLs)
     * get an empty string.
     * @return the digests, as hex strings
     * @throws NoSuchAlgorithmException the algorithm isn't supported
     * @throws IOException there was an IO error
     */
    String[] getFileChecksums() throws NoSuchAlgorithmException, IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(4096)
        String[] array = new String[headers.size()]
        int x = 0
        for (CpioHeader header : headers) {
            Object object = sources.get(header)
            String value = ''
            if (object instanceof File) {
                FileInputStream fileInput = new FileInputStream((File) object)
                final ReadableChannelWrapper input = new ReadableChannelWrapper(fileInput.getChannel())
                final Key<byte[]> key = input.start('SHA-256')
                while (input.read(buffer) != -1) buffer.rewind()
                value = Util.hex(input.finish(key))
                input.close()
                fileInput.close()
            } else if (object instanceof URL) {
                final ReadableChannelWrapper input = new ReadableChannelWrapper(Channels.newChannel(((URL) object).openConnection().getInputStream()))
                final Key<byte[]> key = input.start('SHA-256')
                while (input.read(buffer) != -1) buffer.rewind()
                value = Util.hex(input.finish(key))
                input.close()
            }
            array[x++] = value
        }
        return array
    }

    /**
     * Returns the link target of every entry, or an empty string for entries that are not links.
     * @return the link targets
     */
    String[] getLinkTos() {
        String[] array = new String[headers.size()]
        int x = 0
        for (CpioHeader header : headers) {
            Object object = sources.get(header)
            String value = ''
            if (object instanceof String) value = String.valueOf(object)
            array[x++] = value
        }
        return array
    }

    /**
     * Returns the directive flags of every entry.
     * @return the flags
     */
    int[] getFlags() {
        int[] array = new int[headers.size()]
        int x = 0
        for (CpioHeader header : headers) array[x++] = header.getFlags()
        return array
    }

    /**
     * Returns the owning user name of every entry.
     * @return the user names
     */
    String[] getUsers() {
        String[] array = new String[headers.size()]
        int x = 0
        for (CpioHeader header : headers) {
            array[x++] = header.getUname() == null ? 'root' : header.getUname()
        }
        return array
    }

    /**
     * Returns the owning group name of every entry.
     * @return the group names
     */
    String[] getGroups() {
        String[] array = new String[headers.size()]
        int x = 0
        for (CpioHeader header : headers) {
            array[x++] = header.getGname() == null ? 'root' : header.getGname()
        }
        return array
    }

    /**
     * @return an array of zeros, one per entry
     */
    int[] getColors() {
        return new int[headers.size()]
    }

    /**
     * Returns the verify flags of every entry.
     * @return the verify flags
     */
    int[] getVerifyFlags() {
        int[] array = new int[headers.size()]
        int x = 0
        for (CpioHeader header : headers) {
            array[x++] = header.getVerifyFlags()
        }
        return array
    }

    /**
     * @return an array of ones, one per entry
     */
    int[] getClasses() {
        int[] array = new int[headers.size()]
        Arrays.fill(array, 1)
        return array
    }

    /**
     * Returns the device of every entry.
     * @return the devices
     */
    int[] getDevices() {
        int[] array = new int[headers.size()]
        int x = 0
        for (CpioHeader header : headers) array[x++] = (header.getDevMajor() << 8) + header.getDevMinor()
        return array
    }

    /**
     * Returns the inode of every entry.
     * @return the inodes
     */
    int[] getInodes() {
        int[] array = new int[headers.size()]
        int x = 0
        for (CpioHeader header : headers) array[x++] = header.getInode()
        return array
    }

    /**
     * @return an array of empty strings, one per entry
     */
    String[] getLangs() {
        String[] array = new String[headers.size()]
        Arrays.fill(array, '')
        return array
    }

    /**
     * @return an array of zeros, one per entry
     */
    int[] getDependsX() {
        return new int[headers.size()]
    }

    /**
     * @return an array of zeros, one per entry
     */
    int[] getDependsN() {
        return new int[headers.size()]
    }

    /**
     * @return an array of {@code <<none>>} security contexts, one per entry
     */
    String[] getContexts() {
        String[] array = new String[headers.size()]
        Arrays.fill(array, '<<none>>')
        return array
    }

    /**
     * Builds a list of parent directories, excluding the builtin ones that are assumed to exist.
     * @param parents the list of parents to add to
     * @param file the file whose parents should be listed
     */
    protected void listParents(final List<String> parents, final File file) {
        final File parent = file.getParentFile()
        if (parent == null) return

        final String path = normalizePath(parent.getPath())
        if (builtins.contains(path)) return

        parents.add(path)
        listParents(parents, parent)
    }

    /**
     * Comparator that orders files in the CPIO archive by their file name as present in the header.
     */
    private static class HeaderComparator implements Comparator<CpioHeader> {
        int compare(final CpioHeader one, final CpioHeader two) {
            return one.getName().compareTo(two.getName())
        }
    }
}
