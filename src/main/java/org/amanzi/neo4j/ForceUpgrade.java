package org.amanzi.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.io.fs.DefaultFileSystemAbstraction;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.kernel.impl.storemigration.StoreFile;
import org.neo4j.kernel.impl.storemigration.StoreMigrator;
import org.neo4j.kernel.impl.storemigration.StoreVersionCheck;
import org.neo4j.kernel.impl.storemigration.UpgradableDatabase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A simple forced upgrade of a database directory between two almost-compatible versions:
 * <ul>
 * <li>Force all versions: StoreFile.ensureStoreVersion(new DefaultFileSystemAbstraction(), storeDir, StoreFile.currentStoreFiles())</li>
 * <li>Delete schema directory</li>
 * <li>Start embedded and shutdown (recreate indexes)</li>
 * </ul>
 * <p/>
 * This class performs all these steps on all directories passed on the command-line.
 */
public class ForceUpgrade {
    public static void main(String[] args) {
        if (args.length > 0) {
            for (String dir : args) {
                File path = new File(dir);
                try {
                    if (path.exists() && path.isDirectory()) {
                        forceUpgrade(path);
                    } else {
                        System.out.println("Directory invalid: " + path.getCanonicalPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("Missing directory name for database to upgrade");
        }
    }

    private static void deleteRecursively(File parent, String pattern) throws IOException {
        for (Path entry : Files.newDirectoryStream(parent.toPath(), pattern)) {
            FileUtils.deleteRecursively(entry.toFile());
        }
    }

    public static void forceUpgrade(File path) throws IOException {
        DefaultFileSystemAbstraction fs = new DefaultFileSystemAbstraction();
        UpgradableDatabase udb = new UpgradableDatabase(new StoreVersionCheck(fs));
        if (udb.hasCurrentVersion(fs, path)) {
            System.out.println("Database already at current version: " + path.getCanonicalPath());
        } else {
            System.out.println("Forcefully Upgrading: " + path.getCanonicalPath());
            deleteRecursively(path, "messages.*");
            deleteRecursively(path, "schema");
            StoreFile.ensureStoreVersion(fs, path.getCanonicalFile(), StoreFile.currentStoreFiles());
            GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(path.getCanonicalPath());
            db.shutdown();
            if (udb.hasCurrentVersion(fs, path)) {
                System.out.println("Database successfully upgraded: " + path.getCanonicalPath());
            } else {
                System.out.println("Failed to upgrade the database: " + path.getCanonicalPath());
            }
        }
    }
}
