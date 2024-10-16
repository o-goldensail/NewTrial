define("jira/project/browse/app",["require"],function(e){"use strict";var t=e("underscore"),i=e("jquery"),o=e("backbone"),r=e("marionette"),a=e("jira/project/browse/layout"),s=e("jira/project/browse/tabsview"),c=e("jira/project/browse/projecttypestabsview"),n=e("jira/project/browse/projectcollection"),l=e("jira/project/browse/projectcollectionview"),p=e("jira/project/browse/paginationview"),g=e("jira/project/browse/filtermodel"),h=e("jira/project/browse/filterview"),j=e("jira/util/navigation"),w=e("jira/project/browse/categorycollection"),y=e("jira/project/browse/projecttypecollection"),u=e("jira/util/users/logged-in-user"),v=e("jira/util/data/meta"),b=new r.Application;return b.on("start",function(e){var r=e.container;this.categories=new w(e.categories),this.availableProjectTypes=new y(e.availableProjectTypes),this.projects=new n(e.projects,{mode:"client",state:{pageSize:25,currentPage:1},categories:this.categories}),this.categories.selectCategory(e.selectedCategory),e.availableProjectTypes?(this.availableProjectTypes.selectProjectType(e.selectedProjectType)||this.availableProjectTypes.selectProjectType("all"),this.filter=new g({category:this.categories.getSelected().toJSON(),projectType:this.availableProjectTypes.getSelected().toJSON()},{pageableCollection:this.projects})):this.filter=new g({category:this.categories.getSelected().toJSON()},{pageableCollection:this.projects}),this.layout=new a({el:r}),this.projectCollectionView=new l({model:this.filter,collection:this.projects});var d=function(e){var t=b.categories.getSelected().id,i=b.categories.selectCategory(e);i&&(b.filter.changeCategory(i),"archived"!==e&&"archived"!==t||b.projectCollectionView.render())};v.get("in-admin-mode")&&i(this.layout.sidebar.el).addClass("hidden"),""!==u.username()||this.categories.length>1?(this.tabsView=new s({collection:this.categories}),this.tabsView.on("category-change",d),this.layout.categoryNav.show(this.tabsView)):this.layout.$el.find(this.layout.categoryNav.el).addClass("hidden");var f=function(e){var t=b.availableProjectTypes.selectProjectType(e);t&&b.filter.changeProjectType(t)};if(this.availableProjectTypes.length>0?(this.projectTypesTabsView=new c({collection:this.availableProjectTypes}),this.projectTypesTabsView.on("project-type-change",f),this.layout.projectTypeNav.show(this.projectTypesTabsView)):this.layout.$el.find(".project-type-nav").addClass("hidden"),this.projectCollectionView.on("project-view.click-category",function(e){d(e.attributes.projectCategoryId)}),this.layout.content.show(this.projectCollectionView),this.layout.pagination.show(new p({collection:this.projects,model:this.filter})),this.layout.filter.show(new h({model:this.filter})),this.layout.pagination.currentView.on("navigate",function(e){var i=t.extend(b.filter.getQueryParams(!1),{page:e});j.navigate(i)}),this.filter.on("filter",function(e){j.navigate(e)}),(new(o.Router.extend({initialize:function(){this.route(/(.*)/,"every")}}))).on("route:every",function(){var e=+j.getParam("page",!0)||1,t=j.getParam("contains",!0)||"",i=j.getParam("selectedCategory")||"",o=b.categories.selectCategory(i),r=j.getParam("selectedProjectType")||"",a=b.availableProjectTypes.selectProjectType(r);b.filter.set("contains",t,{silent:!0}),!1!==o&&b.filter.set("category",o.toJSON(),{silent:!0}),!1!==a&&b.filter.set("projectType",a.toJSON(),{silent:!0}),b.filter.filterCollection(),b.projects.getPage(e),b.layout.filter.currentView.render()}),o.History.started||o.history.start({pushState:j.pushStateSupported,root:j.getBackboneHistoryRoot()}),window.location.hash){var T=window.location.hash.substring(1);this.categories.get(T)&&d(T)}this.listenTo(this.projectCollectionView,{"project-view.click-project-name":function(e,t){this.trigger("browse-projects.project-view.click-project-name",e,t)},"project-view.click-lead-user":function(e,t){this.trigger("browse-projects.project-view.click-lead-user",e,t)},"project-view.click-category":function(e,t){this.trigger("browse-projects.project-view.click-category",e,t)},"project-view.click-url":function(e,t){this.trigger("browse-projects.project-view.click-url",e,t)}}),void 0!==this.projectTypesTabsView&&this.listenTo(this.projectTypesTabsView,{"project-type-change":function(e){this.trigger("browse-projects.project-type-change",e)}}),void 0!==this.tabsView&&this.listenTo(this.tabsView,{"category-change":function(e){this.trigger("browse-projects.category-change",e)}}),this.listenTo(this.filter,{filter:function(){var e=b.projects.length;this.trigger("browse-projects.projects-render",e)}}),this.listenTo(this.layout.pagination.currentView,{navigate:function(e){this.trigger("browse-projects.navigate-to-page",e)},"navigate-previous":function(){this.trigger("browse-projects.navigate-to-previous")},"navigate-next":function(){this.trigger("browse-projects.navigate-to-next")}})}),b});