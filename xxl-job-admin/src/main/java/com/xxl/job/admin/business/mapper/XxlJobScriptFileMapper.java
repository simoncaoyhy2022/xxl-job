package com.xxl.job.admin.business.mapper;

import com.xxl.job.admin.business.model.XxlJobScriptFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface XxlJobScriptFileMapper {
    XxlJobScriptFile load(@Param("id") int id);
    List<XxlJobScriptFile> pageList(@Param("offset") int offset, @Param("pagesize") int pagesize, @Param("name") String name, @Param("scriptType") String scriptType);
    int pageListCount(@Param("name") String name, @Param("scriptType") String scriptType);
    int save(XxlJobScriptFile file);
    int update(XxlJobScriptFile file);
    int remove(@Param("id") int id);
    List<XxlJobScriptFile> findByRelativePathPrefix(@Param("pathPrefix") String pathPrefix);
}
