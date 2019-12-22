/*

Copyright (c) 2019, ~angus
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the author nor the names of its contributors may be
used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;

/**
 * This utility may help you keep track of enabled dependencies.
 * 
 * @param <P> the dependent plugin's type
 * 
 * @author ~angus
 */
public abstract class PluginDependency<P extends Plugin> implements Listener {
	/** Reference to the dependent plugin. */
	protected final P dependent;

	/** Reference to the {@link Server} instance. */
	protected final Server server;

	/** Reference to the {@link PluginManager} instance. */
	protected final PluginManager pluginManager;

	/** Reference to the {@link ServicesManager} instance. */
	protected final ServicesManager servicesManager;

	/** String representing the name of the dependency plugin. */
	protected final String dependencyName;

	private final AtomicBoolean enabled;

	/**
	 * {@code dependencyName} is case sensitive.
	 * 
	 * @param dependent      reference to the dependent plugin
	 * @param dependencyName the name of the dependency
	 */
	public PluginDependency(P dependent, String dependencyName) {
		this.dependent = dependent;
		this.dependencyName = dependencyName;
		this.enabled = new AtomicBoolean();

		server = dependent.getServer();
		pluginManager = server.getPluginManager();
		servicesManager = server.getServicesManager();

		if (pluginManager.isPluginEnabled(dependencyName)) {
			doEnable();
		}

		server.getPluginManager().registerEvents(this, dependent);
	}

	/**
	 * @return the dependent plugin
	 */
	public final P getDependent() {
		return dependent;
	}

	/**
	 * This is case sensitive.
	 * 
	 * @return the name of the dependency
	 */
	public final String getDependencyName() {
		return dependencyName;
	}

	/**
	 * Shortcut for {@link PluginManager#isPluginEnabled(String)}.
	 * 
	 * @return whether or not the dependency is enabled
	 */
	public final boolean isEnabled() {
		return pluginManager.isPluginEnabled(dependencyName);
	}

	/**
	 * Called when the dependency is enabled. Should be overridden.
	 * 
	 * @throws Exception
	 */
	protected void onEnable() throws Exception {
	}

	/**
	 * Called when the dependency is disabled. Should be overridden.
	 * 
	 * @throws Exception
	 */
	protected void onDisable() throws Exception {
	}

	private final synchronized void doEnable() {
		if (enabled.compareAndSet(false, true)) {
			try {
				onEnable();
			} catch (Exception cause) {
				dependent.getLogger().log(Level.WARNING, "Exception caught while handling dependency enable. This may cause future problems.", cause);
			}
		}
	}

	private final synchronized void doDisable() {
		if (enabled.compareAndSet(true, false)) {
			try {
				onDisable();
			} catch (Exception cause) {
				dependent.getLogger().log(Level.WARNING, "Exception caught while handling dependency disable. This may cause future problems.", cause);
			} finally {
				dependent.getLogger().warning("Dependency \"" + dependencyName + "\" was disabled. Certain functionality may be limited.");
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private final void onPluginEnableEvent(PluginEnableEvent event) {
		if (!dependent.isEnabled()) {
			return;
		}

		if (event.getPlugin().getName().equals(dependencyName)) {
			doEnable();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private final void onPluginDisableEvent(PluginDisableEvent event) {
		if (dependencyName.equals(event.getPlugin().getName()) || dependent.equals(event.getPlugin())) {
			doDisable();
		}
	}
}
