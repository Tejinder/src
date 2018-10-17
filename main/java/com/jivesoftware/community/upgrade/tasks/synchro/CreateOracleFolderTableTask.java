package com.jivesoftware.community.upgrade.tasks.synchro;

import com.jivesoftware.community.upgrade.UpgradeTask;
import com.jivesoftware.community.upgrade.UpgradeUtils;

/**
 * @author: tejinder
 * @since: 1.0
 */
public class CreateOracleFolderTableTask implements UpgradeTask {

    @Override
    public String getName() {
        return "Create Oracle Folder Table task";
    }

    @Override
    public String getDescription() {
        return "This task creates a new table used by Oracle for creating new Folder.";
    }

    @Override
    public String getEstimatedRunTime() {
        return LESS_TEN_SECONDS;
    }

    @Override
    public String getInstructions() {
        return "To upgrade your installation, create the following tables...\n" + "\n"
                + UpgradeUtils.processSQLGenFile("CreateOracleFolderTable");
    }

    @Override
    public boolean isBackgroundTask() {
        return false;
    }

    @Override
    public void doTask() throws Exception {
        if (UpgradeUtils.doesTableExist("grailOracleFolder")) {        	
        	UpgradeUtils.dropTable("grailOracleFolder");
        }
        UpgradeUtils.executeSQLGenFile("CreateOracleFolderTable");
    }
}
