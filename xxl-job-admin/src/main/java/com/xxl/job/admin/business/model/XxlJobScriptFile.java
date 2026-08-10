package com.xxl.job.admin.business.model;

import java.util.Date;

/** Metadata for an executable script kept in the local script repository. */
public class XxlJobScriptFile {
    private Integer id;
    private String name;
    private String scriptType;
    private String scriptSubtype;
    private String originalFilename;
    private String relativePath;
    private Long fileSize;
    private String sha256;
    private Integer status;
    private String remark;
    private String createUser;
    private String updateUser;
    private Date createTime;
    private Date updateTime;
    public Integer getId(){return id;} public void setId(Integer v){id=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getScriptType(){return scriptType;} public void setScriptType(String v){scriptType=v;}
    public String getScriptSubtype(){return scriptSubtype;} public void setScriptSubtype(String v){scriptSubtype=v;}
    public String getOriginalFilename(){return originalFilename;} public void setOriginalFilename(String v){originalFilename=v;}
    public String getRelativePath(){return relativePath;} public void setRelativePath(String v){relativePath=v;}
    public Long getFileSize(){return fileSize;} public void setFileSize(Long v){fileSize=v;}
    public String getSha256(){return sha256;} public void setSha256(String v){sha256=v;}
    public Integer getStatus(){return status;} public void setStatus(Integer v){status=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public String getCreateUser(){return createUser;} public void setCreateUser(String v){createUser=v;}
    public String getUpdateUser(){return updateUser;} public void setUpdateUser(String v){updateUser=v;}
    public Date getCreateTime(){return createTime;} public void setCreateTime(Date v){createTime=v;}
    public Date getUpdateTime(){return updateTime;} public void setUpdateTime(Date v){updateTime=v;}
}
