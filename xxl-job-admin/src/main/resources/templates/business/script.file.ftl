<!DOCTYPE html>
<html>
<head><#import "../framework/common/common.macro.ftl" as netCommon><@netCommon.commonStyle />
    <style>
        .repository-panel {
            min-height: 560px
        }

        .repository-tree {
            border-right: 1px solid #eee;
            min-height: 560px;
            padding: 10px 15px
        }

        .repository-tree a {
            display: block;
            padding: 7px 9px;
            color: #444;
            border-radius: 3px;
            cursor: pointer
        }

        .repository-tree a:hover, .repository-tree a.active {
            background: #e8f4fb;
            color: #337ab7
        }

        .repository-tree .folder {
            padding-left: 18px
        }

        .repository-path {
            font-size: 16px;
            font-weight: bold
        }

        .repository-path a {
            cursor: pointer
        }

        .repository-table .file-name {
            cursor: pointer
        }

        .repository-table .fa-folder {
            color: #e6a23c
        }

        .repository-table .fa-file {
            color: #409eff
        }

        .repository-empty {
            padding: 30px;
            text-align: center;
            color: #999
        }
    </style>
</head>
<body class="hold-transition" style="background:#ecf0f5">
<div class="wrapper">
    <section class="content">
        <div class="box repository-panel">
            <div class="box-body">
                <div class="row">
                    <div class="col-sm-3 repository-tree">
                        <div class="input-group input-group-sm"><input id="folderSearch" class="form-control"
                                                                       placeholder="筛选目录"><span
                                    class="input-group-btn"><button class="btn btn-default" id="refreshTree"><i
                                            class="fa fa-refresh"></i></button></span></div>
                        <hr style="margin:10px 0">
                        <div id="folderTree"></div>
                    </div>
                    <div class="col-sm-9">
                        <div class="clearfix" style="padding:10px 0 15px">
                            <div class="pull-left repository-path" id="breadcrumb"></div>
                            <div class="pull-right">
                                <button class="btn btn-sm btn-info" id="uploadBtn"><i class="fa fa-upload"></i> 上传脚本
                                </button>
                                <button class="btn btn-sm btn-success" id="newFolderBtn"><i class="fa fa-folder-o"></i>
                                    新建目录
                                </button>
                                <button class="btn btn-sm btn-default" id="refreshList"><i class="fa fa-refresh"></i> 刷新
                                </button>
                            </div>
                        </div>
                        <table class="table table-bordered table-hover repository-table">
                            <thead>
                            <tr>
                                <th width="125">ID</th>
                                <th>名称</th>
                                <th width="110">类型</th>
                                <th width="90">大小</th>
                                <th width="90">状态</th>
                                <th width="165">更新时间</th>
                                <th width="145">操作</th>
                            </tr>
                            </thead>
                            <tbody id="entryList"></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
        <div class="modal fade" id="uploadModal">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header"><h4 class="modal-title">上传脚本</h4></div>
                    <div class="modal-body">
                        <form class="form-horizontal" id="uploadForm"><input type="hidden" name="directory">
                            <div class="form-group"><label class="col-sm-3 control-label">目标目录</label>
                                <div class="col-sm-8"><p class="form-control-static" id="uploadDirectory"></p></div>
                            </div>
                            <div class="form-group"><label class="col-sm-3 control-label">脚本类型 *</label>
                                <div class="col-sm-8"><select name="scriptType" class="form-control">
                                        <option value="KETTLE">KETTLE (.ktr/.kjb)</option>
                                        <option value="HOP">HOP (.hpl/.hwf/.hwl)</option>
                                        <option value="PYTHON">PYTHON (.py)</option>
                                    </select></div>
                            </div>
                            <div class="form-group"><label class="col-sm-3 control-label">文件 *</label>
                                <div class="col-sm-8"><input id="scriptFiles" type="file" class="form-control" multiple>
                                    <p class="help-block">可一次选择多个文件；脚本名称默认使用文件名。</p>
                                    <ul id="selectedFiles" class="list-unstyled text-muted"></ul>
                                </div>
                            </div>
                            <div class="form-group"><label class="col-sm-3 control-label">备注</label>
                                <div class="col-sm-8"><textarea name="remark" class="form-control"
                                                                maxlength="500"></textarea></div>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-primary" id="submitUpload">上传</button>
                        <button class="btn btn-default" data-dismiss="modal">取消</button>
                    </div>
                </div>
            </div>
        </div>
    </section>
</div>
<@netCommon.commonScript />
<script>
    $(function () {
        var currentPath = '', currentEntries = [];

        function esc(v) {
            return $('<div>').text(v || '').html()
        }

        function prettyPath(path) {
            return path ? path : '脚本仓库'
        }

        function typeForPath(path) {
            return (path || '').split('/')[0].toUpperCase()
        }

        function load(path) {
            currentPath = path || '';
            $.get(base_url + '/scriptfile/entries', {path: currentPath}, function (r) {
                if (r.code != 200) {
                    layer.msg(r.msg);
                    return
                }
                currentEntries = r.data || [];
                renderEntries();
                renderBreadcrumb();
                loadTree();
            })
        }

        function renderBreadcrumb() {
            var html = '<a data-path="">脚本仓库</a>', parts = currentPath ? currentPath.split('/') : [], now = '';
            $.each(parts, function (_, part) {
                now = now ? now + '/' + part : part;
                html += ' <span class="text-muted">/</span> <a data-path="' + esc(now) + '">' + esc(part) + '</a>'
            });
            $('#breadcrumb').html(html)
        }


        function renderEntries() {
            var html = '';
            $.each(currentEntries, function (_, e) {
                if (e.directory) {
                    html += '<tr><td>-</td><td class="file-name" data-path="' + esc(e.path) + '"><i class="fa fa-folder"></i> ' + esc(e.name) + '</td><td>目录</td><td>-</td><td>-</td><td>-</td><td><a class="delete-folder text-danger" data-path="' + esc(e.path) + '">删除</a></td></tr>'
                } else {
                    html += '<tr><td><code>scriptId=' + e.id + '</code></td><td><i class="fa fa-file"></i> ' + esc(e.name) + '</td><td>' + esc(e.scriptType) + ' / ' + esc(e.scriptSubtype) + '</td><td>' + formatSize(e.fileSize) + '</td><td>' + (e.status == 1 ? '启用' : '停用') + '</td><td>' + formatTime(e.updateTime) + '</td><td><a href="' + base_url + '/scriptfile/download/' + e.id + '">下载</a> &nbsp; <a class="delete-file text-danger" data-id="' + e.id + '">删除</a></td></tr>'
                }
            });
            $('#entryList').html(html || '<tr><td colspan="7" class="repository-empty">当前目录为空</td></tr>')
        }


        function loadTree() {
            $.get(base_url + '/scriptfile/directories', function (r) {
                if (r.code != 200) {
                    return
                }
                var html = '<a class="' + (!currentPath ? 'active' : '') + '" data-path=""><i class="fa fa-folder-open"></i> 脚本仓库</a>';
                $.each(r.data || [], function (_, e) {
                    var level = e.path.split('/').length - 1;
                    html += '<a class="folder ' + (e.path === currentPath ? 'active' : '') + '" data-path="' + esc(e.path) + '" style="padding-left:' + (18 + level * 18) + 'px"><i class="fa fa-folder"></i> ' + esc(e.name) + '</a>'
                });
                $('#folderTree').html(html)
            })
        }

        function formatSize(size) {
            if (size == null) return '-';
            return size < 1048576 ? (size / 1024).toFixed(1) + ' KB' : (size / 1048576).toFixed(1) + ' MB'
        }

        function formatTime(v) {
            if (!v) return '-';
            return new Date(v).toLocaleString()
        }

        $('#folderTree').on('click', 'a', function () {
            load($(this).data('path'))
        });
        $('#breadcrumb').on('click', 'a', function () {
            load($(this).data('path'))
        });
        $('#entryList').on('click', '.file-name', function () {
            load($(this).data('path'))
        });
        $('#refreshList,#refreshTree').click(function () {
            load(currentPath)
        });
        $('#folderSearch').on('input', function () {
            var q = $(this).val().toLowerCase();
            $('#folderTree a').each(function () {
                $(this).toggle($(this).text().toLowerCase().indexOf(q) >= 0)
            })
        });
        $('#newFolderBtn').click(function () {
            if (!currentPath) {
                layer.msg('请先进入 KETTLE、HOP 或 PYTHON 目录');
                return
            }
            layer.prompt({title: '新建目录', formType: 0}, function (name, index) {
                $.post(base_url + '/scriptfile/directory', {parentPath: currentPath, name: name}, function (r) {
                    if (r.code == 200) {
                        layer.close(index);
                        load(currentPath)
                    } else layer.msg(r.msg)
                })
            })
        });
        $('#uploadBtn').click(function () {
            if (!currentPath) {
                layer.msg('请先进入 KETTLE、HOP 或 PYTHON 目录');
                return
            }
            var type = typeForPath(currentPath);
            if (['KETTLE', 'HOP', 'PYTHON'].indexOf(type) < 0) {
                layer.msg('请在脚本类型目录下上传');
                return
            }
            $('#uploadForm')[0].reset();
            $('#selectedFiles').empty();
            $('#uploadForm [name=directory]').val(currentPath);
            $('#uploadDirectory').text(prettyPath(currentPath));
            $('#uploadForm [name=scriptType]').val(type);
            $('#uploadModal').modal('show')
        });
        $('#scriptFiles').change(function () {
            var html = '';
            $.each(this.files, function (_, file) {
                html += '<li><i class="fa fa-file"></i> ' + esc(file.name) + '</li>'
            });
            $('#selectedFiles').html(html)
        });
        $('#submitUpload').click(function () {
            var files = $('#scriptFiles')[0].files, form = $('#uploadForm')[0];
            if (!files.length) {
                layer.msg('请选择要上传的文件');
                return
            }
            var index = 0, failed = [];
            $('#submitUpload').prop('disabled', true).text('上传中 0/' + files.length);

            function next() {
                if (index >= files.length) {
                    $('#submitUpload').prop('disabled', false).text('上传');
                    if (failed.length) {
                        layer.msg('上传完成，失败 ' + failed.length + ' 个：' + failed.join('、'))
                    } else layer.msg('全部上传成功');
                    $('#uploadModal').modal('hide');
                    load(currentPath);
                    return
                }
                var file = files[index++], data = new FormData();
                data.append('directory', form.directory.value);
                data.append('scriptType', form.scriptType.value);
                data.append('remark', form.remark.value);
                data.append('name', file.name);
                data.append('file', file);
                $.ajax({
                    url: base_url + '/scriptfile/upload',
                    type: 'POST',
                    data: data,
                    processData: false,
                    contentType: false,
                    success: function (r) {
                        if (r.code != 200) failed.push(file.name)
                    },
                    error: function () {
                        failed.push(file.name)
                    },
                    complete: function () {
                        $('#submitUpload').text('上传中 ' + index + '/' + files.length);
                        next()
                    }
                })
            }

            next()
        });
        $('#entryList').on('click', '.delete-file', function () {
            var id = $(this).data('id');
            layer.confirm('确定删除该文件吗？', function (index) {
                $.post(base_url + '/scriptfile/delete', {'ids[]': id}, function (r) {
                    if (r.code == 200) {
                        layer.close(index);
                        load(currentPath)
                    } else layer.msg(r.msg)
                })
            })
        });
        $('#entryList').on('click', '.delete-folder', function () {
            var path = $(this).data('path');
            layer.confirm('删除目录会同时删除其内全部脚本，确定继续吗？', function (index) {
                $.post(base_url + '/scriptfile/directory/delete', {path: path}, function (r) {
                    if (r.code == 200) {
                        layer.close(index);
                        load(currentPath)
                    } else layer.msg(r.msg)
                })
            })
        });
        load('');
    });
</script>
</body>
</html>
