require(['jquery', 'jira/admin/group-browser/group-label-lozenge'], function ($) {
    $(function () {
        $(".operations-list .aui-button[disabled]").tooltip({ gravity: 'e', html: false });
    });
});