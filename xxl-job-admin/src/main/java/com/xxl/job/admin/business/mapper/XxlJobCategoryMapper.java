package com.xxl.job.admin.business.mapper;

import com.xxl.job.admin.business.model.XxlJobCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface XxlJobCategoryMapper {

    List<XxlJobCategory> findAll();

    List<XxlJobCategory> pageList(@Param("offset") int offset,
                                  @Param("pagesize") int pagesize,
                                  @Param("name") String name);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("name") String name);

    XxlJobCategory load(@Param("id") int id);

    int save(XxlJobCategory xxlJobCategory);

    int update(XxlJobCategory xxlJobCategory);

    int remove(@Param("id") int id);

}