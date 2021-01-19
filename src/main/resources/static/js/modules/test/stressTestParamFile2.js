$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'test/stressParamFile/list',
        datatype: "json",
        colModel: [
            {label: '文件ID', name: 'fileId', width: 30, key: true},
            // {label: '用例ID', name: 'caseId', width: 35},
            {label: '用例名称', name: 'caseName', width: 35, sortable: false},
            {
                label: '文件名称',
                name: 'originName',
                width: 100,
                sortable: false,
                formatter: function (value, options, row) {
                    if (!(getExtension(row.originName) && /^(jmx)$/.test(getExtension(row.originName).toLowerCase()))) {
                        return value;
                    }
                    return "<a href='javascript:void(0);' onclick='" +
                        "ShowRunning(" + row.fileId + ")'>" + value + "</a>";
                }
            },
            {label: '添加时间', name: 'addTime', width: 70},
            {
                label: '状态', name: 'status', width: 45, formatter: function (value, options, row) {
                    if (value === 0) {
                        return '<span class="label label-info">创建成功</span>';
                    } else if (value === 1) {
                        return '<span class="label label-warning">正在执行</span>';
                    } else if (value === 2) {
                        if (!(getExtension(row.originName) && /^(jmx)$/.test(getExtension(row.originName).toLowerCase()))) {
                            return '<span class="label label-success">同步成功</span>';
                        }
                        return '<span class="label label-success">执行成功</span>';
                    } else if (value === 3) {
                        return '<span class="label label-danger">出现异常</span>';
                    }
                }
            },
            {
                label: '执行操作', name: '', width: 70, sortable: false, formatter: function (value, options, row) {
                    var btn = '';
                    if (!(getExtension(row.originName) && /^(jmx)$/.test(getExtension(row.originName).toLowerCase()))) {
                        btn = "<a href='#' class='btn btn-primary' onclick='synchronizeFile(" + row.fileId + ")' ><i class='fa fa-arrow-circle-right'></i>&nbsp;同步文件</a>";
                    } else {
                        if (row.status == 1) {
                            btn = "<a href='#' class='btn btn-danger' onclick='stopOnce(" + row.fileId + ")' ><i class='fa fa-stop-circle'></i>&nbsp;停止</a>";
                        } else {
                            btn = "<a href='#' class='btn btn-primary' onclick='runOnce(" + row.fileId + ")' ><i class='fa fa-arrow-circle-right'></i>&nbsp;启动</a>";
                        }
                    }
                    // var stopBtn = "<a href='#' class='btn btn-primary' onclick='stop(" + row.fileId + ")' ><i class='fa fa-stop'></i>&nbsp;停止</a>";
                    // var stopNowBtn = "<a href='#' class='btn btn-primary' onclick='stopNow(" + row.fileId + ")' ><i class='fa fa-times-circle'></i>&nbsp;强制停止</a>";
                    var downloadFileBtn = "&nbsp;&nbsp;<a href='" + baseURL + "test/stressParamFile/downloadFile/" + row.fileId + "' class='btn btn-primary'><i class='fa fa-download'></i>&nbsp;下载</a>";
                    return btn + downloadFileBtn;
                }
            }
        ],
        viewrecords: true,
        height: $(window).height() - 150,
        rowNum: 50,
        rowList: [10, 30, 50, 100, 200],
        rownumbers: true,
        rownumWidth: 25,
        autowidth: true,
        multiselect: true,
        pager: "#jqGridPager",
        jsonReader: {
            root: "page.list",
            page: "page.currPage",
            total: "page.totalPage",
            records: "page.totalCount"
        },
        prmNames: {
            page: "page",
            rows: "limit",
            order: "order"
        },
        gridComplete: function () {
            //隐藏grid底部滚动条
            $("#jqGrid").closest(".ui-jqgrid-bdiv").css({"overflow-x": "hidden"});
        }
    });
});

var vm = new Vue({
    el: '#rrapp',
    data: {
        q: {
            caseId: null,
            fileId: null,
            originName: null
        },
        stressTestFile: {},
        title: null,
        showChart: false,
        showList: true,
        showEdit: false
    },
    methods: {
        query: function () {
            $("#jqGrid").jqGrid('setGridParam', {
                postData: {'caseId': vm.q.caseId, 'fileId': vm.q.fileId, 'originName': vm.q.originName},
                page: 1
            }).trigger("reloadGrid");
        },
        update: function () {
            var fileIds = getSelectedRows();
            if (fileIds == null) {
                return;
            }

            vm.showList = false;
            vm.showChart = false;
            vm.showEdit = true;
            vm.title = "配置";
            if (fileIds.length > 1) {
                vm.stressTestFile.reportStatus = 0;
                vm.stressTestFile.webchartStatus = 0;
                vm.stressTestFile.debugStatus = 0;
                vm.stressTestFile.duration = 3600;
                vm.stressTestFile.fileIdList = fileIds;
            } else {
                var fileId = fileIds[0];
                $.get(baseURL + "test/stressParamFile/info/" + fileId, function (r) {
                    vm.stressTestFile = r.stressTestFile;
                });
            }
        },
        saveOrUpdate: function () {
            var url = (vm.stressTestFile.fileId == null && vm.stressTestFile.fileIdList == null)
                ? "test/stressParamFile/save" : "test/stressParamFile/update";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.stressTestFile),
                success: function (r) {
                    if (r.code === 0) {
                        vm.reload();
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        del: function () {
            var fileIds = getSelectedRows();
            if (fileIds == null) {
                return;
            }

            confirm('确定要删除选中的记录？', function () {
                $.ajax({
                    type: "POST",
                    url: baseURL + "test/stressParamFile/delete",
                    contentType: "application/json",
                    data: JSON.stringify(fileIds),
                    success: function (r) {
                        if (r.code == 0) {
                            alert('操作成功', function () {
                                vm.reload();
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });
            });
        },
        back: function () {
            history.go(-1);
        },
        stopAll: function () {
            confirm('确定要停止所有执行中的脚本？', function () {
                $.ajax({
                    type: "POST",
                    url: baseURL + "test/stressParamFile/stopAll",
                    contentType: "application/json",
                    data: "",
                    success: function (r) {
                        if (r.code == 0) {
                            alert('操作成功', function () {
                                vm.reload();
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });
            });
        },
        stopAllNow: function () {
            var fileIds = getSelectedRows();
            if (fileIds == null) {
                return;
            }
            confirm('确定要立即停止所有选中脚本？', function () {
                $.ajax({
                    type: "POST",
                    url: baseURL + "test/stressParamFile/stopAllNow",
                    contentType: "application/json",
                    data: JSON.stringify(fileIds),
                    success: function (r) {
                        if (r.code == 0) {
                            alert('操作成功', function () {
                                vm.reload();
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });
            });
        },
        reload: function (event) {
            vm.showChart = false;
            vm.showList = true;
            vm.showEdit = false;
            var page = $("#jqGrid").jqGrid('getGridParam', 'page');
            $("#jqGrid").jqGrid('setGridParam', {
                postData: {'caseId': vm.q.caseId, 'fileId': vm.q.fileId, 'originalName': vm.q.originName},
                page: page
            }).trigger("reloadGrid");
            // clearInterval 是自带的函数。
            clearInterval(timeTicket);
        },
        suspendEcharts: function (event) {
            clearInterval(timeTicket);
        },
        startEcharts: function (event) {
            startInterval(fileIdData);
        },
        clearEcharts: function (event) {
            clearEcharts();
        }
    }
});

function synchronizeFile(fileIds) {
    if (!fileIds) {
        return;
    }
    confirm('确定向所有"启用的"分布式节点机推送该文件？文件越大同步时间越长', function () {
        $.ajax({
            type: "POST",
            url: baseURL + "test/stressParamFile/synchronizeFile",
            contentType: "application/json",
            data: JSON.stringify(numberToArray(fileIds)),
            success: function (r) {
                if (r.code == 0) {
                    vm.reload();
                    alert('操作成功', function () {
                    });
                } else {
                    alert(r.msg);
                }
            }
        });
    });
}

var timeTicket;
var responseTimeDataObj = {};
var responseTimeLegendData = [];
var throughputDataObj = {};
var throughputLegendData = [];
var networkSentDataObj = {};
var networkSentLegendData = [];
var networkReceiveDataObj = {};
var networkReceiveLegendData = [];
var successPercentageDataObj = {};
var successPercentageLegendData = [];
var errorPercentageDataObj = {};
var errorPercentageLegendData = [];
var threadCountsDataObj = {};
var threadCountsLegendData = [];
var totalCountsDataObj = {};
var totalCountsLegendData = [];
var xAxisData = [];
var fileIdData;

function ShowRunning(fileId) {
    vm.showChart = true;
    vm.showEdit = false;
    vm.showList = false;
    startInterval(fileId);
}

function clearEcharts() {
    responseTimeDataObj = {};
    responseTimeLegendData = [];
    throughputDataObj = {};
    throughputLegendData = [];
    networkSentDataObj = {};
    networkSentLegendData = [];
    networkReceiveDataObj = {};
    networkReceiveLegendData = [];
    successPercentageDataObj = {};
    successPercentageLegendData = [];
    errorPercentageDataObj = {};
    errorPercentageLegendData = [];
    threadCountsDataObj = {};
    threadCountsLegendData = [];
    totalCountsDataObj = {};
    totalCountsLegendData = [];
    xAxisData = [];

    // 清空数据
    responseTimesEChart.setOption(optionLine, true);
    throughputEChart.setOption(optionLine, true);
    networkSentEChart.setOption(optionLine, true);
    networkReceivedEChart.setOption(optionLine, true);
    successPercentageEChart.setOption(optionLine, true);
    errorPercentageEChart.setOption(optionLine, true);
    threadCountsEChart.setOption(optionLine, true);
    totalCountsEChart.setOption(optionPie, true);
}

setEChartSize();
var responseTimesEChart = echarts.init(document.getElementById('responseTimesChart'), 'shine');
var throughputEChart = echarts.init(document.getElementById('throughputChart'), 'shine');
var networkSentEChart = echarts.init(document.getElementById('networkSentChart'), 'shine');
var networkReceivedEChart = echarts.init(document.getElementById('networkReceivedChart'), 'shine');
var successPercentageEChart = echarts.init(document.getElementById('successPercentageChart'), 'shine');
var errorPercentageEChart = echarts.init(document.getElementById('errorPercentageChart'), 'shine');
var threadCountsEChart = echarts.init(document.getElementById('threadCountsChart'), 'shine');
var totalCountsEChart = echarts.init(document.getElementById('totalCountsChart'), 'shine');

//用于使chart自适应高度和宽度
window.onresize = function () {
    setEChartSize();
    responseTimesEChart.resize();
    throughputEChart.resize();
    networkSentEChart.resize();
    networkReceivedEChart.resize();
    successPercentageEChart.resize();
    errorPercentageEChart.resize();
    threadCountsEChart.resize();
    totalCountsEChart.resize();
};

function setEChartSize() {
    //重置容器高宽
    $("#responseTimesChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
    $("#throughputChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
    $("#networkSentChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
    $("#networkReceivedChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
    $("#successPercentageChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
    $("#errorPercentageChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
    $("#threadCountsChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
    $("#totalCountsChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
}

// 使用刚指定的配置项和数据显示图表。
responseTimesEChart.setOption(optionLine);
throughputEChart.setOption(optionLine);
networkSentEChart.setOption(optionLine);
networkReceivedEChart.setOption(optionLine);
successPercentageEChart.setOption(optionLine);
errorPercentageEChart.setOption(optionLine);
threadCountsEChart.setOption(optionLine);
totalCountsEChart.setOption(optionPie);