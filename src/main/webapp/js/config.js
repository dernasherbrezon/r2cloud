jQuery(document).ready(function($) {
	$("#detect").click(function(){
		if (navigator.geolocation) {
	        navigator.geolocation.getCurrentPosition(setupPosition);
	    }
	});
	
	$("#ddnsType").change(function(){
		$(".js-type-config").hide();
		$("#" + $("option:selected",this).val()).show();
	});
	
	function setupPosition(position) {
		$("input[name='lat']").val(position.coords.latitude);
		$("input[name='lon']").val(position.coords.longitude);
	}
});