package org.safehaus.kiskis.mgmt.api.sqoop;

import org.safehaus.kiskis.mgmt.api.sqoop.setting.ExportSetting;
import org.safehaus.kiskis.mgmt.api.sqoop.setting.ImportSetting;
import org.safehaus.kiskis.mgmt.shared.protocol.ApiBase;

import java.util.UUID;

public interface Sqoop extends ApiBase<Config> {

    public UUID isInstalled(String clusterName, String hostname);

    public UUID addNode(String clusterName, String hostname);

    public UUID destroyNode(String clusterName, String hostname);

    public UUID exportData(ExportSetting settings);

    public UUID importData(ImportSetting settings);
}
