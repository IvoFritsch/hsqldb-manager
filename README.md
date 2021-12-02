# HSQLDB Manager
An standalone all-in-one jar Database + Manager utility to easily manage your HSQLDB Databases, running all of them in one single server and port.
One single jar file contains everything you need to create mantain and access HSQLDB database.

Created and mantained by Haftware SI - 2022

## Features:

- Easily deploy new database
- Straightforward support to moving your databases between computers, simply undeploy and redeploy them.
- Simply, by one single command, backup your database while they're running in production.
- Many ways to access your databases, use the **sqltool** command to access via SSH terminal. The **swing** command to access via desktop using java swing or even **webtool** to access using your browser, connected to an embedded server that is openned only when you need it.
- Event logging: everything that happens during HSQLDB Manager executions is logged and can be easily viewed.
- OS tray icon, all the control can be made via terminal or using an OS tray icon that is created when the manager start (feature not fully completed yet).
- Fully portable: All the features are packed in one single jar, that you can simply double click to start the manager.

## Installation:
The HSQLDB Manager is an all-in-one jar database, you dont need to have nothing more(unless Java) installed in your machine, the installation is as easy as you may want:

1. Extract the [hsqldb-manager.zip](https://github.com/IvoFritsch/hsqldb-manager/raw/master/hsqldb-manager.zip) in any location on your system.
2. If you're in Linux, the `hsqlman` file must be converted into an executable using the command `chmod +x hsqlman`
3. Add the location to your PATH variable (**If you dont want to do this, you must always call the `hsqlman.bat`/`hsqlman` via it's complete path.**)
4. Everything's ready
5. You may now execute all the commands bellow

## Usage as Docker container
The HSQLDB Manager can run as a Docker container hosted in [DockerHub](https://hub.docker.com/r/ivofritsch/hsqldb-manager)

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
| webtool | Control the web access tool. |

## Web access tool:
The HSQLDB Manager jar come with an embedded server that can be started to access any deployed database remotely directly from your browser.

### Using webtool:
For security reasons, the webtool server is always down, you can command it to start by typing  `hsqlman webtool start`. The embedded server will immediately come up in the port *35888* (still not configurable).
If you access this port you will see the manager ready to access any deployed database.
By default, only the first computer that access the webtool after it's start will be permitted to access the databases, after this, for any additional computer, you will have to run the command `hsqlman webtool permit` to access.
Soon after all sessions are closed, the webtool server will automatically shutdown, but you can force it to go down typing `hsqlman webtool stop`.
> All webtool connections have autocommit set to off, remember to always call commit before closing

> To avoid accidental out-of-memory problems, all the webtool connections have the maxRows config set to 100, use OFFSET in your queries to see more rows

All the webtool usage is logged and can be viewed with the **logs** commands.

## Access control:

The access control for the deployed databases is configured inside the supplied `hsqldb-manager.zip -> acl.txt`. 
Open the file for more instructions.
When you modify the acl.txt file, the rules are automatically reloaded.
