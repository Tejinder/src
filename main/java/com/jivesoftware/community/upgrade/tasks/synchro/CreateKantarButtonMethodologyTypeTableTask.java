package com.jivesoftware.community.upgrade.tasks.synchro;

import com.jivesoftware.community.upgrade.UpgradeTask;
import com.jivesoftware.community.upgrade.UpgradeUtils;

/**
 * Created with IntelliJ IDEA.
 * User: Bhakar
 * Date: 1/19/15
 * Time: 10:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class CreateKantarButtonMethodologyTypeTableTask implements UpgradeTask {

    @Override
    public String getName() {
        return "Kantar Button Methodology Type table create task";
    }

    @Override
    public String getDescription() {
        return "This task creates a new table called 'grailkantarbtnmethodologytype' to store all kantar button related methodology types";
    }

    @Override
    public String getEstimatedRunTime() {
        return LESS_ONE_MINUTE;
    }

    @Override
    public String getInstructions() {
        return "To upgrade your installation, create the following tables...\n" + "\n"
                + UpgradeUtils.processSQLGenFile("CreateKantarButtonMethodologyTypeTable");
    }

    @Override
    public boolean isBackgroundTask() {
        return false;
    }

    @Override
    public void doTask() throws Exception {
        if (UpgradeUtils.doesTableExist("grailkantarbtnmethodologytype")) {
            UpgradeUtils.dropTable("grailkantarbtnmethodologytype");
        }
        UpgradeUtils.executeSQLGenFile("CreateKantarButtonMethodologyTypeTable");
    }
}
