# GenericLauncher

An self-updating launcher.

## Configuration

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
    -->
    <package    name=""
                postCommand=""
                postCwd=""
                basePath=""
                depends="" >
        <!--    component: can occur multiple times
                Attributes:
                    source: path to the source file
                    target: path to the target file (can relative to working directory)
                    compare: (optional) file to compare source with instead of target
                    required: (optional) default is true, if false this component can be deselected to avoid
                                downloading it (thus overwriting local files, for example config files)
        -->
        <component  source="" 
                    target="" 
                    compare="" 
                    required="" />
    </package>
</launcher>
