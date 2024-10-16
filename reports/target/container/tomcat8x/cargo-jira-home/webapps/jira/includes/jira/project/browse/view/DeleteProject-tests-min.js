var _typeof="function"==typeof Symbol&&"symbol"==typeof Symbol.iterator?function(e){return typeof e}:function(e){return e&&"function"==typeof Symbol&&e.constructor===Symbol&&e!==Symbol.prototype?"symbol":typeof e};AJS.test.require("jira.webresources:delete-project-progress",function(){var e=require("jquery"),t=require("jira/project/browse/deleteproject"),r=function(e,t){return"undefined"!==_typeof(e.attr(t))&&!1!==e.attr(t)};module("DeleteProject",{setup:function(){e("#qunit-fixture").append('<div id="delete-project-progress" class="aui-progress-indicator"><span class="aui-progress-indicator-value"></span><span id="delete-project-progress-value"></span></div>')}}),test("Should convert percentage to decimal",function(){var r=e("#delete-project-progress-value"),o=e("#delete-project-progress");r.attr("data-value",20),t(),ok("0.2"===o.attr("data-value"),"should convert 20 to 0.2"),r.attr("data-value",0),t(),ok("0"===o.attr("data-value"),"should convert 0 to 0")}),test("Should set progress bar to indeterminate if no percentage given",function(){var o=e("#delete-project-progress");t(),ok(!r(o,"data-value"),"should remove data-value attribute")}),test("Should set progress bar to indeterminate if percentage is not a number",function(){var o=e("#delete-project-progress-value"),a=e("#delete-project-progress");o.attr("data-value","not a number"),t(),ok(!r(a,"data-value"),"should remove data-value attribute")})});