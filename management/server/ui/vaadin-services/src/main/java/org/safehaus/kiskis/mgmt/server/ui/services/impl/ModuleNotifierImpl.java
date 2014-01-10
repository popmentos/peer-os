/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.kiskis.mgmt.server.ui.services.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.safehaus.kiskis.mgmt.server.ui.services.Module;
import org.safehaus.kiskis.mgmt.server.ui.services.ModuleNotifier;
import org.safehaus.kiskis.mgmt.server.ui.services.ModuleServiceListener;

/**
 *
 * @author dilshat
 */
public class ModuleNotifierImpl implements ModuleNotifier {

    private final Queue<Module> modules = new ConcurrentLinkedQueue<Module>();
    private final Queue<ModuleServiceListener> moduleListeners = new ConcurrentLinkedQueue<ModuleServiceListener>();

    @Override
    public Queue<Module> getModules() {
        return modules;
    }

    @Override
    public Queue<ModuleServiceListener> getListeners() {
        return moduleListeners;
    }

    public void setModule(Module module) {
        modules.add(module);
        for (ModuleServiceListener moduleServiceListener : moduleListeners) {
            moduleServiceListener.moduleRegistered(module);
        }
    }

    public void unsetModule(Module module) {
        modules.remove(module);
        for (ModuleServiceListener moduleServiceListener : moduleListeners) {
            moduleServiceListener.moduleUnregistered(module);
        }
    }

    @Override
    public void addListener(ModuleServiceListener listener) {
        moduleListeners.add(listener);
    }

    @Override
    public void removeListener(ModuleServiceListener listener) {
        moduleListeners.remove(listener);
    }

}
