jQuery(document).ready(function($) {
	var bytesGraphs = $(".rrdgraph-BYTES").rrd4j({
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
	}), normalGraphs = $(".rrdgraph-NORMAL").rrd4j({
	    legend: {
	        show: false
	    },
	    yaxis: {
	    	min: 0, 
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
		bytesGraphs.updateInterval(start.getTime(), Date.now());
		normalGraphs.updateInterval(start.getTime(), Date.now());
	});
});