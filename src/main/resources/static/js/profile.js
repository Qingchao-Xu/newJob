$(function(){
	$(".follow-btn").click(follow);
});

function follow() {
	var btn = this;
	if($(btn).hasClass("btn-info")) {
		// 关注TA
		$.post(
			CONTEXT_PATH + "/follow",
			{"entityType":3, "entityId":$(btn).prev().val()},
			function (data) {
				data = $.parseJSON(data);
				if (data.code == 0) {
					// $(btn).text("已关注").removeClass("btn-info").addClass("btn-secondary");
					window.location.reload();
				} else {
					alert(data.msg);
				}
			}
		);

	} else {
		// 取消关注
		$.post(
			CONTEXT_PATH + "/unfollow",
			{"entityType":3, "entityId":$(btn).prev().val()},
			function (data) {
				data = $.parseJSON(data);
				if (data.code == 0) {
					// $(btn).text("关注TA").removeClass("btn-secondary").addClass("btn-info");
					window.location.reload();
				} else {
					alert(data.msg);
				}
			}
		);
	}
}