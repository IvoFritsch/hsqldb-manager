# HSQL Databases Manager

Easily manage your HSQL Databases, running all of them in one single server and port.

Haftware SI - 2018

## Installation:
1. Extract the [hsqldb-manager.zip](https://github.com/IvoFritsch/hsqldb-manager/raw/master/hsqldb-manager.zip) in any location on your system.
2. If you're in Linux, the `hsqlman` file must be converted into an executable using the command `chmod +x hsqlman`
3. Add the location to your PATH variable (**If you dont want to do this, you must always call the `hsqlman.bat`/`hsqlman` via it's complete path.**)
4. Everything's ready

## Usage:

All the usage is made by commands in terminal/cmd, calling the `hsqlman.bat` for Windows or `hsqlman` for Linux, the currently supported commands are:

| Command  | Description |
| --------------- | ----------- |
| start | Start the HSQLDB Manager, running all the deployed databases. |
| stop | Stop all the running HSQLDB instances. |
| status | Display if the manager is currently running. |
| deploy <db_name> | Deploy an database with the provided name, creating it if doesn't exist, and storing its files in the current cmd/terminal location. |
| undeploy <db_name> | Undeploy the database with the provided name, keeping its files as it is. |
| list | List all the currently deployed and running databases. |
| sqltool <db_name> | Open the SQL access tool in the provided database. |
| swing [<db_name>] | Open HSQLDB swing access tool in the provided database(optional). |
| backup <db_name> [<file>] | Makes an hot backup of the database to the current CLI location or provided file/directory(optional). |

## Access control:

The access control for the deployed databases is configured inside the supplied `hsqldb-manager.zip -> acl.txt`. 
Open the file for more instructions.
When you modify the acl.txt file, the rules are automatically reloaded.
