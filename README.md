# HSQL Databases Manager

Easily manage your HSQL Databases, running all of them in one single server and port.

## Installation:
1. Extract [hsqldb-manager-final-pack.zip](hsqldb-manager-final-pack.zip) in any location on your system.
2. Add the location to your PATH variable
3. Everything's ready

## Usage:

All the usage is made by commands in terminal/cmd, calling the `hsqlman.bat`, the currently supported commands are:

| Command  | Description |
| ----------| ----------- |
| hsqlman start | Start the HSQLDB Manager, running all the deployed databases. |
| hsqlman stop | Stop all the running HSQLDB instances.
| hsqlman status | Display if the manager is currently running.
| hsqlman deploy <db_name> | Deploy an database with the provided name, storing its files in the current cmd/terminal location.
| hsqlman undeploy <db_name> | Undeploy the database with the provided name, keeping its files as it is.
| hsqlman list | List all the currently deployed and running databases.
| hsqlman sqltool <db_name> | Open the SQL access tool in the provided database.