package com.jivesoftware.community.upgrade.tasks.synchro;

import com.jivesoftware.community.upgrade.UpgradeTask;
import com.jivesoftware.community.upgrade.UpgradeUtils;

/**
 * @author: Kanwar Grewal
 * @since: 5.0
 */
public class CreateT20T40MappingTableTask implements UpgradeTask {

    @Override
    public String getName() {
        return "Grail Phase 5 Table Task : Creating Mapping Tables";
    }

    @Override
    public String getDescription() {
        return "This task creates a new table used by synchro get config details about mapping b/w endmarket and T20/T40";
    }

    public String getEstimatedRunTime() {
        return LESS_TEN_SECONDS;
    }

    @Override
    public String getInstructions() {
        return "To upgrade your installation, create the following tables...\n" + "\n"
                + UpgradeUtils.processSQLGenFile("CreateT20T40MappingTable");
    }

    @Override
    public boolean isBackgroundTask() {
        return false;
    }

    @Override
    public void doTask() throws Exception {
        if (UpgradeUtils.doesTableExist("grailT20T40MappingMetaFields")) {        	
        	UpgradeUtils.dropTable("grailT20T40MappingMetaFields");
        }
        UpgradeUtils.executeSQLGenFile("CreateT20T40MappingTable");
    }
}
