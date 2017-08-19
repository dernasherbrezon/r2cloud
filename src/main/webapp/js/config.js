jQuery(document).ready(function($) {
	$("#detect").click(function(){
		if (navigator.geolocation) {
	        navigator.geolocation.getCurrentPosition(setupPosition);
	    }
	});
	function setupPosition(position) {
		$("input[name='lat']").val(position.coords.latitude);
		$("input[name='lon']").val(position.coords.longitude);
	}
});