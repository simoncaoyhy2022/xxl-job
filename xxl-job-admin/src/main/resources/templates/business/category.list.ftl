<!DOCTYPE html>
<html>
<head>
    <#-- import macro -->
    <#import "../framework/common/common.macro.ftl" as netCommon>

    <!-- 1-style start -->
    <@netCommon.commonStyle />
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/bootstrap-table/bootstrap-table.min.css">
    <!-- 1-style end -->

</head>
<body class="hold-transition" style="background-color: #ecf0f5;">
<div class="wrapper">
    <section class="content">

        <!-- 2-content start -->

        <#-- 查询区域 -->
        <div class="box" style="margin-bottom:9px;">
            <div class="box-body">
                <div class="row" id="data_filter" >
                    <div class="col-xs-3">
                        <div class="input-group">
                            <span class="input-group-addon">${I18n.jobcategory_field_name}</span>
                            <input type="text" class="form-control" id="name" placeholder="${I18n.system_please_input}${I18n.jobcategory_field_name}" >
                        </div>
                    </div>
                    <div class="col-xs-1">
                        <button class="btn btn-block btn-primary searchBtn" >${I18n.system_search}</button>
                    </div>
                    <div class="col-xs-1">
                        <button class="btn btn-block btn-default resetBtn" >${I18n.system_reset}</button>
                    </div>
                </div>
            </div>
        </div>

        <#-- 数据表格区域 -->
        <div class="row">
            <div class="col-xs-12">
                <div class="box">
                    <div class="box-header pull-left" id="data_operation" >
                        <button class="btn btn-sm btn-info add" type="button"><i class="fa fa-plus" ></i>${I18n.system_opt_add}</button>
                        <button class="btn btn-sm btn-warning selectOnlyOne update" type="button"><i class="fa fa-edit"></i>${I18n.system_opt_edit}</button>
                        <button class="btn btn-sm btn-danger selectOnlyOne delete" type="button"><i class="fa fa-remove "></i>${I18n.system_opt_del}</button>
                    </div>
                    <div class="box-body" >
                        <table id="data_list" class="table table-bordered table-striped" width="100%" >
                            <thead></thead>
                            <tbody></tbody>
                            <tfoot></tfoot>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <!-- 新增.模态框 -->
        <div class="modal fade" id="addModal" tabindex="-1" role="dialog"  aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h4 class="modal-title" >${I18n.system_opt_add} ${I18n.jobcategory}</h4>
                    </div>
                    <div class="modal-body">
                        <form class="form-horizontal form" role="form" >
                            <div class="form-group">
                                <label class="col-sm-2 control-label">${I18n.jobcategory_field_name}<font color="red">*</font></label>
                                <div class="col-sm-9"><input type="text" class="form-control" name="name" placeholder="${I18n.system_please_input}${I18n.jobcategory_field_name}" maxlength="64" ></div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-2 control-label">${I18n.jobcategory_field_remark}</label>
                                <div class="col-sm-9"><input type="text" class="form-control" name="remark" placeholder="${I18n.system_please_input}${I18n.jobcategory_field_remark}" maxlength="255" ></div>
                            </div>
                            <hr>
                            <div class="form-group">
                                <div class="col-sm-offset-4 col-sm-4">
                                    <button type="submit" class="btn btn-primary"  >${I18n.system_save}</button>
                                    <button type="button" class="btn btn-default" data-dismiss="modal">${I18n.system_cancel}</button>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>

        <!-- 更新.模态框 -->
        <div class="modal fade" id="updateModal" tabindex="-1" role="dialog"  aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h4 class="modal-title" >${I18n.system_opt_edit} ${I18n.jobcategory}</h4>
                    </div>
                    <div class="modal-body">
                        <form class="form-horizontal form" role="form" >
                            <div class="form-group">
                                <label class="col-sm-2 control-label">${I18n.jobcategory_field_name}<font color="red">*</font></label>
                                <div class="col-sm-9"><input type="text" class="form-control" name="name" placeholder="${I18n.system_please_input}${I18n.jobcategory_field_name}" maxlength="64" ></div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-2 control-label">${I18n.jobcategory_field_remark}</label>
                                <div class="col-sm-9"><input type="text" class="form-control" name="remark" placeholder="${I18n.system_please_input}${I18n.jobcategory_field_remark}" maxlength="255" ></div>
                            </div>
                            <hr>
                            <div class="form-group">
                                <div class="col-sm-offset-4 col-sm-4">
                                    <button type="submit" class="btn btn-primary"  >${I18n.system_save}</button>
                                    <button type="button" class="btn btn-default" data-dismiss="modal">${I18n.system_cancel}</button>
                                    <input type="hidden" name="id" >
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>

        <!-- 2-content end -->

    </section>
</div>

<!-- 3-script start -->
<@netCommon.commonScript />
<script src="${request.contextPath}/static/plugins/bootstrap-table/bootstrap-table.min.js"></script>
<script src="${request.contextPath}/static/plugins/bootstrap-table/locale/<#if I18n.admin_i18n?? && I18n.admin_i18n == 'en'>bootstrap-table-en-US.min.js<#else>bootstrap-table-zh-CN.min.js</#if>"></script>
<#-- admin table -->
<script src="${request.contextPath}/static/framework/admin.table.js"></script>
<script>
    $(function() {

        /**
         * init table
         */
        $.adminTable.initTable({
            table: '#data_list',
            url: base_url + "/jobcategory/pageList",
            queryParams: function (params) {
                var obj = {};
                obj.name = $('#name').val();
                obj.offset = params.offset;
                obj.pagesize = params.limit;
                return obj;
            },
            columns:[
                {
                    checkbox: true,
                    field: 'state',
                    width: '5',
                    widthUnit: '%',
                    align: 'center',
                    valign: 'middle'
                },{
                    title: I18n.jobcategory_field_name,
                    field: 'name',
                    width: '30',
                    widthUnit: '%',
                    align: 'left'
                },{
                    title: I18n.jobcategory_field_remark,
                    field: 'remark',
                    width: '45',
                    widthUnit: '%',
                    align: 'left',
                    formatter: function(value, row, index) {
                        return value ? value : I18n.system_empty;
                    }
                }
            ]
        });

        /**
         * init delete
         */
        $.adminTable.initDelete({
            url: base_url + "/jobcategory/delete"
        });

        /**
         * init add
         */
        $.adminTable.initAdd( {
            url: base_url + "/jobcategory/insert",
            rules : {
                name : {
                    required : true,
                    rangelength:[2, 64]
                }
            },
            messages : {
                name : {
                    required : I18n.system_please_input+I18n.jobcategory_field_name,
                    rangelength: I18n.system_length_limit + ' [2~64]'
                }
            },
            readFormData: function() {
                return $("#addModal .form").serializeArray();
            }
        });

        /**
         * init update
         */
        $.adminTable.initUpdate( {
            url: base_url + "/jobcategory/update",
            writeFormData: function(row) {
                $("#updateModal .form input[name='id']").val( row.id );
                $("#updateModal .form input[name='name']").val( row.name );
                $("#updateModal .form input[name='remark']").val( row.remark );
            },
            rules : {
                name : {
                    required : true,
                    rangelength:[2, 64]
                }
            },
            messages : {
                name : {
                    required : I18n.system_please_input+I18n.jobcategory_field_name,
                    rangelength: I18n.system_length_limit + ' [2~64]'
                }
            },
            readFormData: function() {
                return $("#updateModal .form").serializeArray();
            }
        });

    });
</script>
<!-- 3-script end -->

</body>
</html>