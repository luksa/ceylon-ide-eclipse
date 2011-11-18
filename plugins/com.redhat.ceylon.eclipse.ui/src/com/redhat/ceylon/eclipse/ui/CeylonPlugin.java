package com.redhat.ceylon.eclipse.ui;

import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_ATTRIBUTE;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_CLASS;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_CORRECTION;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_DEFAULT_REFINEMENT;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_FILE;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_FILE_ERROR;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_FILE_WARNING;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_FORMAL_REFINEMENT;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_IMPORT;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_IMPORT_LIST;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_INTERFACE;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_LOCAL_ATTRIBUTE;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_LOCAL_CLASS;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_LOCAL_INTERFACE;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_LOCAL_METHOD;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_METHOD;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_PARAMETER;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_NEW_FILE;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_OPEN_DECLARATION;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_PACKAGE;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_PROJECT;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_SEARCH_RESULTS;
import static org.eclipse.core.resources.IncrementalProjectBuilder.FULL_BUILD;
import static org.eclipse.imp.preferences.PreferenceConstants.EDITOR_CLOSE_FENCES;
import static org.eclipse.imp.preferences.PreferenceConstants.EDITOR_CORRECTION_INDICATION;
import static org.eclipse.imp.preferences.PreferenceConstants.EDITOR_MATCHING_BRACKETS;
import static org.eclipse.imp.preferences.PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR;
import static org.eclipse.imp.preferences.PreferenceConstants.P_DUMP_TOKENS;
import static org.eclipse.imp.preferences.PreferenceConstants.P_EMIT_BUILDER_DIAGNOSTICS;
import static org.eclipse.imp.preferences.PreferenceConstants.P_EMIT_MESSAGES;
import static org.eclipse.imp.preferences.PreferenceConstants.P_SOURCE_FONT;
import static org.eclipse.imp.preferences.PreferenceConstants.P_SPACES_FOR_TABS;
import static org.eclipse.imp.preferences.PreferenceConstants.P_TAB_WIDTH;
import static org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS;
import static org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH;

import java.net.URL;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.imp.runtime.PluginBase;
import org.eclipse.imp.runtime.RuntimePlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.BundleContext;

public class CeylonPlugin extends PluginBase {

	public static final String PLUGIN_ID = "com.redhat.ceylon.eclipse.ui";
	public static final String LANGUAGE_ID = "ceylon";
	public static final String EDITOR_ID = PLUGIN_ID + ".editor";

	/**
	 * The unique instance of this plugin class
	 */
	protected static CeylonPlugin pluginInstance;

	public static CeylonPlugin getInstance() {
		if (pluginInstance==null) new CeylonPlugin();
		return pluginInstance;
	}

	public CeylonPlugin() {
		pluginInstance = this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
	    super.start(context);
        setPreferenceDefaults(RuntimePlugin.getInstance().getPreferenceStore());
        runInitialBuild();
	}

	@Override
	public String getID() {
		return PLUGIN_ID;
	}

	@Override
	public String getLanguageID() {
		return LANGUAGE_ID;
	}

    private static IPath iconsPath = new Path("icons/");

    private ImageDescriptor image(String file) {
        URL url = FileLocator.find(getBundle(), 
                iconsPath.append(file), null);
        if (url!=null) {
        	return ImageDescriptor.createFromURL(url);
        }
        else {
        	return null;
        }
	}
	    
	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
        reg.put(CEYLON_PROJECT, image("prj_obj.gif"));
		reg.put(CEYLON_PACKAGE, image("package_obj.gif"));
		reg.put(CEYLON_IMPORT_LIST, image("impc_obj.gif"));
		reg.put(CEYLON_IMPORT, image("imp_obj.gif"));
		reg.put(CEYLON_FILE, image("template_obj.gif"));
		reg.put(CEYLON_FILE_WARNING, image("file_obj.gif"));
		reg.put(CEYLON_FILE_ERROR, image("file_obj.gif"));
		reg.put(CEYLON_CLASS, image("class_obj.gif"));
		reg.put(CEYLON_INTERFACE, image("int_obj.gif"));
		reg.put(CEYLON_LOCAL_CLASS, image("innerclass_private_obj.gif"));
		reg.put(CEYLON_LOCAL_INTERFACE, image("innerinterface_private_obj.gif"));
		reg.put(CEYLON_METHOD, image("public_co.gif"));
		reg.put(CEYLON_ATTRIBUTE, image("public_co.gif"));
		reg.put(CEYLON_LOCAL_METHOD, image("private_co.gif"));
		reg.put(CEYLON_LOCAL_ATTRIBUTE, image("private_co.gif"));
        reg.put(CEYLON_PARAMETER, image("methpro_obj.gif"));
	    reg.put(CEYLON_DEFAULT_REFINEMENT, image("over_co.gif"));
        reg.put(CEYLON_FORMAL_REFINEMENT, image("implm_co.gif"));
        reg.put(CEYLON_OPEN_DECLARATION, image("opentype.gif"));
        reg.put(CEYLON_SEARCH_RESULTS, image("search_ref_obj.gif"));
        reg.put(CEYLON_CORRECTION, image("correction_change.gif"));
        reg.put(CEYLON_NEW_FILE, image("new_wiz.png"));
	}
	
	/**
	 * Kick off an initial build at startup time in order
	 * to build the model.
	 */
    private void runInitialBuild() {
        UIJob buildJob = new UIJob("Workspace build for Ceylon") {
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                try {
                    ResourcesPlugin.getWorkspace().build(FULL_BUILD, null);
                } catch (CoreException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return Status.OK_STATUS;
            }
            
        };
        buildJob.schedule();
    }

	/**
	 * Awful little hack to get a nonugly icon on already-open 
	 * editors at startup time, since IMP doesn't init the
	 * editor to the language-specific icon until the editor
	 * becomes active.
	 */
    /*private void setIconForOpenWindows(IWorkbenchWindow window) {
        for (IWorkbenchPage page: window.getPages()) {
             for (IEditorReference ref: page.findEditors(null, UniversalEditor.EDITOR_ID, IWorkbenchPage.MATCH_ID)) {
                 try {
                    Method method = WorkbenchPart.class.getDeclaredMethod("setTitleImage", Image.class);
                    method.setAccessible(true);
                    method.invoke(ref.getEditor(true), CeylonLabelProvider.FILE_IMAGE);
                 }
                 catch (Exception e) {
                     e.printStackTrace();
                 }
             }
         }
    }*/
    
    private static final String FLAG = "preferencesInitialized";

    /**
     * Really awful hack to initialize some preferences the 
     * first time we run the IDE, since IMP sometimes fails
     * to set the defaults in time for already-open editors
     * to detect them.
     */
    private static void setPreferenceDefaults(IPreferenceStore store) {
        if (!store.getBoolean(FLAG)) {
            System.out.println("INITIALIZING PREFERENCES");
            store.setValue(FLAG, true);
            int size = Platform.getOS().equals("macosx") ? 11 : 10;
            PreferenceConverter.setValue(store, P_SOURCE_FONT, 
                    new FontData[] { new FontData("Monaco", size, SWT.NORMAL),
                                     new FontData("Courier New", size, SWT.NORMAL), 
                                     new FontData("Monospace", size, SWT.NORMAL)});
    
            store.setValue(P_EMIT_MESSAGES, false);
            store.setValue(P_EMIT_BUILDER_DIAGNOSTICS, false);
            store.setValue(P_TAB_WIDTH, 4);
            store.setValue(P_SPACES_FOR_TABS, true);
            store.setValue(P_DUMP_TOKENS, false);
            store.setValue(EDITOR_MATCHING_BRACKETS, true);
            store.setValue(EDITOR_CORRECTION_INDICATION, true);
            store.setValue(EDITOR_CLOSE_FENCES, true);
            
            store.setValue(EDITOR_TAB_WIDTH, 4);
            store.setValue(EDITOR_SPACES_FOR_TABS, true);
        
            PreferenceConverter.setValue(store, EDITOR_MATCHING_BRACKETS_COLOR,
                    new RGB(0,120,255));
        }
    }

}
