jQuery(document).ready(function($) {
	$("#detect").click(function(){
		if (navigator.geolocation) {
	        navigator.geolocation.getCurrentPosition(setupPosition);
	    }
	});
	
	if( $("#sslMessages").length > 0 ) {
//		$.ajax({
//			url : "/admin/adsb/data.json",
//			success : function(data) {
//				
//			}
//		});
		var index = 0;
		function appendMessage(){
			$("#sslMessages li:last").find("i").removeClass("fa-circle-o-notch").removeClass("fa-spin");
			$("#sslMessages").append("<li><i class=\"fa-li fa fa-circle-o-notch fa-spin\"></i>Test " + index + "</li>");
			index++;
			if( index < 15 ) {
				setTimeout(appendMessage, 2000);
			}
		}
		setTimeout(appendMessage, 2000);
	}
	
	$("#ddnsType").change(function(){
		$(".js-type-config").hide();
		$("#" + $("option:selected",this).val()).show();
	});
	
	function setupPosition(position) {
		$("input[name='lat']").val(position.coords.latitude);
		$("input[name='lon']").val(position.coords.longitude);
	}
});