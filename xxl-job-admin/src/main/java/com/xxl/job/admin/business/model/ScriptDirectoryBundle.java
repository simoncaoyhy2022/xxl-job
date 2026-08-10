package com.xxl.job.admin.business.model;

import java.util.List;

/** Manifest of all enabled scripts in the same directory as the entry script. */
public class ScriptDirectoryBundle {
    private String directory;
    private Integer entryScriptId;
    private String entryRelativePath;
    private List<XxlJobScriptFile> files;

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Integer getEntryScriptId() {
        return entryScriptId;
    }

    public void setEntryScriptId(Integer entryScriptId) {
        this.entryScriptId = entryScriptId;
    }

    public String getEntryRelativePath() {
        return entryRelativePath;
    }

    public void setEntryRelativePath(String entryRelativePath) {
        this.entryRelativePath = entryRelativePath;
    }

    public List<XxlJobScriptFile> getFiles() {
        return files;
    }

    public void setFiles(List<XxlJobScriptFile> files) {
        this.files = files;
    }
}
