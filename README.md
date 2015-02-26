# GenericLauncher

An self-updating launcher.

## Configuration

### Server List
A server list file contains a list of 'server' the launcher queries for updates.
It is simple structured:
<NAME>=<PATH>
<NAME>=<PATH>
...

The 'server' (path) is where the package and component definitions are located.

### Package
Packages are files containing information for update/download.
At the moment a package file must have the file ending '.cfg'.
Structure:
BASE_PATH=<BASE PATH OF WHERE THE UPDATE FILES ARE LOCATED>
POST_COMMAND=<SHELL COMMAND TO BE EXECUTED AFTER UPDATE>
POST_CWD=<DIRECTORY IN WHICH THE 'POST_COMMAND' SHOULD BE EXECUTED>
COMPONENT=<PATH TO COMPONENT DEFINITION>
COMPONENT=
...
COMPONENT=

### Component
A simple copy definition file
Structure:
SOURCE=<SOURCE FILE OR DIRECTORY>
TARGET=<TARGET FILE OR DIRECTORY>

If source is a directory target must be one too.
If source is a file target can be either a file or directory, in the latter case the original source name will be used.

