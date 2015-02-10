# Forcefully Upgrade Database Directories

This utilty will modify the version fields in the database store files to the version of the current 2.2-SNAPSHOT.
The reason for this is that during 2.2 development there was a change in store version, but the built-in migration code
does not support automatically upgrading databases within the same version. Since the changes to the store format
appear to only affect indexes, it is possible to upgrade by manually modifying the store version fields and deleting
the indexes and finally starting the database so that indexes are rebuilt.

> WARNING: This code might destroy your database.
> It has only been tested with a few specific databases.
> Be sure to backup your database first before trying this out.

### Running the tool

The main method is in the class org.amanzi.neo4j.ForceUpgrade.
Call this with arguments that are the directories (relative paths) to the databases to upgrade. Using maven you can do this as follows:

    mvn exec:java -Dexec.mainClass="org.amanzi.neo4j.ForceUpgrade" -Dexec.args="path to db dir"

