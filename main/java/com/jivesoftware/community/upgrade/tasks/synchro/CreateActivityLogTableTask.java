package com.jivesoftware.community.upgrade.tasks.synchro;

import com.jivesoftware.community.upgrade.UpgradeTask;
import com.jivesoftware.community.upgrade.UpgradeUtils;

/**
 * User: Kanwar Grewal
 * Date: 11/02/15
 */

public class CreateActivityLogTableTask implements UpgradeTask {

	 @Override
	    public String getName() {
	        return "Create ActivityLog Table";
	    }

	    @Override
	    public String getDescription() {
	        return "This task creates a new table used by synchro for saving the logs/audits of the project";
	    }

	    @Override
	    public String getEstimatedRunTime() {
	        return LESS_TEN_SECONDS;
	    }

	    @Override
	    public String getInstructions() {
	        return "To upgrade your installation, create the following tables...\n" + "\n"
	                + UpgradeUtils.processSQLGenFile("CreateActivityLogTable");
	    }

	    @Override
	    public boolean isBackgroundTask() {
	        return false;
	    }

	    @Override
	    public void doTask() throws Exception {
	        if (UpgradeUtils.doesTableExist("grailActivityLog")) {
	        	UpgradeUtils.dropTable("grailActivityLog");
	        }
	        
	        UpgradeUtils.executeSQLGenFile("CreateActivityLogTable");
	    }
}
