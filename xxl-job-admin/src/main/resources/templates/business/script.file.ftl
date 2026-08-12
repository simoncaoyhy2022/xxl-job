<!DOCTYPE html>
<html>
<head><#import "../framework/common/common.macro.ftl" as netCommon><@netCommon.commonStyle />
    <style>

        .repository-panel {
            min-height: 600px
        }

        .repository-tree {
            border-right: 1px solid #eee;
            min-height: 600px;
            padding: 10px 15px
        }

        #folderTree {
            max-height: calc(100vh - 240px); /* 也可以写固定值如: max-height: 520px; */
            overflow-y: auto;
            overflow-x: auto;
        }

        #folderTree::-webkit-scrollbar {
            width: 6px;
            height: 6px;
        }
        #folderTree::-webkit-scrollbar-thumb {
            background: #dcdcdc;
            border-radius: 3px;
        }
        #folderTree::-webkit-scrollbar-thumb:hover {
            background: #b0b0b0;
        }

        .repository-tree a {
            display: block;
            padding: 7px 9px;
            color: #444;
            border-radius: 3px;
            cursor: pointer;
            /* 新增：禁止双击时选中文字 */
            user-select: none;
            -webkit-user-select: none;
            -moz-user-select: none;
            -ms-user-select: none;
        }

        .repository-tree a:hover, .repository-tree a.active {
            background: #e8f4fb;
            color: #337ab7
        }

        .repository-tree .folder {
            padding-left: 18px
        }

        /* 扩大展开/折叠图标的点击热区 (22x22px) 并增加悬浮高亮效果 */
        .repository-tree .toggle-btn {
            display: inline-block;
            width: 22px;
            height: 22px;
            line-height: 22px;
            text-align: center;
            font-size: 13px; /* 稍稍增大图标 */
            margin-right: 3px;
            margin-left: -5px; /* 修正左侧对齐微调 */
            border-radius: 3px;
            color: #666;
            cursor: pointer;
            vertical-align: middle;
            transition: background-color 0.15s ease;
        }

        /* 鼠标悬浮在小箭头上时给出明显的背景高亮 */
        .repository-tree .toggle-btn:hover {
            background-color: #d0e5f2;
            color: #23527c;
        }

        /* 无子目录时的占位块，确保文字对齐 */
        .repository-tree .toggle-spacer {
            display: inline-block;
            width: 22px;
            margin-right: 3px;
            margin-left: -5px;
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
                        <div class="input-group input-group-sm">
                            <input id="folderSearch" class="form-control" placeholder="筛选目录">
                            <span class="input-group-btn"><button class="btn btn-default" id="refreshTree">
                                    <i class="fa fa-refresh"></i></button></span></div>
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
                                <th width="130">ID</th>
                                <th>名称</th>
                                <th width="130">类型</th>
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

        // 1. 新增：记录被折叠的目录路径集合
        var collapsedNodes = {};

        // 标记左侧目录树是否已完成过初始加载
        var initialTreeLoaded = false;

        // 2. 新增：根据节点的父级折叠状态动态更新树节点的显示/隐藏
        function updateTreeVisibility() {
            $('#folderTree a').each(function () {
                var parent = $(this).attr('data-parent');
                if (parent === undefined) return; // 根节点始终展示

                var isHidden = false;
                var curr = parent;
                while (true) {
                    if (collapsedNodes[curr]) {
                        isHidden = true;
                        break;
                    }
                    if (curr === '') break;
                    var idx = curr.lastIndexOf('/');
                    curr = idx > -1 ? curr.substring(0, idx) : '';
                }
                $(this).toggle(!isHidden);
            });
        }

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
                // loadTree();
                updateTreeActive(); // 原来是 loadTree()，改为 updateTreeActive()
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


        // 仅更新左侧树的选中高亮状态，不重绘 DOM 节点
        function updateTreeActive() {
            $('#folderTree a').removeClass('active');
            $('#folderTree a').each(function () {
                var p = $(this).attr('data-path') || '';
                if (p === currentPath) {
                    $(this).addClass('active');
                }
            });
        }


        function loadTree() {
            $.get(base_url + '/scriptfile/directories', function (r) {
                if (r.code != 200) {
                    return;
                }
                var data = r.data || [];

                // 1. 统计各路径是否有子目录，便于显示展开/折叠小箭头
                var hasChildren = {};
                $.each(data, function (_, e) {
                    var parent = e.path.lastIndexOf('/') > -1 ? e.path.substring(0, e.path.lastIndexOf('/')) : '';
                    hasChildren[parent] = true;
                });
                hasChildren[''] = data.length > 0;

                // 1.1 首次加载时，折叠所有层级目录，仅展开到第1层（kettle/hop/python）
                if (!initialTreeLoaded) {
                    $.each(data, function (_, e) {
                        if (hasChildren[e.path]) {
                            collapsedNodes[e.path] = true;
                        }
                    });
                    initialTreeLoaded = true;
                }

                // 2. 渲染根节点（脚本仓库）
                var rootCollapsed = !!collapsedNodes[''];
                var rootIcon = hasChildren['']
                    ? '<i class="fa ' + (rootCollapsed ? 'fa-caret-right' : 'fa-caret-down') + ' toggle-btn"></i>'
                    : '<span class="toggle-spacer"></span>';
                var html = '<a class="' + (!currentPath ? 'active' : '') + (rootCollapsed ? ' collapsed' : '') + '" data-path="" data-parent-root="true">' + rootIcon + '<i class="fa fa-folder-open"></i> 脚本仓库</a>';

                // 3. 渲染各个层级的子目录节点
                $.each(data, function (_, e) {
                    var parentPath = e.path.lastIndexOf('/') > -1 ? e.path.substring(0, e.path.lastIndexOf('/')) : '';
                    var level = e.path.split('/').length - 1;
                    var isParent = !!hasChildren[e.path];
                    var isCollapsed = !!collapsedNodes[e.path];
                    var toggleIcon = isParent
                        ? '<i class="fa ' + (isCollapsed ? 'fa-caret-right' : 'fa-caret-down') + ' toggle-btn"></i>'
                        : '<span class="toggle-spacer"></span>';

                    html += '<a class="folder ' + (e.path === currentPath ? 'active' : '') + (isCollapsed ? ' collapsed' : '') + '" data-path="' + esc(e.path) + '" data-parent="' + esc(parentPath) + '" style="padding-left:' + (18 + level * 18) + 'px">' +
                        toggleIcon +
                        '<i class="fa fa-folder"></i> ' + esc(e.name) + '</a>';
                });

                // 4. 插入页面 DOM，并重新计算应用当前树节点的展开/折叠显隐状态
                $('#folderTree').html(html);
                updateTreeVisibility();
            });
        }

        function formatSize(size) {
            if (size == null) return '-';
            return size < 1048576 ? (size / 1024).toFixed(1) + ' KB' : (size / 1048576).toFixed(1) + ' MB'
        }

        function formatTime(v) {
            if (!v) return '-';
            return new Date(v).toLocaleString()
        }

// 1. 单击目录：只有在切换到“不同目录”时才重新加载，防止重复销毁 DOM
        $('#folderTree').on('click', 'a', function (e) {
            var path = $(this).attr('data-path') || '';
            if (path !== currentPath) {
                load(path);
            }
        });

// 2. 双击整行：展开/折叠该目录
        $('#folderTree').on('dblclick', 'a', function (e) {
            e.preventDefault();

            // 清除双击时浏览器默认选中的蓝色文本高亮
            if (window.getSelection) {
                window.getSelection().removeAllRanges();
            }

            var path = $(this).attr('data-path') || '';
            var $toggleBtn = $(this).find('.toggle-btn');

            // 只有非叶子节点（带展开/折叠图标的节点）才执行折叠/展开
            if ($toggleBtn.length > 0) {
                if (collapsedNodes[path]) {
                    delete collapsedNodes[path];
                    $toggleBtn.removeClass('fa-caret-right').addClass('fa-caret-down');
                } else {
                    collapsedNodes[path] = true;
                    $toggleBtn.removeClass('fa-caret-down').addClass('fa-caret-right');
                }
                updateTreeVisibility();
            }
        });

        // 3.1 折叠/展开图标点击事件（阻止事件冒泡，避免误触发目录选中）
        $('#folderTree').on('click', '.toggle-btn', function (e) {
            e.stopPropagation();
            var $a = $(this).closest('a');
            var path = $a.attr('data-path') || '';
            if (collapsedNodes[path]) {
                delete collapsedNodes[path];
                $(this).removeClass('fa-caret-right').addClass('fa-caret-down');
            } else {
                collapsedNodes[path] = true;
                $(this).removeClass('fa-caret-down').addClass('fa-caret-right');
            }
            updateTreeVisibility();
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

        // 3.2 替换/修改现有的目录筛选事件（清空筛选时恢复折叠/展开状态）
        $('#folderSearch').on('input', function () {
            var q = $(this).val().toLowerCase().trim();
            if (!q) {
                updateTreeVisibility();
                return;
            }
            $('#folderTree a').each(function () {
                $(this).toggle($(this).text().toLowerCase().indexOf(q) >= 0);
            });
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
                        load(currentPath);
                        loadTree(); // 刷新左侧树结构
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
                        load(currentPath);
                        // loadTree(); // 刷新左侧树结构
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
                        load(currentPath);
                        loadTree(); // 刷新左侧树结构
                    } else layer.msg(r.msg)
                })
            })
        });
        loadTree(); // 1. 加载左侧目录树
        load('');
    });
</script>
</body>
</html>
