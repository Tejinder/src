package com.jivesoftware.community.upgrade.tasks.synchro;

import org.apache.log4j.Logger;

import com.grail.synchro.action.ProjectCreateAction;
import com.jivesoftware.community.upgrade.UpgradeTask;
import com.jivesoftware.community.upgrade.UpgradeUtils;

/**
 * @author: tejinder
 * @since: 1.0
 */
public class ModifySpecsTableTask2 implements UpgradeTask {
	
	private static final Logger LOGGER = Logger.getLogger(ModifySpecsTableTask2.class);
	
	private static String ADD_COL1 = "ALTER TABLE grailprojectspecs ADD aboveMarketFinalCost numeric;";
	private static String ADD_COL2 = "ALTER TABLE grailprojectspecs ADD aboveMarketFinalCostType bigint;";
	
    @Override
    public String getName() {
        return "Modify EM Proposal details Table task";
    }

    @Override
    public String getDescription() {
        return "This task modifies exisiting Grail Project Specs Table for adding new Above Market Final Cost and Final Cost Type";
    }

    @Override
    public String getEstimatedRunTime() {
        return LESS_TEN_SECONDS;
    }

    @Override
    public String getInstructions() {
        return "To upgrade your installation, updating the grailprojectspecs tables...\n";
         ///       + UpgradeUtils.processSQLGenFile("CreateProjectTable");
       // UpgradeUtils.executeStatement(sql)
    }

    @Override
    public boolean isBackgroundTask() {
        return false;
    }

    @Override
    public void doTask() throws Exception {
        if (UpgradeUtils.doesTableExist("grailprojectspecs")) {
        	
        	try{
	        	UpgradeUtils.executeStatement(ADD_COL1);
	        	UpgradeUtils.executeStatement(ADD_COL2);
	        	
        	}catch(Exception e){LOGGER.error("Error while updating Specs details table " + e.getMessage());}
        }
    }
}
