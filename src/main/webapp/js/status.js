jQuery(document).ready(function($) {
	
	var loadedMetrics = false;

	$(document).on('shown.bs.tab', '[href="#metrics"]', function (e) {
		if( loadedMetrics ) {
			return;
		}
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
		
		loadedMetrics = true;
	});
	
    var dashboard = document.getElementById("dashboard");
    dashboard.addEventListener("load",function(){
    	update();
    }, false);
    
    function update() {
		$.ajax({
			url : "/admin/status/data.json",
			headers: {          
			    Accept: "application/json; charset=utf-8"         
			},			
			success : function(data) {
				for (var property in data) {
				    if (!data.hasOwnProperty(property)) {
				    	continue;
				    }
				    
				    var value = data[property], color;
				    if( value.status == "SUCCESS" ) {
				    	color = "#3c763d";
				    } else if( value.status == "ERROR" ) {
				    	color = "#a94442";
				    } else {
				    	color = "#777";
				    }
				    
				    var elem = dashboard.contentDocument.getElementById(property); 
				    if( elem != null ) {
				    	elem.style.fill=color;
				    	if( value.status == "ERROR" ) {
				    		elem.innerHTML = "<title>" + value.message + "</title>"; 
				    	} else {
				    		elem.innerHTML = "";
				    	}
				    }
				}
			},
			complete : function() {
				setTimeout(update, 5000);
			}
		});
	}
    

	$(document).ajaxError(function(event, jqXHR, ajaxSettings, thrownError) {
		if (jqXHR.status == 401) {
			location.href = "/";
		}
	});
	
});