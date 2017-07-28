jQuery(document).ready(function($) {
	var rrd4j = $(".rrdgraph").rrd4j({
	    legend: {
	        show: false
	    },
	    yaxis: {
	    	min: 0, 
	    	mode: "byte" 
	    }
	});
	$("#updateInterval").click(function(){
		rrd4j.updateInterval(1275346800000, 1277938800000);
	});
});