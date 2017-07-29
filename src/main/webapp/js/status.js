jQuery(document).ready(function($) {
	var rrd4j = $(".rrdgraph").rrd4j({
	    legend: {
	        show: false
	    },
	    yaxis: {
	    	min: 0, 
	    	mode: "byte" 
	    },
	    tooltip: {
	    	show: true,
	    	content: function() {
	    		return "%y";
	    	}
	    }
	});
	$("#updateInterval").change(function(){
		var option = $("option:selected", $(this)).val(), start = new Date();
		if( option == "DAY" ) {
			start.setDate(start.getDate() - 1);
		} else if( option == "MONTH" ) {
			start.setDate(start.getDate() - 31);
		} else if( option == "YEAR" ) {
			start.setDate(start.getDate() - 360);
		}
		rrd4j.updateInterval(start.getTime(), Date.now());
	});
});