# GenericLauncher

An self-updating launcher.

## Configuration

Server List:
The serverlist is an xml containing addresses to package definition xmls.
It can be either a local file or a file on a web-host.
        
Example:
        
        
        <serverlist>
                <entry>C:\package.xml</entry>
                <entry>http://host.com/package.xml</entry>
        </serverlist>
        
        
Configuration is XML based.

        <!--    launcher: root node
                Attributes:
                    basePath: (optional) overall basepath 
        -->
        <launcher basePath="">
            <!--    package: can occur multiple times
                    Attributes:
                        name: Unique name for the package, serves as ID for referencing with 'depends'
                        postCommand: (optional) command to be executed after this package is executed
                        postCwd: (optional) working directory the postCommand should be executed in
                        basePath: (optional) basePath replaces launcher basePath
                        depends: (optional) another package name that should be executed before this package
                        version: (optional) Package version. Whole package will be updated, beside components that specify 
                                their own version, in this case the component's version is considered.
                        restart: (optional) Update this package triggers a restart.
                                default: false
                                Values: true,false
            -->
            <package    name=""
                        postCommand=""
                        postCwd=""
                        basePath=""
                        depends=""
                        version=""
                        restart="" >
                <!--    component: can occur multiple times
                        Attributes:
                            source: path to the source file
                            target: path to the target file (can relative to working directory)
                            compare: (optional) file to compare source with instead of target
                            required: (optional) default is true, if false this component can be deselected to avoid
                                        downloading it (thus overwriting local files, for example config files)
                            version: (optional) Version string in format "Major.Minor" fe "1.12"
                -->
                <component  source="" 
                            target="" 
                            compare="" 
                            required="" />
            </package>
        </launcher>
