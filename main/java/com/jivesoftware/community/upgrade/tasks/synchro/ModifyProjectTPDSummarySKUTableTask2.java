package com.jivesoftware.community.upgrade.tasks.synchro;

import com.jivesoftware.community.upgrade.UpgradeTask;
import com.jivesoftware.community.upgrade.UpgradeUtils;
import org.apache.log4j.Logger;

/**
 * 
 * User: Tejinder
 * Date: 4/1/17
 * Time: 11:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class ModifyProjectTPDSummarySKUTableTask2 implements UpgradeTask {
    private static Logger LOG = Logger.getLogger(ModifyProjectTPDSummarySKUTableTask2.class);
    
    private static String ADD_ROW_ID = "ALTER TABLE grailprojecttpdskudetails ADD rowid integer;";
    
    
    
    @Override
    public String getName() {
        return "Modify Project TPD Summary SKU task for New Synchro Requirements";
    }

    @Override
    public String getDescription() {
        return "This task modifies 'grailprojecttpdskudetails' table to add new column to capture new Synchro Requirements ";
    }

    @Override
    public String getEstimatedRunTime() {
        return LESS_ONE_MINUTE;
    }

    @Override
    public String getInstructions() {
        return "To upgrade your installation, updating the grailprojecttpdskudetails tables...\n";
    }

    @Override
    public boolean isBackgroundTask() {
        return false;
    }

    @Override
    public void doTask() throws Exception {
        if (UpgradeUtils.doesTableExist("grailprojecttpdskudetails")) {
            try {
                UpgradeUtils.executeStatement(ADD_ROW_ID);
               
             
              
            } catch(Exception e) {
                LOG.error("Error while updating grailprojecttpdskudetails table " + e.getMessage());
            }
        }
    }
}

