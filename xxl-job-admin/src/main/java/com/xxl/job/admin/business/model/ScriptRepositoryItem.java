package com.xxl.job.admin.business.model;

import java.util.Date;

/** A direct child of a directory in the script repository. */
public class ScriptRepositoryItem {
    private String name;
    private String path;
    private boolean directory;
    private Integer id;
    private String scriptType;
    private String scriptSubtype;
    private Long fileSize;
    private Integer status;
    private Date updateTime;

    public String getName() { return name; } public void setName(String value) { name = value; }
    public String getPath() { return path; } public void setPath(String value) { path = value; }
    public boolean isDirectory() { return directory; } public void setDirectory(boolean value) { directory = value; }
    public Integer getId() { return id; } public void setId(Integer value) { id = value; }
    public String getScriptType() { return scriptType; } public void setScriptType(String value) { scriptType = value; }
    public String getScriptSubtype() { return scriptSubtype; } public void setScriptSubtype(String value) { scriptSubtype = value; }
    public Long getFileSize() { return fileSize; } public void setFileSize(Long value) { fileSize = value; }
    public Integer getStatus() { return status; } public void setStatus(Integer value) { status = value; }
    public Date getUpdateTime() { return updateTime; } public void setUpdateTime(Date value) { updateTime = value; }
}
