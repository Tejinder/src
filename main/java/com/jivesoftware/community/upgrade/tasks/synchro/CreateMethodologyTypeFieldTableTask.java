package com.jivesoftware.community.upgrade.tasks.synchro;

import com.jivesoftware.community.upgrade.UpgradeTask;
import com.jivesoftware.community.upgrade.UpgradeUtils;

/**
 * @author: Kanwar
 * @since: 1.0
 */
public class CreateMethodologyTypeFieldTableTask implements UpgradeTask {

    @Override
    public String getName() {
        return "Create MethodologyType Table";
    }

    @Override
    public String getDescription() {
        return "This task creates a table used by synchro for storing Methodology Type fields ";
    }

    @Override
    public String getEstimatedRunTime() {
        return LESS_TEN_SECONDS;
    }

    @Override
    public String getInstructions() {
        return "To upgrade your installation, create the following tables...\n" + "\n"
                + UpgradeUtils.processSQLGenFile("CreateMethodologyTypeFieldTable");
    }

    @Override
    public boolean isBackgroundTask() {
        return false;
    }

    @Override
    public void doTask() throws Exception {

        if (UpgradeUtils.doesTableExist("grailMethodologyTypeFields")) {        	
        	UpgradeUtils.dropTable("grailMethodologyTypeFields");
        }
        UpgradeUtils.executeSQLGenFile("CreateMethodologyTypeFieldTable");
    }
}
