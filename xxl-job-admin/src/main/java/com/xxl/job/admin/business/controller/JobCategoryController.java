package com.xxl.job.admin.business.controller;

import com.xxl.job.admin.business.mapper.XxlJobCategoryMapper;
import com.xxl.job.admin.business.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.business.model.XxlJobCategory;
import com.xxl.job.admin.framework.constant.Consts;
import com.xxl.job.admin.framework.util.I18nUtil;
import com.xxl.job.admin.framework.util.XssUtil;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.tool.core.CollectionTool;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.List;

/**
 * job category controller
 */
@Controller
@RequestMapping("/jobcategory")
public class JobCategoryController {

    @Resource
    private XxlJobCategoryMapper xxlJobCategoryMapper;
    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

    @RequestMapping
    @XxlSso(role = Consts.ADMIN_ROLE)
    public String index(Model model) {
        return "business/category.list";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<PageModel<XxlJobCategory>> pageList(@RequestParam(required = false, defaultValue = "0") int offset,
                                                         @RequestParam(required = false, defaultValue = "10") int pagesize,
                                                         String name) {

        List<XxlJobCategory> list = xxlJobCategoryMapper.pageList(offset, pagesize, name);
        int list_count = xxlJobCategoryMapper.pageListCount(offset, pagesize, name);

        PageModel<XxlJobCategory> pageModel = new PageModel<>();
        pageModel.setData(list);
        pageModel.setTotal(list_count);

        return Response.ofSuccess(pageModel);
    }

    @RequestMapping("/insert")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> insert(XxlJobCategory xxlJobCategory) {

        // valid name
        if (StringTool.isBlank(xxlJobCategory.getName())) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("jobcategory_field_name"));
        }
        if (xxlJobCategory.getName().length() < 2 || xxlJobCategory.getName().length() > 64) {
            return Response.ofFail(I18nUtil.getString("jobcategory_field_name") + I18nUtil.getString("system_length_limit") + " [2~64]");
        }
        if (XssUtil.hasXss(xxlJobCategory.getName())) {
            return Response.ofFail(I18nUtil.getString("jobcategory_field_name") + I18nUtil.getString("system_invalid"));
        }
        if (StringTool.isNotBlank(xxlJobCategory.getRemark()) && XssUtil.hasXss(xxlJobCategory.getRemark())) {
            return Response.ofFail(I18nUtil.getString("jobgroup_field_registryList") + I18nUtil.getString("system_invalid"));
        }

        // valid repeat
        List<XxlJobCategory> exists = xxlJobCategoryMapper.pageList(0, 1, xxlJobCategory.getName());
        if (CollectionTool.isNotEmpty(exists)) {
            for (XxlJobCategory item : exists) {
                if (item.getName().equals(xxlJobCategory.getName())) {
                    return Response.ofFail(I18nUtil.getString("jobcategory_field_name") + I18nUtil.getString("system_repeat"));
                }
            }
        }

        xxlJobCategory.setAddTime(new Date());
        xxlJobCategory.setUpdateTime(new Date());

        int ret = xxlJobCategoryMapper.save(xxlJobCategory);
        return (ret > 0) ? Response.ofSuccess() : Response.ofFail();
    }

    @RequestMapping("/update")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> update(XxlJobCategory xxlJobCategory) {

        XxlJobCategory exists = xxlJobCategoryMapper.load(xxlJobCategory.getId());
        if (exists == null) {
            return Response.ofFail(I18nUtil.getString("system_not_found"));
        }

        if (StringTool.isBlank(xxlJobCategory.getName())) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("jobcategory_field_name"));
        }
        if (xxlJobCategory.getName().length() < 2 || xxlJobCategory.getName().length() > 64) {
            return Response.ofFail(I18nUtil.getString("jobcategory_field_name") + I18nUtil.getString("system_length_limit") + " [2~64]");
        }
        if (XssUtil.hasXss(xxlJobCategory.getName())) {
            return Response.ofFail(I18nUtil.getString("jobcategory_field_name") + I18nUtil.getString("system_invalid"));
        }
        if (StringTool.isNotBlank(xxlJobCategory.getRemark()) && XssUtil.hasXss(xxlJobCategory.getRemark())) {
            return Response.ofFail(I18nUtil.getString("jobgroup_field_registryList") + I18nUtil.getString("system_invalid"));
        }

        xxlJobCategory.setUpdateTime(new Date());
        int ret = xxlJobCategoryMapper.update(xxlJobCategory);
        return (ret > 0) ? Response.ofSuccess() : Response.ofFail();
    }

    @RequestMapping("/delete")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> delete(@RequestParam("ids[]") List<Integer> ids) {

        if (CollectionTool.isEmpty(ids) || ids.size() != 1) {
            return Response.ofFail(I18nUtil.getString("system_please_choose") + I18nUtil.getString("system_one") + I18nUtil.getString("system_data"));
        }
        int id = ids.get(0);

        XxlJobCategory exists = xxlJobCategoryMapper.load(id);
        if (exists == null) {
            return Response.ofSuccess();
        }

        // whether exists job using this category
        int count = xxlJobInfoMapper.pageListCount(0, 10, 0, -1, null, null, null, id);
        if (count > 0) {
            return Response.ofFail(I18nUtil.getString("jobcategory_del_limit_0"));
        }

        int ret = xxlJobCategoryMapper.remove(id);
        return (ret > 0) ? Response.ofSuccess() : Response.ofFail();
    }

    /**
     * open to normal user, for job-list dropdown rendering (no role restriction)
     */
    @RequestMapping("/loadById")
    @ResponseBody
    public Response<XxlJobCategory> loadById(@RequestParam("id") int id) {
        XxlJobCategory item = xxlJobCategoryMapper.load(id);
        return item != null ? Response.ofSuccess(item) : Response.ofFail();
    }

}