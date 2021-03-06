package com.grail.synchro.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.grail.synchro.util.SynchroUserPropertiesUtil;
import com.jivesoftware.base.User;
import org.apache.log4j.Logger;
import org.springframework.web.bind.ServletRequestDataBinder;

import com.grail.synchro.SynchroConstants;
import com.grail.synchro.SynchroGlobal;
import com.grail.synchro.beans.EmailNotificationDetailsBean;
import com.grail.synchro.beans.EndMarketInvestmentDetail;
import com.grail.synchro.beans.PIBMethodologyWaiver;
import com.grail.synchro.beans.PIBReporting;
import com.grail.synchro.beans.PIBStakeholderList;
import com.grail.synchro.beans.Project;
import com.grail.synchro.beans.ProjectInitiation;
import com.grail.synchro.beans.ProposalEndMarketDetails;
import com.grail.synchro.beans.ProposalInitiation;
import com.grail.synchro.beans.ProposalReporting;
import com.grail.synchro.beans.SynchroStageToDoListBean;
import com.grail.synchro.manager.EmailNotificationManager;
import com.grail.synchro.manager.PIBManager;
import com.grail.synchro.manager.ProjectManager;
import com.grail.synchro.manager.ProjectSpecsManager;
import com.grail.synchro.manager.ProposalManager;
import com.grail.synchro.manager.StageManager;
import com.grail.synchro.util.SynchroLogUtils;
import com.grail.synchro.util.SynchroPermHelper;
import com.grail.synchro.util.SynchroUtils;
import com.grail.util.DateUtils;
import com.grail.util.URLUtils;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.jivesoftware.base.UnauthorizedException;
import com.jivesoftware.base.UserNotFoundException;
import com.jivesoftware.community.AttachmentException;
import com.jivesoftware.community.JiveGlobals;
import com.jivesoftware.community.action.JiveActionSupport;
import com.jivesoftware.community.attachments.AttachmentHelper;
import com.jivesoftware.community.impl.dao.AttachmentBean;
import com.jivesoftware.community.mail.EmailMessage;
import com.jivesoftware.community.mail.util.TemplateUtil;
import com.jivesoftware.community.user.profile.ProfileFieldValue;
import com.jivesoftware.community.user.profile.ProfileManager;
import com.jivesoftware.util.InputStreamDataSource;
import com.jivesoftware.util.StringUtils;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.opensymphony.xwork2.Preparable;

/**
 * @author: tejinder
 * @since: 1.0
 */
public class ProposalAction extends JiveActionSupport implements Preparable {

    private static final Logger LOG = Logger.getLogger(ProposalAction.class);
    //Spring Managers
    private ProposalManager proposalManager;
    private ProjectManager synchroProjectManager;
    private PIBManager pibManager;

    //Form related fields
    private ProposalInitiation proposalInitiation;
    private Project project;
    private Long projectID;
    private Long agencyID;
    private Long parentAgencyID;
    private String attachmentName;

    private ProposalReporting proposalReporting;
    private ProposalEndMarketDetails proposalEMDetails;

    private ProposalInitiation proposalInitiation_DB;
    private ProposalReporting proposalReporting_DB;
    private ProposalEndMarketDetails proposalEMDetails_DB;
       
    private boolean isSave;

    private boolean editStage;

    private String notificationTabId;

    private String redirectURL;
    private String approve;
    private String recipients;
    private String subject;
    private String messageBody;
    private String subjectSendToProjOwner;
    private String messageBodySendToProjOwner;
    private String subjectSendToSPI;
    private String messageBodySendToSPI;

    List<SynchroStageToDoListBean> stageToDoList;
    private Integer stageId;

    private Map<String, String> approvers = new LinkedHashMap<String, String>();
    private StageManager stageManager;

    private AttachmentHelper attachmentHelper;
    private Long attachmentFieldID;
    private File attachFile;
    private String attachFileContentType;
    private String attachFileFileName;
    private Long attachmentId;
    private Long fieldCategoryId;
    //private List<Long> endMarketIds;
    private List<EndMarketInvestmentDetail> endMarketDetails;
    private Long endMarketId;
    // This field will contain the updated SingleMarketId in case the End market is changed
    private Long updatedSingleMarketId;
    //This map will contain the list of attachments for each field
    private Map<Integer, List<AttachmentBean>> attachmentMap = new HashMap<Integer, List<AttachmentBean>>();


    // This will contain  the Approvers data for the Checklist Tab
    Map<String,Map<String,String>> stageApprovers = new LinkedHashMap<String, Map<String,String>>();
    // This will containg the Agency Users
    Map<String,Long> agencyMap = new LinkedHashMap<String, Long>();
    Map<String,String> agencyDeptMap = new LinkedHashMap<String, String>();

    // This will contain all the Agency Contacts
    Map<String,Long> agencyContactMap = new LinkedHashMap<String, Long>();

    // This flag will check whether a proposal has been awarded to an agency or not
    private Boolean isProposalAwarded;
    private Map<Long,Long> attachmentUser;
    private Long proposalEndMarketId;
    private Boolean showMandatoryFieldsError;

    private String subjectSubmitProposal;
    private String messageBodySubmitProposal;
    private ProfileManager profileManager;

    private EmailNotificationManager emailNotificationManager;

    private PIBMethodologyWaiver proposalMethodologyWaiver;
    private ProjectSpecsManager projectSpecsManager;

    private File[] mailAttachment;
    private String[] mailAttachmentFileName;
    private String[] mailAttachmentContentType;
    private PIBReporting pibReporting = new PIBReporting();

    public void prepare() throws Exception {
        final String id = getRequest().getParameter("projectID");


        if(id != null ) {

            try{
                projectID = Long.parseLong(id);

            } catch (NumberFormatException nfEx) {
                LOG.error("Invalid ProjectID ");
                throw nfEx;
            }
            String validationError = getRequest().getParameter("validationError");
          /*  if(validationError!=null && validationError.equals("true"))
            {
                showMandatoryFieldsError=true;
            }
            */
            if(getRequest().getParameter("agencyID")!=null && !getRequest().getParameter("agencyID").equals(""))
            {
                agencyID = Long.parseLong(getRequest().getParameter("agencyID"));
                parentAgencyID = Long.parseLong(getRequest().getParameter("agencyID"));
            }
            if(getRequest().getParameter("endMarketId")!=null && !getRequest().getParameter("endMarketId").equals(""))
            {
                endMarketId = Long.parseLong(getRequest().getParameter("endMarketId"));
            }
            project = this.synchroProjectManager.get(projectID);


            // endMarketIds = this.synchroProjectManager.getEndMarketIDs(projectID);
            endMarketDetails = this.synchroProjectManager.getEndMarketDetails(projectID);

            pibReporting = this.pibManager.getPIBReporting(projectID);
            if(pibReporting == null) {
                pibReporting = new PIBReporting();
            }

            PIBStakeholderList pibStakeHolderList = this.pibManager.getPIBStakeholderList(projectID, endMarketDetails.get(0).getEndMarketID());
            boolean isExternalAgencyUser = SynchroPermHelper.isExternalAgencyUser(projectID, endMarketDetails.get(0).getEndMarketID());
            if(pibStakeHolderList!=null)
            {
                // List<ProposalInitiation> proposalAgencyList = this.proposalManager.getProposalDetails(projectID);
                //https://www.svn.sourcen.com/issues/17933
                List<Long> proposalAgencies = new ArrayList<Long>();
                /* for(ProposalInitiation pi:proposalAgencyList)
                {
                    // In case the Stakeholder is requested then we dont display the Agency in Proposal tab
                    if(!SynchroUtils.isReferenceID(pi.getAgencyID()))
                    {
                        proposalAgencies.add(pi.getAgencyID());
                    }
                }*/
                // In case the Stakeholder is requested then we dont display the Agency in Proposal tab
                if(pibStakeHolderList.getAgencyContact1()!=null && pibStakeHolderList.getAgencyContact1()>0  && !SynchroUtils.isReferenceID(pibStakeHolderList.getAgencyContact1()))
                {
                    proposalAgencies.add(pibStakeHolderList.getAgencyContact1());
                }
                if(pibStakeHolderList.getAgencyContact1Optional()!=null && pibStakeHolderList.getAgencyContact1Optional()>0  && !SynchroUtils.isReferenceID(pibStakeHolderList.getAgencyContact1Optional()))
                {
                    proposalAgencies.add(pibStakeHolderList.getAgencyContact1Optional());
                }
                if(pibStakeHolderList.getAgencyContact2()!=null && pibStakeHolderList.getAgencyContact2()>0  && !SynchroUtils.isReferenceID(pibStakeHolderList.getAgencyContact2()))
                {
                    proposalAgencies.add(pibStakeHolderList.getAgencyContact2());
                }
                if(pibStakeHolderList.getAgencyContact2Optional()!=null && pibStakeHolderList.getAgencyContact2Optional()>0  && !SynchroUtils.isReferenceID(pibStakeHolderList.getAgencyContact2Optional()))
                {
                    proposalAgencies.add(pibStakeHolderList.getAgencyContact2Optional());
                }
                if(pibStakeHolderList.getAgencyContact3()!=null && pibStakeHolderList.getAgencyContact3()>0  && !SynchroUtils.isReferenceID(pibStakeHolderList.getAgencyContact3()))
                {
                    proposalAgencies.add(pibStakeHolderList.getAgencyContact3());
                }
                if(pibStakeHolderList.getAgencyContact3Optional()!=null && pibStakeHolderList.getAgencyContact3Optional()>0  && !SynchroUtils.isReferenceID(pibStakeHolderList.getAgencyContact3Optional()))
                {
                    proposalAgencies.add(pibStakeHolderList.getAgencyContact3Optional());
                }
                if(isExternalAgencyUser)
                {
                    if(pibStakeHolderList.getAgencyContact1()!=null && pibStakeHolderList.getAgencyContact1() > 0 )
                    {
                        if((getUser().getID() == pibStakeHolderList.getAgencyContact1() && proposalAgencies.contains(pibStakeHolderList.getAgencyContact1()))
                                || hasAccessToAgencyDepartment(pibStakeHolderList.getAgencyContact1()))
                        {
                            agencyMap.put("Agency1", pibStakeHolderList.getAgencyContact1());
                            if(this.project.getAgencyDept() != null && this.project.getAgencyDept().intValue() > 0) {
                                agencyDeptMap.put("Agency1", SynchroGlobal.getDepartmentNameById(this.project.getAgencyDept().toString()));
                            } else {
                                Map<Long, ProfileFieldValue> profileFieldMap = profileManager.getProfile(userManager.getUser(pibStakeHolderList.getAgencyContact1()));
                                if(profileFieldMap!=null && profileFieldMap.get(2L)!=null) {
                                    String deptVal = profileFieldMap.get(2L).getValue();
                                    agencyDeptMap.put("Agency1", SynchroGlobal.getDepartmentNameById(deptVal));
                                } else {
                                    agencyDeptMap.put("Agency1", "Not Defined");
                                }
                            }
                            if(agencyID==null) {
                                agencyID= pibStakeHolderList.getAgencyContact1();
                            }
                            if(parentAgencyID==null) {
                                parentAgencyID= pibStakeHolderList.getAgencyContact1();
                            }
                        }
                    }
                    if(pibStakeHolderList.getAgencyContact1Optional() != null && pibStakeHolderList.getAgencyContact1Optional() > 0)
                    {
                        if((getUser().getID() == pibStakeHolderList.getAgencyContact1Optional() && proposalAgencies.contains(pibStakeHolderList.getAgencyContact1Optional()))
                                || hasAccessToAgencyDepartment(pibStakeHolderList.getAgencyContact1Optional()))
                        {
                            agencyMap.put("Agency1", pibStakeHolderList.getAgencyContact1Optional());

                            if(this.project.getAgencyDept() != null && this.project.getAgencyDept().intValue() > 0) {
                                agencyDeptMap.put("Agency1", SynchroGlobal.getDepartmentNameById(this.project.getAgencyDept().toString()));
                            } else {
                                Map<Long, ProfileFieldValue> profileFieldMap = profileManager.getProfile(userManager.getUser(pibStakeHolderList.getAgencyContact1()));
                                if(profileFieldMap!=null && profileFieldMap.get(2L)!=null) {
                                    String deptVal = profileFieldMap.get(2L).getValue();
                                    agencyDeptMap.put("Agency1", SynchroGlobal.getDepartmentNameById(deptVal));
                                } else {
                                    agencyDeptMap.put("Agency1", "Not Defined");
                                }
                            }
                            if(agencyID==null || agencyID.intValue()==pibStakeHolderList.getAgencyContact1Optional().intValue())
                            {
                                agencyID= pibStakeHolderList.getAgencyContact1();
                                parentAgencyID= pibStakeHolderList.getAgencyContact1();
                            }

                        }
                    }
                    if(pibStakeHolderList.getAgencyContact2()!=null && pibStakeHolderList.getAgencyContact2()>0)
                    {
                        if((getUser().getID()==pibStakeHolderList.getAgencyContact2() && proposalAgencies.contains(pibStakeHolderList.getAgencyContact2()))
                                || hasAccessToAgencyDepartment(pibStakeHolderList.getAgencyContact2()))
                        {
                            agencyMap.put("Agency2", pibStakeHolderList.getAgencyContact2());
                            Map<Long, ProfileFieldValue> profileFieldMap = profileManager.getProfile(userManager.getUser(pibStakeHolderList.getAgencyContact2()));
                            if(profileFieldMap!=null && profileFieldMap.get(2L)!=null)
                            {
                                String deptVal = profileFieldMap.get(2L).getValue();
                                agencyDeptMap.put("Agency2", SynchroGlobal.getDepartmentNameById(deptVal));
                            }
                            else
                            {
                                agencyDeptMap.put("Agency2", "Not Defined");
                            }
                            if(agencyID==null)
                            {
                                agencyID= pibStakeHolderList.getAgencyContact2();
                            }
                            if(parentAgencyID==null)
                            {
                                parentAgencyID= pibStakeHolderList.getAgencyContact2();
                            }
                        }
                    }

                    if(pibStakeHolderList.getAgencyContact2Optional()!=null && pibStakeHolderList.getAgencyContact2Optional()>0)
                    {
                        if((getUser().getID()==pibStakeHolderList.getAgencyContact2Optional() && proposalAgencies.contains(pibStakeHolderList.getAgencyContact2Optional()))
                                || hasAccessToAgencyDepartment(pibStakeHolderList.getAgencyContact2Optional()))
                        {
                            agencyMap.put("Agency2", pibStakeHolderList.getAgencyContact2Optional());
                            Map<Long, ProfileFieldValue> profileFieldMap = profileManager.getProfile(userManager.getUser(pibStakeHolderList.getAgencyContact2()));
                            if(profileFieldMap!=null && profileFieldMap.get(2L)!=null)
                            {
                                String deptVal = profileFieldMap.get(2L).getValue();
                                agencyDeptMap.put("Agency2", SynchroGlobal.getDepartmentNameById(deptVal));
                            }
                            else
                            {
                                agencyDeptMap.put("Agency2", "Not Defined");
                            }
                            if(agencyID==null || agencyID.intValue()==pibStakeHolderList.getAgencyContact2Optional().intValue())
                            {
                                agencyID= pibStakeHolderList.getAgencyContact2();
                            }
                            if(parentAgencyID==null)
                            {
                                parentAgencyID= pibStakeHolderList.getAgencyContact2();
                            }
                        }
                    }
                    if(pibStakeHolderList.getAgencyContact3()!=null && pibStakeHolderList.getAgencyContact3()>0)
                    {
                        if((getUser().getID()==pibStakeHolderList.getAgencyContact3() && proposalAgencies.contains(pibStakeHolderList.getAgencyContact3()))
                                || hasAccessToAgencyDepartment(pibStakeHolderList.getAgencyContact3()))
                        {
                            agencyMap.put("Agency3", pibStakeHolderList.getAgencyContact3());
                            Map<Long, ProfileFieldValue> profileFieldMap = profileManager.getProfile(userManager.getUser(pibStakeHolderList.getAgencyContact3()));
                            if(profileFieldMap!=null && profileFieldMap.get(2L)!=null)
                            {
                                String deptVal = profileFieldMap.get(2L).getValue();
                                agencyDeptMap.put("Agency3", SynchroGlobal.getDepartmentNameById(deptVal));
                            }
                            else
                            {
                                agencyDeptMap.put("Agency3", "Not Defined");
                            }
                            if(agencyID==null)
                            {
                                agencyID= pibStakeHolderList.getAgencyContact3();
                            }
                            if(parentAgencyID==null)
                            {
                                parentAgencyID= pibStakeHolderList.getAgencyContact3();
                            }
                        }
                    }
                    if(pibStakeHolderList.getAgencyContact3Optional()!=null && pibStakeHolderList.getAgencyContact3Optional()>0)
                    {
                        if((getUser().getID()==pibStakeHolderList.getAgencyContact3Optional() && proposalAgencies.contains(pibStakeHolderList.getAgencyContact3Optional()))
                                || hasAccessToAgencyDepartment(pibStakeHolderList.getAgencyContact3Optional()))
                        {
                            agencyMap.put("Agency3", pibStakeHolderList.getAgencyContact3Optional());
                            Map<Long, ProfileFieldValue> profileFieldMap = profileManager.getProfile(userManager.getUser(pibStakeHolderList.getAgencyContact3()));
                            if(profileFieldMap!=null && profileFieldMap.get(2L)!=null)
                            {
                                String deptVal = profileFieldMap.get(2L).getValue();
                                agencyDeptMap.put("Agency3", SynchroGlobal.getDepartmentNameById(deptVal));
                            }
                            else
                            {
                                agencyDeptMap.put("Agency3", "Not Defined");
                            }
                            if(agencyID==null || agencyID.intValue()==pibStakeHolderList.getAgencyContact3Optional().intValue())
                            {
                                agencyID= pibStakeHolderList.getAgencyContact3();
                            }
                            if(parentAgencyID==null)
                            {
                                parentAgencyID= pibStakeHolderList.getAgencyContact3();
                            }
                        }
                    }
                }
                else
                {
                    if(pibStakeHolderList.getAgencyContact1()!=null && pibStakeHolderList.getAgencyContact1()>0)
                    {
                        if(proposalAgencies.contains(pibStakeHolderList.getAgencyContact1()))
                        {
                            agencyMap.put("Agency1", pibStakeHolderList.getAgencyContact1());
                            agencyContactMap.put("Agency1", pibStakeHolderList.getAgencyContact1());
                            if(this.project.getAgencyDept() != null && this.project.getAgencyDept().intValue() > 0) {
                                agencyDeptMap.put("Agency1", SynchroGlobal.getDepartmentNameById(this.project.getAgencyDept().toString()));
                            } else {
                               
                                try
                                {
                                	Map<Long, ProfileFieldValue> profileFieldMap = profileManager.getProfile(userManager.getUser(pibStakeHolderList.getAgencyContact1()));
                                    if(profileFieldMap!=null && profileFieldMap.get(2L)!=null)
                                    {
                                        String deptVal = profileFieldMap.get(2L).getValue();
                                        agencyDeptMap.put("Agency1", SynchroGlobal.getDepartmentNameById(deptVal));
                                    }
                                    else
                                    {
                                        agencyDeptMap.put("Agency1", "Not Defined");
                                    }
                                }
                                catch(UserNotFoundException ue)
                                {
                                    LOG.error("User not found while Fetching Agency Profile ---"+ pibStakeHolderList.getAgencyContact1());
                                    agencyDeptMap.put("Agency1", "Not Defined");
                                }
                            /*	Map<Long, ProfileFieldValue> profileFieldMap = profileManager.getProfile(userManager.getUser(pibStakeHolderList.getAgencyContact1()));
                                if(profileFieldMap!=null && profileFieldMap.get(2L)!=null)
                                {
                                    String deptVal = profileFieldMap.get(2L).getValue();
                                    agencyDeptMap.put("Agency1", SynchroGlobal.getDepartmentNameById(deptVal));
                                }
                                else
                                {
                                    agencyDeptMap.put("Agency1", "Not Defined");
                                }*/
                            }
                            if(agencyID==null)
                            {
                                agencyID= pibStakeHolderList.getAgencyContact1();
                            }
                            if(parentAgencyID==null)
                            {
                                parentAgencyID= pibStakeHolderList.getAgencyContact1();
                            }
                        }
                    }
                    if(pibStakeHolderList.getAgencyContact1Optional()!=null && pibStakeHolderList.getAgencyContact1Optional()>0)
                    {
                        if(proposalAgencies.contains(pibStakeHolderList.getAgencyContact1Optional()))
                        {
                            agencyMap.put("Agency1", pibStakeHolderList.getAgencyContact1Optional());
                            agencyContactMap.put("Agency1Optional", pibStakeHolderList.getAgencyContact1Optional());
                            if(this.project.getAgencyDept() != null && this.project.getAgencyDept().intValue() > 0) {
                                agencyDeptMap.put("Agency1", SynchroGlobal.getDepartmentNameById(this.project.getAgencyDept().toString()));
                            } else {
                                Map<Long, ProfileFieldValue> profileFieldMap = profileManager.getProfile(userManager.getUser(pibStakeHolderList.getAgencyContact1()));
                                if(profileFieldMap!=null && profileFieldMap.get(2L)!=null)
                                {
                                    String deptVal = profileFieldMap.get(2L).getValue();
                                    agencyDeptMap.put("Agency1", SynchroGlobal.getDepartmentNameById(deptVal));
                                }
                                else
                                {
                                    agencyDeptMap.put("Agency1", "Not Defined");
                                }
                            }
                            if(agencyID==null || agencyID.intValue()==pibStakeHolderList.getAgencyContact1Optional().intValue())
                            {
                                agencyID= pibStakeHolderList.getAgencyContact1();
                                parentAgencyID= pibStakeHolderList.getAgencyContact1();
                            }
                            if(parentAgencyID==null)
                            {
                                parentAgencyID= pibStakeHolderList.getAgencyContact1();
                            }
                        }
                    }
                    if(pibStakeHolderList.getAgencyContact2()!=null && pibStakeHolderList.getAgencyContact2()>0)
                    {
                        if(proposalAgencies.contains(pibStakeHolderList.getAgencyContact2()))
                        {
                            agencyMap.put("Agency2", pibStakeHolderList.getAgencyContact2());
                            agencyContactMap.put("Agency2", pibStakeHolderList.getAgencyContact2());
                            Map<Long, ProfileFieldValue> profileFieldMap = profileManager.getProfile(userManager.getUser(pibStakeHolderList.getAgencyContact2()));
                            if(profileFieldMap!=null && profileFieldMap.get(2L)!=null)
                            {
                                String deptVal = profileFieldMap.get(2L).getValue();
                                agencyDeptMap.put("Agency2", SynchroGlobal.getDepartmentNameById(deptVal));
                            }
                            else
                            {
                                agencyDeptMap.put("Agency2", "Not Defined");
                            }
                            if(agencyID==null)
                            {
                                agencyID= pibStakeHolderList.getAgencyContact2();
                            }
                            if(parentAgencyID==null)
                            {
                                parentAgencyID= pibStakeHolderList.getAgencyContact2();
                            }
                        }
                    }
                    if(pibStakeHolderList.getAgencyContact2Optional()!=null && pibStakeHolderList.getAgencyContact2Optional()>0)
                    {
                        if(proposalAgencies.contains(pibStakeHolderList.getAgencyContact2Optional()))
                        {
                            agencyMap.put("Agency2", pibStakeHolderList.getAgencyContact2Optional());
                            agencyContactMap.put("Agency2Optional", pibStakeHolderList.getAgencyContact2Optional());
                            Map<Long, ProfileFieldValue> profileFieldMap = profileManager.getProfile(userManager.getUser(pibStakeHolderList.getAgencyContact2()));
                            if(profileFieldMap!=null && profileFieldMap.get(2L)!=null)
                            {
                                String deptVal = profileFieldMap.get(2L).getValue();
                                agencyDeptMap.put("Agency2", SynchroGlobal.getDepartmentNameById(deptVal));
                            }
                            else
                            {
                                agencyDeptMap.put("Agency2", "Not Defined");
                            }
                            if(agencyID==null || agencyID.intValue()==pibStakeHolderList.getAgencyContact2Optional().intValue())
                            {
                                agencyID= pibStakeHolderList.getAgencyContact2();
                                parentAgencyID= pibStakeHolderList.getAgencyContact2();
                            }
                            if(parentAgencyID==null)
                            {
                                parentAgencyID= pibStakeHolderList.getAgencyContact2();
                            }
                        }
                    }
                    if(pibStakeHolderList.getAgencyContact3()!=null && pibStakeHolderList.getAgencyContact3()>0)
                    {
                        if(proposalAgencies.contains(pibStakeHolderList.getAgencyContact3()))
                        {
                            agencyMap.put("Agency3", pibStakeHolderList.getAgencyContact3());
                            agencyContactMap.put("Agency3", pibStakeHolderList.getAgencyContact3());

                            Map<Long, ProfileFieldValue> profileFieldMap = profileManager.getProfile(userManager.getUser(pibStakeHolderList.getAgencyContact3()));
                            if(profileFieldMap!=null && profileFieldMap.get(2L)!=null)
                            {
                                String deptVal = profileFieldMap.get(2L).getValue();
                                agencyDeptMap.put("Agency3", SynchroGlobal.getDepartmentNameById(deptVal));
                            }
                            else
                            {
                                agencyDeptMap.put("Agency3", "Not Defined");
                            }
                            if(agencyID==null)
                            {
                                agencyID= pibStakeHolderList.getAgencyContact3();
                            }
                            if(parentAgencyID==null)
                            {
                                parentAgencyID= pibStakeHolderList.getAgencyContact3();
                            }
                        }
                    }
                    if(pibStakeHolderList.getAgencyContact3Optional()!=null && pibStakeHolderList.getAgencyContact3Optional()>0)
                    {
                        if(proposalAgencies.contains(pibStakeHolderList.getAgencyContact3Optional()))
                        {
                            agencyMap.put("Agency3", pibStakeHolderList.getAgencyContact3Optional());
                            agencyContactMap.put("Agency3Optional", pibStakeHolderList.getAgencyContact3Optional());

                            Map<Long, ProfileFieldValue> profileFieldMap = profileManager.getProfile(userManager.getUser(pibStakeHolderList.getAgencyContact3()));
                            if(profileFieldMap!=null && profileFieldMap.get(2L)!=null)
                            {
                                String deptVal = profileFieldMap.get(2L).getValue();
                                agencyDeptMap.put("Agency3", SynchroGlobal.getDepartmentNameById(deptVal));
                            }
                            else
                            {
                                agencyDeptMap.put("Agency3", "Not Defined");
                            }
                            if(agencyID==null || agencyID.intValue()==pibStakeHolderList.getAgencyContact3Optional().intValue())
                            {
                                agencyID= pibStakeHolderList.getAgencyContact3();
                                parentAgencyID= pibStakeHolderList.getAgencyContact3();
                            }
                            if(parentAgencyID==null)
                            {
                                parentAgencyID= pibStakeHolderList.getAgencyContact3();
                            }
                        }
                    }
                }
            }
            if(agencyID!=null && agencyID > 0)
            {

                List<ProposalInitiation> initiationList = this.proposalManager.getProposalDetails(projectID,agencyID);
                /*
                        proposalEndMarketId = initiationList.get(0).getEndMarketID();
                        attachmentMap = this.proposalManager.getDocumentAttachment(projectID, proposalEndMarketId,agencyID);
                        */
                
                //Parameters required for Audit Logs compare                
                List<ProposalInitiation> initiationList_DB = this.proposalManager.getProposalDetails(projectID,agencyID);
                if(initiationList_DB!=null && initiationList_DB.size() > 0)
                {
                	Long proposalEndMarketId_DB = initiationList.get(0).getEndMarketID();
                    proposalInitiation_DB = initiationList_DB.get(0);
                    proposalReporting_DB = this.proposalManager.getProposalReporting(projectID,proposalEndMarketId_DB,agencyID);
                    proposalEMDetails_DB = this.proposalManager.getProposalEMDetails(projectID,proposalEndMarketId_DB,agencyID);
                }
                
                
                if( initiationList != null && initiationList.size() > 0) {

                    proposalEndMarketId = initiationList.get(0).getEndMarketID();
                    attachmentMap = this.proposalManager.getDocumentAttachment(projectID, proposalEndMarketId,agencyID);

                    this.proposalInitiation = initiationList.get(0);
                    
                    // proposalReporting = this.proposalManager.getProposalReporting(projectID,endMarketDetails.get(0).getEndMarketID(),agencyID);
                    // proposalEMDetails = this.proposalManager.getProposalEMDetails(projectID,endMarketDetails.get(0).getEndMarketID(),agencyID);
                    proposalReporting = this.proposalManager.getProposalReporting(projectID,proposalEndMarketId,agencyID);
                    proposalEMDetails = this.proposalManager.getProposalEMDetails(projectID,proposalEndMarketId,agencyID);

                    /*
                             if(endMarketId!=null && endMarketId>0)
                             {
                                  attachmentMap = this.proposalManager.getDocumentAttachment(projectID, endMarketId,agencyID);

                             }
                             else
                             {
                                  attachmentMap = this.proposalManager.getDocumentAttachment(projectID, endMarketDetails.get(0).getEndMarketID(),agencyID);

                             }
                             */
                }  else {
                    this.proposalInitiation = new ProposalInitiation();

                    proposalEndMarketId = endMarketDetails.get(0).getEndMarketID();

                    isSave = true;
                }
                List<AttachmentBean> abList = new ArrayList<AttachmentBean>();
                for(Integer i : attachmentMap.keySet())
                {
                    abList.addAll(attachmentMap.get(i));
                }
                attachmentUser = pibManager.getAttachmentUser(abList);

                if(proposalReporting==null)
                {
                    proposalReporting=new ProposalReporting();
                }
                if(proposalEMDetails==null)
                {
                    proposalEMDetails=new ProposalEndMarketDetails();
                    proposalEMDetails.setProjectID(projectID);

                }
                // Moved from Input Action --
                //    String status=ribDocument.getProperties().get(SynchroConstants.STAGE_STATUS);
                stageId = SynchroGlobal.getProjectActivityTab().get("proposal");
                approvers = stageManager.getStageApprovers(stageId.longValue(), project);
                //	editStage=SynchroPermHelper.canEditStageDocument(ribDocument,projectID);
                editStage=true;
                //TODO - To add the project and stage status check over here whether the Proposal stage is completed or not.
                if(subject==null)
                {
                    //subject=String.format(SynchroGlobal.EmailNotification.PROPOSAL_REQ_CLARIFICATION.getSubject(),project.getName());
                    subject = TemplateUtil.getTemplate("proposal.request.clarification.subject", JiveGlobals.getLocale());
                    subject=subject.replaceAll("\\$\\{projectId\\}", SynchroUtils.generateProjectCode(projectID));
                    subject=subject.replaceAll("\\$\\{projectName\\}", Matcher.quoteReplacement(project.getName()));

                }
                String baseUrl = URLUtils.getBaseURL(request);
                if(messageBody==null)
                {

                    //String stageUrl = JiveGlobals.getDefaultBaseURL()+"/synchro/proposal-details!input.jspa?projectID=" + project.getProjectID();
                    String stageUrl = baseUrl+"/synchro/proposal-details!input.jspa?projectID=" + project.getProjectID();
                    //messageBody=String.format(SynchroGlobal.EmailNotification.PROPOSAL_REQ_CLARIFICATION.getMessageBody(),project.getName(),stageUrl,stageUrl);
                    messageBody = TemplateUtil.getHtmlEscapedTemplate("proposal.request.clarification.htmlBody", JiveGlobals.getLocale());
                    messageBody=messageBody.replaceAll("\\$\\{projectId\\}", SynchroUtils.generateProjectCode(projectID));
                    messageBody=messageBody.replaceAll("\\$\\{projectName\\}", Matcher.quoteReplacement(project.getName()));
                    messageBody=messageBody.replaceAll("\\$\\{stageUrl\\}", stageUrl);
                }
                if(subjectSendToProjOwner==null)
                {
                    //subjectSendToProjOwner=String.format(SynchroGlobal.EmailNotification.PROPOSAL_SEND_TO_PROJECT_OWNER.getSubject(),project.getName());
                    //subject=String.format(SynchroGlobal.EmailNotification.PROPOSAL_REQ_CLARIFICATION.getSubject(),project.getName());
                    subjectSendToProjOwner = TemplateUtil.getTemplate("proposal.send.to.projectowner.subject", JiveGlobals.getLocale());
                    subjectSendToProjOwner=subjectSendToProjOwner.replaceAll("\\$\\{projectId\\}", SynchroUtils.generateProjectCode(projectID));
                    subjectSendToProjOwner=subjectSendToProjOwner.replaceAll("\\$\\{projectName\\}", Matcher.quoteReplacement(project.getName()));

                }
                if(messageBodySendToProjOwner==null)
                {
                    //String stageUrl = JiveGlobals.getDefaultBaseURL()+"/synchro/proposal-details!input.jspa?projectID=" + project.getProjectID();
                    String stageUrl = baseUrl+"/synchro/proposal-details!input.jspa?projectID=" + project.getProjectID();
                    //messageBodySendToProjOwner=String.format(SynchroGlobal.EmailNotification.PROPOSAL_SEND_TO_PROJECT_OWNER.getMessageBody(),project.getName(),stageUrl,stageUrl);
                    messageBodySendToProjOwner = TemplateUtil.getHtmlEscapedTemplate("proposal.send.to.projectowner.htmlBody", JiveGlobals.getLocale());
                    messageBodySendToProjOwner=messageBodySendToProjOwner.replaceAll("\\$\\{projectId\\}", SynchroUtils.generateProjectCode(projectID));
                    messageBodySendToProjOwner=messageBodySendToProjOwner.replaceAll("\\$\\{projectName\\}", Matcher.quoteReplacement(project.getName()));
                    messageBodySendToProjOwner=messageBodySendToProjOwner.replaceAll("\\$\\{stageUrl\\}", stageUrl);
                }
                if(subjectSendToSPI==null)
                {
                    //subjectSendToSPI=String.format(SynchroGlobal.EmailNotification.PROPOSAL_SEND_TO_SPI.getSubject(),project.getName());
                    subjectSendToSPI = TemplateUtil.getTemplate("proposal.send.to.spi.subject", JiveGlobals.getLocale());
                    subjectSendToSPI=subjectSendToSPI.replaceAll("\\$\\{projectId\\}", SynchroUtils.generateProjectCode(projectID));
                    subjectSendToSPI=subjectSendToSPI.replaceAll("\\$\\{projectName\\}", Matcher.quoteReplacement(project.getName()));

                }
                if(messageBodySendToSPI==null)
                {
                    //String stageUrl = JiveGlobals.getDefaultBaseURL()+"/synchro/proposal-details!input.jspa?projectID=" + project.getProjectID();
                    String stageUrl = baseUrl+"/synchro/proposal-details!input.jspa?projectID=" + project.getProjectID();
                    //messageBodySendToSPI=String.format(SynchroGlobal.EmailNotification.PROPOSAL_SEND_TO_SPI.getMessageBody(),project.getName(),stageUrl,stageUrl);
                    messageBodySendToSPI = TemplateUtil.getHtmlEscapedTemplate("proposal.send.to.spi.htmlBody", JiveGlobals.getLocale());
                    messageBodySendToSPI=messageBodySendToSPI.replaceAll("\\$\\{projectId\\}", SynchroUtils.generateProjectCode(projectID));
                    messageBodySendToSPI=messageBodySendToSPI.replaceAll("\\$\\{projectName\\}", Matcher.quoteReplacement(project.getName()));
                    messageBodySendToSPI=messageBodySendToSPI.replaceAll("\\$\\{stageUrl\\}", stageUrl);
                }

                if(subjectSubmitProposal==null)
                {
                    //subject=String.format(SynchroGlobal.EmailNotification.PROPOSAL_REQ_CLARIFICATION.getSubject(),project.getName());
                    subjectSubmitProposal = TemplateUtil.getTemplate("agency.submit.proposal.subject", JiveGlobals.getLocale());
                    subjectSubmitProposal=subjectSubmitProposal.replaceAll("\\$\\{projectId\\}", SynchroUtils.generateProjectCode(projectID));
                    subjectSubmitProposal=subjectSubmitProposal.replaceAll("\\$\\{projectName\\}", Matcher.quoteReplacement(project.getName()));

                }
                if(messageBodySubmitProposal==null)
                {
                    //String stageUrl = JiveGlobals.getDefaultBaseURL()+"/synchro/proposal-details!input.jspa?projectID=" + project.getProjectID();
                    String stageUrl = baseUrl+"/synchro/proposal-details!input.jspa?projectID=" + project.getProjectID();
                    //messageBody=String.format(SynchroGlobal.EmailNotification.PROPOSAL_REQ_CLARIFICATION.getMessageBody(),project.getName(),stageUrl,stageUrl);
                    messageBodySubmitProposal = TemplateUtil.getHtmlEscapedTemplate("agency.submit.proposal.htmlBody", JiveGlobals.getLocale());
                    messageBodySubmitProposal=messageBodySubmitProposal.replaceAll("\\$\\{projectId\\}", SynchroUtils.generateProjectCode(projectID));
                    messageBodySubmitProposal=messageBodySubmitProposal.replaceAll("\\$\\{projectName\\}", Matcher.quoteReplacement(project.getName()));
                    messageBodySubmitProposal=messageBodySubmitProposal.replaceAll("\\$\\{stageUrl\\}", stageUrl);

                }


                if(recipients==null)
                {
                    //recipients="";
                    if(agencyMap!=null && agencyMap.size()>0)
                    {
                        if(agencyMap.containsKey("Agency1"))
                        {
                            try
                            {
                            	recipients=userManager.getUser(agencyMap.get("Agency1")).getEmail();
                            }
                            catch(UserNotFoundException ue)
                            {
                                LOG.error("User not found while fetching recipients --- "+ agencyMap.get("Agency1"));
                                recipients="";
                            }
                        }
                        if(agencyMap.containsKey("Agency2"))
                        {
                            if(recipients!=null && recipients.length()>0)
                            {
                                recipients=recipients+","+userManager.getUser(agencyMap.get("Agency2")).getEmail();
                            }
                            else
                            {
                                recipients=userManager.getUser(agencyMap.get("Agency2")).getEmail();
                            }
                        }
                        if(agencyMap.containsKey("Agency3"))
                        {
                            if(recipients!=null && recipients.length()>0)
                            {
                                recipients=recipients+","+userManager.getUser(agencyMap.get("Agency3")).getEmail();
                            }
                            else
                            {
                                recipients=userManager.getUser(agencyMap.get("Agency3")).getEmail();
                            }
                        }
                    }
                }
                List<ProposalInitiation> iniList = this.proposalManager.getProposalDetails(projectID);
                if(iniList!=null && iniList.size()>0)
                {
                    for(ProposalInitiation pi:iniList)
                    {
                        if(pi.getIsAwarded())
                        {
                            setIsProposalAwarded(pi.getIsAwarded());
                        }
                    }

                }

            }
        }

        if(this.proposalInitiation!=null)
            proposalMethodologyWaiver = this.pibManager.getPIBMethodologyWaiver(projectID, this.proposalInitiation.getEndMarketID());

        // Contenttype check is required to skip the below binding in case odf adding attachments
        if(getRequest().getMethod() == "POST" && !getRequest().getContentType().startsWith("multipart/form-data") && getRequest().getParameter("attachmentId")==null) {
            ServletRequestDataBinder binder = new ServletRequestDataBinder(this.proposalInitiation);
            binder.bind(getRequest());
            if(binder.getBindingResult().hasErrors()){
                LOG.debug("Error occurred while binding the request object with the Proposal Initiation bean.");
                input();
            }

//            if(this.proposalInitiation.getMethodologyGroup() != null && this.proposalInitiation.getMethodologyGroup().intValue() > 0) {
//                Long mtId = SynchroGlobal.getMethodologyTypeByGroup(this.proposalInitiation.getMethodologyGroup());
//                this.proposalInitiation.setMethodologyType(mtId);
//            }

            if(this.proposalInitiation.getProposedMethodology() != null && this.proposalInitiation.getProposedMethodology().size() > 0) {
                this.proposalInitiation.setMethodologyType(SynchroGlobal.getMethodologyTypeByProsedMethodologies(this.proposalInitiation.getProposedMethodology()));
            }

            if(proposalInitiation.getProjectOwner()==null && getRequest().getParameter("projectOwner")!=null)
            {
                proposalInitiation.setProjectOwner(Long.parseLong(getRequest().getParameter("projectOwner")));
            }

            if(StringUtils.isNotBlank(getRequest().getParameter("startDate"))) {
                proposalInitiation.setStartDate(DateUtils.parse(getRequest().getParameter("startDate")));
            }
            if(StringUtils.isNotBlank(getRequest().getParameter("endDate"))) {
                proposalInitiation.setEndDate(DateUtils.parse(getRequest().getParameter("endDate")));
            }
            if(StringUtils.isNotBlank(getRequest().getParameter("stimuliDate"))) {
                proposalInitiation.setStimuliDate(DateUtils.parse(getRequest().getParameter("stimuliDate")));
            }
            // To map the Project Parameters
            /*  binder = new ServletRequestDataBinder(this.project);
            binder.bind(getRequest());
            if(binder.getBindingResult().hasErrors()){
                LOG.debug("Error occurred while binding the request object with the Project bean.");
                input();
            }*/
            // To map the Agency End Market Details
            /*     boolean isTotalCostZero = false;
            
            if(proposalEMDetails.getTotalCost()!=null && proposalEMDetails.getTotalCost().intValue()==0)
            {
                isTotalCostZero = true;
            }*/
            binder = new ServletRequestDataBinder(this.proposalEMDetails);
            binder.bind(getRequest());
            if(binder.getBindingResult().hasErrors()){
                LOG.debug("Error occurred while binding the request object with the Proposal End Market Detail bean.");
                /* if(proposalInitiation.getMethodologyType().intValue()==1 || proposalInitiation.getMethodologyType().intValue()==2 || proposalInitiation.getMethodologyType().intValue()==3 ||  proposalInitiation.getMethodologyType().intValue()==5)
                {
                    if(proposalEMDetails.getTotalCost()==null)
                    {
                        proposalEMDetails.setTotalCost(BigDecimal.valueOf(new Integer("0")));
                    }
                    if(proposalEMDetails.getIntMgmtCost()==null)
                    {
                        proposalEMDetails.setIntMgmtCost(BigDecimal.valueOf(new Integer("0")));
                    }
                    if(proposalEMDetails.getLocalMgmtCost()==null)
                    {
                        proposalEMDetails.setLocalMgmtCost(BigDecimal.valueOf(new Integer("0")));
                    }
                    if(proposalEMDetails.getFieldworkCost()==null)
                    {
                        proposalEMDetails.setFieldworkCost(BigDecimal.valueOf(new Integer("0")));
                    }

                    if(proposalEMDetails.getOtherCost()==null)
                    {
                        proposalEMDetails.setOtherCost(BigDecimal.valueOf(new Integer("0")));
                    }
                    if(proposalEMDetails.getOperationalHubCost()==null)
                    {
                        proposalEMDetails.setOperationalHubCost(BigDecimal.valueOf(new Integer("0")));
                    }
                }
                if(proposalInitiation.getMethodologyType().intValue()==1 || proposalInitiation.getMethodologyType().intValue()==3)
                {
                    if(proposalEMDetails.getTotalNoInterviews()==null)
                    {
                        proposalEMDetails.setTotalNoInterviews(new Integer("0"));
                    }
                    if(proposalEMDetails.getTotalNoOfVisits()==null)
                    {
                        proposalEMDetails.setTotalNoOfVisits(new Integer("0"));
                    }
                    if(proposalEMDetails.getAvIntDuration()==null)
                    {
                        proposalEMDetails.setAvIntDuration(new Integer("0"));
                    }
                }
                if(proposalInitiation.getMethodologyType().intValue()==2 || proposalInitiation.getMethodologyType().intValue()==3)
                {
                    if(proposalEMDetails.getTotalNoOfGroups()==null)
                    {
                        proposalEMDetails.setTotalNoOfGroups(new Integer("0"));
                    }
                    if(proposalEMDetails.getInterviewDuration()==null)
                    {
                        proposalEMDetails.setInterviewDuration(new Integer("0"));
                    }
                    if(proposalEMDetails.getNoOfRespPerGroup()==null)
                    {
                        proposalEMDetails.setNoOfRespPerGroup(new Integer("0"));
                    }
                }
                if(proposalInitiation.getMethodologyType().intValue()==4)
                {
                    if(proposalEMDetails.getTotalCost()==null)
                    {
                        proposalEMDetails.setTotalCost(BigDecimal.valueOf(new Integer("0")));
                    }
                }*/
                input();
            }
            /* if(proposalEMDetails.getTotalCost()==null && isTotalCostZero)
            {
                proposalEMDetails.setTotalCost(BigDecimal.valueOf(new Integer("0")));
            }*/
            //   if(getRequest().getParameterValues("dataCollectionMethod_"+endMarketDetails.get(0).getEndMarketID()) ==null)
            if(getRequest().getParameterValues("dataCollectionMethod_"+proposalEndMarketId) ==null)
            {
                proposalEMDetails.setDataCollectionMethod(null);
            }
            if(getRequest().getParameterValues("dataCollectionMethod_"+proposalEndMarketId) !=null)
            {
                String[] dataCollection = getRequest().getParameterValues("dataCollectionMethod_"+proposalEndMarketId);
                List<Long> dataColl = new ArrayList<Long>();
                for(int j=0;j<dataCollection.length;j++)
                {
                    dataColl.add(new Long(dataCollection[j]));
                }
                proposalEMDetails.setDataCollectionMethod(dataColl);
            }

            if(getRequest().getParameter("updatedSingleMarketId")!=null)
            {
                updatedSingleMarketId = Long.valueOf(getRequest().getParameter("updatedSingleMarketId"));
                //TODO : Currently this is done to update the Single End Market for grailPIB,
                // grailPIBReporting and grailpibstakeholderlist and grailendmarketinvestment tables
                // Need to revisit this logic in case of Multiple End Markets
                proposalInitiation.setEndMarketID(updatedSingleMarketId);
                proposalEMDetails.setEndMarketID(updatedSingleMarketId);
            }
            else
            {
                //proposalEMDetails.setEndMarketID(endMarketDetails.get(0).getEndMarketID());
                proposalEMDetails.setEndMarketID(proposalEndMarketId);
            }
            if(getRequest().getParameter("geoSpread")!=null && getRequest().getParameter("geoSpread").equals("geoSpreadNational"))
            {
                proposalEMDetails.setGeoSpreadNational(true);
                proposalEMDetails.setGeoSpreadUrban(false);
            }
            else if(getRequest().getParameter("geoSpread")!=null && getRequest().getParameter("geoSpread").equals("geoSpreadUrban"))
            {
                proposalEMDetails.setGeoSpreadNational(false);
                proposalEMDetails.setGeoSpreadUrban(true);
            }

            if(StringUtils.isNotBlank(getRequest().getParameter("fwStartDate"))) {
                proposalEMDetails.setFwStartDate(DateUtils.parse(getRequest().getParameter("fwStartDate")));
            }
            if(StringUtils.isNotBlank(getRequest().getParameter("fwEndDate"))) {
                proposalEMDetails.setFwEndDate(DateUtils.parse(getRequest().getParameter("fwEndDate")));
            }



        }
    }

    private boolean hasAccessToAgencyDepartment(final Long agencyID) throws UserNotFoundException {
        boolean hasAccess = false;
        Map<Long, ProfileFieldValue> agencyProfileProperties = profileManager.getProfile(userManager.getUser(agencyID));
        if(agencyProfileProperties != null && agencyProfileProperties.get(2L) != null) {
            String deptVal = agencyProfileProperties.get(2L).getValue();
            if(deptVal != null && !deptVal.equals("")) {
                Map<String, String> userProperties = getUser().getProperties();
                if(userProperties.containsKey(SynchroUserPropertiesUtil.GRAIL_USER_PROFILE_DEPARTMENTS)
                        && userProperties.get(SynchroUserPropertiesUtil.GRAIL_USER_PROFILE_DEPARTMENTS) != null
                        && !userProperties.get(SynchroUserPropertiesUtil.GRAIL_USER_PROFILE_DEPARTMENTS).equals("")) {
                    String departmentStr = userProperties.get(SynchroUserPropertiesUtil.GRAIL_USER_PROFILE_DEPARTMENTS);
                    String [] departmentAccessList = departmentStr.split(",");
                    for(String departmentAccessId : departmentAccessList) {
                        if(deptVal.equals(departmentAccessId)) {
                            hasAccess = true;
                            break;
                        }
                    }
                }
            }
        }
        return hasAccess;

    }

    public String input() {
        // This will check whether the user has accepted the Disclaimer or not.
//        if(!(getSession().get(SynchroConstants.SYNCHRO_DISCLAIMER_KEY)!=null && ((String)getSession().get(SynchroConstants.SYNCHRO_DISCLAIMER_KEY)).equals("yes")))
//        {
//            return "disclaimer-error";
//        }
        if(SynchroPermHelper.isSynchroCommunicationAgencyAdmin())
        {
            return "proposal-unaccessible";
        }
        if (SynchroPermHelper.hasProjectAccess(projectID) || SynchroPermHelper.canAccessProject(projectID)) {
            //https://www.svn.sourcen.com/issues/17910
            if(SynchroPermHelper.isExternalAgencyUser(projectID, endMarketDetails.get(0).getEndMarketID()))
            {
                if(agencyID != null) {
                    if(getUser().getID() == agencyID
                            || agencyMap.get("Agency1")!=null && agencyMap.get("Agency1")==getUser().getID()
                            || agencyMap.get("Agency2")!=null && agencyMap.get("Agency2")==getUser().getID()
                            || agencyMap.get("Agency3")!=null && agencyMap.get("Agency3")==getUser().getID()) {
                        return INPUT;
                    }

                } else if(SynchroPermHelper.isExternalAgencyUser(getUser())) {
                    return INPUT;
                } else {
                    return UNAUTHORIZED;
                }
            }
            return INPUT;
        }
        else
        {
            return UNAUTHORIZED;
        }

    }

    public String execute(){

        LOG.info("Save the Proposal Details ...."+proposalInitiation);
        Boolean manFieldsError = false;
        //  if( projectInitiation != null && ribDocument != null){
        if( proposalInitiation != null){
            proposalInitiation.setProjectID(projectID);


            //   this.synchroProjectManager.save(project);
            if(updatedSingleMarketId!=null)
            {
                //update Endmarket in grailProposal
                //update Endmarket in grailproposalendmarketdetails
                //update Endmarket in grailproposalreporting
                this.proposalManager.updateProposalEndMarketId(projectID, agencyID, updatedSingleMarketId);
                proposalInitiation.setEndMarketID(updatedSingleMarketId);
                proposalEMDetails.setEndMarketID(updatedSingleMarketId);
                //	this.synchroProjectManager.updateSingleEndMarketId(projectID, updatedSingleMarketId);
                //https://www.svn.sourcen.com/issues/18000
                if(attachmentUser!=null && attachmentUser.size()>0)
                {
                    for(Long attId:attachmentUser.keySet())
                    {
                        this.proposalManager.updateDocumentAttachment(attId, projectID, agencyID, updatedSingleMarketId);
                    }
                }

            }
            //  proposalInitiation.setStatus(SynchroGlobal.StageStatus.PROPOSAL_SAVED.ordinal());
            if((proposalInitiation.getBizQuestion()!=null && !proposalInitiation.getBizQuestion().equals("") || attachmentMap.get(SynchroGlobal.SynchroAttachmentObject.BUSINESS_QUESTION.getId())!=null)
                    && (proposalInitiation.getResearchObjective()!=null && !proposalInitiation.getResearchObjective().equals("") || attachmentMap.get(SynchroGlobal.SynchroAttachmentObject.RESEARCH_OBJECTIVE.getId())!=null)
                    && (proposalInitiation.getActionStandard()!=null && !proposalInitiation.getActionStandard().equals("") || attachmentMap.get(SynchroGlobal.SynchroAttachmentObject.ACTION_STANDARD.getId())!=null)
                    && (proposalInitiation.getResearchDesign()!=null && !proposalInitiation.getResearchDesign().equals("") || attachmentMap.get(SynchroGlobal.SynchroAttachmentObject.RESEARCH_DESIGN.getId())!=null)
                    && (proposalInitiation.getSampleProfile()!=null && !proposalInitiation.getSampleProfile().equals("") || attachmentMap.get(SynchroGlobal.SynchroAttachmentObject.SAMPLE_PROFILE.getId())!=null)
                    && (proposalInitiation.getStimulusMaterial()!=null && !proposalInitiation.getStimulusMaterial().equals("") || attachmentMap.get(SynchroGlobal.SynchroAttachmentObject.STIMULUS_MATERIAL.getId())!=null)
                    && (proposalInitiation.getStimulusMaterialShipped()!=null && !proposalInitiation.getStimulusMaterialShipped().equals("") || attachmentMap.get(SynchroGlobal.SynchroAttachmentObject.STIMULUS_MATERIAL_SHIPPED.getId())!=null)
                    //&& proposalInitiation.getOthers()!=null && !proposalInitiation.getOthers().equals("")

                    && (proposalInitiation.getTopLinePresentation()!=null && proposalInitiation.getTopLinePresentation()|| proposalInitiation.getPresentation()!=null && proposalInitiation.getPresentation()
                    || proposalInitiation.getFullreport()!=null && proposalInitiation.getFullreport())
                    //	&& proposalInitiation.getOtherReportingRequirements()!=null && !proposalInitiation.getOtherReportingRequirements().equals("")
                    && (proposalInitiation.getProposalCostTemplate()!=null && !proposalInitiation.getProposalCostTemplate().equals("") || attachmentMap.get(SynchroGlobal.SynchroAttachmentObject.PROP_COST_TEMPLATE.getId())!=null)
                    )

            {
                // Quantitative
                if(proposalInitiation.getMethodologyType().intValue()==1)
                {
                    if(proposalEMDetails.getTotalCost()!=null && proposalEMDetails.getIntMgmtCost()!=null && proposalEMDetails.getLocalMgmtCost()!=null
                            && proposalEMDetails.getFieldworkCost()!=null
                            && proposalEMDetails.getProposedFWAgencyNames()!=null && !proposalEMDetails.getProposedFWAgencyNames().equals("")
                            && proposalEMDetails.getFwEndDate()!=null && proposalEMDetails.getFwStartDate()!=null
                            && proposalEMDetails.getDataCollectionMethod()!=null
                            && proposalEMDetails.getTotalNoInterviews()!=null && proposalEMDetails.getTotalNoInterviews().intValue()>-1
                            && proposalEMDetails.getTotalNoOfVisits()!=null && proposalEMDetails.getTotalNoOfVisits().intValue()>-1
                            && proposalEMDetails.getAvIntDuration()!=null && proposalEMDetails.getAvIntDuration().intValue()>-1)
                    {
                        proposalInitiation.setStatus(SynchroGlobal.StageStatus.PROPOSAL_SAVED.ordinal());
                    }
                    else
                    {
                        proposalInitiation.setStatus(SynchroGlobal.StageStatus.PROPOSAL_STARTED.ordinal());
                        manFieldsError=true;
                    }
                }
                // Qualitative
                else if(proposalInitiation.getMethodologyType().intValue()==2)
                {
                    if(proposalEMDetails.getTotalCost()!=null && proposalEMDetails.getIntMgmtCost()!=null && proposalEMDetails.getLocalMgmtCost()!=null
                            && proposalEMDetails.getFieldworkCost()!=null
                            && proposalEMDetails.getProposedFWAgencyNames()!=null && !proposalEMDetails.getProposedFWAgencyNames().equals("")
                            && proposalEMDetails.getFwEndDate()!=null && proposalEMDetails.getFwStartDate()!=null
                            && proposalEMDetails.getDataCollectionMethod()!=null
                            && proposalEMDetails.getTotalNoOfGroups()!=null && proposalEMDetails.getTotalNoOfGroups().intValue()>-1
                            && proposalEMDetails.getInterviewDuration()!=null && proposalEMDetails.getInterviewDuration().intValue()>-1
                            && proposalEMDetails.getNoOfRespPerGroup()!=null && proposalEMDetails.getNoOfRespPerGroup().intValue()>-1)
                    {
                        proposalInitiation.setStatus(SynchroGlobal.StageStatus.PROPOSAL_SAVED.ordinal());
                    }
                    else
                    {
                        proposalInitiation.setStatus(SynchroGlobal.StageStatus.PROPOSAL_STARTED.ordinal());
                        manFieldsError=true;
                    }
                }
                //Quant & Qual
                else if(proposalInitiation.getMethodologyType().intValue()==3)
                {
                    if(proposalEMDetails.getTotalCost()!=null && proposalEMDetails.getIntMgmtCost()!=null && proposalEMDetails.getLocalMgmtCost()!=null
                            && proposalEMDetails.getFieldworkCost()!=null
                            && proposalEMDetails.getProposedFWAgencyNames()!=null && !proposalEMDetails.getProposedFWAgencyNames().equals("")
                            && proposalEMDetails.getFwEndDate()!=null && proposalEMDetails.getFwStartDate()!=null
                            && proposalEMDetails.getDataCollectionMethod()!=null
                            && proposalEMDetails.getTotalNoInterviews()!=null && proposalEMDetails.getTotalNoInterviews().intValue()>-1
                            && proposalEMDetails.getTotalNoOfVisits()!=null && proposalEMDetails.getTotalNoOfVisits().intValue()>-1
                            && proposalEMDetails.getAvIntDuration()!=null && proposalEMDetails.getAvIntDuration().intValue()>-1
                            && proposalEMDetails.getTotalNoOfGroups()!=null && proposalEMDetails.getTotalNoOfGroups().intValue()>-1
                            && proposalEMDetails.getInterviewDuration()!=null && proposalEMDetails.getInterviewDuration().intValue()>-1
                            && proposalEMDetails.getNoOfRespPerGroup()!=null && proposalEMDetails.getNoOfRespPerGroup().intValue()>-1)
                    {
                        proposalInitiation.setStatus(SynchroGlobal.StageStatus.PROPOSAL_SAVED.ordinal());
                    }
                    else
                    {
                        proposalInitiation.setStatus(SynchroGlobal.StageStatus.PROPOSAL_STARTED.ordinal());
                        manFieldsError=true;
                    }
                }
                //Desk Research/Advanced Analytics
                else
                {
                    if(proposalEMDetails.getTotalCost()!=null )
                    {
                        proposalInitiation.setStatus(SynchroGlobal.StageStatus.PROPOSAL_SAVED.ordinal());
                    }
                    else
                    {
                        proposalInitiation.setStatus(SynchroGlobal.StageStatus.PROPOSAL_STARTED.ordinal());
                        manFieldsError=true;
                    }
                }

            }
            else
            {
                proposalInitiation.setStatus(SynchroGlobal.StageStatus.PROPOSAL_STARTED.ordinal());
                manFieldsError=true;
            }

            // https://svn.sourcen.com/issues/19652

            proposalInitiation.setBizQuestion(SynchroUtils.fixBulletPoint(proposalInitiation.getBizQuestion()));
            proposalInitiation.setResearchObjective(SynchroUtils.fixBulletPoint(proposalInitiation.getResearchObjective()));
            proposalInitiation.setActionStandard(SynchroUtils.fixBulletPoint(proposalInitiation.getActionStandard()));
            proposalInitiation.setResearchDesign(SynchroUtils.fixBulletPoint(proposalInitiation.getResearchDesign()));
            proposalInitiation.setSampleProfile(SynchroUtils.fixBulletPoint(proposalInitiation.getSampleProfile()));
            proposalInitiation.setStimulusMaterial(SynchroUtils.fixBulletPoint(proposalInitiation.getStimulusMaterial()));
            proposalInitiation.setOthers(SynchroUtils.fixBulletPoint(proposalInitiation.getOthers()));
            proposalInitiation.setOtherReportingRequirements(SynchroUtils.fixBulletPoint(proposalInitiation.getOtherReportingRequirements()));
            proposalInitiation.setStimulusMaterialShipped(SynchroUtils.fixBulletPoint(proposalInitiation.getStimulusMaterialShipped()));
            proposalInitiation.setProposalCostTemplate(SynchroUtils.fixBulletPoint(proposalInitiation.getProposalCostTemplate()));
            
            proposalInitiation.setProposalSaveDate(new Date(System.currentTimeMillis()));

            if(isSave) {
                proposalInitiation.setCreationBy(getUser().getID());
                proposalInitiation.setCreationDate(System.currentTimeMillis());

                proposalInitiation.setModifiedBy(getUser().getID());
                proposalInitiation.setModifiedDate(System.currentTimeMillis());

                // These below changes are done in case the Admin user changes Agency Contacts once the Proposal is Awarded.
                // https://www.svn.sourcen.com/issues/19286
                proposalInitiation.setProjectOwner(project.getProjectOwner());
                proposalInitiation.setSpiContact(endMarketDetails.get(0).getSpiContact());
                proposalInitiation.setCapRating(project.getCapRating());
                proposalInitiation.setProposedMethodology(project.getProposedMethodology());


                this.proposalManager.saveProposalDetails(proposalInitiation);
                this.proposalManager.saveProposalEMDetails(proposalEMDetails);
                
                //Audit Logs : Proposal Save
                SynchroLogUtils.ProposalSave(proposalInitiation_DB, proposalInitiation, project, proposalReporting_DB, proposalEMDetails, proposalEMDetails_DB);

            }
            else {
                proposalInitiation.setModifiedBy(getUser().getID());
                proposalInitiation.setModifiedDate(System.currentTimeMillis());
                this.proposalManager.updateProposalDetails(proposalInitiation);
                this.proposalManager.updateProposalEMDetails(proposalEMDetails);
                
                //Audit Logs: Proposal Update
                SynchroLogUtils.ProposalSave(proposalInitiation_DB, proposalInitiation, project, proposalReporting_DB, proposalEMDetails, proposalEMDetails_DB);
            }


        } else {
            LOG.error("Project Initiation was null  ");
            LOG.error("RIB Document has not been configured during the Wizard setup.");
            addActionError("RIB document missing.");
        }
        
      //Audit Logs: Proosal SAVE
        String i18Text = getText("logger.project.saved.text");
        SynchroLogUtils.addLog(SynchroGlobal.PortalType.SYNCHRO.getDescription(), SynchroGlobal.PageType.PROJECT.getId(), SynchroGlobal.Activity.EDIT.getId(), 
								SynchroGlobal.LogProjectStage.PROPOSAL.getId(), i18Text, project.getName(), 
										project.getProjectID(), getUser().getID());
     /*   if(manFieldsError)
        {
            redirectURL="/synchro/proposal-details!input.jspa?projectID="+projectID+"&validationError=true";
            return "validationError";
        }
       */ 
      
        return SUCCESS;
    }


    /**
     * This method will perform the notification activities for the To Do List Actions for each stage.
     *
     */
    public String sendNotification() {

        String baseUrl = URLUtils.getBaseURL(request);
        // This is done for Request Clarification action for https://www.svn.sourcen.com/issues/17655
        if(notificationTabId!=null && notificationTabId.equalsIgnoreCase("PROPOSAL_REQ_CLARIFICATION"))
        {
        	StringBuffer emaillist = new StringBuffer();
            //rejectAgency();
            //this.proposalManager.rejectAgency(project, agencyID,endMarketId);

            // This check has been done for Admin user. In case the Proposal is Awarded and then Admin user removes one of the fields and then reject the
            // proposal. In that case the SAVE and SUBMIT Proposal button is still enabled for Admin and Agency user.
            if(proposalInitiation.getStatus()==SynchroGlobal.StageStatus.PROPOSAL_STARTED.ordinal())
            {
                this.proposalManager.rejectAgency(project, agencyID,endMarketId,SynchroGlobal.StageStatus.PROPOSAL_STARTED.ordinal());
            }
            else
            {
                this.proposalManager.rejectAgency(project, agencyID,endMarketId,SynchroGlobal.StageStatus.PROPOSAL_SAVED.ordinal());
            }
            this.proposalManager.updateRequestClarificationModification(projectID, agencyID, 1);
            this.proposalManager.updateSendToProjectOwner(projectID, agencyID, null);
            EmailMessage email = stageManager.populateNotificationEmail(recipients, subject, messageBody,"proposal.request.clarification.htmlBody","proposal.request.clarification.subject");
            emaillist.append(recipients);
            //email.getContext().put("projectId", projectID);
            email.getContext().put("projectId", SynchroUtils.generateProjectCode(projectID));
            email.getContext().put("projectName",project.getName());
            //	email.getContext().put("agencyName", userManager.getUser(agencyID).getName());
            //	String stageUrl = JiveGlobals.getDefaultBaseURL()+"/synchro/proposal-details!input.jspa?projectID=" + projectID;
            String stageUrl = baseUrl+"/synchro/proposal-details!input.jspa?projectID=" + projectID;
            email.getContext().put ("stageUrl",stageUrl);
            email = handleAttachments(email);
            stageManager.sendNotification(getUser(),email);
            
            //Email Notification TimeStamp Storage
            EmailNotificationDetailsBean emailNotBean = new EmailNotificationDetailsBean();
            emailNotBean.setProjectID(projectID);
            emailNotBean.setEndmarketID(endMarketId);
            emailNotBean.setAgencyID(agencyID);
            emailNotBean.setStageID(SynchroConstants.PROPOSAL_STAGE);
            emailNotBean.setEmailType(SynchroConstants.EMAIL_TYPE_NOTIFICATION);
            emailNotBean.setEmailDesc("PROPOSAL_REQ_CLARIFICATION");

            emailNotBean.setEmailSubject(subject);
            emailNotBean.setEmailSender(getUser().getEmail());
            emailNotBean.setEmailRecipients(recipients);
            emailNotificationManager.saveDetails(emailNotBean);
            
          //Audit Logs: Notification: Notify Users
            if(emaillist!=null)
            {
            	List<String> userNameList = SynchroUtils.fetchUserNames(emaillist.toString());
                String description = getText("logger.proposal.notify.clarification");
                        SynchroLogUtils.addNotificationLog(SynchroGlobal.PortalType.SYNCHRO.getDescription(), SynchroGlobal.PageType.PROJECT.getId(), SynchroGlobal.Activity.NOTIFICATION.getId(), 
            									SynchroGlobal.LogProjectStage.PROPOSAL.getId(), description, project.getName(), 
            											project.getProjectID(), getUser().getID(), userNameList);	
            }
        }
        else if(notificationTabId!=null && notificationTabId.equalsIgnoreCase("SEND_TO_PROJECT_OWNER"))
        {
        	StringBuffer emaillist = new StringBuffer();
            EmailMessage email = stageManager.populateNotificationEmail(recipients, subject, messageBody, null, null);
            emaillist.append(recipients);
            email = handleAttachments(email);
            stageManager.sendNotification(getUser(),email);
            proposalManager.updateSendToProjectOwner(projectID, agencyID, 1);
            this.proposalManager.updateRequestClarificationModification(projectID, agencyID, null);
            //Email Notification TimeStamp Storage
            EmailNotificationDetailsBean emailNotBean = new EmailNotificationDetailsBean();
            emailNotBean.setProjectID(projectID);
            emailNotBean.setEndmarketID(endMarketId);
            emailNotBean.setAgencyID(agencyID);
            emailNotBean.setStageID(SynchroConstants.PROPOSAL_STAGE);
            emailNotBean.setEmailType(SynchroConstants.EMAIL_TYPE_ACTION);
            emailNotBean.setEmailDesc("PROPOSAL_SEND_TO_PROJECT_OWNER");
            emailNotBean.setEmailSubject(subject);
            emailNotBean.setEmailSender(getUser().getEmail());
            emailNotBean.setEmailRecipients(recipients);
            emailNotificationManager.saveDetails(emailNotBean);
         
            //Audit Logs: Notification: Notify Users
            if(emaillist!=null)
            {
            	List<String> userNameList = SynchroUtils.fetchUserNames(emaillist.toString());
                String description = getText("logger.proposal.notify.owner");
                        SynchroLogUtils.addNotificationLog(SynchroGlobal.PortalType.SYNCHRO.getDescription(), SynchroGlobal.PageType.PROJECT.getId(), SynchroGlobal.Activity.NOTIFICATION.getId(), 
            									SynchroGlobal.LogProjectStage.PROPOSAL.getId(), description, project.getName(), 
            											project.getProjectID(), getUser().getID(), userNameList);	
            }
        }
        else if(notificationTabId!=null && notificationTabId.equalsIgnoreCase("SUBMIT_PROPOSAL"))
        {
            submitProposal();
        }
        else if(notificationTabId!=null && notificationTabId.equalsIgnoreCase("REQUEST_FOR_CLARIFICATION")) {
        	StringBuffer emaillist = new StringBuffer();
            this.proposalManager.updateRequestClarificationModification(projectID, agencyID, 1);
            this.proposalManager.updateSendToProjectOwner(projectID, agencyID, null);
            EmailMessage email = stageManager.populateNotificationEmail(recipients, subject, messageBody, null, null);
            emaillist.append(recipients);
            email = handleAttachments(email);
            stageManager.sendNotification(getUser(),email);
            
            //Email Notification TimeStamp Storage
            EmailNotificationDetailsBean emailNotBean = new EmailNotificationDetailsBean();
            emailNotBean.setProjectID(projectID);
            emailNotBean.setEndmarketID(endMarketId);
            emailNotBean.setAgencyID(agencyID);
            emailNotBean.setStageID(SynchroConstants.PROPOSAL_STAGE);
            emailNotBean.setEmailType(SynchroConstants.EMAIL_TYPE_NOTIFICATION);
            emailNotBean.setEmailDesc("PROPOSAL_REQ_CLARIFICATION");
            emailNotBean.setEmailSubject(subject);
            emailNotBean.setEmailSender(getUser().getEmail());
            emailNotBean.setEmailRecipients(recipients);
            emailNotificationManager.saveDetails(emailNotBean);
            
          //Audit Logs: Notification: Notify Users
            if(emaillist!=null)
            {
            	List<String> userNameList = SynchroUtils.fetchUserNames(emaillist.toString());
                String description = getText("logger.proposal.notify.clarification");
                        SynchroLogUtils.addNotificationLog(SynchroGlobal.PortalType.SYNCHRO.getDescription(), SynchroGlobal.PageType.PROJECT.getId(), SynchroGlobal.Activity.NOTIFICATION.getId(), 
            									SynchroGlobal.LogProjectStage.PROPOSAL.getId(), description, project.getName(), 
            											project.getProjectID(), getUser().getID(), userNameList);	
            }
        } else
        {
        	StringBuffer emaillist = new StringBuffer();
            EmailMessage email = stageManager.populateNotificationEmail(recipients, subject, messageBody, null, null);
            emaillist.append(recipients);
            email = handleAttachments(email);
            stageManager.sendNotification(getUser(),email);
            
            //Audit Logs: Notification: Notify Users
            if(emaillist!=null)
            {
            	List<String> userNameList = SynchroUtils.fetchUserNames(emaillist.toString());
                String description = getText("logger.proposal.notify.clarification");
                        SynchroLogUtils.addNotificationLog(SynchroGlobal.PortalType.SYNCHRO.getDescription(), SynchroGlobal.PageType.PROJECT.getId(), SynchroGlobal.Activity.NOTIFICATION.getId(), 
            									SynchroGlobal.LogProjectStage.PROPOSAL.getId(), description, project.getName(), 
            											project.getProjectID(), getUser().getID(), userNameList);	
            }
            
            //Email Notification TimeStamp Storage
            EmailNotificationDetailsBean emailNotBean = new EmailNotificationDetailsBean();
            emailNotBean.setProjectID(projectID);
            emailNotBean.setEndmarketID(endMarketId);
            emailNotBean.setAgencyID(agencyID);
            emailNotBean.setStageID(SynchroConstants.PROPOSAL_STAGE);
            emailNotBean.setEmailType(SynchroConstants.EMAIL_TYPE_NOTIFICATION);
            emailNotBean.setEmailDesc("PROPOSAL_REQ_CLARIFICATION");
            emailNotBean.setEmailSubject(subject);
            emailNotBean.setEmailSender(getUser().getEmail());
            emailNotBean.setEmailRecipients(recipients);
            emailNotificationManager.saveDetails(emailNotBean);
        }
        /*	stageManager.sendNotification(new Long(
                  notificationTabId), getUser(), projectID,
                  approve,email);*/
        return SUCCESS;
    }

    public String submitProposal(){
    	StringBuffer emaillist = new StringBuffer();
    	
        this.proposalManager.submitProposal(projectID, agencyID);
        //this.proposalManager.updateRequestClarificationModification(projectID, agencyID, null);
        try
        {
            //EmailNotification#7 Fetch Recipients for sending Proposal Submit notification
            String recp = userManager.getUser(endMarketDetails.get(0).getSpiContact()).getEmail();
            //EmailMessage email = stageManager.populateNotificationEmail(recp, subject, messageBody,"agency.submit.proposal.htmlBody","agency.submit.proposal.subject");
            EmailMessage email = stageManager.populateNotificationEmail(recipients, subject, messageBody,"agency.submit.proposal.htmlBody","agency.submit.proposal.subject");
            emaillist.append(recipients);
            String baseUrl = URLUtils.getBaseURL(request);
            //String stageUrl = JiveGlobals.getDefaultBaseURL()+"/synchro/proposal-details!input.jspa?projectID=" + projectID;
            String stageUrl = baseUrl+"/synchro/proposal-details!input.jspa?projectID=" + projectID;
            //email.getContext().put("projectId", projectID);
            email.getContext().put("projectId", SynchroUtils.generateProjectCode(projectID));
            email.getContext().put("projectName",project.getName());
            email.getContext().put("agencyName", userManager.getUser(agencyID).getName());
            email.getContext().put ("stageUrl",stageUrl);

            email = handleAttachments(email);
            stageManager.sendNotification(getUser(),email);


            //Email Notification TimeStamp Storage
            EmailNotificationDetailsBean emailNotBean = new EmailNotificationDetailsBean();
            emailNotBean.setProjectID(projectID);
            emailNotBean.setEndmarketID(endMarketId);
            emailNotBean.setAgencyID(agencyID);
            emailNotBean.setStageID(SynchroConstants.PROPOSAL_STAGE);
            emailNotBean.setEmailType(SynchroConstants.EMAIL_TYPE_NOTIFICATION);
            emailNotBean.setEmailDesc("PROPOSAL SUBMITTED");
            emailNotBean.setEmailSubject(subject);
            emailNotBean.setEmailSender(getUser().getEmail());
            emailNotBean.setEmailRecipients(recipients);
            emailNotificationManager.saveDetails(emailNotBean);
            

          //Audit Logs: Notification: Notify Users
            if(emaillist!=null)
            {
            	List<String> userNameList = SynchroUtils.fetchUserNames(emaillist.toString());
                String description = getText("logger.proposal.notify.save");
                        SynchroLogUtils.addNotificationLog(SynchroGlobal.PortalType.SYNCHRO.getDescription(), SynchroGlobal.PageType.PROJECT.getId(), SynchroGlobal.Activity.NOTIFICATION.getId(), 
            									SynchroGlobal.LogProjectStage.PROPOSAL.getId(), description, project.getName(), 
            											project.getProjectID(), getUser().getID(), userNameList);	
            }
                    
          //Audit Logs: Save & Send Proposal
           /* SynchroLogUtils.addLog(SynchroGlobal.PortalType.SYNCHRO.getDescription(), SynchroGlobal.PageType.PROJECT.getId(), SynchroGlobal.Activity.APPROVE.getId(), 
            										SynchroGlobal.LogProjectStage.PROPOSAL.getId(), getText("logger.project.proposal.notfic.sendproposal"), project.getName(), 
            												project.getProjectID(), getUser().getID()); */
        }
        catch(UserNotFoundException ue)
        {
            LOG.error("User not found while submit Proposal");
        }
        return SUCCESS;
    }
    public String awardAgency(){
    	StringBuffer emaillist = new StringBuffer();
        this.proposalManager.awardAgency(project, agencyID,endMarketId, attachmentMap);
        StringBuffer otherAgencies = new StringBuffer();
        String baseUrl = URLUtils.getBaseURL(request);
        //String stageUrl = JiveGlobals.getDefaultBaseURL()+"/synchro/proposal-details!input.jspa?projectID=" + projectID;
        String stageUrl = baseUrl+"/synchro/proposal-details!input.jspa?projectID=" + projectID;
        // This is done to notify other agencies in case proposal is awarded
        if(agencyMap.size()>0)
        {
            for(String agency:agencyMap.keySet())
            {
                if(agencyMap.get(agency)!=null && agencyMap.get(agency).intValue()!=agencyID.intValue() && !isSameAgencyGroup(agencyID, agencyContactMap, agency))
                {

                    try
                    {
                        if(otherAgencies.length()>0)
                        {
                            otherAgencies.append(","+userManager.getUser(agencyMap.get(agency)).getEmail());
                        }
                        else
                        {
                            otherAgencies.append(userManager.getUser(agencyMap.get(agency)).getEmail());
                        }
                    }
                    catch(UserNotFoundException ue)
                    {
                        LOG.error("User not found while award agency send notification" + agencyMap.get(agency));
                    }
                }
            }
            if(otherAgencies.length()>0)
            {


                //EmailNotification#8 Fetch Recipients for Proposal Awarded notification in otherAgencies parameter
                EmailMessage email = stageManager.populateNotificationEmail(otherAgencies.toString(), null, null,"agency.awarded.proposal.htmlBody","agency.awarded.proposal.subject");
                emaillist.append(otherAgencies.toString());
                //email.getContext().put("projectId", projectID);
                email.getContext().put("projectId", SynchroUtils.generateProjectCode(projectID));
                email.getContext().put("projectName",project.getName());
                email.getContext().put ("stageUrl",stageUrl);
                //	email.getContext().put("agencyName", userManager.getUser(agencyID).getName());


                stageManager.sendNotification(getUser(),email);

                //Email Notification TimeStamp Storage
                EmailNotificationDetailsBean emailNotBean = new EmailNotificationDetailsBean();
                emailNotBean.setProjectID(projectID);
                emailNotBean.setEndmarketID(endMarketId);
                emailNotBean.setStageID(SynchroConstants.PROPOSAL_STAGE);
                emailNotBean.setEmailType(SynchroConstants.EMAIL_TYPE_NOTIFICATION);
                emailNotBean.setEmailDesc("PROPOSAL AWARDED EMAIL TO OTHER AGENCIES");

                emailNotBean.setEmailSubject("Notification | Projectnot Awarded ");
                emailNotBean.setEmailSender(getUser().getEmail());
                emailNotBean.setEmailRecipients(otherAgencies.toString());

                emailNotificationManager.saveDetails(emailNotBean);

            }
            // Automatic Notification for Project Owner
            try
            {
                EmailMessage email = stageManager.populateNotificationEmail(userManager.getUser(agencyID).getEmail(), null, null,"projectOwner.notify.awarded.proposal.htmlBody","projectOwner.notify.awarded.proposal.subject");
                if(StringUtils.isNullOrEmpty(emaillist.toString()))
                {
                	emaillist.append(",");
                }
                emaillist.append(userManager.getUser(agencyID).getEmail());
                
                //email.getContext().put("projectId", projectID);
                email.getContext().put("projectId", SynchroUtils.generateProjectCode(projectID));
                email.getContext().put("projectName",project.getName());
                email.getContext().put ("stageUrl",stageUrl);
                stageManager.sendNotification(getUser(),email);

                //Email Notification TimeStamp Storage
                EmailNotificationDetailsBean emailNotBean = new EmailNotificationDetailsBean();
                emailNotBean.setProjectID(projectID);
                emailNotBean.setEndmarketID(endMarketId);
                emailNotBean.setAgencyID(agencyID);
                emailNotBean.setStageID(SynchroConstants.PROPOSAL_STAGE);
                emailNotBean.setEmailType(SynchroConstants.EMAIL_TYPE_NOTIFICATION);
                emailNotBean.setEmailDesc("PROPOSAL AWARDED EMAIL TO PROJECT OWNER");

                emailNotBean.setEmailSubject("Notification | Proposal Accepted ");
                emailNotBean.setEmailSender(getUser().getEmail());
                emailNotBean.setEmailRecipients(userManager.getUser(agencyID).getEmail());

                emailNotificationManager.saveDetails(emailNotBean);
            }
            catch(UserNotFoundException ue)
            {
                LOG.error("Agency Id not found while sending Notification for Proposal Awarded"+agencyID);
            }
            
            
          //Audit Logs: Notification: Notify Users
            if(emaillist!=null)
            {
            	List<String> userNameList = SynchroUtils.fetchUserNames(emaillist.toString());
                String description = getText("logger.proposal.notify.award");
                        SynchroLogUtils.addNotificationLog(SynchroGlobal.PortalType.SYNCHRO.getDescription(), SynchroGlobal.PageType.PROJECT.getId(), SynchroGlobal.Activity.NOTIFICATION.getId(), 
            									SynchroGlobal.LogProjectStage.PROPOSAL.getId(), description, project.getName(), 
            											project.getProjectID(), getUser().getID(), userNameList);	
            }
            
                    
          //Audit Logs: Approve: Award to Agency
            SynchroLogUtils.addLog(SynchroGlobal.PortalType.SYNCHRO.getDescription(), SynchroGlobal.PageType.PROJECT.getId(), SynchroGlobal.Activity.APPROVE.getId(), 
            										SynchroGlobal.LogProjectStage.PROPOSAL.getId(), getText("logger.project.proposal.notfic.awardagency"), project.getName(), 
            												project.getProjectID(), getUser().getID());
        }
        if(StringUtils.isNotBlank(getRequest().getParameter("currentStatus")))
        {
        	return "current-status";
        }
        else
        {
        	//return SUCCESS;
        	 //  Clicking on ‘Move to the next stage’ will navigate user to the next stage 
            return "moveToNextStage";
        }
    }

    // This method will check whether the Agency is part of same agency Group or not
    private boolean isSameAgencyGroup(Long agencyId, Map<String,Long> agencyContactMapAll, String agencyType)
    {
        if(agencyContactMapAll.get(agencyType)!=null && agencyContactMapAll.get(agencyType).intValue()==agencyId.intValue())
        {
            return true;
        }
        if(agencyContactMapAll.get(agencyType+"Optional")!=null && agencyContactMapAll.get(agencyType+"Optional").intValue()==agencyId.intValue())
        {
            return true;
        }
        return false;
    }
    /**
     * This method will be called when the Reject Agency button is clicked
     * @return
     */
    public String rejectAgency(){

    	StringBuffer emaillist = new StringBuffer();
        // This check has been done for Admin user. In case the Proposal is Awarded and then Admin user removes one of the fields and then reject the
        // proposal. In that case the SAVE and SUBMIT Proposal button is still enabled for Admin and Agency user.
        if(proposalInitiation.getStatus()==SynchroGlobal.StageStatus.PROPOSAL_STARTED.ordinal())
        {
            this.proposalManager.rejectAgency(project, agencyID,endMarketId,SynchroGlobal.StageStatus.PROPOSAL_STARTED.ordinal());
        }
        else
        {
            this.proposalManager.rejectAgency(project, agencyID,endMarketId,SynchroGlobal.StageStatus.PROPOSAL_SAVED.ordinal());
        }
        this.proposalManager.updateSendToProjectOwner(projectID, agencyID, null);
        // The System Admin user can reject the Proposal Any time. In that case the Project Specs Details need to be wiped off.
        if(SynchroPermHelper.isSystemAdmin(getUser()))
        {
            this.projectSpecsManager.deleteProjectSpecsDetails(project.getProjectID());
        }
        StringBuffer allAgencies = new StringBuffer();
        //String stageUrl = JiveGlobals.getDefaultBaseURL()+"/synchro/proposal-details!input.jspa?projectID=" + projectID;
        String baseUrl = URLUtils.getBaseURL(request);
        String stageUrl = baseUrl+"/synchro/proposal-details!input.jspa?projectID=" + projectID;
        //https://www.svn.sourcen.com/issues/18895 - Send email only to the rejected agency
        try
        {

            String agencyEmail = userManager.getUser(agencyID).getEmail();
            EmailMessage email = stageManager.populateNotificationEmail(agencyEmail, null,null,"agency.reject.proposal.htmlBody","agency.reject.proposal.subject");
            emaillist.append(agencyEmail);
            //email.getContext().put("projectId", projectID);
            email.getContext().put("projectId", SynchroUtils.generateProjectCode(projectID));
            email.getContext().put("projectName",project.getName());
            email.getContext().put ("stageUrl",stageUrl);
            stageManager.sendNotification(getUser(),email);

            //Email Notification TimeStamp Storage
            EmailNotificationDetailsBean emailNotBean = new EmailNotificationDetailsBean();
            emailNotBean.setProjectID(projectID);
            emailNotBean.setEndmarketID(endMarketId);
            emailNotBean.setAgencyID(agencyID);
            emailNotBean.setStageID(SynchroConstants.PROPOSAL_STAGE);
            emailNotBean.setEmailType(SynchroConstants.EMAIL_TYPE_NOTIFICATION);
            emailNotBean.setEmailDesc("PROPOSAL REJECTED");
            emailNotBean.setEmailSubject("Notification | Proposal Rejected");
            emailNotBean.setEmailSender(getUser().getEmail());
            emailNotBean.setEmailRecipients(agencyEmail);
            emailNotificationManager.saveDetails(emailNotBean);
            
          //Audit Logs: Approve:  Reject Proposal of Agency
            SynchroLogUtils.addLog(SynchroGlobal.PortalType.SYNCHRO.getDescription(), SynchroGlobal.PageType.PROJECT.getId(), SynchroGlobal.Activity.APPROVE.getId(), 
            										SynchroGlobal.LogProjectStage.PROPOSAL.getId(), getText("logger.project.proposal.notfic.rejectproposal"), project.getName(), 
            												project.getProjectID(), getUser().getID());
        }
        catch(UserNotFoundException ue)
        {
            LOG.error("User not found while reject agency send notification" + agencyID);
        }
        /*
          // This is done to notify all agencies in case proposal is rejected
          if(agencyMap.size()>0)
          {
              for(String agency:agencyMap.keySet())
              {
                  if(agencyMap.get(agency)!=null)
                  {
                      try
                      {
                          if(allAgencies.length()>0)
                          {
                              allAgencies.append(","+userManager.getUser(agencyMap.get(agency)).getEmail());
                          }
                          else
                          {
                              allAgencies.append(userManager.getUser(agencyMap.get(agency)).getEmail());
                          }
                      }
                      catch(UserNotFoundException ue)
                      {
                          LOG.error("User not found while reject agency send notification" + agencyMap.get(agency));
                      }
                  }
              }
              if(allAgencies.length()>0)
              {
                  //EmailNotification#9 Fetch Recipients for Proposal Rejection notification in allAgencies parameter
                  EmailMessage email = stageManager.populateNotificationEmail(allAgencies.toString(), null,null,"agency.reject.proposal.htmlBody","agency.reject.proposal.subject");

                  email.getContext().put("projectId", projectID);
                  email.getContext().put("projectName",project.getName());
                  email.getContext().put ("stageUrl",stageUrl);
                  stageManager.sendNotification(getUser(),email);
              }
          }*/
        
      //Audit Logs: Notification: Notify Users
        if(emaillist!=null)
        {
        	List<String> userNameList = SynchroUtils.fetchUserNames(emaillist.toString());
            String description = getText("logger.proposal.notify.reject");
                    SynchroLogUtils.addNotificationLog(SynchroGlobal.PortalType.SYNCHRO.getDescription(), SynchroGlobal.PageType.PROJECT.getId(), SynchroGlobal.Activity.NOTIFICATION.getId(), 
        									SynchroGlobal.LogProjectStage.PROPOSAL.getId(), description, project.getName(), 
        											project.getProjectID(), getUser().getID(), userNameList);	
        }
                
        
        return SUCCESS;
    }
    public String addAttachment() throws UnsupportedEncodingException {

        LOG.info("Checking File Name"+attachFileFileName);
        LOG.info("Checking File Content Type"+attachFileContentType);
        Map<String, Object> result = new HashMap<String, Object>();
        try
        {
            proposalManager.addAttachment(attachFile, attachFileFileName, attachFileContentType, projectID,endMarketId,fieldCategoryId,getUser().getID(),agencyID);
            
          //Add Attachment Audit logs
            String description = SynchroGlobal.SynchroAttachmentObject.getById(fieldCategoryId.intValue()) + " Attachment" + "- " +attachFileFileName;
            SynchroLogUtils.addLog(SynchroGlobal.PortalType.SYNCHRO.getDescription(), SynchroGlobal.PageType.PROJECT.getId(), SynchroGlobal.Activity.UPLOAD.getId(), 
            										SynchroGlobal.LogProjectStage.PROPOSAL.getId(), description, project.getName(), 
            												project.getProjectID(), getUser().getID());
        }
        catch (AttachmentException ae) {
            result.put("success", false);
            result.put("message", "Unable to upload file.");
        } catch (UnauthorizedException ue) {
            result.put("success", false);
            result.put("message", "Unauthorized.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return SUCCESS;
    }
    public String removeAttachment() throws UnsupportedEncodingException {
        try
        {
            proposalManager.removeAttachment(attachmentId);
            
          //Remove Attachment Audit logs
            String description = SynchroGlobal.SynchroAttachmentObject.getById(attachmentFieldID.intValue()) + " Attachment" + "- " + attachmentName +" deleted";
            SynchroLogUtils.addLog(SynchroGlobal.PortalType.SYNCHRO.getDescription(), SynchroGlobal.PageType.PROJECT.getId(), SynchroGlobal.Activity.DELETE.getId(), 
									SynchroGlobal.LogProjectStage.PROPOSAL.getId(), description, project.getName(), 
											project.getProjectID(), getUser().getID());
        }
        catch (Exception e) {
            LOG.error("Exception while removing attachment Id --"+ attachmentId);
        }
        return SUCCESS;
        /*Map<String, Object> result = new HashMap<String, Object>();
       JSONObject out = new JSONObject();
       try {
           proposalManager.removeAttachment(attachmentId);
           result.put("success", true);
           result.put("message", "Successfully removed document");
       } catch (AttachmentException e) {
           result.put("success", false);
           result.put("message", "Attachment not found.");
       } catch (UnauthorizedException e) {
           result.put("success", false);
           result.put("message", "Unauthorized to remove document.");
       } catch (Exception e) {
           result.put("success", false);
           result.put("message", e.getMessage());
       }
       return null;*/
    }


    public String exportToPDF()
    {
        try
        {
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            Document document=new Document();
            NumberFormat nf = NumberFormat.getInstance();
            response.setContentType("application/pdf");
            response.addHeader("Content-Disposition", "attachment; filename=ProposalPDF.pdf");
            PdfWriter.getInstance(document,response.getOutputStream());
            document.open();

            document.add(new Paragraph("Project Code : "+ project.getProjectID()));
            document.add(new Paragraph("Project Name : "+ project.getName()));
            document.add(new Paragraph("Brand / Non-Branded : "+ SynchroGlobal.getBrands().get(proposalInitiation.getBrand().intValue())));
            document.add(new Paragraph("Country : "+ SynchroGlobal.getEndMarkets().get(Integer.valueOf(proposalInitiation.getEndMarketID()+""))));
            document.add(new Paragraph("Project Owner : "+ userManager.getUser(project.getProjectOwner()).getName()));
            document.add(new Paragraph("PIT Creator : "+ userManager.getUser(project.getBriefCreator()).getName()));
            document.add(new Paragraph("SPI Contact : "+ userManager.getUser(endMarketDetails.get(0).getSpiContact()).getName()));
            //TODO kanwar
            if(project.getProposedMethodology()!=null && SynchroGlobal.getMethodologies().get(project.getProposedMethodology()!=null?project.getProposedMethodology().get(0).intValue():-1)!=null)
            {
                for(Long mid : proposalInitiation.getProposedMethodology())
                {
                    document.add(new Paragraph("Proposed Methodology : "+ SynchroGlobal.getMethodologies().get(mid.intValue())));
                }

            }
            else
            {
                document.add(new Paragraph("Proposed Methodology : "));
            }
            document.add(new Paragraph("Project Start (Commissioning) : "+ df.format(proposalInitiation.getStartDate())));
            document.add(new Paragraph("Project End (Results) : "+ df.format(proposalInitiation.getEndDate())));
            List<ProjectInitiation> initiationList = this.pibManager.getPIBDetails(projectID);


            StringBuffer categoryTypes = new StringBuffer();
            for(int i=0;i<project.getCategoryType().size();i++)
            {
                if(i>0)
                {
                    categoryTypes.append(","+SynchroGlobal.getProductTypes().get(project.getCategoryType().get(i).intValue()));
                }
                else
                {
                    categoryTypes.append(SynchroGlobal.getProductTypes().get(project.getCategoryType().get(i).intValue()));
                }
            }
            document.add(new Paragraph("Category Type : "+ categoryTypes.toString()));
            document.add(new Paragraph("Methodolgy Type : "+ SynchroGlobal.getProjectIsMapping().get(proposalInitiation.getMethodologyType().intValue())));
            document.add(new Paragraph("Methodology Group : "+ SynchroGlobal.getMethodologyGroups(true, proposalInitiation.getMethodologyGroup()).get(proposalInitiation.getMethodologyGroup().intValue())));
            /*	if(endMarketDetails.get(0).getInitialCost()!=null)
               {
                   document.add(new Paragraph("Estimated Cost : "+ nf.format(endMarketDetails.get(0).getInitialCost()) + " "+  SynchroGlobal.getCurrencies().get(endMarketDetails.get(0).getInitialCostCurrency().intValue())));
               }
               else
               {
                   document.add(new Paragraph("Estimated Cost : "+ endMarketDetails.get(0).getInitialCost() + " "+  SynchroGlobal.getCurrencies().get(endMarketDetails.get(0).getInitialCostCurrency().intValue())));
               }
               */
            //document.add(new Paragraph("Latest Estimate : "+ projectInitiation.getLatestEstimate() + " "+ SynchroGlobal.getCurrencies().get(projectInitiation.getLatestEstimateType().intValue())));
            document.add(new Paragraph("NPI Number (if appropriate) : "+ proposalInitiation.getNpiReferenceNo()));
            if(initiationList.get(0).getDeviationFromSM()!=null && initiationList.get(0).getDeviationFromSM()==1)
            {
                document.add(new Paragraph("Request for Methodology Waiver : Yes"));
            }
            else
            {
                document.add(new Paragraph("Request for Methodology Waiver : No"));
            }
            document.add(new Paragraph("Project Description : "+ project.getDescription()));

            if(proposalInitiation.getBizQuestion()!=null)
            {
                document.add(new Paragraph("Business Questions : "+ proposalInitiation.getBizQuestion()));
            }
            else
            {
                document.add(new Paragraph("Business Questions : "));
            }
            if(proposalInitiation.getResearchObjective()!=null)
            {
                document.add(new Paragraph("Research Objectives(s) : "+ proposalInitiation.getResearchObjective()));
            }
            else
            {
                document.add(new Paragraph("Research Objectives(s) : "));
            }
            if(proposalInitiation.getActionStandard()!=null)
            {
                document.add(new Paragraph("Action Standard(s) : "+ proposalInitiation.getActionStandard()));
            }
            else
            {
                document.add(new Paragraph("Action Standard(s) : "));
            }
            if(proposalInitiation.getResearchDesign()!=null)
            {
                document.add(new Paragraph("Methodology Approach and Research Design : "+ proposalInitiation.getResearchDesign()));
            }
            else
            {
                document.add(new Paragraph("Methodology Approach and Research Design : "));
            }
            if(proposalInitiation.getSampleProfile()!=null)
            {
                document.add(new Paragraph("Sample Profile(Research) : "+ proposalInitiation.getSampleProfile()));
            }
            else
            {
                document.add(new Paragraph("Sample Profile(Research) : "));
            }
            if(proposalInitiation.getStimulusMaterial()!=null)
            {
                document.add(new Paragraph("Stimulus Material : "+ proposalInitiation.getStimulusMaterial()));
            }
            else
            {
                document.add(new Paragraph("Stimulus Material : "));
            }
            if(proposalInitiation.getStimulusMaterialShipped()!=null)
            {
                document.add(new Paragraph("Stimulus Material need to be shipped to : "+ proposalInitiation.getStimulusMaterialShipped()));
            }
            else
            {
                document.add(new Paragraph("Stimulus Material need to be shipped to : "));
            }
            if(proposalInitiation.getOthers()!=null)
            {
                document.add(new Paragraph("Other Comments : "+ proposalInitiation.getOthers()));
            }
            else
            {
                document.add(new Paragraph("Other Comments : "));
            }
            if(proposalInitiation.getStimuliDate()!=null)
            {
                document.add(new Paragraph("Date Stimuli Available(in Agency) : "+ df.format(proposalInitiation.getStimuliDate())));
            }
            else
            {
                document.add(new Paragraph("Date Stimuli Available(in Agency) : "));
            }
            StringBuffer repRequirements = new StringBuffer();
            if(proposalReporting.getTopLinePresentation()!=null && proposalReporting.getTopLinePresentation())
            {
                repRequirements.append("Top Line Presentation");
            }
            if(proposalReporting.getPresentation()!=null && proposalReporting.getPresentation())
            {
                if(repRequirements.length()>0)
                {
                    repRequirements.append(",Presentation");
                }
                else
                {
                    repRequirements.append("Presentation");
                }
            }
            if(proposalReporting.getFullreport()!=null && proposalReporting.getFullreport())
            {
                if(repRequirements.length()>0)
                {
                    repRequirements.append(",Full Report");
                }
                else
                {
                    repRequirements.append("Full Report");
                }
            }

            document.add(new Paragraph("Reporting Requirements : "+ repRequirements.toString()));


            if(proposalReporting.getOtherReportingRequirements()!=null)
            {
                document.add(new Paragraph("Other Reporting Requirements : "+ proposalReporting.getOtherReportingRequirements()));
            }
            else
            {
                document.add(new Paragraph("Other Reporting Requirements : "));
            }
            if(proposalEMDetails.getTotalCost()!=null)
            {
                document.add(new Paragraph("Total Cost : " +nf.format(proposalEMDetails.getTotalCost()) +" "+  SynchroGlobal.getCurrencies().get(proposalEMDetails.getTotalCostType().intValue()) ));
            }
            else
            {
                document.add(new Paragraph("Total Cost : "));
            }

            if(proposalInitiation.getMethodologyType().intValue()!=4 )
            {
                if(proposalEMDetails.getIntMgmtCost()!=null)
                {
                    document.add(new Paragraph("International Management Cost : "+ nf.format(proposalEMDetails.getIntMgmtCost()) + " "+ SynchroGlobal.getCurrencies().get(proposalEMDetails.getIntMgmtCostType().intValue())));
                }
                else
                {
                    document.add(new Paragraph("International Management Cost : "));
                }
                if(proposalEMDetails.getLocalMgmtCost()!=null)
                {
                    document.add(new Paragraph("Local Management Cost : "+nf.format(proposalEMDetails.getLocalMgmtCost()) + " "+ SynchroGlobal.getCurrencies().get(proposalEMDetails.getLocalMgmtCostType().intValue())));
                }
                else
                {
                    document.add(new Paragraph("Local Management Cost : "));
                }
                if(proposalEMDetails.getFieldworkCost()!=null)
                {
                    document.add(new Paragraph("Fieldwork Cost : "+nf.format(proposalEMDetails.getFieldworkCost())+" " + SynchroGlobal.getCurrencies().get(proposalEMDetails.getFieldworkCostType().intValue())));
                }
                else
                {
                    document.add(new Paragraph("Fieldwork Cost : "));
                }
                if(proposalEMDetails.getProposedFWAgencyNames()!=null)
                {
                    document.add(new Paragraph("Name of Proposed Fieldwork Agencies : "+ proposalEMDetails.getProposedFWAgencyNames()));
                }
                else
                {
                    document.add(new Paragraph("Name of Proposed Fieldwork Agencies : "));
                }
                if(proposalEMDetails.getFwStartDate()!=null)
                {
                    document.add(new Paragraph("Estimated Fieldwork Start : " + df.format(proposalEMDetails.getFwStartDate())));
                }
                else
                {
                    document.add(new Paragraph("Estimated Fieldwork Start : "));
                }
                if(proposalEMDetails.getFwEndDate()!=null)
                {
                    document.add(new Paragraph("Estimated Fieldwork Completion : " + df.format(proposalEMDetails.getFwEndDate())));
                }
                else
                {
                    document.add(new Paragraph("Estimated Fieldwork Completion : "));
                }


                StringBuffer dataCollection = new StringBuffer();
                if(proposalEMDetails.getDataCollectionMethod()!=null)
                {
                    for(Long dc:proposalEMDetails.getDataCollectionMethod())
                    {
                        if(dataCollection.length()>0)
                        {
                            dataCollection.append(","+SynchroGlobal.getDataCollections().get(dc.intValue()));
                        }
                        else
                        {
                            dataCollection.append(SynchroGlobal.getDataCollections().get(dc.intValue()));
                        }

                    }
                }
                document.add(new Paragraph("Data Collection Method : " + dataCollection));
            }

            if(proposalInitiation.getMethodologyType().intValue()==1 || proposalInitiation.getMethodologyType().intValue()==3)
            {
                document.add(new Paragraph("Quantitative : "));
                if( proposalEMDetails.getTotalNoInterviews()!=null)
                {
                    document.add(new Paragraph("Total Number of Interviews : " + proposalEMDetails.getTotalNoInterviews()));
                }
                else
                {
                    document.add(new Paragraph("Total Number of Interviews : " ));
                }
                if(proposalEMDetails.getTotalNoOfVisits()!=null)
                {
                    document.add(new Paragraph("Total Number of Visits per Respondent : " + proposalEMDetails.getTotalNoOfVisits()));
                }
                else
                {
                    document.add(new Paragraph("Total Number of Visits per Respondent : "));
                }
                if(proposalEMDetails.getAvIntDuration()!=null)
                {
                    document.add(new Paragraph("Average Interview Duration : " + proposalEMDetails.getAvIntDuration()));
                }
                else
                {
                    document.add(new Paragraph("Average Interview Duration : "));
                }
            }
            if(proposalInitiation.getMethodologyType().intValue()==2 || proposalInitiation.getMethodologyType().intValue()==3)
            {
                document.add(new Paragraph("Qualitative : "));

                if(proposalEMDetails.getTotalNoOfGroups()!=null)
                {
                    document.add(new Paragraph("Total No of Groups/In-Dept Interviews : " + proposalEMDetails.getTotalNoOfGroups()));
                }
                else
                {
                    document.add(new Paragraph("Total No of Groups/In-Dept Interviews : "));
                }
                if(proposalEMDetails.getInterviewDuration()!=null)
                {
                    document.add(new Paragraph("Group/In-Interview Duration : " + proposalEMDetails.getInterviewDuration()));
                }
                else
                {
                    document.add(new Paragraph("Group/In-Interview Duration : " ));
                }
                if(proposalEMDetails.getNoOfRespPerGroup()!=null)
                {
                    document.add(new Paragraph("Number of Respondents per Group : " + proposalEMDetails.getNoOfRespPerGroup()));
                }
                else
                {
                    document.add(new Paragraph("Number of Respondents per Group : "));
                }


            }
            if(proposalInitiation.getMethodologyType().intValue()==1 || proposalInitiation.getMethodologyType().intValue()==2 || proposalInitiation.getMethodologyType().intValue()==3)
            {
                StringBuffer geoSpread = new StringBuffer();
                if(proposalEMDetails.getGeoSpreadNational()!=null && proposalEMDetails.getGeoSpreadNational())
                {
                    geoSpread.append("National");
                }
                if(proposalEMDetails.getGeoSpreadUrban()!=null && proposalEMDetails.getGeoSpreadUrban())
                {
                    if(geoSpread.length()>0)
                    {
                        geoSpread.append(", Urban Only");
                    }
                    else
                    {
                        geoSpread.append("Urban Only");
                    }
                }
                document.add(new Paragraph("Geographical Spread : "+ geoSpread.toString()));
                if(proposalEMDetails.getGeoSpreadUrban()!=null && proposalEMDetails.getGeoSpreadUrban())
                {
                    document.add(new Paragraph("Cities (Urban Only) : "+ proposalEMDetails.getCities()));
                }
            }
            if(proposalInitiation.getProposalCostTemplate()!=null)
            {
                document.add(new Paragraph("Proposal and Cost Template : "+ proposalInitiation.getProposalCostTemplate()));
            }
            else
            {
                document.add(new Paragraph("Proposal and Cost Template : "));
            }

            document.close();

            // document.write(response.getOutputStream());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (DocumentException e) {
            e.printStackTrace();
        }
        catch (UserNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }




    public void setSynchroProjectManager(final ProjectManager synchroProjectManager) {
        this.synchroProjectManager = synchroProjectManager;
    }


    public Long getProjectID() {
        return projectID;
    }

    public void setProjectID(final Long projectID) {
        this.projectID = projectID;
    }

    public boolean isSave() {
        return isSave;
    }

    public void setSave(final boolean save) {
        isSave = save;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(final Project project) {
        this.project = project;
    }

    public boolean isEditStage() {
        return editStage;
    }

    public void setEditStage(boolean editStage) {
        this.editStage = editStage;
    }

    public String getNotificationTabId() {
        return notificationTabId;
    }

    public void setNotificationTabId(String notificationTabId) {
        this.notificationTabId = notificationTabId;
    }

    public String getRedirectURL() {
        return redirectURL;
    }

    public void setRedirectURL(String redirectURL) {
        this.redirectURL = redirectURL;
    }

    public String getApprove() {
        return approve;
    }

    public void setApprove(String approve) {
        this.approve = approve;
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public List<SynchroStageToDoListBean> getStageToDoList() {
        return stageToDoList;
    }

    public void setStageToDoList(List<SynchroStageToDoListBean> stageToDoList) {
        this.stageToDoList = stageToDoList;
    }

    public Integer getStageId() {
        return stageId;
    }

    public void setStageId(Integer stageId) {
        this.stageId = stageId;
    }




    public StageManager getStageManager() {
        return stageManager;
    }

    public void setStageManager(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    public Map<String, Map<String, String>> getStageApprovers() {
        return stageApprovers;
    }

    public void setStageApprovers(Map<String, Map<String, String>> stageApprovers) {
        this.stageApprovers = stageApprovers;
    }

    public Map<String, String> getApprovers() {
        return approvers;
    }

    public AttachmentHelper getAttachmentHelper() {
        return attachmentHelper;
    }

    public void setAttachmentHelper(AttachmentHelper attachmentHelper) {
        this.attachmentHelper = attachmentHelper;
    }

    public File getAttachFile() {
        return attachFile;
    }

    public void setAttachFile(File attachFile) {
        this.attachFile = attachFile;
    }

    public String getAttachFileContentType() {
        return attachFileContentType;
    }

    public void setAttachFileContentType(String attachFileContentType) {
        this.attachFileContentType = attachFileContentType;
    }

    public String getAttachFileFileName() {
        return attachFileFileName;
    }

    public void setAttachFileFileName(String attachFileFileName) {
        this.attachFileFileName = attachFileFileName;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(Long attachmentId) {
        this.attachmentId = attachmentId;
    }

    public Long getFieldCategoryId() {
        return fieldCategoryId;
    }

    public void setFieldCategoryId(Long fieldCategoryId) {
        this.fieldCategoryId = fieldCategoryId;
    }


    public Long getEndMarketId() {
        return endMarketId;
    }

    public void setEndMarketId(Long endMarketId) {
        this.endMarketId = endMarketId;
    }

    public Long getUpdatedSingleMarketId() {
        return updatedSingleMarketId;
    }

    public void setUpdatedSingleMarketId(Long updatedSingleMarketId) {
        this.updatedSingleMarketId = updatedSingleMarketId;
    }

    public ProposalManager getProposalManager() {
        return proposalManager;
    }

    public void setProposalManager(ProposalManager proposalManager) {
        this.proposalManager = proposalManager;
    }

    public ProposalInitiation getProposalInitiation() {
        return proposalInitiation;
    }

    public void setProposalInitiation(ProposalInitiation proposalInitiation) {
        this.proposalInitiation = proposalInitiation;
    }

    public Long getAgencyID() {
        return agencyID;
    }

    public void setAgencyID(Long agencyID) {
        this.agencyID = agencyID;
    }

    public ProposalReporting getProposalReporting() {
        return proposalReporting;
    }

    public void setProposalReporting(ProposalReporting proposalReporting) {
        this.proposalReporting = proposalReporting;
    }

    public ProposalEndMarketDetails getProposalEMDetails() {
        return proposalEMDetails;
    }

    public void setProposalEMDetails(ProposalEndMarketDetails proposalEMDetails) {
        this.proposalEMDetails = proposalEMDetails;
    }

    public List<EndMarketInvestmentDetail> getEndMarketDetails() {
        return endMarketDetails;
    }

    public void setEndMarketDetails(List<EndMarketInvestmentDetail> endMarketDetails) {
        this.endMarketDetails = endMarketDetails;
    }

    public PIBManager getPibManager() {
        return pibManager;
    }

    public void setPibManager(PIBManager pibManager) {
        this.pibManager = pibManager;
    }

    public Map<String, Long> getAgencyMap() {
        return agencyMap;
    }

    public void setAgencyMap(Map<String, Long> agencyMap) {
        this.agencyMap = agencyMap;
    }

    public Map<Integer, List<AttachmentBean>> getAttachmentMap() {
        return attachmentMap;
    }

    public void setAttachmentMap(Map<Integer, List<AttachmentBean>> attachmentMap) {
        this.attachmentMap = attachmentMap;
    }

    public Boolean getIsProposalAwarded() {
        return isProposalAwarded;
    }

    public void setIsProposalAwarded(Boolean isProposalAwarded) {
        this.isProposalAwarded = isProposalAwarded;
    }

    public Map<Long, Long> getAttachmentUser() {
        return attachmentUser;
    }

    public void setAttachmentUser(Map<Long, Long> attachmentUser) {
        this.attachmentUser = attachmentUser;
    }

    public String getSubjectSendToProjOwner() {
        return subjectSendToProjOwner;
    }

    public void setSubjectSendToProjOwner(String subjectSendToProjOwner) {
        this.subjectSendToProjOwner = subjectSendToProjOwner;
    }

    public String getMessageBodySendToProjOwner() {
        return messageBodySendToProjOwner;
    }

    public void setMessageBodySendToProjOwner(String messageBodySendToProjOwner) {
        this.messageBodySendToProjOwner = messageBodySendToProjOwner;
    }

    public String getSubjectSendToSPI() {
        return subjectSendToSPI;
    }

    public void setSubjectSendToSPI(String subjectSendToSPI) {
        this.subjectSendToSPI = subjectSendToSPI;
    }

    public String getMessageBodySendToSPI() {
        return messageBodySendToSPI;
    }

    public void setMessageBodySendToSPI(String messageBodySendToSPI) {
        this.messageBodySendToSPI = messageBodySendToSPI;
    }

    public ProjectManager getSynchroProjectManager() {
        return synchroProjectManager;
    }

    public Long getProposalEndMarketId() {
        return proposalEndMarketId;
    }

    public void setProposalEndMarketId(Long proposalEndMarketId) {
        this.proposalEndMarketId = proposalEndMarketId;
    }

    public Boolean getShowMandatoryFieldsError() {
        return showMandatoryFieldsError;
    }

    public void setShowMandatoryFieldsError(Boolean showMandatoryFieldsError) {
        this.showMandatoryFieldsError = showMandatoryFieldsError;
    }

    public String getSubjectSubmitProposal() {
        return subjectSubmitProposal;
    }

    public void setSubjectSubmitProposal(String subjectSubmitProposal) {
        this.subjectSubmitProposal = subjectSubmitProposal;
    }

    public String getMessageBodySubmitProposal() {
        return messageBodySubmitProposal;
    }

    public void setMessageBodySubmitProposal(String messageBodySubmitProposal) {
        this.messageBodySubmitProposal = messageBodySubmitProposal;
    }

    public Map<String, String> getAgencyDeptMap() {
        return agencyDeptMap;
    }

    public void setAgencyDeptMap(Map<String, String> agencyDeptMap) {
        this.agencyDeptMap = agencyDeptMap;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public void setProfileManager(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    public Long getParentAgencyID() {
        return parentAgencyID;
    }

    public void setParentAgencyID(Long parentAgencyID) {
        this.parentAgencyID = parentAgencyID;
    }

    public EmailNotificationManager getEmailNotificationManager() {
        return emailNotificationManager;
    }

    public void setEmailNotificationManager(
            EmailNotificationManager emailNotificationManager) {
        this.emailNotificationManager = emailNotificationManager;
    }

    public PIBMethodologyWaiver getProposalMethodologyWaiver() {
        return proposalMethodologyWaiver;
    }

    public ProjectSpecsManager getProjectSpecsManager() {
        return projectSpecsManager;
    }

    public void setProjectSpecsManager(ProjectSpecsManager projectSpecsManager) {
        this.projectSpecsManager = projectSpecsManager;
    }
    private EmailMessage handleAttachments(EmailMessage email)
    {
        if(mailAttachment!=null && mailAttachment.length>0)
        {
            try {
                for(int i=0; i<mailAttachment.length; i++)
                {
                    InputStream fileInputStream = new FileInputStream(mailAttachment[i]);
                    email.addAttachment(new InputStreamDataSource(mailAttachmentFileName[i], mailAttachmentContentType[i], fileInputStream));
                    fileInputStream.close();
                }
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return email;
    }


    public File[] getMailAttachment() {
        return mailAttachment;
    }

    public void setMailAttachment(File[] mailAttachment) {
        this.mailAttachment = mailAttachment;
    }

    public String[] getMailAttachmentFileName() {
        return mailAttachmentFileName;
    }

    public void setMailAttachmentFileName(String[] mailAttachmentFileName) {
        this.mailAttachmentFileName = mailAttachmentFileName;
    }

    public String[] getMailAttachmentContentType() {
        return mailAttachmentContentType;
    }

    public void setMailAttachmentContentType(String[] mailAttachmentContentType) {
        this.mailAttachmentContentType = mailAttachmentContentType;
    }

    public PIBReporting getPibReporting() {
        return pibReporting;
    }

    public void setPibReporting(PIBReporting pibReporting) {
        this.pibReporting = pibReporting;
    }

    public Map<String, Long> getAgencyContactMap() {
        return agencyContactMap;
    }

    public void setAgencyContactMap(Map<String, Long> agencyContactMap) {
        this.agencyContactMap = agencyContactMap;
    }

	public Long getAttachmentFieldID() {
		return attachmentFieldID;
	}

	public void setAttachmentFieldID(Long attachmentFieldID) {
		this.attachmentFieldID = attachmentFieldID;
	}
	public void setAttachmentName(String attachmentName) {
		this.attachmentName = attachmentName;
	}



}
