package org.teiid.rhq.plugin.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.KnownDeploymentTypes;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.profileservice.spi.ProfileService;

public class ProfileServiceUtil {

	protected final Log LOG = LogFactory.getLog(ProfileServiceUtil.class);

	/**
	 * Get the passed in {@link ManagedComponent}
	 * 
	 * @return {@link ManagedComponent}
	 * @throws NamingException
	 * @throws Exception
	 */
	public static ManagedComponent getManagedComponent(
			ComponentType componentType, String componentName)
			throws NamingException, Exception {
		ProfileService ps = getProfileService();
		ManagementView mv = getManagementView(ps, true);

		ManagedComponent mc = mv.getComponent(componentName, componentType);
		return mc;
	}

	/**
	 * Get the {@link ManagedComponent} for the {@link ComponentType} and sub
	 * type.
	 * 
	 * @return Set of {@link ManagedComponent}s
	 * @throws NamingException, Exception
	 * @throws Exception
	 */
	public static Set<ManagedComponent> getManagedComponents(
			ComponentType componentType) throws NamingException, Exception {
		ProfileService ps = getProfileService();
		ManagementView mv = getManagementView(ps, true);

		Set<ManagedComponent> mcSet = mv.getComponentsForType(componentType);

		return mcSet;
	}

	/**
	 * @param {@link ManagementView}
	 * @return
	 */
	public static ManagementView getManagementView(ProfileService ps, boolean load) {
		ManagementView mv = ps.getViewManager();
		if (load) {
			mv.load();
		}
		return mv;
	}

	/**
	 * Get the {@link DeploymentManager} from the ProfileService
	 * 
	 * @return DeploymentManager
	 * @throws NamingException
	 * @throws Exception
	 */
	public static DeploymentManager getDeploymentManager()
			throws NamingException, Exception {
		ProfileService ps = getProfileService();
		DeploymentManager deploymentManager = ps.getDeploymentManager();

		return deploymentManager;
	}

	/**
	 * @return {@link ProfileService}
	 * @throws NamingException, Exception
	 */
	public static ProfileService getProfileService() throws NamingException {
		InitialContext ic = new InitialContext();
		ProfileService ps = (ProfileService) ic
				.lookup(PluginConstants.PROFILE_SERVICE);
		return ps;
	}

	/**
	 * @return {@link File}
	 * @throws NamingException, Exception
	 */
	public static File getDeployDirectory() throws NamingException, Exception {
		ProfileService ps = getProfileService();
		ManagementView mv = getManagementView(ps, false);
		Set<ManagedDeployment> warDeployments;
		try {
			warDeployments = mv
					.getDeploymentsForType(KnownDeploymentTypes.JavaEEWebApplication
							.getType());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		ManagedDeployment standaloneWarDeployment = null;
		for (ManagedDeployment warDeployment : warDeployments) {
			if (warDeployment.getParent() == null) {
				standaloneWarDeployment = warDeployment;
				break;
			}
		}
		if (standaloneWarDeployment == null)
			// This could happen if no standalone WARs, including the admin
			// console WAR, have been fully deployed yet.
			return null;
		URL warUrl;
		try {
			warUrl = new URL(standaloneWarDeployment.getName());
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}
		File warFile = new File(warUrl.getPath());
		File deployDir = warFile.getParentFile();
		return deployDir;
	}

}