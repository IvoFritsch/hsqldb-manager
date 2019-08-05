# HSQLDB Manager
An standalone all-in-one jar Database + Manager utility to easily manage your HSQLDB Databases, running all of them in one single server and port.

Haftware SI - 2019

## Installation:
The HSQLDB Manager is an all-in-one jar database, you dont need to have nothing more(unless Java) installed in your machine, the installation is as easy as you may want:

1. Extract the [hsqldb-manager.zip](https://github.com/IvoFritsch/hsqldb-manager/raw/master/hsqldb-manager.zip) in any location on your system.
2. If you're in Linux, the `hsqlman` file must be converted into an executable using the command `chmod +x hsqlman`
3. Add the location to your PATH variable (**If you dont want to do this, you must always call the `hsqlman.bat`/`hsqlman` via it's complete path.**)
4. Everything's ready
5. You may now execute all the commands bellow

## Usage:

All the usage is made by the tray icon or by commands in terminal/cmd, calling the `hsqlman.bat` for Windows or `hsqlman` for Linux, the currently supported commands are:

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
| backup <db_name> [<file_name>] | Makes an hot backup of the database to the current CLI location or provided file/directory(optional). |
| logs | Print the 'logs.txt' file. |
| clearlogs | Clear the 'logs.txt' file. |


## Access control:

The access control for the deployed databases is configured inside the supplied `hsqldb-manager.zip -> acl.txt`. 
Open the file for more instructions.
When you modify the acl.txt file, the rules are automatically reloaded.
