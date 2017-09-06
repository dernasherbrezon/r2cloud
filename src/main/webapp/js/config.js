jQuery(document).ready(function($) {
	$("#detect").click(function() {
		if (navigator.geolocation) {
			navigator.geolocation.getCurrentPosition(setupPosition);
		}
	});

	if ($("#sslMessages").length > 0) {
		function appendMessage() {
			$.ajax({
				// -1 for top level "Log" message
				url : "/admin/config/ssl/log.json?index=" + ($("#sslMessages li").length - 1),
				success : function(data, textStatus, xhr) {
					if (data.length > 0) {
						$("#sslMessages li:last").find("i").removeClass("fa-circle-o-notch").removeClass("fa-spin");
						for( var i in data ) {
							$("#sslMessages").append("<li><i class=\"fa-li fa\"></i>" + data[i] + "</li>");
						}
						if (xhr.status == 206) {
							$("#sslMessages li:last").find("i").addClass("fa-circle-o-notch").addClass("fa-spin");
						}
					}
					// not finished yet
					if (xhr.status == 206) {
						setTimeout(appendMessage, 2000);
					}
				}
			});
			 
		}
		setTimeout(appendMessage, 2000);
	}

	$("#ddnsType").change(function() {
		$(".js-type-config").hide();
		$("#" + $("option:selected", this).val()).show();
	});

	function setupPosition(position) {
		$("input[name='lat']").val(position.coords.latitude);
		$("input[name='lon']").val(position.coords.longitude);
	}
});