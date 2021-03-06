package com.grail.kantar.action;

import com.grail.kantar.beans.KantarBriefTemplate;
import com.grail.kantar.manager.KantarBriefTemplateManager;
import com.grail.synchro.util.SynchroPermHelper;
import com.jivesoftware.base.UnauthorizedException;
import com.jivesoftware.community.AttachmentException;
import com.jivesoftware.community.AttachmentManager;
import com.jivesoftware.community.AttachmentNotFoundException;
import com.jivesoftware.community.action.JiveActionSupport;
import com.jivesoftware.community.action.util.ActionUtils;
import com.jivesoftware.community.impl.dao.AttachmentBean;
import com.jivesoftware.community.lifecycle.JiveApplication;
import com.jivesoftware.util.ByteFormat;
import com.opensymphony.xwork2.Preparable;
import org.apache.struts2.ServletActionContext;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Bhakar
 * Date: 11/3/14
 * Time: 6:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class KantarDocumentsAction extends JiveActionSupport implements Preparable {

    private Long id;
    private KantarBriefTemplate kantarBriefTemplate;

    private String comments;

    private Long attachmentId;
    List<AttachmentBean> attachments;
    private boolean jiveUploadSizeLimitExceeded = false;
    private boolean canAttach = true;
    private long[] removeAttachID;
    private File[] attachFile;
    private String[] attachFileContentType;
    private String[] attachFileFileName;
    private int attachmentCount = 0;

    private boolean canEditProject;

    private boolean validate = false;

    private KantarBriefTemplateManager kantarBriefTemplateManager;

    @Override
    public void prepare() throws Exception {
        if(getRequest().getRequestURI().contains("/kantar/documents!execute.jspa")) {
            validate = true;
        } else {
            validate = false;
        }
        if(request.getParameter("id") != null && !request.getParameter("id").equals("")) {
            id = Long.parseLong(request.getParameter("id"));
        }
        canEditProject = false;
        if(id != null && id > 0) {
            this.kantarBriefTemplate = kantarBriefTemplateManager.get(id);
            if(kantarBriefTemplate != null && (SynchroPermHelper.isSynchroAdmin(getUser()) || SynchroPermHelper.isSynchroMiniAdmin() ||
                    (kantarBriefTemplate.getBatContact() != null && kantarBriefTemplate.getBatContact().equals(getUser().getID()))
                            || (kantarBriefTemplate.getCreatedBy() != null && kantarBriefTemplate.getCreatedBy().equals(getUser().getID())))) {
                canEditProject = true;
            }
        }
    }

    /**
     *
     * @return
     */
    @Override
    public String input() {
        if(getUser() == null) {
            return UNAUTHENTICATED;
        }
        canEditProject = false;
        if(request.getParameter("id") != null) {
            id = Long.parseLong(request.getParameter("id"));
            kantarBriefTemplate = kantarBriefTemplateManager.get(id);
            if(!SynchroPermHelper.canAccessKantarProject(kantarBriefTemplate, getUser())) {
                return UNAUTHORIZED;
            }
            if(kantarBriefTemplate != null && (SynchroPermHelper.isSynchroAdmin(getUser()) || SynchroPermHelper.isSynchroMiniAdmin() ||
                    (kantarBriefTemplate.getBatContact() != null && kantarBriefTemplate.getBatContact().equals(getUser().getID()))
                            || (kantarBriefTemplate.getCreatedBy() != null && kantarBriefTemplate.getCreatedBy().equals(getUser().getID())))) {
                canEditProject = true;
            }
            attachments = kantarBriefTemplateManager.getKantarAttachments(id);
            if(attachments != null) {
                attachmentCount = attachments.size();
            }

        } else {
            return ERROR;
        }

        canAttach = canEditProject;
        return INPUT;
    }

    @Override
    public String execute() {
        if(canEditProject) {
            if(id != null && id > 0) {
                kantarBriefTemplate = kantarBriefTemplateManager.get(id);
                kantarBriefTemplate.setComments(comments);
                kantarBriefTemplate.setModifiedBy(getUser().getID());
                kantarBriefTemplate.setModificationDate(new Date());
                kantarBriefTemplateManager.save(kantarBriefTemplate);
                handleAttachments();
            }
        }
        return SUCCESS;
    }


    private void handleAttachments() {
        // add any attachments that we've been told to add
        if (attachFile != null && attachFile.length != 0) {
            // Check to see that the incoming request is a multipart request.
            if (ActionUtils.isMultiPart(ServletActionContext.getRequest())) {
                addAttachments();
            }
            else {
                addFieldError("attachFile", getText("attach.err.upload_errors.text"));
            }
        }

        if(removeAttachID != null && removeAttachID.length > 0) {
            for(long id : removeAttachID) {
                try {
                    kantarBriefTemplateManager.removeAttachment(id);
                } catch (AttachmentNotFoundException a) {
                    a.printStackTrace();
                } catch (AttachmentException b) {
                    b.printStackTrace();
                } catch (Exception c) {
                    c.printStackTrace();
                }
            }
        }
    }

    protected void addAttachments() throws UnauthorizedException {
        if (attachFile == null) {
            return;
        }
        log.debug("attachFile size was " + attachFile.length);

        for (int i = 0; i < attachFile.length; i++) {
            File file = attachFile[i];
            if (file == null) {
                log.debug("File was null, skipping");
                continue;
            }

            log.debug("File size was " + file.length());
            String fileName = attachFileFileName[i];
            String contentType = attachFileContentType[i];
            try
            {
                kantarBriefTemplateManager.addKantarAttachments(attachFile[i], attachFileFileName[i], attachFileContentType[i], id, getUser().getID());
                attachments = kantarBriefTemplateManager.getKantarAttachments(id);
            }
            catch (AttachmentException ae) {
                log.error("AttachmentException - " + ae.getMessage());
                handleAttachmentException(file, fileName, contentType, ae);
            } catch (UnauthorizedException ue) {
                log.error("UnauthorizedException - " + ue.getMessage());
            } catch (Exception e) {
                log.error("Exception - " + e.getMessage());
            }
        }

        if(attachments!=null)
            attachmentCount = attachments.size();
    }

    protected void handleAttachmentException(File file, String fileName, String contentType, AttachmentException e) {
        final AttachmentManager attachmentManager = JiveApplication.getContext().getAttachmentManager();
        if (e.getErrorType() == AttachmentException.TOO_LARGE) {
            List<Serializable> args = new ArrayList<Serializable>();
            args.add(fileName);
            String error = getText("attach.err.file_too_large.text", args);
            args.add(file.length());
            args.add(contentType);
            args.add(error);
            addFieldError("attachFile", error);
        }
        else if (e.getErrorType() == AttachmentException.BAD_CONTENT_TYPE) {
            List<Serializable> args = new ArrayList<Serializable>();
            args.add(fileName);
            String error = getText("attach.err.badContentType.text", args);
            args.add(file.length());
            args.add(contentType);
            args.add(error);

            addFieldError("attachFile", error);
        }
        else if (e.getErrorType() == AttachmentException.TOO_MANY_ATTACHMENTS) {
            addFieldError("attachFile", getText("attach.err.tooManyAttchmts.text"));
        }
        else {
            List<Serializable> args = new ArrayList<Serializable>();
            int maxAttachSize = attachmentManager.getMaxAttachmentSize();
            args.add((new ByteFormat()).formatKB(maxAttachSize));
            String error = getText("attach.err.no_read_perm.text", args);
            args.clear();
            args.add(fileName);
            args.add(file.length());
            args.add(contentType);
            args.add(error);

            addFieldError("attachFile", error);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(Long attachmentId) {
        this.attachmentId = attachmentId;
    }

    public List<AttachmentBean> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentBean> attachments) {
        this.attachments = attachments;
    }

    public boolean isJiveUploadSizeLimitExceeded() {
        return jiveUploadSizeLimitExceeded;
    }

    public void setJiveUploadSizeLimitExceeded(boolean jiveUploadSizeLimitExceeded) {
        this.jiveUploadSizeLimitExceeded = jiveUploadSizeLimitExceeded;
    }

    public boolean isCanAttach() {
        return canAttach;
    }

    public void setCanAttach(boolean canAttach) {
        this.canAttach = canAttach;
    }

    public long[] getRemoveAttachID() {
        return removeAttachID;
    }

    public void setRemoveAttachID(long[] removeAttachID) {
        this.removeAttachID = removeAttachID;
    }

    public File[] getAttachFile() {
        return attachFile;
    }

    public void setAttachFile(File[] attachFile) {
        this.attachFile = attachFile;
    }

    public String[] getAttachFileContentType() {
        return attachFileContentType;
    }

    public void setAttachFileContentType(String[] attachFileContentType) {
        this.attachFileContentType = attachFileContentType;
    }

    public String[] getAttachFileFileName() {
        return attachFileFileName;
    }

    public void setAttachFileFileName(String[] attachFileFileName) {
        this.attachFileFileName = attachFileFileName;
    }

    public int getAttachmentCount() {
        return attachmentCount;
    }

    public void setAttachmentCount(int attachmentCount) {
        this.attachmentCount = attachmentCount;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public KantarBriefTemplateManager getKantarBriefTemplateManager() {
        return kantarBriefTemplateManager;
    }

    public void setKantarBriefTemplateManager(KantarBriefTemplateManager kantarBriefTemplateManager) {
        this.kantarBriefTemplateManager = kantarBriefTemplateManager;
    }

    public KantarBriefTemplate getKantarBriefTemplate() {
        return kantarBriefTemplate;
    }

    public void setKantarBriefTemplate(KantarBriefTemplate kantarBriefTemplate) {
        this.kantarBriefTemplate = kantarBriefTemplate;
    }

    public boolean isCanEditProject() {
        return canEditProject;
    }

    public void setCanEditProject(boolean canEditProject) {
        this.canEditProject = canEditProject;
    }
}
