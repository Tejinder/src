package com.jivesoftware.community.upgrade.tasks.synchro;

import com.jivesoftware.community.upgrade.UpgradeTask;
import com.jivesoftware.community.upgrade.UpgradeUtils;

/**
 * @author: kanwar
 * @since: 1.0
 */
public class CreateCountryCurrencyTableTask implements UpgradeTask {

    @Override
    public String getName() {
        return "Grail Country Currency Mapping Table Task";
    }

    @Override
    public String getDescription() {
        return "This task creates a new table used by synchro to save country with corresponding currencies";
    }

    public String getEstimatedRunTime() {
        return LESS_TEN_SECONDS;
    }

    @Override
    public String getInstructions() {
        return "To upgrade your installation, create the following tables...\n" + "\n"
                + UpgradeUtils.processSQLGenFile("CreateCountryCurrencyTable");
    }

    @Override
    public boolean isBackgroundTask() {
        return false;
    }
    @Override
    public void doTask() throws Exception {
        if (UpgradeUtils.doesTableExist("grailCountryCurrency")) {        	
        	UpgradeUtils.dropTable("grailCountryCurrency");
        }
        UpgradeUtils.executeSQLGenFile("CreateCountryCurrencyTable");
    }
}
