!function(){var e=require("jira/ajs/keyboardshortcut/keyboard-shortcut"),r=require("jira/ajs/keyboardshortcut/keyboard-shortcut-toggle"),t=require("jira/util/data/meta"),u=require("jira/dialog/dialog"),o=require("aui/dropdown"),a=require("aui/popup"),i=require("jquery");e.addIgnoreCondition(function(){return a.current||o.current||u.current||r.areKeyboardShortcutsDisabled()}),i(function(){t.get("keyboard-shortcuts-enabled")?r.enable():r.disable(),AJS.keys&&(AJS.activeShortcuts=e.fromJSON(AJS.keys.shortcuts),i(document).bind("aui:keyup",function(e){var r,t;"Esc"===e.key&&(r=i(e.target),r.is(":input:not(button[type='button'])")&&(t=new i.Event("beforeBlurInput"),r.trigger(t,[{reason:"escPressed"}]),t.isDefaultPrevented()||r.blur()))}))})}();