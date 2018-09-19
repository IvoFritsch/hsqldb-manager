# HSQL Databases Manager

Easily manage your HSQL Databases, running all of them in one single server and port.

## Installation:
1. Extract [hsqldb-manager-final-pack.zip](https://github.com/IvoFritsch/hsqldb-manager/raw/master/hsqldb-manager-final-pack.zip) in any location on your system.
2. Add the location to your PATH variable (**If you dont want to do this, you must always call the `hsqlman.bat` via it's complete path.**)
3. Everything's ready

## Usage:

All the usage is made by commands in terminal/cmd, calling the `hsqlman.bat`, the currently supported commands are:

| Command  | Description |
| --------------- | ----------- |
| start | Start the HSQLDB Manager, running all the deployed databases. |
| stop | Stop all the running HSQLDB instances. |
| status | Display if the manager is currently running. |
| deploy <db_name> | Deploy an database with the provided name, storing its files in the current cmd/terminal location. |
| undeploy <db_name> | Undeploy the database with the provided name, keeping its files as it is. |
| list | List all the currently deployed and running databases. |
| sqltool <db_name> | Open the SQL access tool in the provided database. |

