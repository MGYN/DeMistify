package utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.FastHierarchy;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.util.SystemClassHandler;

/**
 * Class containing common utility methods for dealing with Android entry points
 * 
 * @author Steven Arzt
 *
 */
public class EntryPointsUtility {

	private static final Logger logger = LoggerFactory.getLogger(EntryPointsUtility.class);

	private Map<SootClass, ComponentType> componentTypeCache = new HashMap<>();

	private SootClass osClassApplication;
	private SootClass osClassActivity;
	private SootClass osClassMapActivity;
	private SootClass osClassService;
	private SootClass osClassFragment;
	private SootClass osClassSupportFragment;
	private SootClass osClassAndroidXFragment;
	private SootClass osClassBroadcastReceiver;
	private SootClass osClassContentProvider;
	private SootClass osClassGCMBaseIntentService;
	private SootClass osClassGCMListenerService;
	private SootClass osInterfaceServiceConnection;

	/**
	 * Array containing all types of components supported in Android lifecycles
	 */
	public enum ComponentType {
		Application, Activity, Service, Fragment, BroadcastReceiver, ContentProvider, GCMBaseIntentService,
		GCMListenerService, ServiceConnection, Plain
	}

	/**
	 * Creates a new instance of the {@link AndroidEntryPointUtils} class. Soot must
	 * already be running when this constructor is invoked.
	 */
	public EntryPointsUtility() {
		// Get some commonly used OS classes
		osClassApplication = Scene.v().getSootClassUnsafe(EntryPointConstants.APPLICATIONCLASS);
		osClassActivity = Scene.v().getSootClassUnsafe(EntryPointConstants.ACTIVITYCLASS);
		osClassService = Scene.v().getSootClassUnsafe(EntryPointConstants.SERVICECLASS);
		osClassFragment = Scene.v().getSootClassUnsafe(EntryPointConstants.FRAGMENTCLASS);
		osClassSupportFragment = Scene.v().getSootClassUnsafe(EntryPointConstants.SUPPORTFRAGMENTCLASS);
		osClassAndroidXFragment = Scene.v().getSootClassUnsafe(EntryPointConstants.ANDROIDXFRAGMENTCLASS);
		osClassBroadcastReceiver = Scene.v().getSootClassUnsafe(EntryPointConstants.BROADCASTRECEIVERCLASS);
		osClassContentProvider = Scene.v().getSootClassUnsafe(EntryPointConstants.CONTENTPROVIDERCLASS);
		osClassGCMBaseIntentService = Scene.v().getSootClassUnsafe(EntryPointConstants.GCMBASEINTENTSERVICECLASS);
		osClassGCMListenerService = Scene.v().getSootClassUnsafe(EntryPointConstants.GCMLISTENERSERVICECLASS);
		osInterfaceServiceConnection = Scene.v().getSootClassUnsafe(EntryPointConstants.SERVICECONNECTIONINTERFACE);
		osClassMapActivity = Scene.v().getSootClassUnsafe(EntryPointConstants.MAPACTIVITYCLASS);
	}

	/**
	 * Gets the type of component represented by the given Soot class
	 * 
	 * @param currentClass The class for which to get the component type
	 * @return The component type of the given class
	 */
	public ComponentType getComponentType(SootClass currentClass) {
		if (componentTypeCache.containsKey(currentClass))
			return componentTypeCache.get(currentClass);

		// Check the type of this class
		ComponentType ctype = ComponentType.Plain;
		FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();

		if (fh != null) {
			// (1) android.app.Application
			if (osClassApplication != null && fh.canStoreType(currentClass.getType(), osClassApplication.getType()))
				ctype = ComponentType.Application;
			// (2) android.app.Activity
			else if (osClassActivity != null && fh.canStoreType(currentClass.getType(), osClassActivity.getType()))
				ctype = ComponentType.Activity;
			// (3) android.app.Service
			else if (osClassService != null && fh.canStoreType(currentClass.getType(), osClassService.getType()))
				ctype = ComponentType.Service;
			// (4) android.app.BroadcastReceiver
			else if (osClassFragment != null && Scene.v().getOrMakeFastHierarchy().canStoreType(currentClass.getType(),
					osClassFragment.getType()))
				ctype = ComponentType.Fragment;
			else if (osClassSupportFragment != null
					&& fh.canStoreType(currentClass.getType(), osClassSupportFragment.getType()))
				ctype = ComponentType.Fragment;
			else if (osClassAndroidXFragment != null
					&& fh.canStoreType(currentClass.getType(), osClassAndroidXFragment.getType()))
				ctype = ComponentType.Fragment;
			// (5) android.app.BroadcastReceiver
			else if (osClassBroadcastReceiver != null
					&& fh.canStoreType(currentClass.getType(), osClassBroadcastReceiver.getType()))
				ctype = ComponentType.BroadcastReceiver;
			// (6) android.app.ContentProvider
			else if (osClassContentProvider != null
					&& fh.canStoreType(currentClass.getType(), osClassContentProvider.getType()))
				ctype = ComponentType.ContentProvider;
			// (7) com.google.android.gcm.GCMBaseIntentService
			else if (osClassGCMBaseIntentService != null
					&& fh.canStoreType(currentClass.getType(), osClassGCMBaseIntentService.getType()))
				ctype = ComponentType.GCMBaseIntentService;
			// (8) com.google.android.gms.gcm.GcmListenerService
			else if (osClassGCMListenerService != null
					&& fh.canStoreType(currentClass.getType(), osClassGCMListenerService.getType()))
				ctype = ComponentType.GCMListenerService;
			// (9) android.content.ServiceConnection
			else if (osInterfaceServiceConnection != null
					&& fh.canStoreType(currentClass.getType(), osInterfaceServiceConnection.getType()))
				ctype = ComponentType.ServiceConnection;
			// (10) com.google.android.maps.MapActivity
			else if (osClassMapActivity != null
					&& fh.canStoreType(currentClass.getType(), osClassMapActivity.getType()))
				ctype = ComponentType.Activity;
		} else
			logger.warn(String.format("No FastHierarchy, assuming %s is a plain class", currentClass.getName()));

		componentTypeCache.put(currentClass, ctype);
		return ctype;
	}

	/**
	 * Checks whether the given class is derived from android.app.Application
	 * 
	 * @param clazz The class to check
	 * @return True if the given class is derived from android.app.Application,
	 *         otherwise false
	 */
	public boolean isApplicationClass(SootClass clazz) {
		return osClassApplication != null
				&& Scene.v().getOrMakeFastHierarchy().canStoreType(clazz.getType(), osClassApplication.getType());
	}

	/**
	 * Checks whether the given method is an Android entry point, i.e., a lifecycle
	 * method
	 * 
	 * @param method The method to check
	 * @return True if the given method is a lifecycle method, otherwise false
	 */
	public boolean isEntryPointMethod(SootMethod method) {
		if (method == null)
			throw new IllegalArgumentException("Given method is null");
		ComponentType componentType = getComponentType(method.getDeclaringClass());
		String subsignature = method.getSubSignature();

		if (componentType == ComponentType.Activity
				&& EntryPointConstants.getActivityLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.Service
				&& EntryPointConstants.getServiceLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.Application
				&& EntryPointConstants.getApplicationLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.Fragment
				&& EntryPointConstants.getFragmentLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.BroadcastReceiver
				&& EntryPointConstants.getBroadcastLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.ContentProvider
				&& EntryPointConstants.getContentproviderLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.GCMBaseIntentService
				&& EntryPointConstants.getGCMIntentServiceMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.GCMListenerService
				&& EntryPointConstants.getGCMListenerServiceMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.ServiceConnection
				&& EntryPointConstants.getServiceConnectionMethods().contains(subsignature))
			return true;

		return false;
	}

	/**
	 * Gets all lifecycle methods in the given entry point class
	 * 
	 * @param sc The class in which to look for lifecycle methods
	 * @return The set of lifecycle methods in the given class
	 */
	public Collection<? extends MethodOrMethodContext> getLifecycleMethods(SootClass sc) {
		return getLifecycleMethods(getComponentType(sc), sc);
	}

	/**
	 * Gets all lifecycle methods in the given entry point class
	 * 
	 * @param componentType the component type
	 * @param sc            The class in which to look for lifecycle methods
	 * @return The set of lifecycle methods in the given class
	 */
	public static Collection<? extends MethodOrMethodContext> getLifecycleMethods(ComponentType componentType,
			SootClass sc) {
		switch (componentType) {
		case Activity:
			return getLifecycleMethods(sc, EntryPointConstants.getActivityLifecycleMethods());
		case Service:
			return getLifecycleMethods(sc, EntryPointConstants.getServiceLifecycleMethods());
		case Application:
			return getLifecycleMethods(sc, EntryPointConstants.getApplicationLifecycleMethods());
		case BroadcastReceiver:
			return getLifecycleMethods(sc, EntryPointConstants.getBroadcastLifecycleMethods());
		case Fragment:
			return getLifecycleMethods(sc, EntryPointConstants.getFragmentLifecycleMethods());
		case ContentProvider:
			return getLifecycleMethods(sc, EntryPointConstants.getContentproviderLifecycleMethods());
		case GCMBaseIntentService:
			return getLifecycleMethods(sc, EntryPointConstants.getGCMIntentServiceMethods());
		case GCMListenerService:
			return getLifecycleMethods(sc, EntryPointConstants.getGCMListenerServiceMethods());
		case ServiceConnection:
			return getLifecycleMethods(sc, EntryPointConstants.getServiceConnectionMethods());
		case Plain:
			return Collections.emptySet();
		}
		return Collections.emptySet();
	}

	/**
	 * This method takes a lifecycle class and the list of lifecycle method
	 * subsignatures. For each subsignature, it checks whether the given class or
	 * one of its superclass overwrites the respective methods. All findings are
	 * collected in a set and returned.
	 * 
	 * @param sc      The class in which to look for lifecycle method
	 *                implementations
	 * @param methods The list of lifecycle method subsignatures for the type of
	 *                component that the given class corresponds to
	 * @return The set of implemented lifecycle methods in the given class
	 */
	private static Collection<? extends MethodOrMethodContext> getLifecycleMethods(SootClass sc, List<String> methods) {
		List<MethodOrMethodContext> lifecycleMethods = new ArrayList<>();
		SootClass currentClass = sc;
		while (currentClass != null) {
			for (String sig : methods) {
				SootMethod sm = currentClass.getMethodUnsafe(sig);
				if (sm != null)
					if (!SystemClassHandler.v().isClassInSystemPackage(sm.getDeclaringClass().getName()))
						lifecycleMethods.add(sm);
			}
			currentClass = currentClass.hasSuperclass() ? currentClass.getSuperclass() : null;
		}
		return lifecycleMethods;
	}

}
