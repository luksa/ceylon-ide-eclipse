# Eclipse plugin for Ceylon

## Installing from http://ceylon-lang.org/eclipse/updatesite/

1.  Start with a clean install of Eclipse Indigo.
    
    <http://www.eclipse.org/downloads/>
    
2.  Use `Help > Install New Software ... > Available Software Sites > Import`
    Select `updatesites.xml`
    
3.  Close the dialog and now choose ceylon-lang.org updatesite and install the 
    plugin.
    
4.  Restart Eclipse.
    
5.  Use `File > New > Ceylon Project`, enter a project name, and select 
    `Finish` create a new Ceylon project in the workspace.
    
6.  Use `New > Ceylon Unit` to create a new `.ceylon` file in the `src` 
    directory of your project.

## Installing/Building with (pure) Eclipse

1.  Start with a clean install of Eclipse Indigo.
    
    <http://www.eclipse.org/downloads/>
    
2.  Use `Help > Install New Software...` to install all components of 
    IMP from the update site at:
    
    <http://download.eclipse.org/technology/imp/updates/0.2/>
    
3.  Use `File > Import... > Existing Projects into Workspace` 
    to import the project from this root directory: 
    
        ceyon-ide-eclipse/plugins/com.redhat.ceylon.eclipse.ui
    
4.  Select the `com.redhat.ceylon.eclipse.ui` project and run it using
    `Run > Run As > Eclipse Application`. Now go to the new instance of 
    Eclipse.
    
5.  Use `File > New > Ceylon Project`, enter a project name, and select 
    `Finish` create a new Ceylon project in the workspace.
    
6.  Use `New > Ceylon Unit` to create a new `.ceylon` file in the `src` 
    directory of your project.

## Building with Tycho/Maven 3

1.  From this directory, type
    
        mvn clean install
    
2.  `site/target/site` now contains an update site you can install from.
    (Need to have IMP update site added too.)

## License

The content of this repository is released under the EPL v1.0
as provided in the LICENSE file that accompanied this code.

By submitting a "pull request" or otherwise contributing to this repository, you
agree to license your contribution under the license mentioned above.
