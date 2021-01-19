function getUrlParam(name) { //a标签跳转获取参数
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return (r[2]);
    return null;
}

var vm = new Vue({
    el: '#csvapp',
    data: {
        CSVDataFile: [],
        loading: false,
        visible: false,
        queryParam: {
            caseId: null,
            fileId: null,
            originName: null,
            page: 1,
            limit: 10
        },
    },
    mounted() {
        this.query();
    },
    methods: {
        toback: function () {
            window.location.href = 'stressTestParamFile.html';
        },
        tosynSingleFile: function (fileId) {
            window.location.href = 'stressSynchronizeFile.html?id=' + fileId;
        },
        query: function () {
            console.log(JSON.stringify(self.CSVDataFile));
            $.ajax({
                type: "POST",
                url: baseURL + "test/stressParamFile/list",
                contentType: "application/json",
                data: JSON.stringify(this.queryParam),
                success: function (r) {
                    if (r.code ==  0) {
                        vm.CSVDataFile = r.page.list;
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        deleteMasterFile: function (fileId) {
            $.ajax({
                type: "POST",
                url: baseURL + "test/stressParamFile/deleteMasterFile",
                contentType: "application/json",
                data: JSON.stringify(fileId),
                success: function (r) {
                    if (r.code == 0) {
                        vm.toback();
                    } else {
                        vm.loading = false;
                        alert(r.msg);
                    }
                }
            });
        },
        deleteSlaveFile: function (fileId, slaveId) {
            $.ajax({
                type: "POST",
                url: baseURL + "test/stressParamFile/deleteSlaveFile",
                contentType: "application/json",
                data: JSON.stringify({"fileId":fileId,"slaveId":slaveId}),
                success: function (r) {
                    if (r.code == 0) {
                        vm.toback();
                    } else {
                        vm.loading = false;
                        alert(r.msg);
                    }
                }
            });
        },
        synchronizeFile: function (fileId) {
            if (!fileId) {
                return;
            }
            vm.loading = true;
            $.ajax({
                type: "POST",
                url: baseURL + "test/stressParamFile/synchronizeFile",
                contentType: "application/json",
                data: JSON.stringify(numberToArray(fileId)),
                success: function (r) {
                    if (r.code == 0) {
                        vm.toback();
                    } else {
                        vm.loading = false;
                        alert(r.msg);
                    }
                }
            });
        },
        open: function (name, fileId) {
            this.$prompt('请修改文件名（例：token.log）', '提示', {
                inputValue: name,
                confirmButtonText: '确定',
                cancelButtonText: '取消'
            }).then(({
                         value
                     }) => {
                type: 'success',
                    this.updateSlaveFileName(fileId,value);
            }).catch(() => {
                this.$message({
                    type: 'info',
                    message: '取消输入'
                });
            });
        },
        updateSlaveFileName: function (fileId,realname) {
            $.ajax({
                type: "POST",
                url: baseURL + "test/stressParamFile/updateSlaveFileName",
                dataType: "json",
                contentType: "application/json",
                data: JSON.stringify({
                    fileId,
                    realname
                }),
                success: function (r) {
                    if (r.code == 0) {
                        vm.toback();
                    } else {
                        vm.loading = false;
                        alert(r.msg);
                    }
                }
            });
        },
        downloadFile: function (fileId) {
            top.location.href = baseURL + "test/stressFile/downloadFile/" + fileId;
        }
    }
});