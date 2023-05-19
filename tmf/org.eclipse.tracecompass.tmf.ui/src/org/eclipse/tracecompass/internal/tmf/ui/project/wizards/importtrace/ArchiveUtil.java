/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Marc-Andre Laperle - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.project.wizards.importtrace;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceCoreUtils;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;

import com.google.common.annotations.VisibleForTesting;

/**
 * Various utilities for dealing with archives in the context of importing
 * traces.
 */
public class ArchiveUtil {

    /**
     * Threshold for size was selected according to SonarCloud suggestion,
     * entries were revised to be smaller than SonarCould suggestion, and
     * compression ratio was revised to be larger using the template of our own
     * testing files:
     * https://sonarcloud.io/organizations/eclipse/rules?open=java%3AS5042&rule_key=java%3AS5042&tab=how_to_fix
     *
     */
    private static final int THRESHOLD_ENTRIES = 5000;
    static final int THRESHOLD_SIZE = 1000000000;
    private static final double THRESHOLD_RATIO = 100;

    private ArchiveUtil() {
        // Do nothing, not meant to be instantiated
    }

    /**
     * Returns whether or not the source file is an archive file (Zip, tar,
     * tar.gz, gz).
     *
     * @param sourceFile
     *            the source file
     * @return whether or not the source file is an archive file
     */
    public static boolean isArchiveFile(File sourceFile) {
        String absolutePath = sourceFile.getAbsolutePath();
        return isTarFile(absolutePath) || isZipFile(absolutePath) || isGzipFile(absolutePath);
    }

    private static boolean isZipFile(String fileName) {
        try (ZipFile specifiedZipSourceFile = getSpecifiedZipSourceFile(fileName)) {
            if (specifiedZipSourceFile != null) {
                return true;
            }
        } catch (IOException e) {
            // ignore
        }
        return false;
    }

    private static boolean isTarFile(String fileName) {
        TarFile specifiedTarSourceFile = getSpecifiedTarSourceFile(fileName);
        if (specifiedTarSourceFile != null) {
            try {
                specifiedTarSourceFile.close();
                return true;
            } catch (IOException e) {
                // ignore
            }
        }
        return false;
    }

    private static boolean isGzipFile(String fileName) {
        if (!fileName.isEmpty()) {
            try (GzipFile specifiedTarSourceFile = new GzipFile(fileName);) {
                return true;
            } catch (IOException e) {
            }
        }
        return false;
    }

    private static ZipFile getSpecifiedZipSourceFile(String fileName) {
        if (fileName.length() == 0) {
            return null;
        }

        File file = new File(fileName);
        if (file.isDirectory()) {
            return null;
        }

        try {
            return new ZipFile(file);
        } catch (IOException e) {
            // ignore
        }

        return null;
    }

    private static TarFile getSpecifiedTarSourceFile(String fileName) {
        if (fileName.length() == 0) {
            return null;
        }

        // FIXME: Work around Bug 463633. Remove this block once we move to
        // Eclipse 4.5.
        File tarCandidate = new File(fileName);
        if (tarCandidate.length() < 512) {
            return null;
        }

        try {
            return new TarFile(tarCandidate);
        } catch (IOException e) {
            // ignore
        }

        return null;
    }

    static boolean ensureZipSourceIsValid(String archivePath) {
        ZipFile specifiedFile = getSpecifiedZipSourceFile(archivePath);
        if (specifiedFile == null) {
            return false;
        }
        return closeZipFile(specifiedFile);
    }

    static boolean closeZipFile(ZipFile file) {
        try {
            file.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    static boolean ensureTarSourceIsValid(String archivePath) {
        TarFile specifiedFile = getSpecifiedTarSourceFile(archivePath);
        if (specifiedFile == null) {
            return false;
        }
        return closeTarFile(specifiedFile);
    }

    static boolean ensureGzipSourceIsValid(String archivePath) {
        return isGzipFile(archivePath);
    }

    static boolean closeTarFile(TarFile file) {
        try {
            file.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Get the archive size and make sure that each of the entries are valid
     *
     * Since an archive entry could also be an archive: a variable counting the
     * number of file entries was added to the method as per SonarCloud
     * suggestion:
     * https://sonarcloud.io/organizations/eclipse/rules?open=java%3AS5042&rule_key=java%3AS5042&tab=how_to_fix
     *
     * @param archivePath
     *            Path of the archive
     * @return Size of the archive in byte or -1 if there is an error
     */
    public static long getArchiveSize(String archivePath) {
        TarFile tarFile = getSpecifiedTarSourceFile(archivePath);
        long archiveSize = 0;
        if (tarFile != null) {
            ArrayList<TarArchiveEntry> entries = Collections.list(tarFile.entries());
            for (TarArchiveEntry tarArchiveEntry : entries) {
                archiveSize += tarArchiveEntry.getSize();
            }
            closeTarFile(tarFile);
            return archiveSize;
        }

        ZipFile zipFile = getSpecifiedZipSourceFile(archivePath);
        if (zipFile != null) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            long archiveEntries = 0;
            long entrySize = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                entrySize = getZipEntrySize(zipFile, entry);
                if (entrySize == -1) {
                    closeZipFile(zipFile);
                    return -1; // could return -1 if compression ratio of an
                               // entry is too high
                }

                archiveSize += entrySize;
                ++archiveEntries;
            }
            closeZipFile(zipFile);
            // ***SIZE OR LENGTH OF ENTRIES
            if (verifyZipFileIsSafe(archiveSize, archiveEntries)) {
                return archiveSize;
            }
        }
        if (zipFile == null) {
            return 0; // is neither a zip or tar file, not a security issue
        }

        return -1;
    }

    /**
     * Get the size of each zip entry and make sure it is a valid file
     *
     * @param zipFile
     *            - file to be opened
     * @param entry
     *            - the entry in archive that we are calculating the size of
     * @return size of the entry or -1 if there is an error
     */
    private static long getZipEntrySize(ZipFile zipFile, ZipEntry entry) {
        long entrySize = 0;

        InputStream in;
        try {
            in = new BufferedInputStream(zipFile.getInputStream(entry));

            int numBytes = -1;
            byte[] inputBuffer = new byte[2048];

            while ((numBytes = in.read(inputBuffer)) > 0) {
                entrySize += numBytes;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;

        }

        if (verifyEntryIsSafe(entrySize, entry.getCompressedSize())) {
            return entrySize;
        }

        return -1;

    }

    /**
     * Verify if the entry is valid by checking its compression ratio against
     * the set one
     *
     * @param entrySize
     *            - uncompressed size of the archive entry
     * @param compressedSize
     *            - compressed size of the archive entry
     * @return true if the compression ratio is less than the threshold ratio,
     *         otherwise false
     */
    private static boolean verifyEntryIsSafe(long entrySize, long compressedSize) {
        String unsafeZipEntriesMessage = ". File seems unsafe to open."; //$NON-NLS-1$
        if (compressedSize < 0) {
            Activator activator = new Activator();
            activator.logError("Zip file has invalid compression size: " + compressedSize + unsafeZipEntriesMessage); //$NON-NLS-1$
            return false;
        }

        if (compressedSize == 0) {
            return true; // some default files, including our own test ones have
                         // this set to 0
        }

        double compressionRatio = entrySize / compressedSize;

        if (compressionRatio > THRESHOLD_RATIO) {
            Activator activator = new Activator();
            activator.logError("Zip file has entries with too high a compression ratio: " + compressionRatio + unsafeZipEntriesMessage); //$NON-NLS-1$
            return false;
        }
        return true;
    }

    /**
     * Verify if zip file is a zip bomb by checking if number of entries is not
     * above threshold entries, or if archive uncompressed size is not above
     * threshold size.
     *
     * @param archiveSize
     *            the uncompressed size of zip file
     * @param archiveEntries
     *            the number of entries in zip file
     * @return true if file is within size or number of entries threshold,
     *         otherwise it returns false
     */
    @VisibleForTesting
    public static boolean verifyZipFileIsSafe(long archiveSize, long archiveEntries) {
        String unsafeZipEntriesMessage = ". File seems unsafe to open."; //$NON-NLS-1$
        String unsafeZipSizeMessage = " bytes. File seems unsafe to open."; //$NON-NLS-1$

        if (archiveEntries > THRESHOLD_ENTRIES) {
            Activator activator = new Activator();
            activator.logError("Zip file has too many entries: " + archiveEntries + unsafeZipEntriesMessage); //$NON-NLS-1$
            return false;
        }

        if (archiveSize > THRESHOLD_SIZE) {
            Activator activator = new Activator();
            activator.logError("Zip file is too large: " + archiveSize + unsafeZipSizeMessage); //$NON-NLS-1$
            return false;
        }
        return true;
    }

    /**
     * Get the root file system object and it's associated import provider for
     * the specified source file. A shell is used to display messages in case of
     * errors.
     *
     * @param sourceFile
     *            the source file
     * @param shell
     *            the parent shell to use to display error messages
     * @return the root file system object and it's associated import provider
     */
    @SuppressWarnings("resource")
    public static Pair<IFileSystemObject, FileSystemObjectImportStructureProvider> getRootObjectAndProvider(File sourceFile, Shell shell) {
        if (sourceFile == null) {
            return null;
        }

        IFileSystemObject rootElement = null;
        FileSystemObjectImportStructureProvider importStructureProvider = null;

        // Import from directory
        if (!isArchiveFile(sourceFile)) {
            importStructureProvider = new FileSystemObjectImportStructureProvider(FileSystemStructureProvider.INSTANCE, null);
            rootElement = importStructureProvider.getIFileSystemObject(sourceFile);
        } else {
            // Import from archive
            FileSystemObjectLeveledImportStructureProvider leveledImportStructureProvider = null;
            String archivePath = sourceFile.getAbsolutePath();
            if (isTarFile(archivePath)) {
                if (ensureTarSourceIsValid(archivePath)) {
                    // We close the file when we dispose the import provider,
                    // see disposeSelectionGroupRoot
                    TarFile tarFile = getSpecifiedTarSourceFile(archivePath);
                    leveledImportStructureProvider = new FileSystemObjectLeveledImportStructureProvider(new TarLeveledStructureProvider(tarFile), archivePath);
                }
            } else if (ensureZipSourceIsValid(archivePath)) {
                // We close the file when we dispose the import provider, see
                // disposeSelectionGroupRoot
                ZipFile zipFile = getSpecifiedZipSourceFile(archivePath);
                leveledImportStructureProvider = new FileSystemObjectLeveledImportStructureProvider(new ZipLeveledStructureProvider(zipFile), archivePath);
            } else if (ensureGzipSourceIsValid(archivePath)) {
                // We close the file when we dispose the import provider, see
                // disposeSelectionGroupRoot
                GzipFile zipFile = null;
                try {
                    zipFile = new GzipFile(archivePath);
                    leveledImportStructureProvider = new FileSystemObjectLeveledImportStructureProvider(new GzipLeveledStructureProvider(zipFile), archivePath);
                } catch (IOException e) {
                    // do nothing
                }
            }
            if (leveledImportStructureProvider == null) {
                return null;
            }
            rootElement = leveledImportStructureProvider.getRoot();
            importStructureProvider = leveledImportStructureProvider;
        }

        if (rootElement == null) {
            return null;
        }

        return new Pair<>(rootElement, importStructureProvider);
    }

    /**
     * Convert a string path to a path containing valid names. See
     * {@link TmfTraceCoreUtils#validateName(String)}.
     *
     * @param path
     *            the string path to convert
     * @return the path contains valid segment names
     */
    public static IPath toValidNamesPath(String path) {
        IPath newSafePath = TmfTraceCoreUtils.newSafePath(path);
        IPath newFullPath = newSafePath;
        String[] segments = newSafePath.segments();
        for (int i = 0; i < segments.length; i++) {
            String segment = TmfTraceCoreUtils.validateName(TmfTraceCoreUtils.safePathToString(segments[i]));
            if (i == 0) {
                newFullPath = new Path(segment);
            } else {
                newFullPath = newFullPath.append(segment);
            }
        }
        return newFullPath;
    }
}
