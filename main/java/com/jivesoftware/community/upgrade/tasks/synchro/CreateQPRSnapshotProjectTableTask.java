package com.jivesoftware.community.upgrade.tasks.synchro;

import com.jivesoftware.community.upgrade.UpgradeTask;
import com.jivesoftware.community.upgrade.UpgradeUtils;

/**
 * @author: tejinder
 * @since: 1.0
 */
public class CreateQPRSnapshotProjectTableTask implements UpgradeTask {

    @Override
    public String getName() {
        return "Create QPR Snapshot Project Table task";
    }

    @Override
    public String getDescription() {
        return "This task creates a new table used by new synchro for QPR Snapshot.";
    }

    @Override
    public String getEstimatedRunTime() {
        return LESS_TEN_SECONDS;
    }

    @Override
    public String getInstructions() {
        return "To upgrade your installation, create the following tables...\n" + "\n"
                + UpgradeUtils.processSQLGenFile("CreateQPRSnapshotProjectTable");
    }

    @Override
    public boolean isBackgroundTask() {
        return false;
    }

    @Override
    public void doTask() throws Exception {
        if (UpgradeUtils.doesTableExist("grailQPRSnapshotProject")) {        	
        	UpgradeUtils.dropTable("grailQPRSnapshotProject");
        }
        UpgradeUtils.executeSQLGenFile("CreateQPRSnapshotProjectTable");
    }
}
