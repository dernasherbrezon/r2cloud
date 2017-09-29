jQuery(document).ready(function($) {
	$("#satelliteId").change(function(){
		var option = $("option:selected", $(this)).val();
		$(".js-satellite-tle").hide();
		$("#" + option).show();
	});	
});