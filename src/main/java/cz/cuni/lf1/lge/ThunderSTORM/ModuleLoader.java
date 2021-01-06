package cz.cuni.lf1.lge.ThunderSTORM;

import cz.cuni.lf1.lge.ThunderSTORM.calibration.DefocusFunction;
import cz.cuni.lf1.lge.ThunderSTORM.results.PostProcessingModule;
import ij.IJ;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Vector;

/**
 * A class for loading modules at runtime.
 */
public class ModuleLoader {

    /**
     * Loads implementations of sublcasses of IModule interface. It uses the
     * imagej plugin classloader, so it looks for implementations in the imagej
     * plugin directory. For the module to be loaded, you must place a jar with
     * an implementation of one of the subclasses of IModule. The jar must
     * contain, in the folder META-INF/services, a file named after the full
     * name of the implemented interface (for example
     * cz.cuni.lf1.lge.ThunderSTORM.detectors.IDetector) and the content of the
     * file is full names of the classes implementing the interface. Each on a
     * separate line. The file must be in UTF-8 (without BOM!) See
     * {@link ServiceLoader} for more details.
     *
     * <br/>
     * The implementation must provide a no-arguments constructor so the module
     * can be instantiated.
     *
     * If there is an error while loading a module, the error is logged and the
     * method attempts to continue loading other modules. Exception is thrown
     * only when no modules are succesfully loaded.
     *
     * @return a vector of instances of the specified class (instantiated by the
     * no-args constructor)
     * @throws RuntimeException if no modules were loaded.
     */
    public static <T extends IModuleUI> Vector<T> getUIModules(Class<T> c) {
        //workaround a bug when service loading does not work after refreshing menus in ImageJ
        boolean oldUseCaching = setUseCaching(false);

        Vector<T> retval = new Vector<T>();
        try {
            ServiceLoader loader = ServiceLoader.load(c, IJ.getClassLoader());
            for(Iterator<T> it = loader.iterator(); it.hasNext();) {
                //when something goes wrong while loading modules, log the error and try to continue
                try {
                    retval.add(it.next());
                } catch(ServiceConfigurationError e) {
                    IJ.handleException(e);
                }
            }
        } catch(Throwable e) {
            IJ.handleException(e);
        } finally {
            setUseCaching(oldUseCaching);
        }
        if(retval.isEmpty()) {
            //throw exception only when no modules are succesfully loaded
            throw new RuntimeException("No modules of type " + c.getSimpleName() + " loaded.");
        }
        return retval;
    }

    public static <T extends IModule> Vector<T> getModules(Class<T> c) {
        //workaround a bug when service loading does not work after refreshing menus in ImageJ
        boolean oldUseCaching = setUseCaching(false);

        Vector<T> retval = new Vector<T>();
        try {
            ServiceLoader loader = ServiceLoader.load(c, IJ.getClassLoader());
            for(Iterator<T> it = loader.iterator(); it.hasNext();) {
                //when something goes wrong while loading modules, log the error and try to continue
                try {
                    retval.add(it.next());
                } catch(ServiceConfigurationError e) {
                    IJ.handleException(e);
                }
            }
        } catch(Throwable e) {
            IJ.handleException(e);
        } finally {
            setUseCaching(oldUseCaching);
        }
        if(retval.isEmpty()) {
            //throw exception only when no modules are succesfully loaded
            throw new RuntimeException("No modules of type " + c.getSimpleName() + " loaded.");
        }
        return retval;
    }

    public static List<PostProcessingModule> getPostProcessingModules() {
        //workaround a bug when service loading does not work after refreshing menus in ImageJ
        boolean oldUseCaching = setUseCaching(false);

        List<PostProcessingModule> retval = new ArrayList<PostProcessingModule>();
        try {
            ServiceLoader loader = ServiceLoader.load(PostProcessingModule.class, IJ.getClassLoader());
            for(Iterator<PostProcessingModule> it = loader.iterator(); it.hasNext();) {
                //when something goes wrong while loading modules, log the error and try to continue
                try {
                    retval.add(it.next());
                } catch(ServiceConfigurationError e) {
                    IJ.handleException(e);
                }
            }
        } catch(Throwable e) {
            IJ.handleException(e);
        } finally {
            setUseCaching(oldUseCaching);
        }
        if(retval.isEmpty()) {
            //throw exception only when no modules are succesfully loaded
            throw new RuntimeException("No modules of type " + PostProcessingModule.class.getSimpleName() + " loaded.");
        }
        return retval;
    }

    /**
     * Enables or disables caching for URLConnection.
     *
     * @return the value of useCaching before this call
     */
    public static boolean setUseCaching(boolean useCache) {
        try {
            URLConnection URLConnection = new URL("http://localhost/").openConnection();
            boolean oldValue = URLConnection.getDefaultUseCaches();
            URLConnection.setDefaultUseCaches(useCache);
            return oldValue;
        } catch(Exception ex) {
            return true;
        }
    }

    public static <T extends IModuleUI> T moduleByName(List<T> knownModules, String name) {
        for(IModuleUI module : knownModules) {
            if(module.getName().equals(name)) {
                return (T) module;
            }
        }
        return null;
    }

    public static <T extends IModuleUI> int moduleIndexByName(List<T> knownModules, String name) {
        for(int i = 0; i < knownModules.size(); i++) {
            if(knownModules.get(i).getName().equals(name)) {
                return i;
            }
        }
        return 0;
    }
}
