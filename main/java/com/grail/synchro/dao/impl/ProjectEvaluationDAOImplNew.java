package com.grail.synchro.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.grail.synchro.beans.ProjectEvaluationInitiation;
import com.grail.synchro.dao.ProjectEvaluationDAONew;
import com.grail.synchro.object.SynchroAttachment;
import com.jivesoftware.base.database.dao.DAOException;
import com.jivesoftware.base.database.dao.JiveJdbcDaoSupport;
import com.jivesoftware.community.impl.dao.AttachmentBean;

/**
 * @author: tejinder
 * @since: 1.0
 */
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class ProjectEvaluationDAOImplNew extends JiveJdbcDaoSupport implements ProjectEvaluationDAONew {

    private static final Logger LOG = Logger.getLogger(ProjectEvaluationDAOImplNew.class.getName());
    
    private static final String INSERT_PROJ_EVAL_SQL = "INSERT INTO grailprojecteval( " +
            " projectid, endmarketid, agencyperformanceim, batcommentsim, agencycommentsim, agencyperformancelm, batcommentslm, agencycommentslm, " +
            " agencyperformancefa, batcommentsfa, agencycommentsfa," +
            " creationby, modificationby, creationdate, modificationdate, status, agencyid, agencyrating, agencycomment) " +
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
            

    private static final String LOAD_PROJ_EVAL = " SELECT projectid, endmarketid, agencyperformanceim, batcommentsim, agencycommentsim," +
    		" agencyperformancefa, batcommentsfa, agencycommentsfa," +
    		" agencyperformancelm, batcommentslm, agencycommentslm, " +
            " creationby,   modificationby, creationdate, modificationdate, status, agencyid, agencyrating, agencycomment " +
            "  FROM grailprojecteval ";

    private static final String GET_PROJ_EVAL_BY_PROJECT_ID = LOAD_PROJ_EVAL + " where projectid = ? and endmarketid=? ";
    private static final String GET_PROJ_EVAL_BY_PROJECT_ID_END_MARKET_ID = LOAD_PROJ_EVAL + " where projectid = ?  ";
    
    private static final String GET_PROJ_EVAL_BY_PROJECT_ID_AGENCY_ID = LOAD_PROJ_EVAL + " where projectid = ? and agencyid=? ";

    private static final String UPDATE_PROJ_EVAL_BY_PROJECT_ID = "UPDATE grailprojecteval " +
            "   SET  agencyperformanceim=?, batcommentsim=?,agencycommentsim=?, agencyperformancelm=?, batcommentslm=? , agencycommentslm=?,  " +
            "   agencyperformancefa=?, batcommentsfa=?, agencycommentsfa=?," +
            "   modificationby=?, modificationdate=?, status=?  " +
            "   WHERE projectid = ? and endmarketid=? ";
    
    private static final String UPDATE_PROJ_EVAL_MIGRATION_BY_PROJECT_ID = "UPDATE grailprojecteval " +
            "   SET  agencyid=?, agencyrating=?, agencycomment=?  " +
            "   WHERE projectid = ? and endmarketid=? ";
  
    private static final String DELETE_PROJ_EVAL_BY_PROJECT_ID = "delete from  grailprojecteval  WHERE projectid = ?";
      
    @Override
    @Transactional(readOnly = false, propagation = Propagation.MANDATORY)
    public ProjectEvaluationInitiation save(final ProjectEvaluationInitiation projectEvaluationInitiation) {
        try {
            getSimpleJdbcTemplate().update(INSERT_PROJ_EVAL_SQL, projectEvaluationInitiation.getProjectID(), projectEvaluationInitiation.getEndMarketId(),
            		projectEvaluationInitiation.getAgencyPerfIM(),projectEvaluationInitiation.getBatCommentsIM(), projectEvaluationInitiation.getAgencyCommentsIM(),
            		projectEvaluationInitiation.getAgencyPerfLM(),projectEvaluationInitiation.getBatCommentsLM(),projectEvaluationInitiation.getAgencyCommentsLM(),
            		projectEvaluationInitiation.getAgencyPerfFA(),projectEvaluationInitiation.getBatCommentsFA(),projectEvaluationInitiation.getAgencyCommentsFA(),
            		projectEvaluationInitiation.getCreationBy(), projectEvaluationInitiation.getModifiedBy(), projectEvaluationInitiation.getCreationDate(), 
            		projectEvaluationInitiation.getModifiedDate(), projectEvaluationInitiation.getStatus(), projectEvaluationInitiation.getAgencyId(),
            		projectEvaluationInitiation.getAgencyRating(), projectEvaluationInitiation.getAgencyComments());
            return projectEvaluationInitiation;
        } catch (DataAccessException daEx) {
            final String message = "Failed to SAVE Project Evaluation Details for projectID" + projectEvaluationInitiation.getProjectID() +" and End Market Id -"+ projectEvaluationInitiation.getEndMarketId();
            LOG.log(Level.SEVERE, message, daEx);
            throw new DAOException(message, daEx);
        }
    	
    }
   
    @Override
    @Transactional(readOnly = false, propagation = Propagation.MANDATORY)
    public ProjectEvaluationInitiation update(final ProjectEvaluationInitiation projectEvaluationInitiation) {
       try{
           getSimpleJdbcTemplate().update( UPDATE_PROJ_EVAL_BY_PROJECT_ID,
        		   projectEvaluationInitiation.getAgencyPerfIM(),
        		   projectEvaluationInitiation.getBatCommentsIM(),
        		   projectEvaluationInitiation.getAgencyCommentsIM(),
        		   projectEvaluationInitiation.getAgencyPerfLM(),
        		   projectEvaluationInitiation.getBatCommentsLM(),
        		   projectEvaluationInitiation.getAgencyCommentsLM(),
        		   projectEvaluationInitiation.getAgencyPerfFA(),
        		   projectEvaluationInitiation.getBatCommentsFA(),
        		   projectEvaluationInitiation.getAgencyCommentsFA(),
        		   projectEvaluationInitiation.getModifiedBy(),
        		   projectEvaluationInitiation.getModifiedDate(),
        		   projectEvaluationInitiation.getStatus(),
                   //where clause
        		   projectEvaluationInitiation.getProjectID(),
        		   projectEvaluationInitiation.getEndMarketId());
           return projectEvaluationInitiation;
       } catch (DataAccessException daEx) {
    	   final String message = "Failed to Update Project Evaluation Details for projectID" + projectEvaluationInitiation.getProjectID() +" and End Market Id -"+ projectEvaluationInitiation.getEndMarketId();
           LOG.log(Level.SEVERE, message, daEx);
           throw new DAOException(message, daEx);
       }
    }

    @Override
    @Transactional(readOnly = false, propagation = Propagation.MANDATORY)
    public ProjectEvaluationInitiation updateProjectMigration(final ProjectEvaluationInitiation projectEvaluationInitiation) {
       try{
           getSimpleJdbcTemplate().update( UPDATE_PROJ_EVAL_MIGRATION_BY_PROJECT_ID,
        		   projectEvaluationInitiation.getAgencyId(),
        		   projectEvaluationInitiation.getAgencyRating(),
        		   projectEvaluationInitiation.getAgencyComments(),
                   //where clause
        		   projectEvaluationInitiation.getProjectID(),
        		   projectEvaluationInitiation.getEndMarketId());
           return projectEvaluationInitiation;
       } catch (DataAccessException daEx) {
    	   final String message = "Failed to Update Project Evaluation Details for Project Migration for projectID" + projectEvaluationInitiation.getProjectID() +" and End Market Id -"+ projectEvaluationInitiation.getEndMarketId();
           LOG.log(Level.SEVERE, message, daEx);
           throw new DAOException(message, daEx);
       }
    }
    
    @Override
    public List<ProjectEvaluationInitiation> getProjectEvaluationInitiation(final Long projectID, final Long endMarketId) {
        return getSimpleJdbcTemplate().query(GET_PROJ_EVAL_BY_PROJECT_ID, projectEvaluationInitiationRowMapper, projectID,endMarketId);
    }
    @Override
    public List<ProjectEvaluationInitiation> getProjectEvaluationInitiation(final Long projectID) {
        return getSimpleJdbcTemplate().query(GET_PROJ_EVAL_BY_PROJECT_ID_END_MARKET_ID, projectEvaluationInitiationRowMapper, projectID);
    }
    
    @Override
    public List<ProjectEvaluationInitiation> getProjectEvaluationInitiationAgency(final Long projectID, final Long agencyId) {
        return getSimpleJdbcTemplate().query(GET_PROJ_EVAL_BY_PROJECT_ID_AGENCY_ID, projectEvaluationInitiationRowMapper, projectID, agencyId);
    }
    
    @Override
    @Transactional(readOnly = false, propagation = Propagation.MANDATORY)
    public void deleteProjectEvaluationInitiation(final Long projectID) {
        try {
            getSimpleJdbcTemplate().getJdbcOperations().update(DELETE_PROJ_EVAL_BY_PROJECT_ID, projectID);
        }
        catch (DataAccessException e) {
            final String message = "Failed to delete the project Evaluation " + projectID;
           
            throw new DAOException(message, e);
        }
    }

      /**
     * Reusable row mapper for mapping a result set to Project Evaluation Initiation
     */
    private final RowMapper<ProjectEvaluationInitiation> projectEvaluationInitiationRowMapper = new RowMapper<ProjectEvaluationInitiation>() {
        public ProjectEvaluationInitiation mapRow(ResultSet rs, int row) throws SQLException {
        	ProjectEvaluationInitiation initiationBean = new ProjectEvaluationInitiation();
            initiationBean.setProjectID(rs.getLong("projectid"));
            initiationBean.setEndMarketId(rs.getLong("endmarketid"));
            initiationBean.setAgencyPerfIM(rs.getInt("agencyperformanceim"));
            initiationBean.setBatCommentsIM(rs.getString("batcommentsim"));
            initiationBean.setAgencyCommentsIM(rs.getString("agencycommentsim"));
            initiationBean.setAgencyPerfLM(rs.getInt("agencyperformancelm"));
            initiationBean.setBatCommentsLM(rs.getString("batcommentslm"));
            initiationBean.setAgencyCommentsLM(rs.getString("agencycommentslm"));
            
            initiationBean.setAgencyPerfFA(rs.getInt("agencyperformancefa"));
            initiationBean.setBatCommentsFA(rs.getString("batcommentsfa"));
            initiationBean.setAgencyCommentsFA(rs.getString("agencycommentsfa"));
            
            initiationBean.setStatus(rs.getInt("status"));
            initiationBean.setModifiedDate(rs.getLong("modificationDate"));
            initiationBean.setModifiedBy(rs.getLong("modificationby"));
            
            initiationBean.setAgencyId(rs.getLong("agencyId"));
            initiationBean.setAgencyRating(rs.getInt("agencyrating"));
            initiationBean.setAgencyComments(rs.getString("agencycomment"));
            
            
            return initiationBean;
        }
    };
   
   
}
